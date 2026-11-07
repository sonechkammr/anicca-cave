package com.lifeclock.widget

import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Nearby and well-known stars. The order matches `R.array.star_names`, so keep both sorted by distance.
 * Distances in light years and right ascension in hours, rounded from common catalogues.
 */
object Stars {
    class Star(val ly: Double, val raHours: Double)

    val ALL = listOf(
        Star(4.24, 14.50),   // Proxima Centauri
        Star(4.37, 14.66),   // Alpha Centauri
        Star(5.96, 17.96),   // Barnard's Star
        Star(7.86, 10.94),   // Wolf 359
        Star(8.31, 11.06),   // Lalande 21185
        Star(8.61, 6.75),    // Sirius
        Star(8.73, 1.65),    // UV Ceti
        Star(9.68, 18.83),   // Ross 154
        Star(10.30, 23.70),  // Ross 248
        Star(10.47, 3.55),   // Epsilon Eridani
        Star(10.72, 23.08),  // Lacaille 9352
        Star(11.01, 11.80),  // Ross 128
        Star(11.40, 21.11),  // 61 Cygni
        Star(11.46, 7.65),   // Procyon
        Star(11.87, 22.05),  // Epsilon Indi
        Star(11.91, 1.74),   // Tau Ceti
        Star(12.83, 5.20),   // Kapteyn's Star
        Star(16.73, 19.85),  // Altair
        Star(20.40, 15.32),  // Gliese 581
        Star(25.04, 18.62),  // Vega
        Star(25.13, 22.96),  // Fomalhaut
        Star(33.78, 7.76),   // Pollux
        Star(35.90, 11.82),  // Denebola
        Star(36.70, 14.26),  // Arcturus
        Star(40.66, 23.11),  // TRAPPIST-1
        Star(42.90, 5.28),   // Capella
        Star(51.50, 7.58),   // Castor
        Star(63.40, 5.79),   // Beta Pictoris
        Star(65.30, 4.60),   // Aldebaran
        Star(79.30, 10.14),  // Regulus
        Star(90.00, 3.14),   // Algol
    )

    private const val YEAR_SECONDS = 365.25 * 86_400

    /** Age in years as the distance, in light years, the light of your birth day has covered. */
    fun lightYears(birth: ZonedDateTime, epochMs: Long): Double {
        val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMs), birth.zone)
        if (!now.isAfter(birth)) return 0.0
        return ChronoUnit.SECONDS.between(birth, now) / YEAR_SECONDS
    }

    /** Index of the first star the light has not reached yet, or -1 if it has passed them all. */
    fun nextIndex(lightYears: Double): Int = ALL.indexOfFirst { it.ly > lightYears }
}
