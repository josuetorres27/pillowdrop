package com.josuetorres.pillowdrop.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TimeWindowCalculatorTest {

    @Test
    fun `handles overnight bedtime schedule crossing midnight`() {
        val start = LocalTime.of(22, 0)
        val end = LocalTime.of(7, 0)

        assertTrue(
            TimeWindowCalculator.isWithinNightWindow(
                now = LocalTime.of(23, 15),
                start = start,
                end = end,
                nightWindowEnabled = true,
                allDayTestMode = false
            )
        )
        assertTrue(
            TimeWindowCalculator.isWithinNightWindow(
                now = LocalTime.of(2, 45),
                start = start,
                end = end,
                nightWindowEnabled = true,
                allDayTestMode = false
            )
        )
        assertFalse(
            TimeWindowCalculator.isWithinNightWindow(
                now = LocalTime.of(15, 30),
                start = start,
                end = end,
                nightWindowEnabled = true,
                allDayTestMode = false
            )
        )
    }

    @Test
    fun `computes next target clock hour tomorrow when already passed today`() {
        val zone = ZoneId.of("America/Costa_Rica")
        val now = ZonedDateTime.of(2026, 11, 24, 23, 30, 0, 0, zone)
        val triggerEpoch = TimeWindowCalculator.computeNextTargetEpochMillis(now, 3, 0)
        val expected = ZonedDateTime.of(2026, 11, 25, 3, 0, 0, 0, zone).toInstant().toEpochMilli()

        assertEquals(expected, triggerEpoch)
    }

    @Test
    fun `formats countdown cleanly`() {
        assertEquals("01:00", TimeWindowCalculator.formatCountdown(60_000L))
        assertEquals("01:30:05", TimeWindowCalculator.formatCountdown((90 * 60 + 5) * 1000L))
    }
}
