package com.lifeclock.widget

import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Whole years plus eight decimals of the age fraction at one instant, from integer maths. */
class AgeReading(val years: Int, val digits: IntArray)

fun ageReading(birth: ZonedDateTime, epochMs: Long): AgeReading {
    var n = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMs), birth.zone)
    if (n.isBefore(birth)) n = birth
    val y = ChronoUnit.YEARS.between(birth, n).toInt()
    val last = birth.plusYears(y.toLong())
    val next = birth.plusYears(y + 1L)
    val span = Duration.between(last, next).toMillis().coerceAtLeast(1)
    val done = Duration.between(last, n).toMillis().coerceIn(0, span - 1)
    val scaled = (done * 100_000_000L) / span
    val s = scaled.toString().padStart(8, '0')
    return AgeReading(y, IntArray(8) { s[it] - '0' })
}

/** Decimal digits of a 0..1 fraction: 0.58947328 -> [5, 8, 9, 4, 7, 3, 2, 8]. */
fun fractionDigits(progress: Double, count: Int = 8): IntArray {
    var scale = 1L
    repeat(count) { scale *= 10 }
    val n = (progress * scale).toLong().coerceIn(0, scale - 1)
    val s = n.toString().padStart(count, '0')
    return IntArray(count) { s[it] - '0' }
}

data class LifeSnapshot(
    val years: Int, val months: Int, val weeks: Int, val days: Int,
    val hours: Int, val minutes: Int, val seconds: Int, val millis: Int,
    val totalMonths: Long, val totalWeeks: Long, val totalDays: Long,
    val totalHours: Long, val totalMinutes: Long, val totalSeconds: Long, val totalMillis: Long,
    /** Share of the current birthday-to-birthday year that has passed, 0..1. */
    val yearProgress: Double,
    val daysToBirthday: Long,
)

object LifeMath {
    fun snapshot(birth: ZonedDateTime, now: ZonedDateTime = ZonedDateTime.now(birth.zone)): LifeSnapshot {
        val n = if (now.isBefore(birth)) birth else now

        val y = ChronoUnit.YEARS.between(birth, n)
        val b1 = birth.plusYears(y)
        val m = ChronoUnit.MONTHS.between(b1, n)
        val b2 = b1.plusMonths(m)
        val d = ChronoUnit.DAYS.between(b2, n)
        val b3 = b2.plusDays(d)
        val rem = Duration.between(b3, n)

        val last = birth.plusYears(y)
        val next = birth.plusYears(y + 1)
        val span = Duration.between(last, next).toMillis().coerceAtLeast(1)
        val done = Duration.between(last, n).toMillis()

        val total = Duration.between(birth, n)
        return LifeSnapshot(
            years = y.toInt(), months = m.toInt(), weeks = (d / 7).toInt(), days = (d % 7).toInt(),
            hours = rem.toHoursPart(), minutes = rem.toMinutesPart(),
            seconds = rem.toSecondsPart(), millis = rem.toMillisPart(),
            totalMonths = ChronoUnit.MONTHS.between(birth, n),
            totalWeeks = ChronoUnit.WEEKS.between(birth, n),
            totalDays = total.toDays(),
            totalHours = total.toHours(),
            totalMinutes = total.toMinutes(),
            totalSeconds = total.seconds,
            totalMillis = total.toMillis(),
            yearProgress = (done.toDouble() / span).coerceIn(0.0, 1.0),
            daysToBirthday = ChronoUnit.DAYS.between(n, next),
        )
    }
}

fun Long.grouped(): String = "%,d".format(this)
fun Int.pad(n: Int = 2): String = toString().padStart(n, '0')
fun Double.pct(decimals: Int = 2): String = "%.${decimals}f%%".format(this * 100)
