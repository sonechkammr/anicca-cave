package com.lifeclock.widget.ui.settings

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lifeclock.widget.R
import com.lifeclock.widget.ui.theme.Anicca
import java.time.LocalDate
import java.time.LocalTime

/*
 * The three pickers, as bottom sheets: the standard Material calendar, the standard clock dial and a searchable
 * city list. Setup and Settings share them. Open state lives in the caller (rememberSaveable), so rotation keeps
 * a sheet open; the picker states inside use rememberSaveable too.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerSheet(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        // Scrolls, so in landscape (or at large font sizes) the buttons at the bottom stay reachable.
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Anicca.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Anicca.spacing.m),
        ) {
            Text(title, style = Anicca.type.headline)
            content()
        }
    }
}

@Composable
private fun SheetButtons(confirmEnabled: Boolean, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = Anicca.spacing.l),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = Anicca.spacing.touch)) {
            Text(stringResource(R.string.sheet_cancel))
        }
        Button(onClick = onConfirm, enabled = confirmEnabled, modifier = Modifier.heightIn(min = Anicca.spacing.touch)) {
            Text(stringResource(R.string.sheet_ok))
        }
    }
}

/** Calendar, 1900 to today. [initial] is shown selected; null opens on today. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSheet(initial: LocalDate?, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.toPickerMillis(),
        yearRange = MIN_BIRTH_YEAR..LocalDate.now().year,
        selectableDates = remember {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = isSelectableBirthDate(utcTimeMillis)
                override fun isSelectableYear(year: Int) = isSelectableBirthYear(year)
            }
        },
    )
    PickerSheet(stringResource(R.string.sheet_date_title), onDismiss) {
        DatePicker(state = state, title = null, headline = null, showModeToggle = true)
        SheetButtons(
            confirmEnabled = state.selectedDateMillis != null,
            onCancel = onDismiss,
            onConfirm = { state.selectedDateMillis?.let { onConfirm(pickerMillisToDate(it)) } },
        )
    }
}

/**
 * Clock dial with an "I don't know" switch. Unknown keeps [UNKNOWN_BIRTH_TIME] (12:00), so the age is off by at most
 * half a day either way.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSheet(initial: LocalTime, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val timeState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = DateFormat.is24HourFormat(ctx),
    )
    var unknown by rememberSaveable { mutableStateOf(initial == UNKNOWN_BIRTH_TIME) }
    PickerSheet(stringResource(R.string.sheet_time_title), onDismiss) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = Anicca.spacing.touch),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.time_unknown), style = Anicca.type.body, modifier = Modifier.weight(1f))
            Switch(checked = unknown, onCheckedChange = { unknown = it })
        }
        if (unknown) {
            Text(
                stringResource(R.string.time_unknown_hint),
                style = Anicca.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = timeState)
            }
        }
        SheetButtons(
            confirmEnabled = true,
            onCancel = onDismiss,
            onConfirm = {
                onConfirm(if (unknown) UNKNOWN_BIRTH_TIME else LocalTime.of(timeState.hour, timeState.minute))
            },
        )
    }
}

/** Searchable city list; picking a row confirms. */
@Composable
fun CitySheet(selectedZoneId: String, onPick: (City) -> Unit, onDismiss: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val all = remember(locale) {
        Cities.all(displayName = { localizedCityName(it, locale) }, isCanonical = ::isCanonicalZone)
    }
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(query) { Cities.search(all, query) }
    PickerSheet(stringResource(R.string.sheet_city_title), onDismiss) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.city_search)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (shown.isEmpty()) {
            Text(
                stringResource(R.string.city_none),
                style = Anicca.type.body, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Anicca.spacing.l),
            )
        }
        LazyColumn(Modifier.heightIn(max = 420.dp)) {
            items(shown, key = { it.zoneId }) { city ->
                val selected = city.zoneId == selectedZoneId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(role = Role.Button) { onPick(city) }
                        .padding(vertical = Anicca.spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            city.name,
                            style = if (selected) Anicca.type.title else Anicca.type.body,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(city.zoneId, style = Anicca.type.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = Anicca.spacing.touch)) {
            Text(stringResource(R.string.sheet_cancel))
        }
    }
}

/**
 * Shows the sheet for [open], if any. Each confirm closes the sheet and reports the new value; Setup keeps it in a
 * draft, Settings applies it at once.
 */
@Composable
fun BirthSheetHost(
    open: BirthField,
    date: LocalDate?,
    time: LocalTime,
    zoneId: String,
    onClose: () -> Unit,
    onDate: (LocalDate) -> Unit,
    onTime: (LocalTime) -> Unit,
    onCity: (City) -> Unit,
) {
    when (open) {
        BirthField.Date -> DateSheet(date, onConfirm = { onDate(it); onClose() }, onDismiss = onClose)
        BirthField.Time -> TimeSheet(time, onConfirm = { onTime(it); onClose() }, onDismiss = onClose)
        BirthField.City -> CitySheet(zoneId, onPick = { onCity(it); onClose() }, onDismiss = onClose)
        BirthField.None -> Unit
    }
}
