package com.josuetorres.pillowdrop.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.josuetorres.pillowdrop.data.diagnostics.DiagnosticLogStore
import com.josuetorres.pillowdrop.data.diagnostics.LogLevel
import com.josuetorres.pillowdrop.data.preferences.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ShutdownAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_WARNING = "com.josuetorres.pillowdrop.ACTION_WARNING"
        const val ACTION_EXECUTE = "com.josuetorres.pillowdrop.ACTION_EXECUTE"
        const val ACTION_SNOOZE_30M = "com.josuetorres.pillowdrop.ACTION_SNOOZE_30M"
        const val ACTION_CANCEL = "com.josuetorres.pillowdrop.ACTION_CANCEL"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PillowDrop:AlarmReceiverWakeLock"
        ).apply {
            acquire(10_000L)
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repo = SettingsRepository(appContext)
                val scheduler = ShutdownScheduler(appContext, repo)

                when (action) {
                    ACTION_WARNING -> {
                        val timer = repo.getActiveTimerSnapshot()
                        val settings = repo.getSettingsSnapshot()
                        if (timer.isActive) {
                            DiagnosticLogStore.log(
                                tag = "Alarm",
                                message = "Aviso de 2 minutos activado.",
                                level = LogLevel.INFO
                            )
                            if (settings.warningVibrationEnabled) {
                                NotificationHelper.triggerGentleWarningVibration(appContext)
                            }
                            NotificationHelper.showActiveTimerNotification(
                                context = appContext,
                                triggerEpochMillis = timer.triggerEpochMillis,
                                isTwoMinuteWarning = true
                            )
                        }
                    }

                    ACTION_EXECUTE -> {
                        val coordinator = ShutdownCoordinator(appContext, repo)
                        coordinator.executeRoutine("Alarma del temporizador cumplida")
                    }

                    ACTION_SNOOZE_30M -> {
                        scheduler.snoozeThirtyMinutes()
                    }

                    ACTION_CANCEL -> {
                        scheduler.cancelTimer("Cancelado desde la notificación")
                    }
                }
            } catch (t: Throwable) {
                DiagnosticLogStore.log(
                    tag = "Alarm",
                    message = "Error en receptor ($action): ${t.message}",
                    level = LogLevel.ERROR
                )
            } finally {
                if (wakeLock.isHeld) {
                    runCatching { wakeLock.release() }
                }
                pendingResult.finish()
            }
        }
    }
}
