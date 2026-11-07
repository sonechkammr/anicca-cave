package com.lifeclock.widget

import java.time.ZonedDateTime
import kotlin.math.max

/** Heartbeats since birth at a fixed resting rate, against the usual total for a human lifetime. */
object Pulse {
    const val BEATS_PER_MINUTE = 70
    /** About 70 beats a minute over roughly 68 years. An average over people, not a forecast. */
    const val AVERAGE_LIFETIME_BEATS = 2_500_000_000L

    fun beatsAt(birth: ZonedDateTime, epochMs: Long): Long {
        val ms = max(epochMs - birth.toInstant().toEpochMilli(), 0L)
        return (ms / 60_000.0 * BEATS_PER_MINUTE).toLong()
    }

    /** Beats left until the average, or 0 once it has been passed. */
    fun leftOnAverage(beats: Long): Long = max(AVERAGE_LIFETIME_BEATS - beats, 0L)

    /**
     * Beats left on average, rounded to the nearest [step] (a thousand by default, about 14 minutes of beating). The
     * rounding is the point: the figure is an average over people, so it is shown as an approximation, never as an exact
     * countdown.
     */
    fun leftApprox(beats: Long, step: Long = 1_000L): Long = (leftOnAverage(beats) + step / 2) / step * step

    fun share(beats: Long): Double = (beats.toDouble() / AVERAGE_LIFETIME_BEATS).coerceIn(0.0, 1.0)
}