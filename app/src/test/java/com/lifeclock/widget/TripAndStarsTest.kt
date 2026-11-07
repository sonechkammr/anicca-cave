package com.lifeclock.widget

import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripAndStarsTest {
    private fun at(y: Int, m: Int, d: Int) = ZonedDateTime.of(y, m, d, 12, 0, 0, 0, ZoneOffset.UTC)

    @Test
    fun oneYearIsAboutNineHundredFortyMillionKm() {
        val birth = at(2000, 1, 1)
        val km = Trip.kmAt(birth, at(2001, 1, 1).toInstant().toEpochMilli())
        assertEquals(940e6, km, 2e6)
    }

    @Test
    fun partialYearAddsUpSmoothly() {
        val birth = at(2000, 1, 1)
        val half = Trip.kmAt(birth, at(2000, 7, 2).toInstant().toEpochMilli())
        assertEquals(470e6, half, 8e6)
    }

    @Test
    fun kilometresAreGroupedByThousandsWithoutADecimalPoint() {
        assertEquals(listOf(2, 6, -1, 8, 2, 2, -1, 0, 3, 7, -1, 4, 0, 2), Trip.cells(26_822_037_402L).toList())
        assertEquals(listOf(9, 9, 9), Trip.cells(999L).toList())
        assertEquals(listOf(1, -1, 0, 0, 0), Trip.cells(1000L).toList())
    }
    @Test
    fun lightYearsEqualAgeInYears() {
        val birth = at(2000, 1, 1)
        assertEquals(26.85, Stars.lightYears(birth, at(2026, 11, 7).toInstant().toEpochMilli()), 0.05)
    }

    @Test
    fun nextStarAfterTwentyEightYearsIsPollux() {
        // Pollux is at 33.78 ly, the first one beyond 28.5
        assertEquals(33.78, Stars.ALL[Stars.nextIndex(28.5)].ly, 0.001)
    }

    @Test
    fun starsAreSortedByDistance() {
        assertTrue(Stars.ALL.zipWithNext().all { (a, b) -> a.ly <= b.ly })
    }
}