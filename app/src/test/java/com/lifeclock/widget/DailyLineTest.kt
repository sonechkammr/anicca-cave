package com.lifeclock.widget

import com.lifeclock.widget.ui.today.DailyLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DailyLineTest {
    @Test
    fun noLinesMeansNoCard() {
        assertNull(DailyLine.pick(emptyList(), LocalDate.of(2026, 11, 7)))
    }

    @Test
    fun oneLinePerDayInOrderWrappingAround() {
        val lines = listOf("a", "b", "c")
        val day = LocalDate.of(2026, 11, 7)
        val first = DailyLine.pick(lines, day)!!
        val next = DailyLine.pick(lines, day.plusDays(1))!!
        assertEquals((lines.indexOf(first) + 1) % 3, lines.indexOf(next))
        assertEquals(first, DailyLine.pick(lines, day.plusDays(3)))
    }

    @Test
    fun sameDayGivesTheSameLineAndDatesBefore1970Work() {
        val lines = listOf("a", "b", "c", "d")
        assertEquals(DailyLine.pick(lines, LocalDate.of(1950, 11, 13)), DailyLine.pick(lines, LocalDate.of(1950, 11, 13)))
        // A negative epoch day must still land inside the list.
        val picked = DailyLine.pick(lines, LocalDate.of(1900, 1, 1))
        assertEquals(true, picked in lines)
    }
}
