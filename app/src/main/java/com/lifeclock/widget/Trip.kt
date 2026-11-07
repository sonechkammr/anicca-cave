package com.lifeclock.widget

import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.min

/** How far you have travelled around the Sun since birth, riding on Earth. */
object Trip {
    private const val AU_KM = 149_597_870.7
    private const val ECCENTRICITY = 0.0167
    private const val SIDEREAL_YEAR_DAYS = 365.25636
    private const val CALENDAR_YEAR_DAYS = 365.2425
    private const val DAY_MS = 86_400_000L

    /** Length of the orbit, scaled to a calendar year rather than the time Earth needs to come back to the same stars. */
    private val YEAR_KM = 2 * PI * AU_KM * (1 - ECCENTRICITY * ECCENTRICITY / 4) * CALENDAR_YEAR_DAYS / SIDEREAL_YEAR_DAYS

    /** Whole birthdays count as full laps; the part since the last birthday is integrated day by day. */
    fun kmAt(birth: ZonedDateTime, epochMs: Long): Double {
        val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMs), birth.zone)
        if (!now.isAfter(birth)) return 0.0
        val years = ChronoUnit.YEARS.between(birth, now)
        var km = years * YEAR_KM
        var t = birth.plusYears(years).toInstant().toEpochMilli()
        while (t < epochMs) {
            val dt = min(DAY_MS, epochMs - t)
            km += Planets.speedKmS(Planets.EARTH, t + dt / 2) * dt / 1000.0
            t += dt
        }
        return km
    }

    /** Where the current year of life began on the orbit, in degrees of heliocentric longitude. */
    fun yearStartLongitude(birth: ZonedDateTime, epochMs: Long): Double {
        val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMs), birth.zone)
        val years = if (now.isAfter(birth)) ChronoUnit.YEARS.between(birth, now) else 0
        return Planets.position(Planets.EARTH, birth.plusYears(years).toInstant().toEpochMilli()).longitude
    }

    /** Digits of [km] with -1 marking the gap between each group of three: 26822037402 -> 2 6 _ 8 2 2 _ 0 3 7 _ 4 0 2. */
    fun cells(km: Long): IntArray {
        val s = km.coerceAtLeast(0).toString()
        val out = ArrayList<Int>()
        for ((i, ch) in s.withIndex()) {
            if (i > 0 && (s.length - i) % 3 == 0) out.add(-1)
            out.add(ch - '0')
        }
        return out.toIntArray()
    }
}