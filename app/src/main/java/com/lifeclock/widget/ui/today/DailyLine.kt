package com.lifeclock.widget.ui.today

import java.time.LocalDate

/**
 * The daily line about impermanence. The texts live in the string array `impermanence_lines`, one per language, and
 * only the project owner adds or approves them; the app ships with that array empty, and the card stays hidden until
 * it has lines. One line per calendar day, in order, wrapping around.
 */
object DailyLine {
    /** The line for [day], or null when there are no lines. */
    fun pick(lines: List<String>, day: LocalDate): String? {
        if (lines.isEmpty()) return null
        return lines[Math.floorMod(day.toEpochDay(), lines.size.toLong()).toInt()]
    }
}
