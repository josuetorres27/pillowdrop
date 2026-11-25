package com.josuetorres.pillowdrop.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.josuetorres.pillowdrop.data.apps.InstalledAppItem
import com.josuetorres.pillowdrop.data.apps.InstalledAppsRepository
import com.josuetorres.pillowdrop.data.diagnostics.DiagnosticEntry
import com.josuetorres.pillowdrop.data.diagnostics.DiagnosticLogStore
import com.josuetorres.pillowdrop.data.preferences.SettingsRepository
import com.josuetorres.pillowdrop.domain.action.ActiveCallSnapshot
import com.josuetorres.pillowdrop.domain.action.CallEndResult
import com.josuetorres.pillowdrop.domain.model.ActiveTimerState
import com.josuetorres.pillowdrop.domain.model.ShutdownSettings
import com.josuetorres.pillowdrop.domain.model.TimerMode
import com.josuetorres.pillowdrop.service.BackgroundAppCloser
import com.josuetorres.pillowdrop.service.PermissionChecker
import com.josuetorres.pillowdrop.service.PermissionStatusSnapshot
import com.josuetorres.pillowdrop.service.ShutdownScheduler
import com.josuetorres.pillowdrop.service.WhatsAppCallHandler
import com.josuetorres.pillowdrop.service.WhatsAppCallRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PillowDropUiState(
    val settings: ShutdownSettings = ShutdownSettings(),
    val activeTimer: ActiveTimerState = ActiveTimerState(),
    val remainingMillis: Long = 0L,
    val activeCall: ActiveCallSnapshot? = null,
    val listenerConnected: Boolean = false,
    val permissions: PermissionStatusSnapshot = PermissionStatusSnapshot(),
    val installedApps: List<InstalledAppItem> = emptyList(),
    val logs: List<DiagnosticEntry> = emptyList(),
    val bannerFeedback: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val settingsRepository = SettingsRepository(appContext)
    private val installedAppsRepository = InstalledAppsRepository(appContext)
    private val scheduler = ShutdownScheduler(appContext, settingsRepository)
    private val whatsAppCallHandler = WhatsAppCallHandler(appContext)
    private val backgroundAppCloser = BackgroundAppCloser(appContext, installedAppsRepository)

    private val permissionsFlow = MutableStateFlow(PermissionChecker.inspect(appContext))
    private val installedAppsFlow = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    private val nowMillisFlow = MutableStateFlow(System.currentTimeMillis())
    private val bannerFeedbackFlow = MutableStateFlow<String?>(null)

    private val coreFlow = combine(
        settingsRepository.settingsFlow,
        settingsRepository.activeTimerFlow,
        nowMillisFlow,
        WhatsAppCallRegistry.activeCallFlow,
        WhatsAppCallRegistry.listenerConnected
    ) { settings, timer, nowMs, call, connected ->
        val remaining = if (timer.isActive) {
            (timer.triggerEpochMillis - nowMs).coerceAtLeast(0L)
        } else {
            0L
        }
        CoreTuple(settings, timer, remaining, call, connected)
    }

    val uiState: StateFlow<PillowDropUiState> = combine(
        coreFlow,
        permissionsFlow,
        installedAppsFlow,
        DiagnosticLogStore.entries,
        bannerFeedbackFlow
    ) { core, perms, apps, logs, feedback ->
        PillowDropUiState(
            settings = core.settings,
            activeTimer = core.timer,
            remainingMillis = core.remaining,
            activeCall = core.call,
            listenerConnected = core.connected,
            permissions = perms,
            installedApps = apps,
            logs = logs,
            bannerFeedback = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = PillowDropUiState(permissions = PermissionChecker.inspect(appContext))
    )

    private data class CoreTuple(
        val settings: ShutdownSettings,
        val timer: ActiveTimerState,
        val remaining: Long,
        val call: ActiveCallSnapshot?,
        val connected: Boolean
    )

    init {
        refreshPermissions()
        loadInstalledApps()
        startTicker()
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (true) {
                nowMillisFlow.value = System.currentTimeMillis()
                delay(1_000L)
            }
        }
    }

    fun refreshPermissions() {
        permissionsFlow.value = PermissionChecker.inspect(appContext)
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            installedAppsFlow.value = installedAppsRepository.getLaunchableApps()
        }
    }

    fun clearBanner() {
        bannerFeedbackFlow.value = null
    }

    private fun showBanner(message: String) {
        bannerFeedbackFlow.value = message
    }

    fun startQuickTimerMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(timerMode = TimerMode.DURATION, defaultDurationMinutes = minutes)
            }
            scheduler.scheduleCountdownSeconds(
                durationSeconds = minutes * 60L,
                autoStartedFromCall = false,
                label = "Manual ${minutes}m"
            )
            showBanner("Temporizador iniciado: ${minutes} min.")
        }
    }

    fun startSixtySecondLockTest() {
        viewModelScope.launch {
            scheduler.scheduleCountdownSeconds(
                durationSeconds = 60L,
                autoStartedFromCall = false,
                label = "Prueba 60s (bloquea la pantalla)"
            )
            showBanner("Prueba de 60s activa. ¡Bloquea tu S25 ahora!")
        }
    }

    fun startTargetClockTimer() {
        viewModelScope.launch {
            val s = settingsRepository.getSettingsSnapshot()
            settingsRepository.updateSettings { it.copy(timerMode = TimerMode.TARGET_TIME) }
            scheduler.scheduleTargetClockTime(
                targetHour = s.targetHour,
                targetMinute = s.targetMinute,
                autoStartedFromCall = false
            )
            showBanner("Programado para las %02d:%02d.".format(s.targetHour, s.targetMinute))
        }
    }

    fun snoozeThirtyMinutes() {
        viewModelScope.launch {
            scheduler.snoozeThirtyMinutes()
            showBanner("Se añadieron +30 min al temporizador.")
        }
    }

    fun extendFiveMinutes() {
        viewModelScope.launch {
            scheduler.extendMinutes(5)
            showBanner("Se añadieron +5 min al temporizador.")
        }
    }

    fun cancelActiveTimer() {
        viewModelScope.launch {
            scheduler.cancelTimer("Cancelado desde la app")
            showBanner("Temporizador cancelado.")
        }
    }

    fun updateSettings(transform: (ShutdownSettings) -> ShutdownSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(transform)
        }
    }

    fun toggleSelectedApp(packageName: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { current ->
                val updated = current.selectedPackagesToClose.toMutableSet()
                if (!updated.add(packageName)) {
                    updated.remove(packageName)
                }
                current.copy(selectedPackagesToClose = updated)
            }
        }
    }

    fun hangUpCallNow() {
        viewModelScope.launch {
            val feedback = when (val res = whatsAppCallHandler.endActiveCall("Botón manual Spike Lab")) {
                is CallEndResult.Ended -> "Colgado enviado a '${res.callerTitle}' (${res.actionLabelUsed})."
                CallEndResult.NoActiveCall -> "No se detecta ninguna llamada activa de WhatsApp."
                is CallEndResult.MissingHangUpAction -> "Llamada detectada, pero sin botón reconocido: ${res.availableActions}"
                is CallEndResult.Failed -> "Error al colgar: ${res.errorMessage}"
            }
            showBanner(feedback)
        }
    }

    fun runSpike2KillAppsNow() {
        viewModelScope.launch {
            val count = backgroundAppCloser.runSpike2KillAllNonProtected()
            showBanner("Spike 2 ejecutado: $count apps en 2do plano solicitadas para cierre.")
        }
    }

    fun clearLogs() {
        DiagnosticLogStore.clear()
    }
}
