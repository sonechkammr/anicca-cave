package com.lifeclock.widget.ui.today

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/** The red that marks "now" across the app and the widgets. */
val NowRed = Color(0xFFE5242B)

private const val LABEL_COLUMNS = 0.9f // width of the decade-number column, in cell pitches

/** Rows in the grid: ten years to a row, room for at least [horizon] years and the one being lived. */
fun lifeGridRows(years: Int, horizon: Int = 90): Int = ceil(max(horizon, years + 1) / 10.0).toInt()

/** Width over height of the grid, so a composable can size itself. */
fun lifeGridAspect(years: Int, horizon: Int = 90, labels: Boolean = true): Float =
    (10f + if (labels) LABEL_COLUMNS else 0f) / lifeGridRows(years, horizon)

/**
 * A circle per year, ten to a row, decade numbers on the left. Years lived are filled, the current one fills by its
 * [share] in red, the rest are faint. The same code draws the screen and the shared image.
 */
fun DrawScope.drawLifeGrid(
    years: Int,
    share: Double,
    filled: Color,
    empty: Color,
    accent: Color = NowRed,
    labelColor: Color = filled.copy(alpha = 0.6f),
    horizon: Int = 90,
    labels: Boolean = true,
) {
    val rows = lifeGridRows(years, horizon)
    val labelCols = if (labels) LABEL_COLUMNS else 0f
    val pitch = min(size.width / (10f + labelCols), size.height / rows)
    val gridW = pitch * (10f + labelCols)
    val ox = (size.width - gridW) / 2f + pitch * labelCols
    val oy = (size.height - pitch * rows) / 2f
    val r = pitch * 0.39f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelColor.toArgb()
        textAlign = Paint.Align.RIGHT
        textSize = pitch * 0.5f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    for (row in 0 until rows) {
        val cy = oy + row * pitch + pitch / 2f
        if (labels) drawContext.canvas.nativeCanvas.drawText((row * 10).toString(), ox - pitch * 0.12f, cy + paint.textSize * 0.35f, paint)
        for (col in 0 until 10) {
            val idx = row * 10 + col
            val c = Offset(ox + col * pitch + pitch / 2f, cy)
            when {
                idx < years -> drawCircle(filled, r, c)
                idx == years -> {
                    drawCircle(accent, r - pitch * 0.02f, c, style = Stroke(width = pitch * 0.05f))
                    drawArc(accent, -90f, (360 * share).toFloat(), true, Offset(c.x - r, c.y - r), Size(r * 2, r * 2))
                }
                else -> drawCircle(empty, r, c)
            }
        }
    }
}

/** The grid as a composable. Sizes itself to the grid's own proportions. */
@Composable
fun LifeGrid(years: Int, share: Double, filled: Color, empty: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(lifeGridAspect(years))) {
        drawLifeGrid(years, share, filled, empty)
    }
}

/**
 * The moon as seen from the northern hemisphere: lit limb on the right while waxing, on the left while waning. The lit
 * part is a half disc plus or minus half an ellipse whose width follows cos(2 pi phase).
 */
fun DrawScope.drawMoon(phase: Double, lit: Color, dark: Color) {
    val r = min(size.width, size.height) / 2f
    val centre = Offset(size.width / 2f, size.height / 2f)
    drawCircle(dark, r, centre)

    val waxing = phase < 0.5
    val p = if (waxing) phase else 1 - phase
    val a = (r * cos(2 * PI * p)).toFloat() // terminator offset from the centre: + crescent, - gibbous
    val disc = Rect(centre.x - r, centre.y - r, centre.x + r, centre.y + r)
    val terminator = Rect(centre.x - abs(a), centre.y - r, centre.x + abs(a), centre.y + r)
    val path = Path().apply {
        arcTo(disc, -90f, 180f, true)                              // right limb, top to bottom
        arcTo(terminator, 90f, if (a >= 0) -180f else 180f, false) // back up along the terminator
        close()
    }
    if (waxing) drawPath(path, lit) else withTransform({ scale(-1f, 1f, centre) }) { drawPath(path, lit) }
}

@Composable
fun MoonDisc(phase: Double, lit: Color, dark: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(1f)) { drawMoon(phase, lit, dark) }
}

