package com.lifeclock.widget

import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        assertEquals(1_850_000_000L, Pulse.leftOnAverage(1_000_000_000L))
        assertEquals(0L, Pulse.leftOnAverage(3_500_000_000L))
    }

    @Test
    fun shareIsClamped() {
        assertEquals(1_000_000_000.0 / 2_850_000_000.0, Pulse.share(1_000_000_000L), 1e-9)
        assertEquals(1.0, Pulse.share(9_000_000_000L), 1e-9)
    }

    @Test
    fun theCountRunsDownEverySecondOrSo() {
        // 70 beats a minute is 1.17 a second: over ten seconds the beats so far grow by 11 or 12, so the count left
        // shrinks by the same, to the last digit
        val t = birth.plusYears(30).toInstant().toEpochMilli()
        val now = Pulse.leftOnAverage(Pulse.beatsAt(birth, t))
        val later = Pulse.leftOnAverage(Pulse.beatsAt(birth, t + 10_000))
        assertTrue("fell by " + (now - later), now - later in 11L..12L)
        assertEquals(now - 1, Pulse.leftOnAverage(Pulse.beatsAt(birth, t) + 1))
    }
}
