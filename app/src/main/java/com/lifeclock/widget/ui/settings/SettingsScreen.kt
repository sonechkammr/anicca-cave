package com.lifeclock.widget.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.lifeclock.widget.R
import com.lifeclock.widget.ui.components.Screen
import com.lifeclock.widget.ui.theme.Anicca
import com.lifeclock.widget.ui.theme.raised
import java.time.ZonedDateTime

/**
 * Grouped list: Birth (date, time, city: each row opens the same sheet Setup uses and applies at once),
 * Appearance (theme) and Language (Android 13 and newer).
 */
@Composable
fun SettingsScreen(
    birth: ZonedDateTime,
    themeMode: Int,
    onBirth: (ZonedDateTime) -> Unit,
    onTheme: (Int) -> Unit,
) {
    val ctx = LocalContext.current
    var open by rememberOpenField()
    var futureError by rememberSaveable { mutableStateOf(false) }

    /** Applies a change to one part of the birth, unless it would put the moment in the future. */
    fun change(date: java.time.LocalDate = birth.toLocalDate(), time: java.time.LocalTime = birth.toLocalTime(), zone: java.time.ZoneId = birth.zone) {
        val next = birthOf(date, time, zone)
        if (isFuture(next)) futureError = true else { futureError = false; onBirth(next) }
    }

    Screen {
        Text(stringResource(R.string.settings_title), style = Anicca.type.screenTitle)

        SettingsGroup(stringResource(R.string.settings_group_birth)) {
            SettingsRow(stringResource(R.string.setting_date), formatBirthDate(birth.toLocalDate())) { open = BirthField.Date }
            HorizontalDivider()
            SettingsRow(stringResource(R.string.setting_time), formatBirthTime(birth.toLocalTime())) { open = BirthField.Time }
            HorizontalDivider()
            SettingsRow(stringResource(R.string.setting_city), localizedCityName(birth.zone.id, LocalConfiguration.current.locales[0])) { open = BirthField.City }
        }
        if (futureError) {
            Text(stringResource(R.string.form_err_future), style = Anicca.type.body, color = MaterialTheme.colorScheme.error)
        }

        SettingsGroup(stringResource(R.string.theme_title)) {
            val options = listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark)
            SegmentedChoice(options.map { stringResource(it) }, selected = themeMode, onSelect = onTheme)
        }

        if (AppLanguage.supported) {
            SettingsGroup(stringResource(R.string.language_title)) {
                val tags = listOf("", "en", "ru")
                val labels = listOf(stringResource(R.string.language_system), stringResource(R.string.language_en), stringResource(R.string.language_ru))
                SegmentedChoice(labels, selected = tags.indexOf(AppLanguage.current(ctx)).coerceAtLeast(0)) {
                    AppLanguage.set(ctx, tags[it])
                }
            }
        }

        Text(
            stringResource(R.string.settings_footer),
            style = Anicca.type.small, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    BirthSheetHost(
        open = open,
        date = birth.toLocalDate(),
        time = birth.toLocalTime(),
        zoneId = birth.zone.id,
        onClose = { open = BirthField.None },
        onDate = { change(date = it) },
        onTime = { change(time = it) },
        onCity = { change(zone = java.time.ZoneId.of(it.zoneId)) },
    )
}

/** A titled, raised block holding rows or controls. */
@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Anicca.spacing.s)) {
        Text(title, style = Anicca.type.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.fillMaxWidth().raised(Anicca.shapes.medium).padding(Anicca.spacing.l)) { content() }
    }
}

@Composable
private fun SettingsRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Anicca.spacing.touch)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Anicca.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = Anicca.type.body)
        Text(value, style = Anicca.type.body, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SegmentedChoice(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { i, label ->
            SegmentedButton(
                selected = selected == i,
                onClick = { onSelect(i) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = labels.size),
                modifier = Modifier.heightIn(min = Anicca.spacing.touch),
            ) { Text(label) }
        }
    }
}
