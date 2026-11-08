package com.lifeclock.widget.ui.settings

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale

/** Pure rules for the birth date, time and city. No Android or Compose here, so they are unit-tested. */

/** Earliest birth year the date picker offers. */
const val MIN_BIRTH_YEAR = 1900

/** The time assumed when the user doesn't know it. */
val UNKNOWN_BIRTH_TIME: LocalTime = LocalTime.NOON

/** A place the user can pick: one IANA zone, shown by its city name. */
data class City(val zoneId: String, val name: String, val region: String)

object Cities {
    /** Real region prefixes only: no `Etc/`, `SystemV/`, `US/` aliases or bare names like `UTC`. */
    private val regions = setOf("Africa", "America", "Antarctica", "Asia", "Atlantic", "Australia", "Europe", "Indian", "Pacific")

    /** Whether [zoneId] is one of the selectable cities (a real region, not `Etc/GMT+5` or `UTC`). */
    fun isCity(zoneId: String): Boolean = '/' in zoneId && zoneId.substringBefore('/') in regions

    /** "America/Argentina/Buenos_Aires" becomes "Buenos Aires". */
    fun nameOf(zoneId: String): String = zoneId.substringAfterLast('/').replace('_', ' ')

    fun of(zoneId: String, name: String = nameOf(zoneId)): City = City(zoneId, name, zoneId.substringBefore('/'))

    /**
     * Every selectable city, sorted by name. [displayName] turns a zone id into the name shown (and searched); the
     * app passes a localized one, tests use the English default.
     */
    fun all(
        ids: Set<String> = ZoneId.getAvailableZoneIds(),
        displayName: (String) -> String = ::nameOf,
        isCanonical: (String) -> Boolean = { true },
    ): List<City> =
        ids.filter { isCity(it) && isCanonical(it) }
            .map { of(it, displayName(it)) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    /** Cities whose name (or region) contains [query], names that start with it first. Blank gives everything. */
    fun search(cities: List<City>, query: String): List<City> {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return cities
        val hits = cities.filter { q in it.name.lowercase(Locale.ROOT) || q in it.zoneId.lowercase(Locale.ROOT) }
        // A name that starts with the query, in the shown language or the English one from the id, comes first.
        fun startsWithQuery(c: City) =
            c.name.lowercase(Locale.ROOT).startsWith(q) || nameOf(c.zoneId).lowercase(Locale.ROOT).startsWith(q)
        return hits.sortedBy { if (startsWithQuery(it)) 0 else 1 }
    }
}

/** The moment of birth. A time that doesn't exist in that zone (a spring DST gap) moves forward, as java.time does. */
fun birthOf(date: LocalDate, time: LocalTime, zone: ZoneId): ZonedDateTime = date.atTime(time).atZone(zone)

fun isFuture(birth: ZonedDateTime, now: Instant = Instant.now()): Boolean = birth.toInstant().isAfter(now)

/**
 * Whether the date picker may offer [utcMillis] (midnight UTC of that day): from 1 January [MIN_BIRTH_YEAR] up to
 * today. Material's date picker works in UTC.
 */
fun isSelectableBirthDate(utcMillis: Long, today: LocalDate = LocalDate.now()): Boolean {
    val day = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return day.year >= MIN_BIRTH_YEAR && !day.isAfter(today)
}

fun isSelectableBirthYear(year: Int, today: LocalDate = LocalDate.now()): Boolean =
    year in MIN_BIRTH_YEAR..today.year

fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun pickerMillisToDate(utcMillis: Long): LocalDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
