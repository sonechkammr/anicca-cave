package com.lifeclock.widget.widget

import androidx.annotation.StringRes
import com.lifeclock.widget.Kind
import com.lifeclock.widget.R

/** Sections of the Widgets tab. */
enum class Group(@param:StringRes val title: Int) {
    AGE(R.string.group_age),
    SKY(R.string.group_sky),
    CALM(R.string.group_calm),
}

/** Everything the app needs to know about one widget, in one place. */
class WidgetSpec(
    val kind: Kind,
    val provider: Class<out BaseLifeProvider>,
    @param:StringRes val label: Int,
    @param:StringRes val blurb: Int,
    val group: Group,
    val previewWidthDp: Int,
    val previewHeightDp: Int,
    /** A second, larger preview for widgets that change a lot when stretched. */
    val extraPreview: Pair<Int, Int>? = null,
    /** False for widgets that only need the clock. */
    val needsBirth: Boolean = true,
    /** Digits fonts, decimals and the style presets apply. */
    val hasDigits: Boolean = true,
)

/** The picker, the Widgets tab, refresh and settings all read this list. A new widget is one more entry. */
object WidgetRegistry {
    val all = listOf(
        WidgetSpec(Kind.NUMBER, LifeMinimalProvider::class.java, R.string.widget_number_label, R.string.blurb_number, Group.AGE, 160, 160),
        WidgetSpec(Kind.GRID, LifeGridProvider::class.java, R.string.widget_grid_label, R.string.blurb_grid, Group.AGE, 160, 160),
        WidgetSpec(Kind.RULER, LifeDetailedProvider::class.java, R.string.widget_ruler_label, R.string.blurb_ruler, Group.AGE, 300, 140),
        WidgetSpec(Kind.DOTS, LifeDotsProvider::class.java, R.string.widget_dots_label, R.string.blurb_dots, Group.AGE, 300, 140),
        WidgetSpec(Kind.PLANETS, LifePlanetsProvider::class.java, R.string.widget_planets_label, R.string.blurb_planets, Group.SKY, 160, 160, extraPreview = 250 to 250, needsBirth = false, hasDigits = false),
        WidgetSpec(Kind.PATH, LifePathProvider::class.java, R.string.widget_path_label, R.string.blurb_path, Group.SKY, 160, 160),
        WidgetSpec(Kind.LIGHT, LifeLightProvider::class.java, R.string.widget_light_label, R.string.blurb_light, Group.SKY, 160, 160, hasDigits = false),
        WidgetSpec(Kind.MALA, LifeMalaProvider::class.java, R.string.widget_mala_label, R.string.blurb_mala, Group.AGE, 160, 160),
        WidgetSpec(Kind.PULSE, LifePulseProvider::class.java, R.string.widget_pulse_label, R.string.blurb_pulse, Group.AGE, 160, 160),
        WidgetSpec(Kind.GARDEN, LifeGardenProvider::class.java, R.string.widget_garden_label, R.string.blurb_garden, Group.CALM, 300, 140, needsBirth = false, hasDigits = false),
    )

    fun of(kind: Kind): WidgetSpec = all.first { it.kind == kind }

    fun forProvider(className: String?): WidgetSpec? = all.firstOrNull { it.provider.name == className }
}
