package com.lifeclock.widget

import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeMathTest {
    private val birth = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)

    private fun utc(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0) =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, ZoneOffset.UTC)

    @Test
    fun beforeBirthEverythingIsZero() {
        val s = LifeMath.snapshot(birth, birth.minusDays(3))
        assertEquals(0, s.years)
        assertEquals(0L, s.totalDays)
        assertEquals(0L, s.totalSeconds)
    }

    @Test
    fun totalsAfterExactlyThousandDays() {
        val s = LifeMath.snapshot(birth, birth.plusDays(1000))
        assertEquals(1000L, s.totalDays)
        assertEquals(86_400_000L, s.totalSeconds)
        assertEquals(24_000L, s.totalHours)
    }

    @Test
    fun daysToBirthdayOnTheEveOfOne() {
        val s = LifeMath.snapshot(birth, utc(2020, 12, 31))
        assertEquals(20, s.years)
        assertEquals(1L, s.daysToBirthday)
    }

    @Test
    fun yearProgressStaysInRange() {
        val s = LifeMath.snapshot(birth, utc(2013, 6, 15, 13, 7))
        assertTrue(s.yearProgress in 0.0..1.0)
    }

    @Test
    fun leapDayBirthIsOneYearOldBySecondOfMarch() {
        val leap = utc(2000, 2, 29)
        assertEquals(0, LifeMath.snapshot(leap, utc(2000, 12, 31)).years)
        assertEquals(1, LifeMath.snapshot(leap, utc(2001, 3, 1)).years)
        assertEquals(4, LifeMath.snapshot(leap, utc(2004, 2, 29)).years)
    }

    @Test
    fun birthTimeInsideASpringForwardGapDoesNotCrash() {
        // 2024-03-31 03:30 does not exist in Tallinn; java.time moves it to 04:30.
        val tallinn = ZoneId.of("Europe/Tallinn")
        val gap = ZonedDateTime.of(2024, 3, 31, 3, 30, 0, 0, tallinn)
        assertEquals(4, gap.hour)
        val now = gap.plusYears(1).plusDays(10)
        assertEquals(1, LifeMath.snapshot(gap, now).years)
        assertEquals(8, ageReading(gap, now.toInstant().toEpochMilli()).digits.size)
    }

    @Test
    fun ageReadingHalfwayThroughALeapYear() {
        // 183 of 366 days: exactly one half.
        val r = ageReading(birth, utc(2000, 7, 2).toInstant().toEpochMilli())
        assertEquals(0, r.years)
        assertArrayEquals(intArrayOf(5, 0, 0, 0, 0, 0, 0, 0), r.digits)
    }

    @Test
    fun ageReadingBeforeBirthClampsToBirth() {
        val r = ageReading(birth, birth.toInstant().toEpochMilli() - 10_000)
        assertEquals(0, r.years)
        assertArrayEquals(IntArray(8), r.digits)
    }

    @Test
    fun fractionDigitsHalfAndClamp() {
        assertArrayEquals(intArrayOf(5, 0, 0, 0, 0, 0, 0, 0), fractionDigits(0.5))
        assertArrayEquals(IntArray(8) { 9 }, fractionDigits(1.0))
        assertArrayEquals(IntArray(8), fractionDigits(-0.2))
    }

    @Test
    fun padKeepsLeadingZeros() {
        assertEquals("07", 7.pad())
        assertEquals("005", 5.pad(3))
    }
}
