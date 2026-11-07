package com.lifeclock.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lifeclock.widget.ui.components.BirthForm
import com.lifeclock.widget.widget.BaseLifeProvider
import com.lifeclock.widget.widget.WidgetRefresh
import com.lifeclock.widget.widget.WidgetRegistry
import com.lifeclock.widget.widget.WidgetRenderer
import kotlinx.coroutines.delay
import kotlin.math.min

/** Settings for one widget. Opens when a widget is added and when it is reconfigured from the launcher. */
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        setResult(RESULT_CANCELED, result)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        val mgr = AppWidgetManager.getInstance(this)
        val def = WidgetRegistry.forProvider(mgr.getAppWidgetInfo(id)?.provider?.className)?.kind ?: Kind.NUMBER
        val (wDp, hDp) = BaseLifeProvider.sizeOf(this, mgr, id, def)

        setContent {
            val ctx = LocalContext.current
            val scheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            MaterialTheme(colorScheme = scheme) {
                Surface(Modifier.fillMaxSize()) {
                    var birth by remember { mutableStateOf(Prefs.birth(ctx)) }
                    var cfg by remember { mutableStateOf(Prefs.widgetConfig(ctx, id, def)) }
                    var backdrop by remember { mutableIntStateOf(0) }

                    Column(
                        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(stringResource(R.string.config_title), fontSize = 24.sp)
                        if (birth == null && WidgetRegistry.of(cfg.kind).needsBirth) {
                            Text(stringResource(R.string.config_need_birth))
                            BirthForm(initial = null) { b ->
                                Prefs.saveBirth(ctx, b)
                                birth = b
                            }
                        } else {
                            WidgetPreview(id, def, wDp, hDp, cfg, backdrop)
                            Section(R.string.preview_on) {
                                Chips(
                                    listOf(R.string.preview_mid, R.string.preview_light, R.string.preview_dark).map { stringResource(it) },
                                    backdrop,
                                ) { backdrop = it }
                            }
                            ConfigForm(def, cfg) { cfg = it }
                            Button(
                                onClick = {
                                    Prefs.saveWidgetConfig(ctx, id, cfg)
                                    WidgetRefresh.all(ctx)
                                    setResult(RESULT_OK, result)
                                    finish()
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(stringResource(R.string.config_done)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun kindName(k: Kind): String = stringResource(WidgetRegistry.of(k).label)

private class Preset(val name: Int, val font: Int, val colorMode: Int, val opacity: Int)

private val PRESETS = listOf(
    Preset(R.string.preset_plain, font = 1, colorMode = 0, opacity = 0),
    Preset(R.string.preset_dots, font = 0, colorMode = 0, opacity = 0),
    Preset(R.string.preset_segments, font = 2, colorMode = 0, opacity = 0),
    Preset(R.string.preset_card, font = 1, colorMode = 0, opacity = 60),
)

@Composable
private fun ConfigForm(def: Kind, cfg: WidgetConfig, onChange: (WidgetConfig) -> Unit) {
    val hint = MaterialTheme.colorScheme.onSurfaceVariant
    val group = WidgetRegistry.of(def).group
    val digits = WidgetRegistry.of(cfg.kind).hasDigits

    val kinds = WidgetRegistry.all.filter { it.group == group && it.kind.wide == def.wide }.map { it.kind }
    if (kinds.size > 1) Section(R.string.section_style) {
        Chips(kinds.map { kindName(it) }, kinds.indexOf(cfg.kind)) { onChange(cfg.copy(kind = kinds[it])) }
    }
    if (cfg.kind == Kind.PLANETS || cfg.kind == Kind.GARDEN) Section(R.string.section_palette) {
        val palettes = listOf(R.string.palette_system, if (cfg.kind == Kind.GARDEN) R.string.palette_garden else R.string.palette_planets)
        Chips(palettes.map { stringResource(it) }, cfg.palette) { onChange(cfg.copy(palette = it)) }
    }
    if (digits) Section(R.string.section_presets) {
        val selected = PRESETS.indexOfFirst {
            it.font == cfg.font && it.colorMode == cfg.colorMode && it.opacity == cfg.opacity
        }
        Chips(PRESETS.map { stringResource(it.name) }, selected) { i ->
            val p = PRESETS[i]
            onChange(cfg.copy(font = p.font, colorMode = p.colorMode, opacity = p.opacity))
        }
    }
    if (digits) Section(R.string.section_font) {
        val fonts = listOf(R.string.font_dots, R.string.font_clean, R.string.font_segments)
        Chips(fonts.map { stringResource(it) }, cfg.font) { onChange(cfg.copy(font = it)) }
    }
    Section(R.string.section_color) {
        val colors = listOf(R.string.color_system, R.string.color_light, R.string.color_dark, R.string.color_custom)
        Chips(colors.map { stringResource(it) }, cfg.colorMode) { onChange(cfg.copy(colorMode = it)) }
        if (cfg.colorMode == 3) CustomColor(cfg.customColor) { onChange(cfg.copy(customColor = it)) }
    }
    Section(title = stringResource(R.string.section_opacity, cfg.opacity)) {
        Slider(
            value = cfg.opacity.toFloat(),
            onValueChange = { onChange(cfg.copy(opacity = it.toInt())) },
            valueRange = 0f..100f,
            steps = 9,
        )
    }
    Section(R.string.section_plate) {
        Chips(listOf(stringResource(R.string.plate_auto), stringResource(R.string.plate_off)), cfg.scrim) {
            onChange(cfg.copy(scrim = it))
        }
        Text(stringResource(R.string.plate_hint), fontSize = 12.sp, color = hint)
    }
    if (digits) Section(R.string.section_decimals) {
        val options = listOf(2, 4, 6, 8)
        Chips(options.map { "$it" }, options.indexOf(cfg.decimals).coerceAtLeast(0)) {
            onChange(cfg.copy(decimals = options[it]))
        }
        Text(stringResource(R.string.decimals_hint), fontSize = 12.sp, color = hint)
    }
    if (cfg.kind == Kind.NUMBER) {
        Section(R.string.section_layout) {
            val layouts = listOf(R.string.layout_auto, R.string.layout_row, R.string.layout_stacked)
            Chips(layouts.map { stringResource(it) }, cfg.layout) { onChange(cfg.copy(layout = it)) }
        }
    }
    if (cfg.kind == Kind.GRID) {
        Section(R.string.section_horizon) {
            val options = listOf(80, 90, 100)
            Chips(options.map { stringResource(R.string.horizon_years, it) }, options.indexOf(cfg.horizon).coerceAtLeast(0)) {
                onChange(cfg.copy(horizon = options[it]))
            }
        }
    }
}

@Composable
private fun Section(title: Int, content: @Composable () -> Unit) = Section(stringResource(title), content)

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun Chips(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, label ->
            FilterChip(selected = i == selected, onClick = { onSelect(i) }, label = { Text(label) })
        }
    }
}

private val PALETTE = listOf(
    0xFFFFFFFF, 0xFFF2E9DC, 0xFFFFD60A, 0xFFFF9F0A, 0xFFFF453A, 0xFFFF6BB5,
    0xFFBF5AF2, 0xFF64D2FF, 0xFF30D158, 0xFFB7C4B0, 0xFF8E8E93, 0xFF121212,
).map { it.toInt() }

@Composable
private fun CustomColor(current: Int, onPick: (Int) -> Unit) {
    var hex by remember(current) { mutableStateOf("#%06X".format(current and 0xFFFFFF)) }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PALETTE.forEach { c ->
            val chosen = (c and 0xFFFFFF) == (current and 0xFFFFFF)
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(Color(c))
                    .border(
                        if (chosen) 3.dp else 1.dp,
                        if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        CircleShape,
                    )
                    .clickable { onPick(c) },
            )
        }
    }
    OutlinedTextField(
        value = hex,
        onValueChange = {
            hex = it
            val s = it.trim().removePrefix("#")
            if (s.length == 6) s.toLongOrNull(16)?.let { v -> onPick((0xFF000000 or v).toInt()) }
        },
        label = { Text(stringResource(R.string.hex_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Flat colours standing in for a mid, a light and a dark wallpaper. */
internal val BACKDROPS = intArrayOf(0xFF6E6878.toInt(), 0xFFE8E5DE.toInt(), 0xFF0D0E12.toInt())

/**
 * The real widget RemoteViews drawn over a flat colour that stands in for the wallpaper.
 * The same colour feeds the contrast check, so the readability plate shows up here too.
 * Redrawn every second; the flipping decimals are left out and the digits just tick.
 */
@Composable
internal fun WidgetPreview(
    id: Int, def: Kind, wDp: Int, hDp: Int, cfg: WidgetConfig, backdrop: Int = 0,
    /** Draws a soft gradient instead of a flat colour, so the stage looks more like a wallpaper. */
    wallpaper: Boolean = false,
    /** Space around the widget, inside the stage. */
    stagePadding: androidx.compose.ui.unit.Dp = 20.dp,
) {
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    val scale = min(1f, 320f / wDp)
    val pw = (wDp * scale).toInt()
    val ph = (hDp * scale).toInt()
    val color = BACKDROPS[backdrop.coerceIn(0, BACKDROPS.lastIndex)]
    val base = Color(color)
    val fill = if (wallpaper) {
        androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(
                androidx.compose.ui.graphics.lerp(base, Color.White, 0.18f),
                base,
                androidx.compose.ui.graphics.lerp(base, Color.Black, 0.30f),
            ),
        )
    } else {
        androidx.compose.ui.graphics.SolidColor(base)
    }
    Box(
        Modifier.fillMaxWidth().height(ph.dp + stagePadding * 2).clip(RoundedCornerShape(24.dp)).background(fill),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            modifier = Modifier.size(pw.dp, ph.dp),
            factory = { FrameLayout(it) },
            update = { host ->
                host.removeAllViews()
                runCatching {
                    val r = WidgetRenderer.render(ctx, id, pw, ph, def, override = cfg, nowMs = now, light = true, backdrop = color)
                    host.addView(r.views.apply(ctx, host))
                }.onFailure { Log.e("Anicca", "widget preview failed for $def", it) }
            },
        )
    }
}
