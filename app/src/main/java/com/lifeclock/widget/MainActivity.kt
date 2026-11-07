package com.lifeclock.widget

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.lifeclock.widget.widget.WidgetPreviews
import com.lifeclock.widget.widget.WidgetRegistry
import com.lifeclock.widget.widget.WidgetRefresh
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WidgetPreviews.update(applicationContext)
        setContent { LifeApp() }
    }
}

@Composable
private fun LifeApp() {
    val ctx = LocalContext.current
    var themeMode by remember { mutableIntStateOf(Prefs.theme(ctx)) }
    val dark = when (themeMode) { 0 -> isSystemInDarkTheme(); 1 -> false; else -> true }
    val scheme = if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }
    MaterialTheme(colorScheme = scheme) {
        AppRoot(themeMode) { themeMode = it; Prefs.setTheme(ctx, it) }
    }
}

/** Wall-clock millis, refreshed every frame. */
@Composable
private fun rememberFrameMillis(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        androidx.compose.runtime.withFrameNanos { value = System.currentTimeMillis() }
    }
}

private const val TAB_AGE = 0
private const val TAB_WIDGETS = 1
private const val TAB_SETTINGS = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(themeMode: Int, onTheme: (Int) -> Unit) {
    val ctx = LocalContext.current
    var birth by remember { mutableStateOf(Prefs.birth(ctx)) }
    var tab by rememberSaveable { mutableIntStateOf(if (Prefs.birth(ctx) == null) TAB_SETTINGS else TAB_AGE) }
    var confetti by remember { mutableIntStateOf(0) }

    // Celebrate every 1000th day and every exact birthday, once per calendar day.
    LaunchedEffect(birth) {
        val b = birth ?: return@LaunchedEffect
        val s = LifeMath.snapshot(b)
        val today = LocalDate.now().toString()
        val milestone = (s.totalDays > 0 && s.totalDays % 1000L == 0L) ||
            (s.years > 0 && s.months == 0 && s.weeks == 0 && s.days == 0)
        if (milestone && Prefs.celebrated(ctx) != today) {
            Prefs.setCelebrated(ctx, today)
            delay(900)
            confetti++
        }
    }

    // Text defaults to black otherwise, which disappears on the dark theme.
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                    listOf(R.string.tab_age, R.string.tab_widgets, R.string.tab_settings).forEachIndexed { i, label ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(label)) })
                    }
                }
                when (tab) {
                    TAB_AGE -> AgeScreen(birth) { tab = TAB_SETTINGS }
                    TAB_WIDGETS -> WidgetsScreen(birth) { tab = TAB_SETTINGS }
                    else -> SettingsScreen(
                        birth = birth,
                        themeMode = themeMode,
                        onBirth = { b ->
                            Prefs.saveBirth(ctx, b)
                            birth = b
                            WidgetRefresh.all(ctx)
                            tab = TAB_AGE
                        },
                        onTheme = onTheme,
                    )
                }
            }
            Confetti(confetti)
        }
    }
}

@Composable
private fun EmptyPrompt(onGo: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
        Panel(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.empty_title), fontSize = 22.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.empty_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onGo) { Text(stringResource(R.string.empty_action)) }
        }
    }
}

/** Tabular figures, so digits don't jitter as they change. */
private val numberStyle = TextStyle(fontFeatureSettings = "tnum")

@Composable
private fun Screen(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}

@Composable
private fun AgeScreen(birth: ZonedDateTime?, onGoSettings: () -> Unit) {
    if (birth == null) { EmptyPrompt(onGoSettings); return }
    val cs = MaterialTheme.colorScheme
    val nowMs = rememberFrameMillis()
    val sec by remember { derivedStateOf { nowMs.value / 1000 } }
    val snap = remember(sec) { LifeMath.snapshot(birth) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    Screen {
        Text(
            stringResource(R.string.born_on, birth.toLocalDate().toString(), birth.zone.id),
            fontSize = 13.sp, color = cs.onSurfaceVariant,
        )
        AgeCard(birth, nowMs, onClick = { expanded = !expanded })
        Text(
            stringResource(if (expanded) R.string.hint_collapse else R.string.hint_expand),
            fontSize = 12.sp, color = cs.onSurfaceVariant,
        )
        AnimatedVisibility(expanded) { Breakdown(snap, birth, nowMs) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.stat_days), snap.totalDays.grouped(), Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_seconds), snap.totalSeconds.grouped(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Panel(modifier) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Medium, style = numberStyle)
    }
}

@Composable
private fun AgeCard(birth: ZonedDateTime, nowMs: State<Long>, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMs.value), birth.zone)
    val s = LifeMath.snapshot(birth, now)
    val decimals = fractionDigits(s.yearProgress).joinToString("")

    Panel(Modifier.fillMaxWidth().clickable(onClick = onClick), radius = 28.dp) {
        Text(stringResource(R.string.age_label), fontSize = 13.sp, color = cs.onSurfaceVariant)
        Row {
            Text(
                s.years.toString(),
                style = numberStyle.copy(fontSize = 88.sp, fontWeight = FontWeight.Medium, letterSpacing = (-3).sp),
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                ".$decimals",
                style = numberStyle.copy(fontSize = 30.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.alignByBaseline(),
            )
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(progress = { s.yearProgress.toFloat() }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.year_elapsed, s.yearProgress.pct(2)), fontSize = 12.sp, color = cs.onSurfaceVariant)
            val left = s.daysToBirthday.toInt()
            Text(pluralStringResource(R.plurals.birthday_in, left, left), fontSize = 12.sp, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun Breakdown(s: LifeSnapshot, birth: ZonedDateTime, nowMs: State<Long>) {
    val cs = MaterialTheme.colorScheme
    Panel(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.breakdown_title), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            Cell(stringResource(R.string.unit_years), s.years.toString(), Modifier.weight(1f))
            Cell(stringResource(R.string.unit_months), s.months.toString(), Modifier.weight(1f))
            Cell(stringResource(R.string.unit_weeks), s.weeks.toString(), Modifier.weight(1f))
            Cell(stringResource(R.string.unit_days), s.days.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        val ms by remember { derivedStateOf { (nowMs.value % 1000).toInt() } }
        Row(Modifier.fillMaxWidth()) {
            Cell(stringResource(R.string.unit_hours), s.hours.pad(), Modifier.weight(1f))
            Cell(stringResource(R.string.unit_minutes), s.minutes.pad(), Modifier.weight(1f))
            Cell(stringResource(R.string.unit_seconds), s.seconds.pad(), Modifier.weight(1f))
            Cell(stringResource(R.string.unit_millis), ms.pad(3), Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.total_title), fontSize = 12.sp, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        TotalRow(stringResource(R.string.total_months), s.totalMonths.grouped())
        TotalRow(stringResource(R.string.total_weeks), s.totalWeeks.grouped())
        TotalRow(stringResource(R.string.total_hours), s.totalHours.grouped())
        TotalRow(stringResource(R.string.total_minutes), s.totalMinutes.grouped())
        TotalRow(stringResource(R.string.total_millis), (nowMs.value - birth.toInstant().toEpochMilli()).grouped())
    }
}

@Composable
private fun Cell(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Medium, style = numberStyle)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TotalRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = numberStyle)
    }
}

@Composable
private fun WidgetsScreen(birth: ZonedDateTime?, onGoSettings: () -> Unit) {
    if (birth == null) { EmptyPrompt(onGoSettings); return }
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var opacity by remember { mutableFloatStateOf(Prefs.opacityDefault(ctx).toFloat()) }
    var saved by remember { mutableStateOf(false) }
    LaunchedEffect(saved) { if (saved) { delay(1600); saved = false } }

    Screen {
        Column {
            Text(stringResource(R.string.widgets_title), fontSize = 26.sp, fontWeight = FontWeight.Medium)
            Text(stringResource(R.string.widgets_subtitle), fontSize = 13.sp, color = cs.onSurfaceVariant)
        }

        val pinUnsupported = stringResource(R.string.pin_unsupported)
        val groups = WidgetRegistry.all.groupBy { it.group }
        groups.forEach { (group, specs) ->
            if (groups.size > 1) {
                Text(stringResource(group.title), fontSize = 14.sp, color = cs.onSurfaceVariant)
            }
            specs.forEach { spec ->
                Panel(Modifier.fillMaxWidth()) {
                    Text(stringResource(spec.label), fontWeight = FontWeight.Medium, fontSize = 17.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(spec.blurb), fontSize = 12.sp, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    val preview = Prefs.widgetConfig(ctx, 0, spec.kind).copy(scrim = 1)
                    WidgetPreview(0, spec.kind, spec.previewWidthDp, spec.previewHeightDp, preview)
                    spec.extraPreview?.let { (w, h) ->
                        Spacer(Modifier.height(8.dp))
                        WidgetPreview(0, spec.kind, w, h, preview)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (!WidgetRefresh.requestPin(ctx, spec.kind)) {
                                Toast.makeText(ctx, pinUnsupported, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.widget_add)) }
                }
            }
        }

        Panel(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.opacity_title), fontWeight = FontWeight.Medium)
                Text("${opacity.roundToInt()}%", style = numberStyle)
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
            Text(
                stringResource(if (saved) R.string.opacity_done else R.string.opacity_hint),
                fontSize = 12.sp, color = cs.onSurfaceVariant,
            )
        }
        Text(stringResource(R.string.widgets_footer), fontSize = 12.sp, color = cs.onSurfaceVariant)
    }
}

@Composable
private fun SettingsScreen(
    birth: ZonedDateTime?,
    themeMode: Int,
    onBirth: (ZonedDateTime) -> Unit,
    onTheme: (Int) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Screen {
        Column {
            Text(stringResource(R.string.settings_title), fontSize = 26.sp, fontWeight = FontWeight.Medium)
            if (birth == null) {
                Text(stringResource(R.string.settings_start), fontSize = 13.sp, color = cs.onSurfaceVariant)
            }
        }
        Panel(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.birth_title), fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            BirthForm(initial = birth, onSave = onBirth)
        }
        Panel(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.theme_title), fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark).forEachIndexed { i, label ->
                    FilterChip(selected = themeMode == i, onClick = { onTheme(i) }, label = { Text(stringResource(label)) })
                }
            }
        }
        Text(stringResource(R.string.settings_footer), fontSize = 12.sp, color = cs.onSurfaceVariant)
    }
}
