package com.lifeclock.widget

import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class PulseTest {
    private val birth = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)

    @Test
    fun aDayIsAbout100800Beats() {
        val next = birth.plusDays(1).toInstant().toEpochMilli()
        assertEquals(100_800L, Pulse.beatsAt(birth, next))
    }

    @Test
    fun nothingBeforeBirth() {
        assertEquals(0L, Pulse.beatsAt(birth, birth.toInstant().toEpochMilli() - 5_000))
    }

    @Test
    fun leftOnAverageNeverGoesNegative() {
        assertEquals(1_500_000_000L, Pulse.leftOnAverage(1_000_000_000L))
        assertEquals(0L, Pulse.leftOnAverage(3_000_000_000L))
    }

    @Test
    fun shareIsClamped() {
        assertEquals(0.4, Pulse.share(1_000_000_000L), 1e-9)
        assertEquals(1.0, Pulse.share(9_000_000_000L), 1e-9)
    }

    @Test
    fun leftApproxRoundsToThousandsAndStopsAtZero() {
        // 2.5 bn minus 1,050,940,525 beats is 1,449,059,475 left: rounded to 1,449,059,000
        assertEquals(1_449_059_000L, Pulse.leftApprox(1_050_940_525L))
        assertEquals(1_449_060_000L, Pulse.leftApprox(1_050_940_000L))
        assertEquals(0L, Pulse.leftApprox(3_000_000_000L))
        assertEquals(2_500_000_000L, Pulse.leftApprox(0L))
    }

    @Test
    fun leftApproxStaysPutBetweenRoundingSteps() {
        // at 70 a minute, 1,000 beats take about 14 minutes: two readings a minute apart round to the same value
        val a = Pulse.leftApprox(1_000_000_300L)
        val b = Pulse.leftApprox(1_000_000_370L)
        assertEquals(a, b)
    }
}