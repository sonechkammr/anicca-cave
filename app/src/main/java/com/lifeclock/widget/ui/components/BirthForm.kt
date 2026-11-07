package com.lifeclock.widget.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lifeclock.widget.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Date, time and time zone of birth. Replaced by the one-sentence setup with Material pickers in Phase 3. */
@Composable
fun BirthForm(initial: ZonedDateTime?, onSave: (ZonedDateTime) -> Unit) {
    val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    var date by remember { mutableStateOf(initial?.toLocalDate()?.format(dateFmt) ?: "") }
    var time by remember { mutableStateOf(initial?.toLocalTime()?.format(timeFmt) ?: "12:00") }
    var zone by remember { mutableStateOf(initial?.zone?.id ?: ZoneId.systemDefault().id) }
    var error by remember { mutableStateOf<Int?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            date, { date = it },
            label = { Text(stringResource(R.string.form_date)) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            time, { time = it },
            label = { Text(stringResource(R.string.form_time)) },
            supportingText = { Text(stringResource(R.string.form_time_hint)) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            zone, { zone = it },
            label = { Text(stringResource(R.string.form_zone)) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            val z = runCatching { ZoneId.of(zone.trim()) }.getOrNull()
            val d = runCatching { LocalDate.parse(date.trim(), dateFmt) }.getOrNull()
            val t = runCatching { LocalTime.parse(time.trim(), timeFmt) }.getOrNull()
            @StringRes val problem: Int? = when {
                z == null || d == null || t == null -> R.string.form_err_invalid
                d.atTime(t).atZone(z).toInstant().isAfter(Instant.now()) -> R.string.form_err_future
                else -> null
            }
            error = problem
            if (problem == null) onSave(d!!.atTime(t!!).atZone(z!!))
        }) { Text(stringResource(R.string.form_save)) }
    }
}
