package com.lifeclock.widget.ui.settings

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
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
import com.lifeclock.widget.ui.theme.inset
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Which picker is open. Saved across rotation. */
enum class BirthField { None, Date, Time, City }

/** The birth being entered on the Setup screen. Every field survives rotation and process death. */
@Stable
class BirthDraft(epochDay: Long?, minuteOfDay: Int, zoneId: String) {
    var epochDay by mutableStateOf(epochDay)
    var minuteOfDay by mutableStateOf(minuteOfDay)
    var zoneId by mutableStateOf(zoneId)

    val date: LocalDate? get() = epochDay?.let(LocalDate::ofEpochDay)
    val time: LocalTime get() = LocalTime.ofSecondOfDay(minuteOfDay * 60L)
    val zone: ZoneId get() = runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault())

    /** The moment, or null until a date is chosen. */
    val birth: ZonedDateTime? get() = date?.let { birthOf(it, time, zone) }

    companion object {
        private const val NO_DATE = Long.MIN_VALUE

        val Saver: Saver<BirthDraft, Any> = Saver(
            save = { arrayListOf<Any>(it.epochDay ?: NO_DATE, it.minuteOfDay, it.zoneId) },
            restore = {
                @Suppress("UNCHECKED_CAST") val v = it as List<Any>
                BirthDraft((v[0] as Long).takeIf { d -> d != NO_DATE }, v[1] as Int, v[2] as String)
            },
        )

        /** Starts from [initial] if there is one, else no date, 12:00 and the device's zone. */
        fun from(initial: ZonedDateTime?) = BirthDraft(
            epochDay = initial?.toLocalDate()?.toEpochDay(),
            minuteOfDay = initial?.toLocalTime()?.let { it.hour * 60 + it.minute } ?: (UNKNOWN_BIRTH_TIME.hour * 60),
            zoneId = initial?.zone?.id ?: defaultZoneId(),
        )

        /** The device's zone if it is a listed city, else a sensible fallback. */
        private fun defaultZoneId(): String {
            val id = ZoneId.systemDefault().id
            return if (Cities.isCity(id)) id else "Europe/London"
        }
    }
}

@Composable
fun rememberBirthDraft(initial: ZonedDateTime?): BirthDraft =
    rememberSaveable(saver = BirthDraft.Saver) { BirthDraft.from(initial) }

@Composable
fun rememberOpenField(): androidx.compose.runtime.MutableState<BirthField> =
    rememberSaveable { mutableStateOf(BirthField.None) }

/** "13 April 1999", in the app's language. */
@Composable
fun formatBirthDate(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(date)
}

/** "12:00" or "12:00 PM", following the device's 12/24-hour setting. */
@Composable
fun formatBirthTime(time: LocalTime): String {
    val locale = LocalConfiguration.current.locales[0]
    val pattern = if (DateFormat.is24HourFormat(LocalContext.current)) "HH:mm" else "h:mm a"
    return DateTimeFormatter.ofPattern(pattern, locale).format(time)
}

/**
 * "I was born on [13 April 1999] at [12:00] in [Tallinn]". The bracketed parts are 48 dp inset fields; each opens its
 * sheet. The word order comes from the string resource, so each language can arrange the parts its own way.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BirthSentence(
    date: LocalDate?,
    time: LocalTime,
    zone: ZoneId,
    onOpen: (BirthField) -> Unit,
    modifier: Modifier = Modifier,
) {
    val template = stringResource(R.string.setup_sentence)
    val dateText = date?.let { formatBirthDate(it) } ?: stringResource(R.string.setup_pick_date)
    val timeText = formatBirthTime(time)
    val cityText = localizedCityName(zone.id, LocalConfiguration.current.locales[0])
    val parts = remember(template) { splitTemplate(template) }

    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(Anicca.spacing.s),
        verticalArrangement = Arrangement.spacedBy(Anicca.spacing.s),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        for (part in parts) when (part) {
            is TemplatePart.Text -> Text(part.text, style = Anicca.type.headline)
            is TemplatePart.Slot -> when (part.field) {
                BirthField.Date -> FieldChip(dateText, placeholder = date == null) { onOpen(BirthField.Date) }
                BirthField.Time -> FieldChip(timeText, placeholder = false) { onOpen(BirthField.Time) }
                else -> FieldChip(cityText, placeholder = false) { onOpen(BirthField.City) }
            }
        }
    }
}

@Composable
private fun FieldChip(text: String, placeholder: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = Anicca.spacing.touch)
            .inset(Anicca.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Anicca.spacing.m),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = Anicca.type.headline,
            color = if (placeholder) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
        )
    }
}

internal sealed interface TemplatePart {
    data class Text(val text: String) : TemplatePart
    data class Slot(val field: BirthField) : TemplatePart
}

private val slots = listOf("{date}" to BirthField.Date, "{time}" to BirthField.Time, "{city}" to BirthField.City)

/**
 * Splits "I was born on {date} at {time} in {city}" into text and slots, in order. Plain string search, not a regex:
 * Android's regex engine rejects an unescaped `}` that the desktop JVM accepts.
 */
internal fun splitTemplate(template: String): List<TemplatePart> {
    val out = mutableListOf<TemplatePart>()
    var rest = template
    while (true) {
        val next = slots.map { (token, field) -> Triple(rest.indexOf(token), token, field) }
            .filter { it.first >= 0 }
            .minByOrNull { it.first }
        if (next == null) break
        val (at, token, field) = next
        rest.substring(0, at).trim().takeIf { it.isNotEmpty() }?.let { out += TemplatePart.Text(it) }
        out += TemplatePart.Slot(field)
        rest = rest.substring(at + token.length)
    }
    rest.trim().takeIf { it.isNotEmpty() }?.let { out += TemplatePart.Text(it) }
    return out
}
