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
}