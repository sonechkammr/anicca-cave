package com.lifeclock.widget

import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanetsTest {
    private fun ms(y: Int, m: Int, d: Int) = ZonedDateTime.of(y, m, d, 12, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun earthAtJ2000() {
        // Sun's geocentric longitude at J2000 is 280.38 deg, so Earth seen from the Sun is 100.38
        assertEquals(100.38, Planets.position(Planets.EARTH, ms(2000, 1, 1)).longitude, 0.2)
    }

    @Test
    fun earthInNovember2026() {
        // the Sun sits around 224-225 deg in early November, Earth is opposite
        assertEquals(44.5, Planets.position(Planets.EARTH, ms(2026, 11, 7)).longitude, 1.5)
    }

    @Test
    fun jupiterAtJ2000() {
        assertEquals(36.3, Planets.position(4, ms(2000, 1, 1)).longitude, 1.0)
    }

    @Test
    fun earthSpeedStaysInItsRealRange() {
        val v = Planets.speedKmS(Planets.EARTH, ms(2026, 11, 7))
        assertTrue("speed was $v", v in 29.2..30.4)
    }

    @Test
    fun innerPlanetsAreFasterThanOuter() {
        val t = ms(2026, 11, 7)
        assertTrue(Planets.travelled(0, t, 30) > Planets.travelled(1, t, 30))
        assertTrue(Planets.travelled(1, t, 30) > Planets.travelled(2, t, 30))
        assertTrue(Planets.travelled(7, t, 30) < 1.0)
    }

    private val dayMs = 86_400_000L

    @Test
    fun nothingTravelledOverNoTime() {
        val t = ms(2026, 11, 7)
        assertEquals(0.0, Planets.travelledSince(0, t, t), 1e-9)
        assertEquals(0.0, Planets.travelledSince(0, t, t - dayMs), 1e-9)
    }

    @Test
    fun earthTravelsAboutAFullCircleInAYear() {
        val a = ms(2025, 11, 13)
        val b = ms(2026, 11, 13)
        assertEquals(360.0, Planets.travelledSince(Planets.EARTH, a, b), 1.0)
    }

    @Test
    fun innerPlanetsLapMoreThanOnceAYear() {
        val a = ms(2025, 11, 13)
        val b = ms(2026, 11, 13)
        // Mercury: 365.25 / 87.97 = 4.15 laps; Venus: 365.25 / 224.7 = 1.63 laps
        assertEquals(4.152 * 360.0, Planets.travelledSince(0, a, b), 25.0)
        assertEquals(1.625 * 360.0, Planets.travelledSince(1, a, b), 8.0)
    }

    @Test
    fun outerPlanetsCoverASmallPartOfTheirOrbit() {
        val a = ms(2025, 11, 13)
        val b = ms(2026, 11, 13)
        // Jupiter: 365.25 / 4332.6 = 8.4 % of an orbit; Neptune: 365.25 / 60190 = 0.6 %
        assertEquals(0.084 * 360.0, Planets.travelledSince(4, a, b), 2.0)
        assertEquals(0.006 * 360.0, Planets.travelledSince(7, a, b), 0.5)
    }

    @Test
    fun partOfAYearScalesDown() {
        val a = ms(2025, 11, 13)
        val half = ms(2026, 5, 13)
        val full = ms(2026, 11, 13)
        val h = Planets.travelledSince(Planets.EARTH, a, half)
        val f = Planets.travelledSince(Planets.EARTH, a, full)
        assertTrue("half $h of $f", h in f * 0.45..f * 0.55)
    }
}