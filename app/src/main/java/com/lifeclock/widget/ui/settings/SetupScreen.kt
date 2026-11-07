package com.lifeclock.widget.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lifeclock.widget.R
import com.lifeclock.widget.ui.components.Screen
import com.lifeclock.widget.ui.theme.Anicca
import com.lifeclock.widget.ui.theme.raised
import java.time.ZonedDateTime

/** First launch: one sentence, three tappable parts, one button. No keyboard needed. */
@Composable
fun SetupScreen(onSave: (ZonedDateTime) -> Unit) {
    val draft = rememberBirthDraft(null)
    var open by rememberOpenField()
    val birth = draft.birth
    val future = birth != null && isFuture(birth)

    Screen {
        Column {
            Text(stringResource(R.string.setup_title), style = Anicca.type.screenTitle)
            Text(
                stringResource(R.string.settings_start),
                style = Anicca.type.caption, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.fillMaxWidth().raised(Anicca.shapes.large).padding(Anicca.spacing.l)) {
            BirthSentence(draft.date, draft.time, draft.zone, onOpen = { open = it })
        }
        Text(
            stringResource(R.string.form_time_hint),
            style = Anicca.type.small, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (future) {
            Text(stringResource(R.string.form_err_future), style = Anicca.type.body, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = { birth?.let(onSave) },
            enabled = birth != null && !future,
            modifier = Modifier.heightIn(min = Anicca.spacing.touch),
        ) { Text(stringResource(R.string.form_save)) }
    }

    BirthSheetHost(
        open = open,
        date = draft.date,
        time = draft.time,
        zoneId = draft.zoneId,
        onClose = { open = BirthField.None },
        onDate = { draft.epochDay = it.toEpochDay() },
        onTime = { draft.minuteOfDay = it.hour * 60 + it.minute },
        onCity = { draft.zoneId = it.zoneId },
    )
}
