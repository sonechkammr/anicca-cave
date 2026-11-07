package com.lifeclock.widget

import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GardenTest {
    private val width = 560.0

    @Test
    fun theSameDayGivesTheSameGarden() {
        val a = Garden.layout(20_400, width)
        val b = Garden.layout(20_400, width)
        assertEquals(a.map { it.x }, b.map { it.x })
        assertEquals(a.map { it.r }, b.map { it.r })
    }

    @Test
    fun neighbouringDaysDiffer() {
        val a = Garden.layout(20_400, width).map { it.x }
        val b = Garden.layout(20_401, width).map { it.x }
        assertNotEquals(a, b)
    }

    @Test
    fun stonesAreFewAndNeverTouch() {
        for (day in 20_000L..20_400L) {
            val stones = Garden.layout(day, width)
            assertTrue("day $day has ${stones.size} stones", stones.size in 4..7)
            for (i in stones.indices) for (j in i + 1 until stones.size) {
                val gap = hypot(stones[i].x - stones[j].x, stones[i].y - stones[j].y) - stones[i].r - stones[j].r
                assertTrue("day $day: stones $i and $j are $gap apart", gap > 40)
            }
        }
    }

    @Test
    fun stonesStayInsideTheFrame() {
        for (day in 20_000L..20_200L) for (s in Garden.layout(day, width)) {
            assertTrue(s.x - s.r > 10 && s.x + s.r < width - 10)
            assertTrue(s.y - s.r > 2 && s.y + s.r < Garden.UNIT_HEIGHT - 2)
        }
    }

    @Test
    fun pixelsAreDrawnForBothPalettes() {
        val stones = Garden.layout(20_400, width)
        val sand = Garden.pixels(140, 63, stones, natural = true)
        val tint = Garden.pixels(140, 63, stones, natural = false)
        assertEquals(140 * 63, sand.size)
        assertTrue(sand.all { (it ushr 24) == 0xFF })
        assertTrue(tint.any { (it ushr 24) > 200 })      // a stone
        assertTrue(tint.any { (it ushr 24) < 10 })       // calm sand
    }
}