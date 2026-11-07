package com.lifeclock.widget.widget

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.lifeclock.widget.R
import com.lifeclock.widget.WidgetConfig
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Puts a faint plate under the widget when its background is nearly clear and the text would
 * fall under 4.5:1 contrast against the wallpaper. Android only gives the wallpaper's dominant
 * colours, not the pixels behind the widget, so this is a rough guard, not an exact one.
 */
object Contrast {
    private const val MIN_RATIO = 4.5
    private const val CLEAR_BELOW = 30     // opacity percent

    private fun lin(v: Int): Double {
        val s = v / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(c: Int): Double =
        0.2126 * lin(Color.red(c)) + 0.7152 * lin(Color.green(c)) + 0.0722 * lin(Color.blue(c))

    private fun ratio(a: Int, b: Int): Double {
        val l1 = luminance(a)
        val l2 = luminance(b)
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    private fun blend(bg: Int, fg: Int, alpha: Double): Int = Color.rgb(
        (Color.red(bg) * (1 - alpha) + Color.red(fg) * alpha).toInt(),
        (Color.green(bg) * (1 - alpha) + Color.green(fg) * alpha).toInt(),
        (Color.blue(bg) * (1 - alpha) + Color.blue(fg) * alpha).toInt(),
    )

    private fun textColor(ctx: Context, cfg: WidgetConfig): Int = when (cfg.colorMode) {
        1 -> Color.WHITE
        2 -> 0xFF121212.toInt()
        3 -> cfg.customColor or 0xFF000000.toInt()
        else -> ctx.getColor(R.color.lc_text)
    }

    private fun wallpaperColors(ctx: Context): List<Int> = runCatching {
        val c = WallpaperManager.getInstance(ctx).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        listOfNotNull(c?.primaryColor, c?.secondaryColor, c?.tertiaryColor).map { it.toArgb() }
    }.getOrDefault(emptyList())

    /** ARGB of the plate, or null if none is needed. [backdrop] stands in for the wallpaper in previews. */
    fun scrim(ctx: Context, cfg: WidgetConfig, backdrop: Int? = null): Int? {
        if (cfg.scrim == 1 || cfg.opacity >= CLEAR_BELOW) return null
        val colors = if (backdrop != null) listOf(backdrop) else wallpaperColors(ctx)
        if (colors.isEmpty()) return null
        val text = textColor(ctx, cfg)
        val worst = colors.minOf { ratio(text, it) }
        if (worst >= MIN_RATIO) return null

        val base = if (luminance(text) > 0.5) Color.BLACK else Color.WHITE
        for (a in doubleArrayOf(0.22, 0.32, 0.42)) {
            if (colors.minOf { ratio(text, blend(it, base, a)) } >= MIN_RATIO) return withAlpha(base, a)
        }
        // nothing reaches 4.5:1; keep the strongest plate only if it helps at all
        val best = colors.minOf { ratio(text, blend(it, base, 0.42)) }
        return if (best > worst) withAlpha(base, 0.42) else null
    }

    private fun withAlpha(c: Int, a: Double): Int =
        Color.argb((a * 255).toInt(), Color.red(c), Color.green(c), Color.blue(c))

    /** Rounded plate [insetDp] inside the widget; its corner radius is the widget's minus the inset. */
    fun plate(ctx: Context, wDp: Float, hDp: Float, insetDp: Float, argb: Int): Bitmap {
        val d = ctx.resources.displayMetrics.density
        val bmp = Bitmap.createBitmap(
            (wDp * d).toInt().coerceAtLeast(2),
            (hDp * d).toInt().coerceAtLeast(2),
            Bitmap.Config.ARGB_8888,
        )
        val outer = ctx.resources.getDimension(android.R.dimen.system_app_widget_background_radius)
        val inset = insetDp * d
        val r = max(outer - inset, 0f)
        Canvas(bmp).drawRoundRect(
            RectF(inset, inset, bmp.width - inset, bmp.height - inset), r, r,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = argb },
        )
        return bmp
    }
}
