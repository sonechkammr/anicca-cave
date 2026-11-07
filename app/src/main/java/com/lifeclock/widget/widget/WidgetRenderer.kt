package com.lifeclock.widget.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.lifeclock.widget.AgeReading
import com.lifeclock.widget.Kind
import com.lifeclock.widget.LifeMath
import com.lifeclock.widget.MainActivity
import com.lifeclock.widget.Planets
import com.lifeclock.widget.Pulse
import com.lifeclock.widget.Stars
import com.lifeclock.widget.Trip
import com.lifeclock.widget.Prefs
import com.lifeclock.widget.R
import com.lifeclock.widget.WidgetConfig
import com.lifeclock.widget.ageReading
import com.lifeclock.widget.pct
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** [refreshMs] is how long this picture stays good. */
class Rendered(val views: RemoteViews, val flows: Boolean, val refreshMs: Long = if (flows) Tick.FLOW_DELAY_MS else Tick.STATIC_DELAY_MS)

/**
 * Builds the RemoteViews for one widget at its current size.
 *
 * The fast decimals are a ViewFlipper whose frames are snapshots of the age at t0, t0+step, t0+2*step...
 * Each frame is computed from one instant, so carries between digits stay correct. An inexact alarm
 * ([TickReceiver]) builds a fresh set of frames before the current one runs out.
 */
object WidgetRenderer {
    private const val ASPECT = 0.75f      // digit width / height
    private const val DOT = 0.25f         // decimal point width / height
    private const val GAP = 2f
    private const val MIN_DEC = 9f        // smallest readable decimal height, dp
    private const val GAP_WIDTH = 0.35f   // thin gap between thousands, as a share of the digit height
    private const val MALA_DAYS = 108
    private const val KM_FRAMES = 150
    private const val KM_STEP_MS = 950L
    private const val DIM_ALPHA = 150     // applied to the last two digits
    private const val FLOW_FROM = 6       // decimals from this one on change fast enough to animate

    private const val SKY_REFRESH_MS = 3 * 60 * 60 * 1000L   // planets crawl, a few hours is plenty
    private const val PAD_NUMBER = 8f
    private const val PAD_GRID = 12f
    private const val PAD_WIDE = 16f

    private val DOTS = intArrayOf(
        R.drawable.dm_0, R.drawable.dm_1, R.drawable.dm_2, R.drawable.dm_3, R.drawable.dm_4,
        R.drawable.dm_5, R.drawable.dm_6, R.drawable.dm_7, R.drawable.dm_8, R.drawable.dm_9,
    )
    private val CLEAN = intArrayOf(
        R.drawable.in_0, R.drawable.in_1, R.drawable.in_2, R.drawable.in_3, R.drawable.in_4,
        R.drawable.in_5, R.drawable.in_6, R.drawable.in_7, R.drawable.in_8, R.drawable.in_9,
    )
    private val SEGS = intArrayOf(
        R.drawable.sg_0, R.drawable.sg_1, R.drawable.sg_2, R.drawable.sg_3, R.drawable.sg_4,
        R.drawable.sg_5, R.drawable.sg_6, R.drawable.sg_7, R.drawable.sg_8, R.drawable.sg_9,
    )
    private val BACKGROUNDS = intArrayOf(
        R.drawable.bg_card_0, R.drawable.bg_card_10, R.drawable.bg_card_20, R.drawable.bg_card_30,
        R.drawable.bg_card_40, R.drawable.bg_card_50, R.drawable.bg_card_60, R.drawable.bg_card_70,
        R.drawable.bg_card_80, R.drawable.bg_card_90, R.drawable.bg_card_100,
    )

    private fun glyph(cfg: WidgetConfig, d: Int) = when (cfg.font) { 1 -> CLEAN; 2 -> SEGS; else -> DOTS }[d]
    private fun dotRes(cfg: WidgetConfig) = when (cfg.font) { 1 -> R.drawable.in_dot; 2 -> R.drawable.sg_dot; else -> R.drawable.dm_dot }

    /** Null means follow the system colours from XML. */
    private fun fixedColor(cfg: WidgetConfig): Int? = when (cfg.colorMode) {
        1 -> 0xFFFFFFFF.toInt()
        2 -> 0xFF121212.toInt()
        3 -> cfg.customColor or 0xFF000000.toInt()
        else -> null
    }

    /** Launcher cells for a size in dp: the platform uses size = 70 * cells - 30. */
    private fun cells(dp: Float) = ((dp + 30f) / 70f).toInt().coerceAtLeast(1)

    private class Plan(val stacked: Boolean, val big: Float, val small: Float, val decimals: Int)

    /** Chooses row or stacked layout and digit sizes. Drops decimals only when they'd be too small to read. */
    private fun plan(w: Float, h: Float, yearDigits: Int, cfg: WidgetConfig): Plan {
        val yw = ASPECT * yearDigits
        var p = cfg.decimals.coerceIn(1, 8)
        while (true) {
            val rowBig = min(h, w / (yw + DOT + ASPECT * 0.5f * p))
            val rowSmall = rowBig * 0.5f

            val smallMax = w / (ASPECT * p + DOT)
            val stackBig = min(w / yw, (h - GAP) / 1.45f)
            val stackSmall = min(min(smallMax, 0.45f * stackBig), h - GAP - stackBig).coerceAtLeast(1f)

            val stacked = when (cfg.layout) {
                1 -> false
                2 -> true
                else -> stackBig > rowBig * 1.3f
            }
            val big = if (stacked) stackBig else rowBig
            val small = if (stacked) stackSmall else rowSmall
            if (small >= MIN_DEC || p <= 1) return Plan(stacked, big, small, p)
            p--
        }
    }

    private fun styled(v: RemoteViews, id: Int, res: Int, cfg: WidgetConfig, dim: Boolean) {
        v.setImageViewResource(id, res)
        fixedColor(cfg)?.let { v.setColorStateList(id, "setImageTintList", ColorStateList.valueOf(it)) }
        if (dim) v.setInt(id, "setImageAlpha", DIM_ALPHA)
    }

    private fun digit(ctx: Context, cfg: WidgetConfig, res: Int, w: Float, h: Float, dim: Boolean): RemoteViews =
        RemoteViews(ctx.packageName, R.layout.digit).apply {
            styled(this, R.id.dg, res, cfg, dim)
            setViewLayoutWidth(R.id.dg, w, TypedValue.COMPLEX_UNIT_DIP)
            setViewLayoutHeight(R.id.dg, h, TypedValue.COMPLEX_UNIT_DIP)
        }

    /** Whole years, a point, then the decimals: still digits first, one flipper for the fast tail. */
    private fun buildNumber(
        ctx: Context, cfg: WidgetConfig, read: (Long) -> AgeReading, nowMs: Long, r0: AgeReading,
        w: Float, h: Float, light: Boolean,
    ): Pair<RemoteViews, Boolean> {
        val yearsStr = r0.years.toString()
        val plan = plan(w, h, yearsStr.length, cfg)
        val p = plan.decimals
        val dp = TypedValue.COMPLEX_UNIT_DIP

        val number = RemoteViews(ctx.packageName, if (plan.stacked) R.layout.number_stacked else R.layout.number_inline)
        val decRow = if (plan.stacked) R.id.row2 else R.id.row1

        for (ch in yearsStr) {
            number.addView(R.id.row1, digit(ctx, cfg, glyph(cfg, ch - '0'), plan.big * ASPECT, plan.big, false))
        }
        if (plan.stacked) {
            number.addView(decRow, digit(ctx, cfg, dotRes(cfg), plan.small * DOT, plan.small, false))
        } else {
            number.addView(decRow, digit(ctx, cfg, dotRes(cfg), plan.big * DOT, plan.big, false))
        }

        val flows = p >= FLOW_FROM && !light
        if (flows) {
            val step = when (p) { 8 -> 950L; 7 -> 3150L; else -> 31500L }
            val count = when (p) { 8 -> 150; 7 -> 48; else -> 5 }

            val frames = ArrayList(listOf(r0))
            for (i in 1 until count) {
                val r = read(nowMs + i * step)
                if (r.years != r0.years) break      // birthday falls inside the window, the next tick sorts it out
                frames.add(r)
            }
            // the flipper starts at the first decimal that actually changes in this window
            var first = FLOW_FROM
            for (r in frames) {
                for (k in 0 until p) if (r.digits[k] != r0.digits[k]) { first = min(first, k + 1); break }
            }
            val firstFlow = min(first, p)

            for (k in 1 until firstFlow) {
                number.addView(
                    decRow,
                    digit(ctx, cfg, glyph(cfg, r0.digits[k - 1]), plan.small * ASPECT, plan.small, k > p - 2),
                )
            }

            val flip = RemoteViews(
                ctx.packageName,
                when (p) { 8 -> R.layout.flip_p8; 7 -> R.layout.flip_p7; else -> R.layout.flip_p6 },
            )
            val n = p - firstFlow + 1
            flip.setViewLayoutWidth(R.id.flipper, plan.small * ASPECT * n, dp)
            flip.setViewLayoutHeight(R.id.flipper, plan.small, dp)
            for (r in frames) {
                val row = RemoteViews(ctx.packageName, R.layout.frame_row)
                for (k in firstFlow..p) {
                    val cell = RemoteViews(ctx.packageName, R.layout.digit_cell)
                    styled(cell, R.id.cell, glyph(cfg, r.digits[k - 1]), cfg, k > p - 2)
                    row.addView(R.id.frame_row, cell)
                }
                flip.addView(R.id.flipper, row)
            }
            number.addView(decRow, flip)
        } else {
            for (k in 1..p) {
                number.addView(
                    decRow,
                    digit(ctx, cfg, glyph(cfg, r0.digits[k - 1]), plan.small * ASPECT, plan.small, k > p - 2),
                )
            }
        }
        return number to flows
    }

    private fun px(ctx: Context, dp: Float) = (dp * ctx.resources.displayMetrics.density).toInt()

    /**
     * Shows the readability plate or hides it. It must always do one or the other: when a widget is redrawn the
     * launcher re-applies the actions on the view it already has, so a plate that was shown once stays on screen
     * unless it is explicitly hidden again.
     */
    private fun applyPlate(v: RemoteViews, ctx: Context, cfg: WidgetConfig, wDp: Float, hDp: Float, insetDp: Float, backdrop: Int?) {
        val argb = Contrast.scrim(ctx, cfg, backdrop)
        if (argb != null) {
            v.setImageViewBitmap(R.id.plate, Contrast.plate(ctx, wDp, hDp, insetDp, argb))
            v.setViewVisibility(R.id.plate, View.VISIBLE)
        } else {
            v.setViewVisibility(R.id.plate, View.GONE)
        }
    }
    /** A new raked-sand garden every day, from the date alone. */
    private fun renderGarden(
        ctx: Context, cfg: WidgetConfig, widthDp: Int, heightDp: Int, background: Int,
        open: PendingIntent, nowMs: Long,
    ): Rendered {
        val w = max(widthDp, 120).toFloat()
        val h = max(heightDp, 60).toFloat()
        val day = Instant.ofEpochMilli(nowMs).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
        val layers = Visuals.garden(ctx, w, h, day, natural = cfg.palette == 1)

        val v = RemoteViews(ctx.packageName, R.layout.widget_planets)
        v.setInt(R.id.root, "setBackgroundResource", background)
        v.setOnClickPendingIntent(R.id.root, open)
        v.setImageViewBitmap(R.id.vis, layers.base)
        v.setImageViewBitmap(R.id.vis_color, layers.bodies)
        fixedColor(cfg)?.let { v.setColorStateList(R.id.vis, "setImageTintList", ColorStateList.valueOf(it)) }
        v.setViewVisibility(R.id.plate, View.GONE)
        return Rendered(v, false, SKY_REFRESH_MS)
    }

    /**
     * Planets need no birth date, only the clock. With one, the 4x4 face shows how far each planet has gone along its own
     * orbit since your last birthday: a bold arc behind it (a full ring once it has made a whole lap) and the share next to
     * its name. Without one it falls back to the last month's track.
     */
    private fun renderSky(
        ctx: Context, cfg: WidgetConfig, birth: ZonedDateTime?, widthDp: Int, heightDp: Int, background: Int,
        open: PendingIntent, nowMs: Long, backdrop: Int?,
    ): Rendered {
        val w = max(widthDp, 60).toFloat()
        val h = max(heightDp, 60).toFloat()
        val detailed = min(w, h) >= 200f
        val longitudes = DoubleArray(Planets.COUNT) { Planets.position(it, nowMs).longitude }
        val sinceBirthday = if (detailed && birth != null) {
            val start = Trip.yearStartMs(birth, nowMs)
            DoubleArray(Planets.COUNT) { Planets.travelledSince(it, start, nowMs) }
        } else null
        val trails = sinceBirthday?.let { d -> DoubleArray(d.size) { min(d[it], 360.0) } }
            ?: DoubleArray(Planets.COUNT) { Planets.travelled(it, nowMs, 30) }
        val planetNames = ctx.resources.getStringArray(R.array.planet_names)
        val names = if (sinceBirthday == null) planetNames else Array(planetNames.size) {
            planetNames[it] + " " + orbitShare(sinceBirthday[it] / 360.0)
        }
        val date = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()).format(Instant.ofEpochMilli(nowMs).atZone(ZoneId.systemDefault()))
        val speed = "%.1f".format(Planets.speedKmS(Planets.EARTH, nowMs))
        val layers = Visuals.sky(
            ctx, w, h, longitudes, trails, detailed, cfg.palette == 1,
            names, ctx.getString(R.string.sky_footer, date, speed), legendInner = sinceBirthday != null,
        )

        val v = RemoteViews(ctx.packageName, R.layout.widget_planets)
        v.setInt(R.id.root, "setBackgroundResource", background)
        v.setOnClickPendingIntent(R.id.root, open)
        v.setImageViewBitmap(R.id.vis, layers.base)
        v.setImageViewBitmap(R.id.vis_color, layers.bodies)
        fixedColor(cfg)?.let { v.setColorStateList(R.id.vis, "setImageTintList", ColorStateList.valueOf(it)) }
        applyPlate(v, ctx, cfg, w, h, PAD_NUMBER / 2f, backdrop)
        return Rendered(v, false, SKY_REFRESH_MS)
    }

    /** "98%" for most; a decimal under 10 % so the slow outer planets do not all read "0%" or "1%". */
    private fun orbitShare(share: Double): String {
        val pct = share * 100.0
        return if (pct < 10.0) String.format(Locale.getDefault(), "%.1f%%", pct) else String.format(Locale.getDefault(), "%.0f%%", pct)
    }

    /**
     * A whole number with thin gaps between the thousands (kilometres, heartbeats, laps): head cells stay put, the tail that changes
     * inside the next couple of minutes is one flipper of pre-computed frames, like the age decimals.
     */
    private fun buildGrouped(
        ctx: Context, cfg: WidgetConfig, read: (Long) -> Long, nowMs: Long, w: Float, h: Float, light: Boolean,
    ): Pair<RemoteViews, Boolean> {
        val now = Trip.cells(read(nowMs))
        val digits = now.count { it >= 0 }
        val gaps = now.size - digits
        val dh = min(h, w / (digits * ASPECT + gaps * GAP_WIDTH))
        val dw = dh * ASPECT
        val gw = dh * GAP_WIDTH
        fun width(c: Int) = if (c < 0) gw else dw
        val dp = TypedValue.COMPLEX_UNIT_DIP

        val frames = ArrayList<IntArray>()
        frames.add(now)
        if (!light) {
            for (i in 1 until KM_FRAMES) {
                val f = Trip.cells(read(nowMs + i * KM_STEP_MS))
                if (f.size != now.size) break
                frames.add(f)
            }
        }
        var first = now.size
        for (f in frames) for (k in now.indices) if (f[k] != now[k]) { first = min(first, k); break }
        val flows = first < now.size

        val number = RemoteViews(ctx.packageName, R.layout.number_inline)
        for (k in 0 until first) {
            number.addView(R.id.row1, groupedCell(ctx, cfg, now[k], width(now[k]), dh, dim = false))
        }
        if (flows) {
            val flip = RemoteViews(ctx.packageName, R.layout.flip_p8)
            flip.setViewLayoutWidth(R.id.flipper, (first until now.size).sumOf { width(now[it]).toDouble() }.toFloat(), dp)
            flip.setViewLayoutHeight(R.id.flipper, dh, dp)
            for (f in frames) {
                val row = RemoteViews(ctx.packageName, R.layout.frame_row)
                for (k in first until f.size) {
                    val cell = RemoteViews(ctx.packageName, R.layout.digit_cell)
                    if (f[k] >= 0) styled(cell, R.id.cell, glyph(cfg, f[k]), cfg, dim = k >= f.size - 2)
                    cell.setViewLayoutWidth(R.id.cell, width(f[k]), dp)
                    row.addView(R.id.frame_row, cell)
                }
                flip.addView(R.id.flipper, row)
            }
            number.addView(R.id.row1, flip)
        }
        return number to flows
    }

    private fun groupedCell(ctx: Context, cfg: WidgetConfig, cell: Int, w: Float, h: Float, dim: Boolean): RemoteViews =
        RemoteViews(ctx.packageName, R.layout.digit).apply {
            if (cell >= 0) styled(this, R.id.dg, glyph(cfg, cell), cfg, dim)
            setViewLayoutWidth(R.id.dg, w, TypedValue.COMPLEX_UNIT_DIP)
            setViewLayoutHeight(R.id.dg, h, TypedValue.COMPLEX_UNIT_DIP)
        }

    /** Kilometres around the Sun since birth, the orbit and this year's share of it, "km" under the number. */
    private fun renderPath(
        ctx: Context, cfg: WidgetConfig, birth: ZonedDateTime, widthDp: Int, heightDp: Int, background: Int,
        open: PendingIntent, nowMs: Long, light: Boolean, backdrop: Int?,
    ): Rendered {
        val w = max(widthDp, 60).toFloat()
        val h = max(heightDp, 60).toFloat()
        val innerW = w - 2 * PAD_GRID
        val innerH = h - 2 * PAD_GRID
        val unitH = 14f

        val km = Trip.kmAt(birth, nowMs)
        val speed = Planets.speedKmS(Planets.EARTH, nowMs)
        val read = { ms: Long -> (km + speed * (ms - nowMs) / 1000.0).toLong() }
        val numberH = min(innerW / (11 * ASPECT + 3 * GAP_WIDTH), innerH * 0.3f)
        val artH = max(innerH - numberH - unitH - 8f, 20f)

        val longitude = Planets.position(Planets.EARTH, nowMs).longitude
        var travelled = longitude - Trip.yearStartLongitude(birth, nowMs)
        if (travelled < 0) travelled += 360.0
        val layers = Visuals.trip(ctx, innerW, artH, longitude, travelled)

        val v = RemoteViews(ctx.packageName, R.layout.widget_path)
        v.setInt(R.id.root, "setBackgroundResource", background)
        v.setOnClickPendingIntent(R.id.root, open)
        v.setViewPadding(R.id.content, px(ctx, PAD_GRID), px(ctx, PAD_GRID), px(ctx, PAD_GRID), px(ctx, PAD_GRID))
        v.setViewLayoutHeight(R.id.art, artH, TypedValue.COMPLEX_UNIT_DIP)
        v.setImageViewBitmap(R.id.vis, layers.base)
        v.setImageViewBitmap(R.id.vis_color, layers.bodies)
        fixedColor(cfg)?.let { v.setColorStateList(R.id.vis, "setImageTintList", ColorStateList.valueOf(it)) }
        v.setViewLayoutHeight(R.id.number_host, numberH, TypedValue.COMPLEX_UNIT_DIP)
        val (number, flows) = buildGrouped(ctx, cfg, read, nowMs, innerW, numberH, light)
        v.removeAllViews(R.id.number_host)
        v.addView(R.id.number_host, number)
        v.setTextViewText(R.id.unit, ctx.getString(R.string.unit_km))
        fixedColor(cfg)?.let { v.setTextColor(R.id.unit, (it and 0x00FFFFFF) or (0xB8 shl 24)) }
        applyPlate(v, ctx, cfg, w, h, PAD_GRID / 2f, backdrop)
        return Rendered(v, flows)
    }
    /** 108 beads, one a day: the ring of the current lap with the number of finished laps in the middle. */
    private fun renderMala(
        ctx: Context, cfg: WidgetConfig, birth: ZonedDateTime, widthDp: Int, heightDp: Int, background: Int,
        open: PendingIntent, nowMs: Long, backdrop: Int?,
    ): Rendered {
        val w = max(widthDp, 60).toFloat()
        val h = max(heightDp, 60).toFloat()
        val today = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMs), birth.zone).toLocalDate()
        val days = max(java.time.temporal.ChronoUnit.DAYS.between(birth.toLocalDate(), today), 0L)
        val laps = days / MALA_DAYS
        val dayInLap = (days % MALA_DAYS).toInt()
        val layers = Visuals.mala(ctx, w, h, dayInLap)

        val ring = min(w, h) / 2f - 6f
        val inner = (ring * 2f - 18f).coerceAtLeast(30f)
        val digits = laps.toString().length
        val numberH = min(inner * 0.4f, inner * 0.8f / (digits * ASPECT))

        val v = RemoteViews(ctx.packageName, R.layout.widget_mala)
        v.setInt(R.id.root, "setBackgroundResource", background)
        v.setOnClickPendingIntent(R.id.root, open)
        v.setImageViewBitmap(R.id.vis, layers.base)
        v.setImageViewBitmap(R.id.vis_color, layers.bodies)
        fixedColor(cfg)?.let { v.setColorStateList(R.id.vis, "setImageTintList", ColorStateList.valueOf(it)) }
        v.setViewLayoutWidth(R.id.center, inner, TypedValue.COMPLEX_UNIT_DIP)
        v.setViewLayoutHeight(R.id.number_host, numberH, TypedValue.COMPLEX_UNIT_DIP)
        val (number, _) = buildGrouped(ctx, cfg, { laps }, nowMs, inner * 0.8f, numberH, light = true)
        v.removeAllViews(R.id.number_host)
        v.addView(R.id.number_host, number)
        v.setTextViewText(R.id.caption1, ctx.resources.getQuantityString(R.plurals.mala_laps, laps.toInt().coerceAtMost(Int.MAX_VALUE), laps))
        v.setTextViewText(R.id.caption2, ctx.getString(R.string.mala_day, dayInLap + 1, MALA_DAYS))
        fixedColor(cfg)?.let {
            val soft = (it and 0x00FFFFFF) or (0xB8 shl 24)
            v.setTextColor(R.id.caption1, soft)
            v.setTextColor(R.id.caption2, soft)
        }
        applyPlate(v, ctx, cfg, w, h, PAD_NUMBER / 2f, backdrop)
        return Rendered(v, false, SKY_REFRESH_MS)
    }

    /**
     * A trace and, under it, how many heartbeats are left on average (70 a minute against a lifetime of 2.5 billion),
     * with the single word "left" or "ещё". The count runs down to the last digit, like the age decimals; it is an average
     * over people, so it says "on average" in the description and never claims to be a forecast.
     */
    private fun renderPulse(
        ctx: Context, cfg: WidgetConfig, birth: ZonedDateTime, widthDp: Int, heightDp: Int, background: Int,
        open: PendingIntent, nowMs: Long, light: Boolean, backdrop: Int?,
    ): Rendered {
        val w = max(widthDp, 60).toFloat()
        val h = max(heightDp, 60).toFloat()
        val innerW = w - 2 * PAD_GRID
        val innerH = h - 2 * PAD_GRID
        val beats = Pulse.beatsAt(birth, nowMs)
        val numberH = min(innerW / (10 * ASPECT + 3 * GAP_WIDTH), innerH * 0.3f)
        val textH = 14f
        val artH = max(innerH - numberH - textH - 8f, 20f)
        val layers = Visuals.pulse(ctx, innerW, artH, Pulse.share(beats))

        val v = RemoteViews(ctx.packageName, R.layout.widget_pulse)
        v.setInt(R.id.root, "setBackgroundResource", background)
        v.setOnClickPendingIntent(R.id.root, open)
        v.setViewPadding(R.id.content, px(ctx, PAD_GRID), px(ctx, PAD_GRID), px(ctx, PAD_GRID), px(ctx, PAD_GRID))
        v.setViewLayoutHeight(R.id.art, artH, TypedValue.COMPLEX_UNIT_DIP)
        v.setImageViewBitmap(R.id.vis, layers.base)
        v.setImageViewBitmap(R.id.vis_color, layers.bodies)
        v.removeAllViews(R.id.ecg)
        val sweep = Visuals.pulseFrames(innerW, artH)
        for (frame in if (light) sweep.take(1) else sweep) {
            val f = RemoteViews(ctx.packageName, R.layout.ecg_frame)
            f.setImageViewBitmap(R.id.frame, frame)
            v.addView(R.id.ecg, f)
        }
        fixedColor(cfg)?.let { v.setColorStateList(R.id.vis, "setImageTintList", ColorStateList.valueOf(it)) }
        v.setViewLayoutHeight(R.id.number_host, numberH, TypedValue.COMPLEX_UNIT_DIP)
        val (number, flows) = buildGrouped(
            ctx, cfg, { ms -> Pulse.leftOnAverage(Pulse.beatsAt(birth, ms)) }, nowMs, innerW, numberH, light,
        )
        v.removeAllViews(R.id.number_host)
        v.addView(R.id.number_host, number)
        v.setTextViewText(R.id.unit, ctx.getString(R.string.pulse_left_word))
        fixedColor(cfg)?.let { v.setTextColor(R.id.unit, (it and 0x00FFFFFF) or (0xB8 shl 24)) }
        applyPlate(v, ctx, cfg, w, h, PAD_GRID / 2f, backdrop)
        return Rendered(v, flows)
    }

    /** The light that left Earth on your birth day: how far it got and which star it reaches next. */
    private fun renderLight(
        ctx: Context, cfg: WidgetConfig, birth: ZonedDateTime, widthDp: Int, heightDp: Int, background: Int,
        open: PendingIntent, nowMs: Long, backdrop: Int?,
    ): Rendered {
        val w = max(widthDp, 60).toFloat()
        val h = max(heightDp, 60).toFloat()
        val age = Stars.lightYears(birth, nowMs)
        val next = Stars.nextIndex(age)
        val names = ctx.resources.getStringArray(R.array.star_names)
        val line1 = if (next >= 0) ctx.getString(R.string.light_next, names[next]) else ctx.getString(R.string.light_none)
        val line2 = if (next >= 0) ctx.getString(R.string.light_in, "%.1f".format(Stars.ALL[next].ly - age)) else ""
        val layers = Visuals.light(ctx, w, h, age, max(age * 1.35, 12.0), next, line1, line2)

        val v = RemoteViews(ctx.packageName, R.layout.widget_planets)
        v.setInt(R.id.root, "setBackgroundResource", background)
        v.setOnClickPendingIntent(R.id.root, open)
        v.setImageViewBitmap(R.id.vis, layers.base)
        v.setImageViewBitmap(R.id.vis_color, layers.bodies)
        fixedColor(cfg)?.let { v.setColorStateList(R.id.vis, "setImageTintList", ColorStateList.valueOf(it)) }
        applyPlate(v, ctx, cfg, w, h, PAD_NUMBER / 2f, backdrop)
        return Rendered(v, false, SKY_REFRESH_MS)
    }

    fun render(
        ctx: Context,
        widgetId: Int,
        widthDp: Int,
        heightDp: Int,
        def: Kind,
        override: WidgetConfig? = null,
        nowMs: Long = System.currentTimeMillis(),
        light: Boolean = false,                 // skip the heavy frame set, e.g. while the user drags a resize handle
        backdrop: Int? = null,                  // previews only: stand-in wallpaper colour for the contrast check
        birthOverride: ZonedDateTime? = null,   // picker previews use a sample date when none is saved
    ): Rendered {
        val cfg = override ?: Prefs.widgetConfig(ctx, widgetId, def)
        val background = BACKGROUNDS[(cfg.opacity / 10).coerceIn(0, 10)]
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (cfg.kind == Kind.GARDEN) return renderGarden(ctx, cfg, widthDp, heightDp, background, open, nowMs)
        val birth = birthOverride ?: Prefs.birth(ctx)
        if (cfg.kind == Kind.PLANETS) return renderSky(ctx, cfg, birth, widthDp, heightDp, background, open, nowMs, backdrop)
        if (birth == null) {
            val e = RemoteViews(ctx.packageName, R.layout.widget_empty).apply {
                setInt(R.id.root, "setBackgroundResource", background)
                setOnClickPendingIntent(R.id.root, open)
            }
            return Rendered(e, false)
        }

        if (cfg.kind == Kind.PATH) return renderPath(ctx, cfg, birth, widthDp, heightDp, background, open, nowMs, light, backdrop)
        if (cfg.kind == Kind.MALA) return renderMala(ctx, cfg, birth, widthDp, heightDp, background, open, nowMs, backdrop)
        if (cfg.kind == Kind.PULSE) return renderPulse(ctx, cfg, birth, widthDp, heightDp, background, open, nowMs, light, backdrop)
        if (cfg.kind == Kind.LIGHT) return renderLight(ctx, cfg, birth, widthDp, heightDp, background, open, nowMs, backdrop)

        val read = { ms: Long -> ageReading(birth, ms) }
        val r0 = read(nowMs)
        val s = LifeMath.snapshot(birth, ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMs), birth.zone))
        val kind = cfg.kind
        val pad = when (kind) { Kind.NUMBER -> PAD_NUMBER; Kind.GRID -> PAD_GRID; else -> PAD_WIDE }
        val innerW = max(widthDp.toFloat(), 60f) - 2 * pad
        val innerH = max(heightDp.toFloat(), 30f) - 2 * pad
        val yi = yearInfo(birth, nowMs)

        val layout = when (kind) { Kind.NUMBER -> R.layout.widget_minimal; Kind.GRID -> R.layout.widget_grid; else -> R.layout.widget_detailed }
        val v = RemoteViews(ctx.packageName, layout)
        v.setInt(R.id.root, "setBackgroundResource", background)
        v.setOnClickPendingIntent(R.id.root, open)
        v.setViewPadding(R.id.content, px(ctx, pad), px(ctx, pad), px(ctx, pad), px(ctx, pad))
        val soft = fixedColor(cfg)?.let { (it and 0x00FFFFFF) or (0xB8 shl 24) }
        val tint = fixedColor(cfg)?.let { ColorStateList.valueOf(it) }
        var flows = false

        when (kind) {
            Kind.NUMBER -> {
                val (number, f) = buildNumber(ctx, cfg, read, nowMs, r0, innerW, innerH, light)
                flows = f
                v.removeAllViews(R.id.number_host)
                v.addView(R.id.number_host, number)
            }

            Kind.GRID -> {
                val capH = if (innerH >= 110f) 22f else 18f
                val gridH = max(innerH - capH - 4f, 20f)
                v.setImageViewBitmap(R.id.grid, Visuals.lifeGrid(ctx, innerW, gridH, r0.years, yi.progress, cfg.horizon))
                v.setViewLayoutWidth(R.id.grid, innerW, TypedValue.COMPLEX_UNIT_DIP)
                v.setViewLayoutHeight(R.id.grid, gridH, TypedValue.COMPLEX_UNIT_DIP)
                tint?.let { v.setColorStateList(R.id.grid, "setImageTintList", it) }
                v.setViewLayoutHeight(R.id.number_host, capH, TypedValue.COMPLEX_UNIT_DIP)
                val (number, f) = buildNumber(ctx, cfg.copy(layout = 1), read, nowMs, r0, innerW, capH, light)
                flows = f
                v.removeAllViews(R.id.number_host)
                v.addView(R.id.number_host, number)
            }

            Kind.PLANETS, Kind.PATH, Kind.LIGHT, Kind.MALA, Kind.PULSE, Kind.GARDEN -> Unit    // returned earlier

            Kind.RULER, Kind.DOTS -> {
                // launcher cell counts decide how much fits
                val rows = cells(heightDp.toFloat())
                val cols = cells(widthDp.toFloat())
                var visH = when (rows) { 1 -> 28f; 2 -> 40f; else -> 52f }
                var foot = rows >= 2
                fun topH() = innerH - visH - (if (foot) 18f else 0f) - 6f
                if (topH() < 30f && foot) foot = false
                if (topH() < 30f && visH > 28f) visH = 28f
                val topH = max(topH(), 18f)
                val showAge = cols >= 4

                v.setViewLayoutHeight(R.id.top, topH, TypedValue.COMPLEX_UNIT_DIP)
                v.setViewLayoutHeight(R.id.visual, visH, TypedValue.COMPLEX_UNIT_DIP)
                v.setViewVisibility(R.id.foot, if (foot) View.VISIBLE else View.GONE)
                v.setViewVisibility(R.id.age_text, if (showAge) View.VISIBLE else View.GONE)

                val numW = innerW - (if (showAge) 78f else 0f)
                val (number, f) = buildNumber(ctx, cfg.copy(layout = 1), read, nowMs, r0, numW, topH, light)
                flows = f
                v.removeAllViews(R.id.number_host)
                v.addView(R.id.number_host, number)

                if (kind == Kind.RULER) {
                    v.setImageViewBitmap(R.id.vis, Visuals.ruler(ctx, innerW, visH, yi))
                    v.setViewVisibility(R.id.needle, View.VISIBLE)
                    v.setViewVisibility(R.id.acc, View.GONE)
                    v.setTextViewText(R.id.foot, ctx.getString(R.string.foot_elapsed, s.yearProgress.pct(2)))
                } else {
                    val (dots, acc) = Visuals.yearDots(ctx, innerW, visH, yi)
                    v.setImageViewBitmap(R.id.vis, dots)
                    v.setImageViewBitmap(R.id.acc, acc)
                    v.setViewVisibility(R.id.needle, View.GONE)
                    v.setViewVisibility(R.id.acc, View.VISIBLE)
                    v.setTextViewText(R.id.foot, ctx.getString(R.string.foot_day, yi.day + 1, yi.total))
                }
                tint?.let { v.setColorStateList(R.id.vis, "setImageTintList", it) }
                v.setTextViewText(R.id.age_text, ctx.getString(R.string.age_short, s.months, s.weeks, s.days))
                soft?.let {
                    v.setTextColor(R.id.age_text, it)
                    v.setTextColor(R.id.foot, it)
                }
            }
        }

        applyPlate(v, ctx, cfg, widthDp.toFloat(), heightDp.toFloat(), pad / 2f, backdrop)

        val birthday = r0.years > 0 &&
            ZonedDateTime.now(birth.zone).toLocalDate() == birth.plusYears(r0.years.toLong()).toLocalDate()
        v.setViewVisibility(R.id.cake, if (birthday) View.VISIBLE else View.GONE)
        return Rendered(v, flows)
    }
}
