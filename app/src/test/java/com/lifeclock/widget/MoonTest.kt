package com.lifeclock.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.math.abs

class MoonTest {
    private fun phaseAt(iso: String) = Moon.phase(Instant.parse(iso).toEpochMilli())

    /** Distance between two phases on the cycle, so 0.99 and 0.01 are 0.02 apart. */
    private fun apart(a: Double, b: Double): Double {
        val d = abs(a - b)
        return minOf(d, 1 - d)
    }

    // Dates of the real events (UTC). The mean-month clock may be off by up to about 14 hours = 0.02 of a cycle.
    private val tolerance = 0.03

    @Test
    fun newMoonsAreNearZero() {
        assertTrue(apart(phaseAt("2000-01-06T18:14:00Z"), 0.0) < 0.001)
        assertTrue(apart(phaseAt("2024-04-08T18:21:00Z"), 0.0) < tolerance)
    }

    @Test
    fun fullMoonsAreNearOneHalf() {
        assertTrue(apart(phaseAt("2000-01-21T04:40:00Z"), 0.5) < tolerance)
        assertTrue(apart(phaseAt("2024-04-23T23:49:00Z"), 0.5) < tolerance)
    }

    @Test
    fun firstQuarterIsNearOneQuarter() {
        assertTrue(apart(phaseAt("2024-04-15T19:13:00Z"), 0.25) < tolerance)
    }

    @Test
    fun phaseStaysInZeroToOneBeforeTheEpochAndBefore1970() {
        for (iso in listOf("1999-12-31T00:00:00Z", "1969-07-20T20:17:00Z", "1950-11-13T12:00:00Z", "1900-01-01T00:00:00Z")) {
            val p = phaseAt(iso)
            assertTrue("$iso gave $p", p >= 0.0 && p < 1.0)
        }
    }

    @Test
    fun aWholeSynodicMonthLaterIsTheSamePhase() {
        val t = Instant.parse("1950-11-13T12:00:00Z").toEpochMilli()
        val later = t + (Moon.SYNODIC_DAYS * 86_400_000.0).toLong()
        assertTrue(apart(Moon.phase(t), Moon.phase(later)) < 1e-6)
    }

    @Test
    fun illuminationRunsFromDarkToFullToDark() {
        assertEquals(0.0, Moon.illumination(0.0), 1e-9)
        assertEquals(0.5, Moon.illumination(0.25), 1e-9)
        assertEquals(1.0, Moon.illumination(0.5), 1e-9)
        assertEquals(0.5, Moon.illumination(0.75), 1e-9)
        assertEquals(0.0, Moon.illumination(1.0), 1e-9)
    }

    @Test
    fun namesFollowTheEightPhasesAndWrapAround() {
        assertEquals(Moon.Phase.NEW, Moon.name(0.0))
        assertEquals(Moon.Phase.WAXING_CRESCENT, Moon.name(0.125))
        assertEquals(Moon.Phase.FIRST_QUARTER, Moon.name(0.25))
        assertEquals(Moon.Phase.WAXING_GIBBOUS, Moon.name(0.375))
        assertEquals(Moon.Phase.FULL, Moon.name(0.5))
        assertEquals(Moon.Phase.WANING_GIBBOUS, Moon.name(0.625))
        assertEquals(Moon.Phase.LAST_QUARTER, Moon.name(0.75))
        assertEquals(Moon.Phase.WANING_CRESCENT, Moon.name(0.875))
        // Just under 1 is still "new moon", not an out-of-range index.
        assertEquals(Moon.Phase.NEW, Moon.name(0.99))
        assertEquals(Moon.Phase.NEW, Moon.name(0.9999))
    }
}
