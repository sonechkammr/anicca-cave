package com.lifeclock.widget.ui.today

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.lifeclock.widget.LifeMath
import com.lifeclock.widget.Moon
import com.lifeclock.widget.R
import com.lifeclock.widget.Trip
import com.lifeclock.widget.fractionDigits
import com.lifeclock.widget.grouped
import com.lifeclock.widget.pct
import com.lifeclock.widget.ui.components.EmptyPrompt
import com.lifeclock.widget.ui.components.GroupCard
import com.lifeclock.widget.ui.components.ValueRow
import com.lifeclock.widget.ui.theme.Anicca
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Today: the age as a large header that shrinks into a compact bar as the list scrolls, with plain grouped lists
 * below. [nowMs] is the app's single 10 Hz clock; only the header reads it every tick. Everything else follows a
 * once-a-second value, and the path around the Sun is worked out once per second, not per tick.
 */
@Composable
fun TodayScreen(birth: ZonedDateTime?, nowMs: State<Long>, onGoSettings: () -> Unit) {
    if (birth == null) { EmptyPrompt(onGoSettings); return }
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val sp = Anicca.spacing
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val sec by remember { derivedStateOf { nowMs.value / 1000 } }
    val snap = remember(sec) { LifeMath.snapshot(birth, ZonedDateTime.ofInstant(Instant.ofEpochSecond(sec), birth.zone)) }
    val km = remember(sec) { Trip.kmAt(birth, sec * 1000) }
    val minute = sec / 60
    val moonNow = remember(minute) { Moon.phase(sec * 1000) }
    val moonBirth = remember(birth) { Moon.phase(birth.toInstant().toEpochMilli()) }
    val lines = stringArrayResource(R.array.impermanence_lines).toList()
    val dailyLine = DailyLine.pick(lines, LocalDate.now(birth.zone))

    // Header geometry: measured at its two extremes, so large fonts and other languages just work.
    val listState = rememberLazyListState()
    var expandedPx by rememberSaveable { mutableIntStateOf(0) }
    var collapsedPx by rememberSaveable { mutableIntStateOf(0) }
    val fallbackCollapsedPx = with(density) { 72.dp.roundToPx() }
    val collapse by remember {
        derivedStateOf {
            val range = (expandedPx - if (collapsedPx > 0) collapsedPx else fallbackCollapsedPx).coerceAtLeast(1)
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / range.toFloat()).coerceIn(0f, 1f)
        }
    }

    val ageLabel = stringResource(R.string.age_label)
    val appName = stringResource(R.string.app_name)
    val chooser = stringResource(R.string.share_chooser)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = sp.xl, end = sp.xl, bottom = sp.xl),
            verticalArrangement = Arrangement.spacedBy(sp.l),
        ) {
            // Room for the expanded header, which floats above the list.
            item(key = "headerRoom") { Spacer(Modifier.height(with(density) { expandedPx.toDp() })) }
            item(key = "born") {
                Text(
                    stringResource(R.string.born_on, birth.toLocalDate().toString(), birth.zone.id),
                    style = Anicca.type.caption, color = cs.onSurfaceVariant,
                )
            }
            dailyLine?.let { line ->
                item(key = "dailyLine") {
                    GroupCard(null) { Text(line, style = Anicca.type.title) }
                }
            }
            item(key = "breakdown") {
                GroupCard(stringResource(R.string.breakdown_title)) {
                    ValueRow(stringResource(R.string.unit_years), snap.years.toString())
                    ValueRow(stringResource(R.string.unit_months), snap.months.toString())
                    ValueRow(stringResource(R.string.unit_weeks), snap.weeks.toString())
                    ValueRow(stringResource(R.string.unit_days), snap.days.toString())
                    ValueRow(stringResource(R.string.unit_hours), snap.hours.toString().padStart(2, '0'))
                    ValueRow(stringResource(R.string.unit_minutes), snap.minutes.toString().padStart(2, '0'))
                    ValueRow(stringResource(R.string.unit_seconds), snap.seconds.toString().padStart(2, '0'))
                }
            }
            item(key = "totals") {
                GroupCard(stringResource(R.string.total_title)) {
                    ValueRow(stringResource(R.string.total_months), snap.totalMonths.grouped())
                    ValueRow(stringResource(R.string.total_weeks), snap.totalWeeks.grouped())
                    ValueRow(stringResource(R.string.stat_days), snap.totalDays.grouped())
                    ValueRow(stringResource(R.string.total_hours), snap.totalHours.grouped())
                    ValueRow(stringResource(R.string.total_minutes), snap.totalMinutes.grouped())
                    ValueRow(stringResource(R.string.stat_seconds), snap.totalSeconds.grouped())
                    LiveMillisRow(nowMs, birth.toInstant().toEpochMilli())
                    ValueRow(
                        stringResource(R.string.widget_path_label),
                        "${km.toLong().grouped()} ${stringResource(R.string.unit_km)}",
                    )
                }
            }
            item(key = "moon") {
                GroupCard(stringResource(R.string.moon_title)) {
                    MoonRow(stringResource(R.string.moon_today), moonNow)
                    HorizontalDivider()
                    MoonRow(stringResource(R.string.moon_birth), moonBirth)
                    Text(
                        stringResource(R.string.moon_note),
                        style = Anicca.type.small, color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(top = sp.s),
                    )
                }
            }
            item(key = "grid") {
                GroupCard(stringResource(R.string.lifegrid_title)) {
                    LifeGrid(
                        years = snap.years, share = snap.yearProgress,
                        filled = cs.onSurface, empty = cs.onSurface.copy(alpha = 0.17f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(sp.m))
                    OutlinedButton(
                        onClick = {
                            val years = snap.years
                            val share = snap.yearProgress
                            val ageText = String.format(Locale.getDefault(), "%.2f", years + share)
                            scope.launch {
                                val bitmap = withContext(Dispatchers.Default) {
                                    ShareImage.render(years, share, ageText, ageLabel, appName)
                                }
                                ShareImage.share(ctx, bitmap, chooser)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(sp.touch),
                    ) { Text(stringResource(R.string.share_image)) }
                }
            }
        }

        AgeHeader(
            birth = birth,
            nowMs = nowMs,
            collapse = collapse,
            modifier = Modifier
                .align(Alignment.TopStart)
                .onSizeChanged { size ->
                    if (collapse <= 0.001f) expandedPx = size.height
                    if (collapse >= 0.999f) collapsedPx = size.height
                },
        )
    }
}

/** The age. Expanded it is the big number with its progress; collapsed it is one compact line. */
@Composable
private fun AgeHeader(birth: ZonedDateTime, nowMs: State<Long>, collapse: Float, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val sp = Anicca.spacing
    val f = collapse
    val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMs.value), birth.zone)
    val s = LifeMath.snapshot(birth, now)
    val decimals = fractionDigits(s.yearProgress).joinToString("")

    Column(
        modifier
            .fillMaxWidth()
            .shadow(lerp(0f, 6f, f).dp, RectangleShape)
            .background(cs.background)
            .padding(horizontal = sp.xl, vertical = lerp(sp.m.value, sp.s.value, f).dp),
    ) {
        Collapsing(f) {
            Text(stringResource(R.string.age_label), style = Anicca.type.caption, color = cs.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                s.years.toString(),
                style = Anicca.type.hero.copy(fontSize = lerp(88f, 30f, f).sp, letterSpacing = lerp(-3f, -0.5f, f).sp),
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                ".$decimals",
                style = Anicca.type.heroFraction.copy(fontSize = lerp(30f, 16f, f).sp),
                modifier = Modifier.alignByBaseline(),
            )
        }
        Spacer(Modifier.height(lerp(sp.m.value, sp.xs.value, f).dp))
        LinearProgressIndicator(progress = { s.yearProgress.toFloat() }, modifier = Modifier.fillMaxWidth())
        Collapsing(f) {
            Row(
                Modifier.fillMaxWidth().padding(top = sp.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.year_elapsed, s.yearProgress.pct(2)),
                    style = Anicca.type.small, color = cs.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false).padding(end = sp.m),
                )
                val left = s.daysToBirthday.toInt()
                Text(
                    pluralStringResource(R.plurals.birthday_in, left, left),
                    style = Anicca.type.small, color = cs.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
    }
}

/** Shrinks to nothing and fades out as [f] goes from 0 to 1, so the header can fold without a fixed height. */
@Composable
private fun Collapsing(f: Float, content: @Composable () -> Unit) {
    Column(
        Modifier
            .clipToBounds()
            .layout { measurable, constraints ->
                val p = measurable.measure(constraints)
                val h = (p.height * (1f - f)).roundToInt()
                layout(p.width, h) { p.placeRelative(0, 0) }
            }
            .graphicsLayer { alpha = 1f - f },
    ) { content() }
}

/** Milliseconds lived, the only row that follows every 10 Hz tick. */
@Composable
private fun LiveMillisRow(nowMs: State<Long>, birthMs: Long) {
    ValueRow(stringResource(R.string.total_millis), (nowMs.value - birthMs).grouped())
}

@Composable
private fun MoonRow(label: String, phase: Double) {
    val cs = MaterialTheme.colorScheme
    val sp = Anicca.spacing
    Row(
        Modifier.fillMaxWidth().padding(vertical = sp.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(sp.l),
    ) {
        MoonDisc(phase, lit = cs.primary, dark = cs.onSurface.copy(alpha = 0.12f), modifier = Modifier.size(48.dp))
        Column {
            Text(label, style = Anicca.type.small, color = cs.onSurfaceVariant)
            Text(stringResource(Moon.name(phase).label()), style = Anicca.type.title)
            Text(
                stringResource(R.string.moon_lit, Moon.illumination(phase).pct(0)),
                style = Anicca.type.small, color = cs.onSurfaceVariant,
            )
        }
    }
}

@StringRes
private fun Moon.Phase.label(): Int = when (this) {
    Moon.Phase.NEW -> R.string.phase_new
    Moon.Phase.WAXING_CRESCENT -> R.string.phase_waxing_crescent
    Moon.Phase.FIRST_QUARTER -> R.string.phase_first_quarter
    Moon.Phase.WAXING_GIBBOUS -> R.string.phase_waxing_gibbous
    Moon.Phase.FULL -> R.string.phase_full
    Moon.Phase.WANING_GIBBOUS -> R.string.phase_waning_gibbous
    Moon.Phase.LAST_QUARTER -> R.string.phase_last_quarter
    Moon.Phase.WANING_CRESCENT -> R.string.phase_waning_crescent
}
