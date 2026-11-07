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
}