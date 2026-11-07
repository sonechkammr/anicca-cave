package com.lifeclock.widget.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeclock.widget.LifeMath
import com.lifeclock.widget.LifeSnapshot
import com.lifeclock.widget.R
import com.lifeclock.widget.fractionDigits
import com.lifeclock.widget.grouped
import com.lifeclock.widget.pad
import com.lifeclock.widget.pct
import com.lifeclock.widget.ui.components.EmptyPrompt
import com.lifeclock.widget.ui.components.Panel
import com.lifeclock.widget.ui.components.Screen
import com.lifeclock.widget.ui.components.numberStyle
import java.time.Instant
import java.time.ZonedDateTime

@Composable
fun TodayScreen(birth: ZonedDateTime?, nowMs: State<Long>, onGoSettings: () -> Unit) {
    if (birth == null) { EmptyPrompt(onGoSettings); return }
    val cs = MaterialTheme.colorScheme
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
