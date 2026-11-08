package com.lifeclock.widget.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.lifeclock.widget.Garden
import com.lifeclock.widget.Planets
import com.lifeclock.widget.R
import com.lifeclock.widget.Stars
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Where "now" sits inside the current birthday-to-birthday year. */
class YearInfo(
    val years: Int,
    val total: Int,                    // days in this year of life: 365 or 366
    val pos: Double,                   // days elapsed, fractional
    val monthMarks: Map<Int, Int>,     // day offset -> month index (0 = January)
) {
    val day: Int get() = floor(pos).toInt().coerceIn(0, total - 1)
    val progress: Double get() = (pos / total).coerceIn(0.0, 1.0)
}

fun yearInfo(birth: ZonedDateTime, epochMs: Long): YearInfo {
    var n = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMs), birth.zone)
    if (n.isBefore(birth)) n = birth
    val y = ChronoUnit.YEARS.between(birth, n).toInt()
    val last = birth.plusYears(y.toLong())
    val next = birth.plusYears(y + 1L)
    val total = ChronoUnit.DAYS.between(last.toLocalDate(), next.toLocalDate()).toInt().coerceAtLeast(1)
    val pos = Duration.between(last, n).toMillis() / 86_400_000.0
    val marks = HashMap<Int, Int>()
    for (m in 0..11) {
        val d = last.plusMonths(m.toLong())
        marks[ChronoUnit.DAYS.between(last.toLocalDate(), d.toLocalDate()).toInt()] = d.monthValue - 1
    }
    return YearInfo(y, total, pos, marks)
}

/**
 * Bitmaps for the picture-based faces. Everything is drawn white with varying alpha;
 * the ImageView tint turns it into the real colour, same as the vector digits.
 */
object Visuals {
    private class Surface(val bitmap: Bitmap, val canvas: Canvas, val density: Float)

    private fun surface(ctx: Context, wDp: Float, hDp: Float): Surface {
        val d = ctx.resources.displayMetrics.density
        val b = Bitmap.createBitmap(
            (wDp * d).toInt().coerceAtLeast(2),
            (hDp * d).toInt().coerceAtLeast(2),
            Bitmap.Config.ARGB_8888,
        )
        return Surface(b, Canvas(b), d)
    }

    private fun paint(alpha: Int = 255, stroke: Float = 0f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        this.alpha = alpha
        if (stroke > 0f) {
            style = Paint.Style.STROKE
            strokeWidth = stroke
        }
    }

    /** One tick per day with today pinned at the centre: past ticks bright, future ones dim. */
    fun ruler(ctx: Context, wDp: Float, hDp: Float, yi: YearInfo): Bitmap {
        val s = surface(ctx, wDp, hDp)
        val c = s.canvas
        val d = s.density
        val w = s.bitmap.width.toFloat()
        val h = s.bitmap.height.toFloat()
        val pitch = 5f * d
        val months = ctx.resources.getStringArray(R.array.months_short)
        val showLabels = hDp >= 36f
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            alpha = 200
            textSize = 8f * d
            letterSpacing = 0.08f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }

        val layer = c.saveLayer(0f, 0f, w, h, null)
        for (i in 0..yi.total) {
            val x = w / 2f + (i - yi.pos.toFloat()) * pitch
            if (x < -pitch || x > w + pitch) continue
            val month = yi.monthMarks[i]
            val len = h * when {
                month != null -> 0.55f
                i % 7 == 0 -> 0.34f
                else -> 0.2f
            }
            val p = paint(if (i <= yi.pos) 255 else 80, (if (month != null) 1.6f else 1f) * d)
            c.drawLine(x, 0f, x, len, p)
            if (month != null && showLabels) c.drawText(months[month], x + 3f * d, h * 0.9f, label)
        }
        // fade both ends so the tape doesn't stop dead at the edge
        val fade = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            shader = LinearGradient(
                0f, 0f, w, 0f,
                intArrayOf(0, -1, -1, 0), floatArrayOf(0f, 0.14f, 0.86f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        c.drawRect(0f, 0f, w, h, fade)
        c.restoreToCount(layer)
        return s.bitmap
    }

    /** 7 rows by ~53 columns, a dot per day. Returns the dots and, separately, today's red dot. */
    fun yearDots(ctx: Context, wDp: Float, hDp: Float, yi: YearInfo): Pair<Bitmap, Bitmap> {
        val dots = surface(ctx, wDp, hDp)
        val today = surface(ctx, wDp, hDp)
        val d = dots.density
        val cols = ceil(yi.total / 7.0).toInt()
        val pitch = min(wDp / cols, hDp / 7f) * d
        val ox = (dots.bitmap.width - pitch * cols) / 2f
        val oy = (dots.bitmap.height - pitch * 7) / 2f
        val r = pitch * 0.36f
        val todayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE5242B.toInt() }
        for (i in 0 until yi.total) {
            val cx = ox + (i / 7) * pitch + pitch / 2f
            val cy = oy + (i % 7) * pitch + pitch / 2f
            when {
                i < yi.day -> dots.canvas.drawCircle(cx, cy, r, paint(255))
                i > yi.day -> dots.canvas.drawCircle(cx, cy, r, paint(46))
                else -> today.canvas.drawCircle(cx, cy, r * 1.3f, todayPaint)
            }
        }
        return dots.bitmap to today.bitmap
    }

    /**
     * [base] gets the orbits, labels, trails and, in system colours, the planets; it is tinted by the widget.
     * [bodies] is drawn untinted: just Earth in system colours, every body in planet colours.
     */
    class SkyLayers(val base: Bitmap, val bodies: Bitmap)

    // fraction of the outer radius, spaced on a compressed (roughly logarithmic) scale
    private val ORBIT = floatArrayOf(0.167f, 0.286f, 0.345f, 0.429f, 0.667f, 0.786f, 0.917f, 1f)
    private val BODY_DP = floatArrayOf(2f, 2.4f, 2.6f, 2.2f, 3.6f, 3.2f, 2.6f, 2.6f)
    private val BODY_COLOR = intArrayOf(
        0xFFB4B2A9.toInt(), 0xFFE8D5A0.toInt(), 0xFF378ADD.toInt(), 0xFFD85A30.toInt(),
        0xFFC99A66.toInt(), 0xFFE8D08A.toInt(), 0xFF7FD1D1.toInt(), 0xFF6C7BE0.toInt(),
    )
    private const val SUN_COLOR = 0xFFEF9F27.toInt()
    private const val EARTH_RED = 0xFFE5242B.toInt()
    private const val TRAIL_ALPHA = 90

    /** The solar system from above, [longitudes] and [trails] in degrees for each planet. */
    fun sky(
        ctx: Context, wDp: Float, hDp: Float, longitudes: DoubleArray, trails: DoubleArray,
        detailed: Boolean, planetColors: Boolean, names: Array<String>, footer: String,
    ): SkyLayers {
        val base = surface(ctx, wDp, hDp)
        val bodies = surface(ctx, wDp, hDp)
        val d = base.density
        val footerH = if (detailed) 24f else 0f
        val cx = wDp / 2f * d
        val cy = (hDp - footerH) / 2f * d
        val outer = (minOf(wDp, hDp - footerH) / 2f - if (detailed) 14f else 8f) * d
        val scale = if (detailed) 1.3f else 1f

        val ring = paint(90, 0.8f * d)
        for (f in ORBIT) base.canvas.drawCircle(cx, cy, outer * f, ring)

        fun pos(i: Int, extra: Float = 0f): Pair<Float, Float> {
            val a = Math.toRadians(longitudes[i])
            val r = outer * ORBIT[i] + extra
            return (cx + r * Math.cos(a)).toFloat() to (cy - r * Math.sin(a)).toFloat()
        }

        if (detailed) {
            for (i in ORBIT.indices) {
                val r = outer * ORBIT[i]
                val box = RectF(cx - r, cy - r, cx + r, cy + r)
                // The trail has the planet's own colour: Earth red and, with planet colours, every planet in its colour
                // (those go on the untinted layer); otherwise it is the widget tint like the planet itself.
                val untinted = planetColors || i == Planets.EARTH
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = when { planetColors -> BODY_COLOR[i]; i == Planets.EARTH -> EARTH_RED; else -> 0xFFFFFFFF.toInt() }
                    alpha = TRAIL_ALPHA
                    style = Paint.Style.STROKE
                    strokeWidth = 3f * d
                    strokeCap = Paint.Cap.BUTT   // round caps would show as dark dots where passes meet
                }
                val canvas = if (untinted) bodies.canvas else base.canvas
                // One translucent pass per lap, all ending at the planet: places it has crossed more often get darker.
                var left = trails[i]
                while (left > 0.0) {
                    canvas.drawArc(box, (-longitudes[i]).toFloat(), minOf(left, 360.0).toFloat(), false, p)
                    left -= 360.0
                }
            }
        }

        // Sun
        val sunR = (if (detailed) 5.5f else 4.2f) * d
        if (planetColors) bodies.canvas.drawCircle(cx, cy, sunR, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SUN_COLOR })
        else base.canvas.drawCircle(cx, cy, sunR, paint(255))

        for (i in ORBIT.indices) {
            val (x, y) = pos(i)
            val r = BODY_DP[i] * scale * d
            val fill = Paint(Paint.ANTI_ALIAS_FLAG)
            when {
                planetColors -> { fill.color = BODY_COLOR[i]; bodies.canvas.drawCircle(x, y, r, fill) }
                i == Planets.EARTH -> { fill.color = EARTH_RED; bodies.canvas.drawCircle(x, y, r * 1.15f, fill) }
                else -> base.canvas.drawCircle(x, y, r, paint(255))
            }
        }

        if (detailed) {
            val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt(); alpha = 210; textSize = 10f * d
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            }
            val taken = ArrayList<RectF>()
            val w = wDp * d
            for (i in Planets.EARTH until ORBIT.size) {
                val label = names[i]
                val tw = text.measureText(label)
                val th = text.textSize
                val (px, py) = pos(i)
                val gap = (BODY_DP[i] * scale + 3f) * d
                // beside the planet on the roomier side first, then the other side, then a little above or below
                val xs = if (px >= cx) listOf(px + gap, px - gap - tw) else listOf(px - gap - tw, px + gap)
                var placed: RectF? = null
                search@ for (dy in listOf(0f, -th * 0.95f, th * 0.95f)) for (left in xs) {
                    if (left < 2f * d || left + tw > w - 2f * d) continue
                    val box = RectF(left, py + dy - th * 0.5f, left + tw, py + dy + th * 0.5f)
                    if (taken.none { RectF.intersects(it, box) }) { placed = box; break@search }
                }
                if (placed != null) {
                    taken.add(placed)
                    base.canvas.drawText(label, placed.left, placed.centerY() + th * 0.35f, text)
                }
            }
            text.textAlign = Paint.Align.CENTER
            text.alpha = 200
            text.textSize = 10f * d
            base.canvas.drawText(footer, cx, hDp * d - 7f * d, text)
        }
        return SkyLayers(base.bitmap, bodies.bitmap)
    }

    /** Earth's orbit with the part travelled in the current year of life drawn bold. */
    fun trip(ctx: Context, wDp: Float, hDp: Float, longitude: Double, travelled: Double): SkyLayers {
        val base = surface(ctx, wDp, hDp)
        val bodies = surface(ctx, wDp, hDp)
        val d = base.density
        val cx = wDp / 2f * d
        val cy = hDp / 2f * d
        val r = (minOf(wDp, hDp) / 2f - 6f) * d
        val box = RectF(cx - r, cy - r, cx + r, cy + r)

        base.canvas.drawCircle(cx, cy, r, paint(70, 1f * d))
        base.canvas.drawArc(box, (-longitude).toFloat(), travelled.toFloat(), false, paint(255, 2.6f * d).apply { strokeCap = Paint.Cap.ROUND })
        base.canvas.drawCircle(cx, cy, 3.6f * d, paint(255))

        val a = Math.toRadians(longitude)
        bodies.canvas.drawCircle(
            (cx + r * Math.cos(a)).toFloat(), (cy - r * Math.sin(a)).toFloat(), 4f * d,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = EARTH_RED },
        )

        return SkyLayers(base.bitmap, bodies.bitmap)
    }

    /**
     * Light from your birth day as a red ring, the stars it has passed filled in, the rest hollow.
     * Stars sit at their distance and their right ascension, seen from above the pole.
     */
    fun light(
        ctx: Context, wDp: Float, hDp: Float, ageLy: Double, rangeLy: Double, next: Int,
        line1: String, line2: String,
    ): SkyLayers {
        val base = surface(ctx, wDp, hDp)
        val bodies = surface(ctx, wDp, hDp)
        val d = base.density
        val captionH = 38f
        val cx = wDp / 2f * d
        val cy = (hDp - captionH) / 2f * d
        val r = (minOf(wDp, hDp - captionH) / 2f - 6f) * d

        base.canvas.drawCircle(cx, cy, r, paint(60, 0.8f * d))
        for ((i, star) in Stars.ALL.withIndex()) {
            if (star.ly > rangeLy) continue
            val a = Math.toRadians(star.raHours * 15.0)
            val sr = (star.ly / rangeLy * r).toFloat()
            val x = (cx + sr * Math.cos(a)).toFloat()
            val y = (cy - sr * Math.sin(a)).toFloat()
            when {
                star.ly <= ageLy -> base.canvas.drawCircle(x, y, 2.2f * d, paint(255))
                i == next -> base.canvas.drawCircle(x, y, 2.8f * d, paint(255, 1.2f * d))
                else -> base.canvas.drawCircle(x, y, 1.6f * d, paint(110))
            }
        }

        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = EARTH_RED; style = Paint.Style.STROKE; strokeWidth = 1.6f * d
        }
        bodies.canvas.drawCircle(cx, cy, (ageLy / rangeLy * r).toFloat(), ring)
        bodies.canvas.drawCircle(cx, cy, 2.6f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = EARTH_RED })

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt(); alpha = 210; textSize = 10f * d; textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val bottom = hDp * d
        base.canvas.drawText(line1, cx, bottom - 24f * d, text)
        text.alpha = 160
        base.canvas.drawText(line2, cx, bottom - 11f * d, text)
        return SkyLayers(base.bitmap, bodies.bitmap)
    }

    /**
     * A mala: a larger guru bead at the top, then 108 beads clockwise. [passed] beads are behind you,
     * the next one, drawn in red on [SkyLayers.bodies], is today.
     */
    fun mala(ctx: Context, wDp: Float, hDp: Float, passed: Int): SkyLayers {
        val base = surface(ctx, wDp, hDp)
        val bodies = surface(ctx, wDp, hDp)
        val d = base.density
        val cx = wDp / 2f * d
        val cy = hDp / 2f * d
        val r = (minOf(wDp, hDp) / 2f - 6f) * d
        val step = 360.0 / 109
        fun at(i: Int): Pair<Float, Float> {
            val a = Math.toRadians(-90.0 + i * step)
            return (cx + r * Math.cos(a)).toFloat() to (cy + r * Math.sin(a)).toFloat()
        }
        val (gx, gy) = at(0)
        base.canvas.drawCircle(gx, gy, 3.4f * d, paint(255))
        for (i in 1..108) {
            val (x, y) = at(i)
            when {
                i <= passed -> base.canvas.drawCircle(x, y, 1.7f * d, paint(255))
                i == passed + 1 -> bodies.canvas.drawCircle(x, y, 2.9f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = EARTH_RED })
                else -> base.canvas.drawCircle(x, y, 1.5f * d, paint(80))
            }
        }
        return SkyLayers(base.bitmap, bodies.bitmap)
    }

    /** A stroke that is [alphaLeft] opaque at x = 0 and [alphaRight] at x = [w]; optionally blurred into a glow. */
    private fun glowPaint(color: Int, widthPx: Float, blurPx: Float, w: Float, alphaLeft: Int, alphaRight: Int) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = widthPx
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            shader = LinearGradient(
                0f, 0f, w, 0f,
                (color and 0x00FFFFFF) or (alphaLeft shl 24), (color and 0x00FFFFFF) or (alphaRight shl 24),
                Shader.TileMode.CLAMP,
            )
            if (blurPx > 0f) maskFilter = android.graphics.BlurMaskFilter(blurPx, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }

    private const val LAMP_GLOW = 0xFFE5242B.toInt()
    private const val LAMP_BODY = 0xFFFF4B3E.toInt()
    private const val LAMP_CORE = 0xFFFFC9BE.toInt()

    /** The bar under the heartbeat trace, lit like the trace: how much of the average lifetime total is behind you. */
    fun pulse(ctx: Context, wDp: Float, hDp: Float, share: Double): SkyLayers {
        val base = surface(ctx, wDp, hDp)
        val bodies = surface(ctx, wDp, hDp)
        val d = base.density
        val w = wDp * d
        val h = hDp * d
        val inset = 7f * d
        val barH = 3.5f * d
        val barY = h - inset - barH / 2f
        val barLeft = inset
        val barRight = w - inset

        base.canvas.drawRoundRect(RectF(barLeft, barY - barH / 2, barRight, barY + barH / 2), barH / 2, barH / 2, paint(60))
        val fillRight = barLeft + maxOf((barRight - barLeft) * share.toFloat(), barH)
        val fill = RectF(barLeft, barY - barH / 2, fillRight, barY + barH / 2)
        val barGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (LAMP_GLOW and 0x00FFFFFF) or (150 shl 24)
            maskFilter = android.graphics.BlurMaskFilter(4f * d, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        bodies.canvas.drawRoundRect(RectF(fill.left - 1f * d, fill.top - 2f * d, fill.right + 1f * d, fill.bottom + 2f * d), barH, barH, barGlow)
        bodies.canvas.drawRoundRect(fill, barH / 2, barH / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LAMP_BODY })
        return SkyLayers(base.bitmap, bodies.bitmap)
    }

    /** One sweep of the trace shows two beats, so at 70 beats a minute it takes 2 * 60 / 70 s = 1.71 s. */
    const val PULSE_FRAMES = 24

    private var pulseFramesKey: String? = null
    private var pulseFramesCache: List<Bitmap>? = null

    private fun ecgPoints(w: Float, inset: Float, mid: Float, amp: Float, d: Float): FloatArray {
        val span = w - 2 * inset
        val pts = ArrayList<Float>()
        fun add(x: Float, y: Float) { pts.add(x); pts.add(y) }
        add(inset, mid)
        for (frac in floatArrayOf(0.10f, 0.60f)) {     // evenly spaced, also across the wrap-around
            val x0 = inset + span * frac
            add(x0, mid)
            add(x0 + 6f * d, mid - 0.15f * amp)
            add(x0 + 10f * d, mid)
            add(x0 + 14f * d, mid - amp)
            add(x0 + 19f * d, mid + 0.8f * amp)
            add(x0 + 23f * d, mid)
            add(x0 + 31f * d, mid - 0.25f * amp)
            add(x0 + 37f * d, mid)
        }
        add(w - inset, mid)
        return pts.toFloatArray()
    }

    private fun yAt(pts: FloatArray, x: Float): Float {
        var i = 0
        while (i + 3 < pts.size && pts[i + 2] < x) i += 2
        val x0 = pts[i]; val y0 = pts[i + 1]; val x1 = pts[i + 2]; val y1 = pts[i + 3]
        return if (x1 == x0) y1 else y0 + (y1 - y0) * ((x - x0) / (x1 - x0)).coerceIn(0f, 1f)
    }

    /** A paint whose colour is bright at [head] and fades out over [tail] pixels behind it, and is gone ahead of it. */
    private fun trailPaint(color: Int, widthPx: Float, blurPx: Float, head: Float, tail: Float, maxAlpha: Int, lead: Float) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = widthPx
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            val rgb = color and 0x00FFFFFF
            shader = LinearGradient(
                head - tail, 0f, head + lead, 0f,
                intArrayOf(rgb, (maxAlpha shl 24) or rgb, rgb),
                floatArrayOf(0f, tail / (tail + lead), 1f),
                Shader.TileMode.CLAMP,
            )
            if (blurPx > 0f) maskFilter = android.graphics.BlurMaskFilter(blurPx, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }

    /**
     * The frames of a phosphor-style sweep: a bright head runs left to right along the trace and leaves a glowing
     * tail that fades out behind it, wrapping around at the end. Drawn at a fixed 1.6 px per dp to keep the
     * 24 bitmaps small; the ImageView scales them up and the glow is soft anyway.
     */
    fun pulseFrames(wDp: Float, hDp: Float): List<Bitmap> {
        val d = 1.6f
        val w = (wDp * d).toInt().coerceAtLeast(40)
        val h = (hDp * d).toInt().coerceAtLeast(30)
        val key = "$w:$h"
        pulseFramesCache?.let { if (key == pulseFramesKey) return it }

        val inset = 7f * d
        val barH = 3.5f * d
        val barY = h - inset - barH / 2f
        val mid = (inset + barY - 8f * d) / 2f
        val amp = (mid - inset) * 0.95f
        val pts = ecgPoints(w.toFloat(), inset, mid, amp, d)
        val path = android.graphics.Path().apply {
            moveTo(pts[0], pts[1])
            var i = 2
            while (i < pts.size) { lineTo(pts[i], pts[i + 1]); i += 2 }
        }
        val span = w - 2 * inset
        val tail = span * 0.6f
        val lead = 3f * d

        val frames = ArrayList<Bitmap>(PULSE_FRAMES)
        for (k in 0 until PULSE_FRAMES) {
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val head = inset + span * k / PULSE_FRAMES
            for (h0 in floatArrayOf(head, head + span)) {   // the second pass is the tail of the previous sweep
                c.drawPath(path, trailPaint(LAMP_GLOW, 8f * d, 7f * d, h0, tail, 120, lead))
                c.drawPath(path, trailPaint(LAMP_GLOW, 4.5f * d, 3f * d, h0, tail, 210, lead))
                c.drawPath(path, trailPaint(LAMP_BODY, 2.2f * d, 0f, h0, tail, 255, lead))
                c.drawPath(path, trailPaint(LAMP_CORE, 0.9f * d, 0f, h0, tail, 235, lead))
            }
            val hy = yAt(pts, head)
            c.drawCircle(head, hy, 4.5f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = (LAMP_GLOW and 0x00FFFFFF) or (170 shl 24)
                maskFilter = android.graphics.BlurMaskFilter(5f * d, android.graphics.BlurMaskFilter.Blur.NORMAL)
            })
            c.drawCircle(head, hy, 2f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LAMP_CORE })
            frames.add(bmp)
        }
        pulseFramesKey = key
        pulseFramesCache = frames
        return frames
    }
    private var gardenKey: String? = null
    private var gardenCache: SkyLayers? = null

    /**
     * The day's stone garden at about 1.5 px per dp, soft enough to stretch. [natural] gives sand and grey stone
     * in [SkyLayers.bodies] with rounded corners; otherwise a tintable alpha picture in [SkyLayers.base].
     * The last one is kept, so resizing a widget or a second tick does not redo the work.
     */
    fun garden(ctx: Context, wDp: Float, hDp: Float, epochDay: Long, natural: Boolean): SkyLayers {
        val w = (wDp * 1.5f).toInt().coerceIn(120, 900)
        val h = (hDp * 1.5f).toInt().coerceIn(60, 600)
        val key = "$epochDay:$w:$h:$natural"
        gardenCache?.let { if (key == gardenKey) return it }

        val stones = Garden.layout(epochDay, w / (h / Garden.UNIT_HEIGHT))
        val raw = Bitmap.createBitmap(Garden.pixels(w, h, stones, natural), w, h, Bitmap.Config.ARGB_8888)
        val blank = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val layers = if (natural) {
            val rounded = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val radius = ctx.resources.getDimension(android.R.dimen.system_app_widget_background_radius) /
                ctx.resources.displayMetrics.density * 1.5f
            Canvas(rounded).drawRoundRect(
                RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = android.graphics.BitmapShader(raw, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) },
            )
            SkyLayers(blank, rounded)
        } else {
            SkyLayers(raw, blank)
        }
        gardenKey = key
        gardenCache = layers
        return layers
    }
    /** A circle per year, ten to a row, decade numbers on the left. The current year is filled by its share. */
    fun lifeGrid(ctx: Context, wDp: Float, hDp: Float, years: Int, share: Double, horizon: Int): Bitmap {
        val s = surface(ctx, wDp, hDp)
        val c = s.canvas
        val d = s.density
        val rows = ceil(max(horizon, years + 1) / 10.0).toInt()
        val labelW = 14f
        val pitchDp = min((wDp - labelW) / 10f, hDp / rows)
        val pitch = pitchDp * d
        val gridW = labelW * d + pitch * 10
        val ox = (s.bitmap.width - gridW) / 2f + labelW * d
        val oy = (s.bitmap.height - pitch * rows) / 2f
        val r = pitch * 0.39f
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            alpha = 160
            textAlign = Paint.Align.RIGHT
            textSize = min(7f * d, pitch * 0.8f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        for (row in 0 until rows) {
            val cy = oy + row * pitch + pitch / 2f
            if (pitchDp >= 8f) c.drawText((row * 10).toString(), ox - 3f * d, cy + label.textSize * 0.35f, label)
            for (col in 0 until 10) {
                val idx = row * 10 + col
                val cx = ox + col * pitch + pitch / 2f
                when {
                    idx < years -> c.drawCircle(cx, cy, r, paint(255))
                    idx == years -> {
                        c.drawCircle(cx, cy, r - d * 0.5f, paint(255, d))
                        val box = RectF(cx - r, cy - r, cx + r, cy + r)
                        c.drawArc(box, -90f, (360 * share).toFloat(), true, paint(255))
                    }
                    else -> c.drawCircle(cx, cy, r, paint(43))
                }
            }
        }
        return s.bitmap
    }
}
