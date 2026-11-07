package com.lifeclock.widget

import java.time.Instant
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor

/**
 * Moon phase from the mean synodic month, counted from a known new moon. It is an approximation: real new moons drift
 * up to about 14 hours from this clock, so the phase is right to within roughly 2 % of a cycle. Fine for "what did the
 * moon look like", not for eclipse tables.
 */
object Moon {
    const val SYNODIC_DAYS = 29.530588853

    /** New moon of 6 January 2000, 18:14 UTC. */
    private val EPOCH_MS: Long = Instant.parse("2000-01-06T18:14:00Z").toEpochMilli()
    private const val DAY_MS = 86_400_000.0

    /** Where in the cycle [epochMs] falls: 0 new, 0.25 first quarter, 0.5 full, 0.75 last quarter, back to 1 = 0. */
    fun phase(epochMs: Long): Double {
        val cycles = (epochMs - EPOCH_MS) / DAY_MS / SYNODIC_DAYS
        return cycles - floor(cycles) // floor, not truncation: dates before 2000 and 1970 stay in 0..1
    }

    /** Share of the disc that is lit, 0 at new moon and 1 at full moon. */
    fun illumination(phase: Double): Double = (1 - cos(2 * PI * phase)) / 2

    enum class Phase { NEW, WAXING_CRESCENT, FIRST_QUARTER, WAXING_GIBBOUS, FULL, WANING_GIBBOUS, LAST_QUARTER, WANING_CRESCENT }

    /** The eight traditional names, each centred on its own phase (new moon covers 0.9375 to 0.0625). */
    fun name(phase: Double): Phase = Phase.entries[(floor(phase * 8 + 0.5).toInt()) % 8]
}
