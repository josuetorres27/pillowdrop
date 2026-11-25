package com.josuetorres.pillowdrop.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.josuetorres.pillowdrop.data.diagnostics.DiagnosticLogStore
import com.josuetorres.pillowdrop.data.diagnostics.LogLevel
import com.josuetorres.pillowdrop.data.preferences.SettingsRepository
import com.josuetorres.pillowdrop.domain.model.ActiveTimerState
import com.josuetorres.pillowdrop.domain.util.TimeWindowCalculator
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class ShutdownScheduler(
    private val context: Context,
    private val settingsRepository: SettingsRepository = SettingsRepository(context)
) {

    companion object {
        private const val REQ_WARNING = 2001
        private const val REQ_EXECUTE = 2002
        private const val TWO_MINUTES_MS = 2 * 60 * 1000L
        private const val SNOOZE_30_MIN_MS = 30 * 60 * 1000L
    }

    suspend fun scheduleCountdownSeconds(
        durationSeconds: Long,
        autoStartedFromCall: Boolean,
        label: String
    ) {
        val now = System.currentTimeMillis()
        val triggerAt = now + (durationSeconds.coerceAtLeast(5L) * 1000L)
        scheduleExactTimestamp(
            triggerEpochMillis = triggerAt,
            autoStartedFromCall = autoStartedFromCall,
            label = label
        )
    }

    suspend fun scheduleTargetClockTime(
        targetHour: Int,
        targetMinute: Int,
        autoStartedFromCall: Boolean
    ) {
        val triggerAt = TimeWindowCalculator.computeNextTargetEpochMillis(
            now = ZonedDateTime.now(),
            targetHour = targetHour,
            targetMinute = targetMinute
        )
        val formatted = TimeWindowCalculator.formatHourMinute(targetHour, targetMinute)
        scheduleExactTimestamp(
            triggerEpochMillis = triggerAt,
            autoStartedFromCall = autoStartedFromCall,
            label = "Hora fija $formatted"
        )
    }

    suspend fun scheduleExactTimestamp(
        triggerEpochMillis: Long,
        autoStartedFromCall: Boolean,
        label: String
    ) {
        val now = System.currentTimeMillis()
        val remainingMs = triggerEpochMillis - now
        val warningAt = if (remainingMs > TWO_MINUTES_MS + 15_000L) {
            triggerEpochMillis - TWO_MINUTES_MS
        } else {
            0L
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelPendingAlarmsOnly(alarmManager)

        if (warningAt > now) {
            setIdleSafeAlarm(alarmManager, warningAt, buildWarningPendingIntent())
        }
        setIdleSafeAlarm(alarmManager, triggerEpochMillis, buildExecutePendingIntent())

        val state = ActiveTimerState(
            isActive = true,
            triggerEpochMillis = triggerEpochMillis,
            warningEpochMillis = warningAt,
            startedEpochMillis = now,
            autoStartedFromCall = autoStartedFromCall,
            label = label
        )
        settingsRepository.setActiveTimer(state)
        NotificationHelper.showActiveTimerNotification(
            context = context,
            triggerEpochMillis = triggerEpochMillis,
            isTwoMinuteWarning = false
        )

        val triggerTimeText = Instant.ofEpochMilli(triggerEpochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .withNano(0)
        DiagnosticLogStore.log(
            tag = "Scheduler",
            message = "Temporizador armado ($label) -> se ejecutará a las $triggerTimeText (exactAlarms=${canScheduleExactAlarms(context)}).",
            level = LogLevel.INFO
        )
    }

    suspend fun snoozeThirtyMinutes() {
        val current = settingsRepository.getActiveTimerSnapshot()
        val base = if (current.isActive && current.triggerEpochMillis > System.currentTimeMillis()) {
            current.triggerEpochMillis
        } else {
            System.currentTimeMillis()
        }
        scheduleExactTimestamp(
            triggerEpochMillis = base + SNOOZE_30_MIN_MS,
            autoStartedFromCall = current.autoStartedFromCall,
            label = "+30 min snooze"
        )
    }

    suspend fun extendMinutes(minutes: Int) {
        val current = settingsRepository.getActiveTimerSnapshot()
        if (!current.isActive) return
        val newTrigger = current.triggerEpochMillis + (minutes * 60_000L)
        scheduleExactTimestamp(
            triggerEpochMillis = newTrigger,
            autoStartedFromCall = current.autoStartedFromCall,
            label = "+${minutes}m"
        )
    }

    suspend fun cancelTimer(reason: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelPendingAlarmsOnly(alarmManager)
        settingsRepository.clearActiveTimer()
        NotificationHelper.dismissActiveTimerNotification(context)
        DiagnosticLogStore.log("Scheduler", "Temporizador cancelado ($reason).", LogLevel.INFO)
    }

    private fun cancelPendingAlarmsOnly(alarmManager: AlarmManager) {
        alarmManager.cancel(buildWarningPendingIntent())
        alarmManager.cancel(buildExecutePendingIntent())
    }

    private fun setIdleSafeAlarm(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        if (canScheduleExactAlarms(context)) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun buildWarningPendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQ_WARNING,
            Intent(context, ShutdownAlarmReceiver::class.java).apply {
                action = ShutdownAlarmReceiver.ACTION_WARNING
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun buildExecutePendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQ_EXECUTE,
            Intent(context, ShutdownAlarmReceiver::class.java).apply {
                action = ShutdownAlarmReceiver.ACTION_EXECUTE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun canScheduleExactAlarms(ctx: Context = context): Boolean {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
