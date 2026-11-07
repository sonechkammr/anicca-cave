package com.lifeclock.widget.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.lifeclock.widget.BACKDROPS
import com.lifeclock.widget.Prefs
import com.lifeclock.widget.R
import com.lifeclock.widget.WidgetConfigActivity
import com.lifeclock.widget.WidgetPreview
import com.lifeclock.widget.ui.components.EmptyPrompt
import com.lifeclock.widget.ui.components.GroupCard
import com.lifeclock.widget.ui.components.Screen
import com.lifeclock.widget.ui.theme.Anicca
import com.lifeclock.widget.widget.Group
import com.lifeclock.widget.widget.WidgetRefresh
import com.lifeclock.widget.widget.WidgetRegistry
import com.lifeclock.widget.widget.WidgetSpec
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import kotlin.math.roundToInt

/**
 * The widget showcase: category chips, one big live preview at a time on a wallpaper-like stage (swipe for the next),
 * Add and Customize, and the background slider underneath. Providers, renderers and the manifest are untouched.
 */
@Composable
fun WidgetsScreen(birth: ZonedDateTime?, onGoSettings: () -> Unit) {
    if (birth == null) { EmptyPrompt(onGoSettings); return }
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val sp = Anicca.spacing

    val groups = Group.entries
    var groupIndex by rememberSaveable { mutableIntStateOf(0) }
    var tone by rememberSaveable { mutableIntStateOf(0) }
    var opacity by remember { mutableFloatStateOf(Prefs.opacityDefault(ctx).toFloat()) }
    var saved by remember { mutableStateOf(false) }
    LaunchedEffect(saved) { if (saved) { delay(1600); saved = false } }

    // Placed widgets can change while the launcher is in front, so look again whenever the app comes back.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { resumes++; onPauseOrDispose { } }

    val specs = remember(groupIndex) { WidgetRegistry.all.filter { it.group == groups[groupIndex] } }
    val pager = key(groupIndex) { rememberPagerState { specs.size } }
    val spec = specs[pager.currentPage.coerceIn(0, specs.lastIndex)]
    val placedIds = remember(spec, resumes) {
        AppWidgetManager.getInstance(ctx).getAppWidgetIds(ComponentName(ctx, spec.provider)).toList()
    }
    val pinUnsupported = stringResource(R.string.pin_unsupported)

    Screen {
        Column {
            Text(stringResource(R.string.widgets_title), style = Anicca.type.screenTitle, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.widgets_subtitle), style = Anicca.type.caption, color = cs.onSurfaceVariant)
        }

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(sp.s)) {
            groups.forEachIndexed { i, g ->
                FilterChip(
                    selected = i == groupIndex,
                    onClick = { groupIndex = i },
                    label = { Text(stringResource(g.title)) },
                )
            }
        }

        HorizontalPager(state = pager, pageSpacing = sp.l, verticalAlignment = Alignment.Top) { page ->
            ShowcasePage(specs[page], tone, opacity.roundToInt())
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            PageDots(count = specs.size, current = pager.currentPage)
            ToneSwatches(tone) { tone = it }
        }

        ActionButtons(
            stacked = LocalDensity.current.fontScale > 1.3f,
            canCustomize = placedIds.isNotEmpty(),
            onAdd = {
                if (!WidgetRefresh.requestPin(ctx, spec.kind)) {
                    Toast.makeText(ctx, pinUnsupported, Toast.LENGTH_LONG).show()
                }
            },
            onCustomize = {
                val open = Intent(ctx, WidgetConfigActivity::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, placedIds.first())
                ctx.startActivity(open)
            },
        )
        if (placedIds.isEmpty()) {
            Text(
                stringResource(R.string.widget_customize_hint),
                style = Anicca.type.small, color = cs.onSurfaceVariant,
            )
        }

        GroupCard(stringResource(R.string.opacity_title)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.opacity_hint), style = Anicca.type.small, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f).padding(end = sp.m))
                Text("${opacity.roundToInt()}%", style = Anicca.type.number)
            }
            Slider(
                value = opacity,
                onValueChange = { opacity = it },
                onValueChangeFinished = {
                    WidgetRefresh.setOpacityAll(ctx, opacity.roundToInt())
                    saved = true
                },
                valueRange = 0f..100f,
                steps = 9,
            )
            if (saved) Text(stringResource(R.string.opacity_done), style = Anicca.type.small, color = cs.primary)
        }
        Text(stringResource(R.string.widgets_footer), style = Anicca.type.small, color = cs.onSurfaceVariant)
    }
}

/** One widget: its name and blurb, then the live preview on the stage (plus a second size where it matters). */
@Composable
private fun ShowcasePage(spec: WidgetSpec, tone: Int, opacityPercent: Int) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val sp = Anicca.spacing
    // scrim = 1 turns the readability plate off, so the preview shows the widget itself.
    val config = Prefs.widgetConfig(ctx, 0, spec.kind).copy(scrim = 1, opacity = opacityPercent)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(sp.s)) {
        Text(stringResource(spec.label), style = Anicca.type.headline)
        Text(stringResource(spec.blurb), style = Anicca.type.caption, color = cs.onSurfaceVariant)
        Spacer(Modifier.size(sp.xs))
        // A widget that changes a lot when stretched has a second size; one at a time keeps the page short.
        var large by rememberSaveable(spec.kind.name) { mutableStateOf(false) }
        val extra = spec.extraPreview
        if (extra != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(sp.s)) {
                FilterChip(selected = !large, onClick = { large = false }, label = { Text("2×2") })
                FilterChip(selected = large, onClick = { large = true }, label = { Text("4×4") })
            }
        }
        if (large && extra != null) {
            WidgetPreview(0, spec.kind, extra.first, extra.second, config, tone, wallpaper = true, stagePadding = 24.dp)
        } else {
            WidgetPreview(0, spec.kind, spec.previewWidthDp, spec.previewHeightDp, config, tone, wallpaper = true, stagePadding = 36.dp)
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    val cs = MaterialTheme.colorScheme
    val description = stringResource(R.string.widget_position, current + 1, count)
    Row(
        Modifier.semantics { contentDescription = description }.heightIn(min = Anicca.spacing.touch),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Anicca.spacing.s),
    ) {
        repeat(count) { i ->
            Box(
                Modifier.size(if (i == current) 10.dp else 8.dp).clip(CircleShape)
                    .background(if (i == current) cs.primary else cs.outlineVariant),
            )
        }
    }
}

/** Three stand-ins for the wallpaper behind the widget: mid, light, dark. */
@Composable
private fun ToneSwatches(selected: Int, onSelect: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val names = listOf(R.string.preview_mid, R.string.preview_light, R.string.preview_dark).map { stringResource(it) }
    Row {
        BACKDROPS.forEachIndexed { i, color ->
            Box(
                Modifier
                    .size(Anicca.spacing.touch)
                    .clickable(role = Role.RadioButton) { onSelect(i) }
                    .semantics { contentDescription = names[i] },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape).background(Color(color))
                        .border(if (i == selected) 3.dp else 1.dp, if (i == selected) cs.primary else cs.outline, CircleShape),
                )
            }
        }
    }
}

/** Add and Customize: side by side, or stacked when the font is large so labels never break mid-word. */
@Composable
private fun ActionButtons(stacked: Boolean, canCustomize: Boolean, onAdd: () -> Unit, onCustomize: () -> Unit) {
    val sp = Anicca.spacing
    val add: @Composable (Modifier) -> Unit = { m ->
        Button(onClick = onAdd, modifier = m.heightIn(min = sp.touch)) { Text(stringResource(R.string.widget_add)) }
    }
    val customize: @Composable (Modifier) -> Unit = { m ->
        OutlinedButton(onClick = onCustomize, enabled = canCustomize, modifier = m.heightIn(min = sp.touch)) {
            Text(stringResource(R.string.widget_customize))
        }
    }
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(sp.s)) {
            add(Modifier.fillMaxWidth())
            customize(Modifier.fillMaxWidth())
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(sp.m)) {
            add(Modifier.weight(1f))
            customize(Modifier.weight(1f))
        }
    }
}
