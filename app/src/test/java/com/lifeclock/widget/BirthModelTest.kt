package com.lifeclock.widget

import com.lifeclock.widget.ui.settings.Cities
import com.lifeclock.widget.ui.settings.birthOf
import com.lifeclock.widget.ui.settings.isFuture
import com.lifeclock.widget.ui.settings.isSelectableBirthDate
import com.lifeclock.widget.ui.settings.isSelectableBirthYear
import com.lifeclock.widget.ui.settings.pickerMillisToDate
import com.lifeclock.widget.ui.settings.toPickerMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class BirthModelTest {
    @org.junit.Test
    fun cityNameDropsRegionAndUnderscores() {
        assertEquals("Tallinn", Cities.nameOf("Europe/Tallinn"))
        assertEquals("New York", Cities.nameOf("America/New_York"))
        assertEquals("Buenos Aires", Cities.nameOf("America/Argentina/Buenos_Aires"))
    }

    @org.junit.Test
    fun cityListHasRealPlacesOnly() {
        val ids = Cities.all().map { it.zoneId }
        assertTrue("Europe/Tallinn" in ids)
        assertTrue("Asia/Tokyo" in ids)
        assertTrue(ids.none { it.startsWith("Etc/") || it.startsWith("US/") || it.startsWith("SystemV/") })
        assertTrue(ids.none { '/' !in it })
    }

    @org.junit.Test
    fun cityListIsSortedByName() {
        val names = Cities.all().map { it.name }
        assertEquals(names.sortedWith(String.CASE_INSENSITIVE_ORDER), names)
    }

    @org.junit.Test
    fun searchFindsByNameAndPutsPrefixMatchesFirst() {
        val cities = Cities.all()
        val hits = Cities.search(cities, "tall").map { it.zoneId }
        assertTrue("Europe/Tallinn" in hits)
        val york = Cities.search(cities, "york")
        assertTrue(york.any { it.zoneId == "America/New_York" })
        // "Port" starts several names and sits inside others (Newport...): starts-with comes first.
        val port = Cities.search(cities, "port")
        val firstNonPrefix = port.indexOfFirst { !it.name.lowercase().startsWith("port") }
        val lastPrefix = port.indexOfLast { it.name.lowercase().startsWith("port") }
        assertTrue(firstNonPrefix == -1 || lastPrefix < firstNonPrefix)
    }

    @org.junit.Test
    fun blankSearchReturnsEverythingAndNonsenseReturnsNothing() {
        val cities = Cities.all()
        assertEquals(cities, Cities.search(cities, "  "))
        assertTrue(Cities.search(cities, "zzzzqqq").isEmpty())
    }

    @org.junit.Test
    fun birthInSpringDstGapMovesForward() {
        // 28 March 2021, 02:30 does not exist in Berlin: clocks jump from 02:00 to 03:00.
        val b = birthOf(LocalDate.of(2021, 3, 28), LocalTime.of(2, 30), ZoneId.of("Europe/Berlin"))
        assertEquals(LocalTime.of(3, 30), b.toLocalTime())
    }

    @org.junit.Test
    fun futureMomentIsDetectedByZone() {
        val now = Instant.parse("2026-01-01T12:00:00Z")
        // 13:00 on 1 Jan in Tokyo is 04:00Z: already past. 23:00 on 1 Jan in Los Angeles is 07:00Z on the 2nd: future.
        assertFalse(isFuture(birthOf(LocalDate.of(2026, 1, 1), LocalTime.of(13, 0), ZoneId.of("Asia/Tokyo")), now))
        assertTrue(isFuture(birthOf(LocalDate.of(2026, 1, 1), LocalTime.of(23, 0), ZoneId.of("America/Los_Angeles")), now))
    }

    @org.junit.Test
    fun pickerAllowsOnlyDatesFrom1900UntilToday() {
        val today = LocalDate.of(2026, 11, 7)
        assertTrue(isSelectableBirthDate(LocalDate.of(1900, 1, 1).toPickerMillis(), today))
        assertTrue(isSelectableBirthDate(today.toPickerMillis(), today))
        assertFalse(isSelectableBirthDate(LocalDate.of(1899, 12, 31).toPickerMillis(), today))
        assertFalse(isSelectableBirthDate(today.plusDays(1).toPickerMillis(), today))
        assertTrue(isSelectableBirthYear(1900, today))
        assertTrue(isSelectableBirthYear(2026, today))
        assertFalse(isSelectableBirthYear(1899, today))
        assertFalse(isSelectableBirthYear(2027, today))
    }

    @org.junit.Test
    fun pickerMillisRoundTripsIncludingLeapDayAndPre1970() {
        for (d in listOf(LocalDate.of(2000, 2, 29), LocalDate.of(1955, 7, 1), LocalDate.of(1900, 1, 1))) {
            assertEquals(d, pickerMillisToDate(d.toPickerMillis()))
        }
    }

    @org.junit.Test
    fun aliasesCanBeFilteredAndNamesLocalized() {
        val ids = setOf("Australia/ACT", "Australia/Sydney", "Europe/Tallinn", "Etc/GMT+5")
        val cities = Cities.all(ids, displayName = { if (it == "Europe/Tallinn") "Таллин" else Cities.nameOf(it) }, isCanonical = { it != "Australia/ACT" })
        assertEquals(listOf("Sydney", "Таллин"), cities.map { it.name })
        // A Latin query still finds the Cyrillic-named city through its zone id, and ranks it first when the English name starts with it.
        assertEquals("Europe/Tallinn", Cities.search(cities, "tall").first().zoneId)
    }
}
