package com.lifeclock.widget.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
