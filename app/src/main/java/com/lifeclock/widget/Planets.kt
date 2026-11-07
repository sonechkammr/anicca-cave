package com.lifeclock.widget

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Planet positions from the Keplerian elements in "Approximate Positions of the Planets" (JPL, Table 1,
 * valid 1800-2050). Good to a fraction of a degree, which is plenty for a picture, and needs no network.
 * Everything is heliocentric, in the ecliptic frame.
 */
object Planets {
    const val EARTH = 2

    /** Value at J2000 and change per Julian century, for each element. */
    private class Elements(
        val a: Pair<Double, Double>, val e: Pair<Double, Double>, val incl: Pair<Double, Double>,
        val meanLon: Pair<Double, Double>, val periLon: Pair<Double, Double>, val nodeLon: Pair<Double, Double>,
    )

    // Mercury, Venus, Earth (Earth-Moon barycentre), Mars, Jupiter, Saturn, Uranus, Neptune
    private val BODIES = listOf(
        Elements(0.38709927 to 0.00000037, 0.20563593 to 0.00001906, 7.00497902 to -0.00594749,
            252.25032350 to 149472.67411175, 77.45779628 to 0.16047689, 48.33076593 to -0.12534081),
        Elements(0.72333566 to 0.00000390, 0.00677672 to -0.00004107, 3.39467605 to -0.00078890,
            181.97909950 to 58517.81538729, 131.60246718 to 0.00268329, 76.67984255 to -0.27769418),
        Elements(1.00000261 to 0.00000562, 0.01671123 to -0.00004392, -0.00001531 to -0.01294668,
            100.46457166 to 35999.37244981, 102.93768193 to 0.32327364, 0.0 to 0.0),
        Elements(1.52371034 to 0.00001847, 0.09339410 to 0.00007882, 1.84969142 to -0.00813131,
            -4.55343205 to 19140.30268499, -23.94362959 to 0.44441088, 49.55953891 to -0.29257343),
        Elements(5.20288700 to -0.00011607, 0.04838624 to -0.00013253, 1.30439695 to -0.00183714,
            34.39644051 to 3034.74612775, 14.72847983 to 0.21252668, 100.47390909 to 0.20469106),
        Elements(9.53667594 to -0.00125060, 0.05386179 to -0.00050991, 2.48599187 to 0.00193609,
            49.95424423 to 1222.49362201, 92.59887831 to -0.41897216, 113.66242448 to -0.28867794),
        Elements(19.18916464 to -0.00196176, 0.04725744 to -0.00004397, 0.77263783 to -0.00242939,
            313.23810451 to 428.48202785, 170.95427630 to 0.40805281, 74.01692503 to 0.04240589),
        Elements(30.06992276 to 0.00026291, 0.00859048 to 0.00005105, 1.77004347 to 0.00035372,
            -55.12002969 to 218.45945325, 44.96476227 to -0.32241464, 131.78422574 to -0.00508664),
    )

    val COUNT = BODIES.size

    /** Heliocentric ecliptic longitude in degrees (0..360) and distance from the Sun in AU. */
    class Position(val longitude: Double, val distanceAu: Double, val semiMajorAu: Double)

    private fun centuries(epochMs: Long): Double = (epochMs / 86_400_000.0 + 2_440_587.5 - 2_451_545.0) / 36_525.0

    private fun at(el: Pair<Double, Double>, t: Double) = el.first + el.second * t

    private fun rad(deg: Double) = deg * PI / 180.0

    fun position(index: Int, epochMs: Long): Position {
        val b = BODIES[index]
        val t = centuries(epochMs)
        val a = at(b.a, t)
        val e = at(b.e, t)
        val incl = rad(at(b.incl, t))
        val periLon = at(b.periLon, t)
        val node = rad(at(b.nodeLon, t))
        val argPeri = rad(periLon) - node

        // mean anomaly in -180..180, then Kepler's equation by Newton's method
        var m = (at(b.meanLon, t) - periLon) % 360.0
        if (m > 180) m -= 360 else if (m < -180) m += 360
        val mRad = rad(m)
        var ea = mRad + e * sin(mRad)
        for (i in 0 until 8) {
            val d = (ea - e * sin(ea) - mRad) / (1 - e * cos(ea))
            ea -= d
            if (abs(d) < 1e-10) break
        }

        val xp = a * (cos(ea) - e)
        val yp = a * sqrt(1 - e * e) * sin(ea)
        val cw = cos(argPeri); val sw = sin(argPeri)
        val cn = cos(node); val sn = sin(node)
        val ci = cos(incl)
        val x = (cw * cn - sw * sn * ci) * xp + (-sw * cn - cw * sn * ci) * yp
        val y = (cw * sn + sw * cn * ci) * xp + (-sw * sn + cw * cn * ci) * yp

        var lon = atan2(y, x) * 180.0 / PI
        if (lon < 0) lon += 360.0
        return Position(lon, hypot(xp, yp), a)
    }

    /** Degrees the planet has moved along its orbit since [daysAgo] days before [epochMs]. */
    fun travelled(index: Int, epochMs: Long, daysAgo: Int): Double {
        val now = position(index, epochMs).longitude
        val before = position(index, epochMs - daysAgo * 86_400_000L).longitude
        var d = now - before
        if (d < 0) d += 360.0
        return d
    }

    /**
     * Degrees the planet has moved along its orbit between [startMs] and [endMs], counting full laps (Mercury does about
     * four in a year, so this can pass 360). The wrapped true longitudes say where it ended up; the unwrapped mean
     * longitude says how many laps it made. They never differ by more than the equation of centre, under 25 degrees.
     */
    fun travelledSince(index: Int, startMs: Long, endMs: Long): Double {
        if (endMs <= startMs) return 0.0
        val b = BODIES[index]
        val meanDelta = at(b.meanLon, centuries(endMs)) - at(b.meanLon, centuries(startMs))
        var trueDelta = (position(index, endMs).longitude - position(index, startMs).longitude) % 360.0
        if (trueDelta < 0) trueDelta += 360.0
        val laps = Math.round((meanDelta - trueDelta) / 360.0)
        return (trueDelta + 360.0 * laps).coerceAtLeast(0.0)
    }

    /** Orbital speed from the vis-viva equation, km/s. */
    fun speedKmS(index: Int, epochMs: Long): Double {
        val p = position(index, epochMs)
        return 29.7847 * sqrt(2 / p.distanceAu - 1 / p.semiMajorAu)   // 29.78 km/s is a circular orbit at 1 AU
    }
}
