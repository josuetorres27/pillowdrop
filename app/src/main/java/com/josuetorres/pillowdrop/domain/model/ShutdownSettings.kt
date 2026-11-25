package com.josuetorres.pillowdrop.domain.model

enum class TimerMode {
    DURATION,
    TARGET_TIME
}

data class ShutdownSettings(
    val contactKeyword: String = "muñequita",
    val matchAnyWhatsAppCall: Boolean = false,
    val timerMode: TimerMode = TimerMode.DURATION,
    val defaultDurationMinutes: Int = 45,
    val targetHour: Int = 3,
    val targetMinute: Int = 0,
    val hangUpWhatsAppEnabled: Boolean = true,
    val autoStartOnWhitelistedCall: Boolean = true,
    val nightWindowEnabled: Boolean = true,
    val nightStartHour: Int = 22,
    val nightStartMinute: Int = 0,
    val nightEndHour: Int = 7,
    val nightEndMinute: Int = 0,
    val allDayTestMode: Boolean = false,
    val batteryGuardEnabled: Boolean = true,
    val batteryThresholdPercent: Int = 15,
    val warningVibrationEnabled: Boolean = true,
    val nukeAppsEnabled: Boolean = false,
    val nukeAllNonProtected: Boolean = false,
    val selectedPackagesToClose: Set<String> = emptySet()
)

data class ActiveTimerState(
    val isActive: Boolean = false,
    val triggerEpochMillis: Long = 0L,
    val warningEpochMillis: Long = 0L,
    val startedEpochMillis: Long = 0L,
    val autoStartedFromCall: Boolean = false,
    val label: String = ""
)
