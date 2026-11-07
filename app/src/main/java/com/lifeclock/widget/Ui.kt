package com.lifeclock.widget

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** A plain tonal block for grouping content. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    radius: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(radius),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

private class Particle(val angle: Float, val speed: Float, val radius: Float, val color: Int)

/** Dots flying out from the upper middle. Plays whenever [trigger] changes to a new non-zero value. */
@Composable
fun Confetti(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger == 0) return
    val progress = remember(trigger) { Animatable(0f) }
    val particles = remember(trigger) {
        val rnd = java.util.Random()
        List(80) {
            Particle(
                angle = rnd.nextFloat() * 2f * PI.toFloat(),
                speed = 0.25f + rnd.nextFloat() * 0.85f,
                radius = 2f + rnd.nextFloat() * 3.5f,
                color = rnd.nextInt(4),
            )
        }
    }
    LaunchedEffect(trigger) { progress.animateTo(1f, tween(2600, easing = LinearEasing)) }
    val cs = MaterialTheme.colorScheme
    val colors = listOf(cs.primary, cs.secondary, cs.tertiary, Color.White)
    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        for (p in particles) {
            val x = size.width * 0.5f + cos(p.angle) * p.speed * t * size.width * 0.7f
            val y = size.height * 0.3f + sin(p.angle) * p.speed * t * size.width * 0.7f + 1100f * t * t
            drawCircle(colors[p.color].copy(alpha = 1f - t), p.radius.dp.toPx(), Offset(x, y))
        }
    }
}

/** Date, time and time zone of birth. */
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
