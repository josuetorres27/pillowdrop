package com.josuetorres.pillowdrop.domain.util

import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.Locale

object TimeWindowCalculator {

    /**
     * Evaluates whether [now] falls inside the configured night schedule.
     * Supports windows that cross midnight (e.g., 22:00 -> 07:00) as well as same-day windows.
     */
    fun isWithinNightWindow(
        now: LocalTime,
        start: LocalTime,
        end: LocalTime,
        nightWindowEnabled: Boolean,
        allDayTestMode: Boolean
    ): Boolean {
        if (allDayTestMode || !nightWindowEnabled) return true
        if (start == end) return true

        return if (start.isBefore(end)) {
            // Same-day range, e.g., 00:30 -> 06:30
            !now.isBefore(start) && now.isBefore(end)
        } else {
            // Overnight range crossing midnight, e.g., 22:00 -> 07:00
            !now.isBefore(start) || now.isBefore(end)
        }
    }

    /**
     * Computes the next future epoch millisecond matching [targetHour]:[targetMinute].
     * If that clock time has already passed today, schedules for tomorrow.
     */
    fun computeNextTargetEpochMillis(
        now: ZonedDateTime,
        targetHour: Int,
        targetMinute: Int
    ): Long {
        var candidate = now
            .withHour(targetHour.coerceIn(0, 23))
            .withMinute(targetMinute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)

        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1)
        }
        return candidate.toInstant().toEpochMilli()
    }

    fun formatHourMinute(hour: Int, minute: Int): String =
        String.format(Locale.ROOT, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))

    fun formatCountdown(remainingMillis: Long): String {
        val clampedSeconds = (remainingMillis.coerceAtLeast(0L) + 999L) / 1000L
        val hours = clampedSeconds / 3600L
        val minutes = (clampedSeconds % 3600L) / 60L
        val seconds = clampedSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
        }
    }
}
