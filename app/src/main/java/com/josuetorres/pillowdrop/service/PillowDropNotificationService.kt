package com.josuetorres.pillowdrop.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.josuetorres.pillowdrop.data.diagnostics.DiagnosticLogStore
import com.josuetorres.pillowdrop.data.diagnostics.LogLevel
import com.josuetorres.pillowdrop.data.preferences.SettingsRepository
import com.josuetorres.pillowdrop.domain.model.TimerMode
import com.josuetorres.pillowdrop.domain.util.ContactNormalizer
import com.josuetorres.pillowdrop.domain.util.TimeWindowCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalTime

class PillowDropNotificationService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var scheduler: ShutdownScheduler

    // Deduplicates WhatsApp's per-second duration updates ("00:01", "00:02"...) in the diagnostic log
    @Volatile
    private var lastLoggedCallSignature: String? = null

    @Volatile
    private var lastBatteryGuardTriggerMs: Long = 0L

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (level < 0 || scale <= 0) return

            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL ||
                plugged != 0

            if (isCharging) return

            val batteryPct = (level * 100) / scale
            val now = System.currentTimeMillis()
            if (now - lastBatteryGuardTriggerMs < 10 * 60_000L) return

            serviceScope.launch {
                val settings = settingsRepository.getSettingsSnapshot()
                if (!settings.batteryGuardEnabled) return@launch
                if (batteryPct > settings.batteryThresholdPercent) return@launch

                val activeCall = WhatsAppCallRegistry.activeCallFlow.value
                val activeTimer = settingsRepository.getActiveTimerSnapshot()
                val inNightWindow = TimeWindowCalculator.isWithinNightWindow(
                    now = LocalTime.now(),
                    start = LocalTime.of(settings.nightStartHour, settings.nightStartMinute),
                    end = LocalTime.of(settings.nightEndHour, settings.nightEndMinute),
                    nightWindowEnabled = settings.nightWindowEnabled,
                    allDayTestMode = settings.allDayTestMode
                )

                if (activeCall != null || activeTimer.isActive || (settings.nukeAppsEnabled && inNightWindow)) {
                    lastBatteryGuardTriggerMs = now
                    DiagnosticLogStore.log(
                        tag = "BatteryGuard",
                        message = "Batería en $batteryPct% (<= ${settings.batteryThresholdPercent}%) sin cargador. Ejecutando rutina.",
                        level = LogLevel.WARN
                    )
                    ShutdownCoordinator(applicationContext, settingsRepository)
                        .executeRoutine("Guardia de batería ($batteryPct%)")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
        scheduler = ShutdownScheduler(applicationContext, settingsRepository)
        runCatching {
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }
    }

    override fun onDestroy() {
        WhatsAppCallRegistry.setListenerConnected(false)
        runCatching { unregisterReceiver(batteryReceiver) }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        WhatsAppCallRegistry.setListenerConnected(true)
        DiagnosticLogStore.log(
            tag = "Listener",
            message = "NotificationListenerService conectado en el dispositivo.",
            level = LogLevel.SUCCESS
        )
        scanExistingNotifications()
    }

    override fun onListenerDisconnected() {
        WhatsAppCallRegistry.setListenerConnected(false)
        DiagnosticLogStore.log(
            tag = "Listener",
            message = "NotificationListenerService desconectado.",
            level = LogLevel.WARN
        )
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || !WhatsAppCallRegistry.isWhatsAppPackage(sbn.packageName)) return
        handleWhatsAppNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null || !WhatsAppCallRegistry.isWhatsAppPackage(sbn.packageName)) return
        val wasActiveCall = WhatsAppCallRegistry.clearIfMatchesKey(sbn.key)
        if (wasActiveCall) {
            lastLoggedCallSignature = null
            DiagnosticLogStore.log(
                tag = "Listener",
                message = "Notificación de llamada de WhatsApp finalizada/removida.",
                level = LogLevel.INFO
            )
            serviceScope.launch {
                val timer = settingsRepository.getActiveTimerSnapshot()
                if (timer.isActive && timer.autoStartedFromCall) {
                    scheduler.cancelTimer("La llamada terminó antes del temporizador")
                }
            }
        }
    }

    private fun scanExistingNotifications() {
        val currentList = runCatching { activeNotifications }.getOrNull() ?: return
        for (sbn in currentList) {
            if (WhatsAppCallRegistry.isWhatsAppPackage(sbn.packageName)) {
                handleWhatsAppNotification(sbn)
            }
        }
    }

    private fun handleWhatsAppNotification(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        val extras = notification.extras
        val title = extras?.getCharSequence( android.app.Notification.EXTRA_TITLE )?.toString().orEmpty()
        val text = extras?.getCharSequence( android.app.Notification.EXTRA_TEXT )?.toString().orEmpty()
        val category = notification.category
        val ongoing = (notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0
        val actions = notification.actions?.map { a ->
            "${a.title ?: "<null>"}(intent=${a.actionIntent != null},sem=${a.semanticAction})"
        }.orEmpty()

        val callSnapshot = WhatsAppCallRegistry.inspectCallCandidate(sbn)
        val signature = "${sbn.key}|$category|$ongoing|$title|$actions"

        if (signature != lastLoggedCallSignature) {
            lastLoggedCallSignature = signature
            DiagnosticLogStore.log(
                tag = if (callSnapshot != null) "WhatsAppCall" else "WhatsAppNotif",
                message = "pkg=${sbn.packageName} | cat=$category | ongoing=$ongoing | title='$title' | text='$text' | actions=$actions",
                level = if (callSnapshot?.hasHangUpAction == true) LogLevel.SUCCESS else LogLevel.INFO
            )
        }

        if (callSnapshot == null) return

        val hadCallBefore = WhatsAppCallRegistry.activeCallFlow.value != null
        WhatsAppCallRegistry.updateActiveCall(sbn, callSnapshot)

        if (!hadCallBefore) {
            maybeAutoArmTimerForCall(callSnapshot.callerTitle)
        }
    }

    private fun maybeAutoArmTimerForCall(callerTitle: String) {
        serviceScope.launch {
            val settings = settingsRepository.getSettingsSnapshot()
            if (!settings.autoStartOnWhitelistedCall || !settings.hangUpWhatsAppEnabled) return@launch

            val existingTimer = settingsRepository.getActiveTimerSnapshot()
            if (existingTimer.isActive) return@launch

            val matches = ContactNormalizer.matchesWhitelist(
                notificationTitle = callerTitle,
                keyword = settings.contactKeyword,
                matchAnyWhatsAppCall = settings.matchAnyWhatsAppCall
            )
            if (!matches) return@launch

            val inWindow = TimeWindowCalculator.isWithinNightWindow(
                now = LocalTime.now(),
                start = LocalTime.of(settings.nightStartHour, settings.nightStartMinute),
                end = LocalTime.of(settings.nightEndHour, settings.nightEndMinute),
                nightWindowEnabled = settings.nightWindowEnabled,
                allDayTestMode = settings.allDayTestMode
            )
            if (!inWindow) {
                DiagnosticLogStore.log(
                    tag = "AutoArm",
                    message = "Llamada con '$callerTitle' detectada fuera del horario nocturno; no se armó temporizador.",
                    level = LogLevel.INFO
                )
                return@launch
            }

            when (settings.timerMode) {
                TimerMode.DURATION -> scheduler.scheduleCountdownSeconds(
                    durationSeconds = settings.defaultDurationMinutes * 60L,
                    autoStartedFromCall = true,
                    label = "Auto ($callerTitle · ${settings.defaultDurationMinutes}m)"
                )
                TimerMode.TARGET_TIME -> scheduler.scheduleTargetClockTime(
                    targetHour = settings.targetHour,
                    targetMinute = settings.targetMinute,
                    autoStartedFromCall = true
                )
            }
        }
    }
}
