package com.lifeclock.widget

import android.content.Context
import com.lifeclock.widget.widget.WidgetRegistry
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Widget faces. NUMBER, GRID, PLANETS, PATH, LIGHT, MALA and PULSE start at 2x2, RULER and DOTS at 4x2. */
enum class Kind(val wide: Boolean) {
    NUMBER(false), GRID(false), RULER(true), DOTS(true), PLANETS(false), PATH(false), LIGHT(false), MALA(false), PULSE(false), GARDEN(true);

    companion object {
        /** A saved face only counts if it belongs to the same group and size family as the provider, else [def]. */
        fun of(i: Int, def: Kind): Kind = entries.getOrNull(i)?.takeIf {
            it.wide == def.wide && WidgetRegistry.of(it).group == WidgetRegistry.of(def).group
        } ?: def
    }
}

/**
 * Per-widget look.
 * font: 0 dots, 1 clean, 2 segments. colorMode: 0 system, 1 light, 2 dark, 3 custom.
 * layout: 0 auto, 1 row, 2 stacked. scrim: 0 auto, 1 off. horizon: years shown by the life grid.
 * palette (planets): 0 system colours, 1 planet colours.
 */
data class WidgetConfig(
    val kind: Kind = Kind.NUMBER,
    val font: Int = 0,
    val colorMode: Int = 0,
    val customColor: Int = 0xFFFFFFFF.toInt(),
    val opacity: Int = 0,
    val decimals: Int = 8,
    val layout: Int = 0,
    val scrim: Int = 0,
    val horizon: Int = 90,
    val palette: Int = 0,
)

object Prefs {
    private const val FILE = "life_clock"
    private const val KEY_BIRTH = "birth_millis"
    private const val KEY_ZONE = "zone_id"
    private const val KEY_OPACITY_DEFAULT = "opacity_default"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun birth(ctx: Context): ZonedDateTime? {
        val sp = sp(ctx)
        if (!sp.contains(KEY_BIRTH)) return null
        val zone = runCatching { ZoneId.of(sp.getString(KEY_ZONE, null)) }.getOrDefault(ZoneId.systemDefault())
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(sp.getLong(KEY_BIRTH, 0)), zone)
    }

    fun saveBirth(ctx: Context, birth: ZonedDateTime) {
        sp(ctx).edit()
            .putLong(KEY_BIRTH, birth.toInstant().toEpochMilli())
            .putString(KEY_ZONE, birth.zone.id)
            .apply()
    }

    /** Background opacity, percent. Falls back to the app-wide value, then to 0 (2x2) or 60 (4x2). */
    fun opacity(ctx: Context, widgetId: Int, def: Kind): Int {
        val s = sp(ctx)
        return when {
            s.contains("opacity_$widgetId") -> s.getInt("opacity_$widgetId", 0)
            s.contains(KEY_OPACITY_DEFAULT) -> s.getInt(KEY_OPACITY_DEFAULT, 60)
            else -> if (def.wide) 60 else 0
        }
    }

    fun opacityDefault(ctx: Context): Int = sp(ctx).getInt(KEY_OPACITY_DEFAULT, 60)

    fun setOpacityDefault(ctx: Context, v: Int) {
        sp(ctx).edit().putInt(KEY_OPACITY_DEFAULT, v).apply()
    }

    fun setOpacity(ctx: Context, widgetId: Int, v: Int) {
        sp(ctx).edit().putInt("opacity_$widgetId", v).apply()
    }

    /** [def] is the kind of the provider the widget belongs to. */
    fun widgetConfig(ctx: Context, id: Int, def: Kind): WidgetConfig {
        val s = sp(ctx)
        return WidgetConfig(
            kind = Kind.of(s.getInt("kind_$id", -1), def),
            font = s.getInt("font_$id", 0).coerceIn(0, 2),
            colorMode = s.getInt("cmode_$id", 0),
            customColor = s.getInt("ccolor_$id", 0xFFFFFFFF.toInt()),
            opacity = opacity(ctx, id, def),
            decimals = s.getInt("dec_$id", 8).coerceIn(1, 8),
            layout = s.getInt("layout_$id", 0),
            scrim = s.getInt("scrim_$id", 0),
            horizon = s.getInt("horizon_$id", 90),
            palette = s.getInt("palette_$id", if (def == Kind.GARDEN) 1 else 0),
        )
    }

    fun saveWidgetConfig(ctx: Context, id: Int, c: WidgetConfig) {
        sp(ctx).edit()
            .putInt("kind_$id", c.kind.ordinal)
            .putInt("font_$id", c.font)
            .putInt("cmode_$id", c.colorMode)
            .putInt("ccolor_$id", c.customColor)
            .putInt("opacity_$id", c.opacity)
            .putInt("dec_$id", c.decimals)
            .putInt("layout_$id", c.layout)
            .putInt("scrim_$id", c.scrim)
            .putInt("horizon_$id", c.horizon)
            .putInt("palette_$id", c.palette)
            .apply()
    }

    fun clearWidget(ctx: Context, id: Int) {
        val e = sp(ctx).edit()
        for (key in listOf("kind", "font", "cmode", "ccolor", "opacity", "dec", "layout", "scrim", "horizon", "palette")) {
            e.remove("${key}_$id")
        }
        e.apply()
    }

    /** 0 follows the system, 1 light, 2 dark. */
    fun theme(ctx: Context): Int = sp(ctx).getInt("theme", 0)
    fun setTheme(ctx: Context, v: Int) = sp(ctx).edit().putInt("theme", v).apply()

    fun celebrated(ctx: Context): String = sp(ctx).getString("celebrated_day", "") ?: ""
    fun setCelebrated(ctx: Context, day: String) = sp(ctx).edit().putString("celebrated_day", day).apply()
}
