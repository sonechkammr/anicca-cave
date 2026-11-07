package com.lifeclock.widget.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * "Medium" depth: raised cards, tiles, buttons and widget plates; inset wells for tracks and fields; text rows
 * stay flat. Built from the standard shadow modifier, a gradient background edge and a gradient rim, so it needs
 * nothing newer than the current Compose BOM. Colours come from the Material You scheme, so it follows light,
 * dark and dynamic colour.
 */

/** A surface that stands above the page: soft shadow, a lit top-left rim, tonal fill. */
@Composable
fun Modifier.raised(
    shape: Shape = Anicca.shapes.medium,
    elevation: Dp = 6.dp,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
): Modifier {
    val dark = isDarkTheme()
    val shadowColor = Color.Black.copy(alpha = if (dark) 0.6f else 0.22f)
    val rim = Color.White.copy(alpha = if (dark) 0.10f else 0.75f)
    return this
        .shadow(elevation, shape, ambientColor = shadowColor, spotColor = shadowColor)
        .background(color, shape)
        .border(1.dp, Brush.linearGradient(listOf(rim, Color.Transparent)), shape)
}

/** A well pressed into the page, for tracks and fields: darker fill, shade along the top edge, lit lower rim. */
@Composable
fun Modifier.inset(
    shape: Shape = Anicca.shapes.small,
    color: Color = Color.Unspecified,
): Modifier {
    val dark = isDarkTheme()
    // A well is always darker than the page. surfaceContainerLowest is white in light themes and near black in dark
    // ones, so derive the fill from the page colour instead.
    val fill = if (color != Color.Unspecified) color else
        Color.Black.copy(alpha = if (dark) 0.30f else 0.08f).compositeOver(MaterialTheme.colorScheme.background)
    val shade = Color.Black.copy(alpha = if (dark) 0.55f else 0.16f)
    val rim = Color.White.copy(alpha = if (dark) 0.08f else 0.70f)
    return this
        .clip(shape)
        .background(fill)
        .drawBehind {
            drawRect(Brush.verticalGradient(listOf(shade, Color.Transparent), endY = 8.dp.toPx()))
        }
        .border(1.dp, Brush.verticalGradient(listOf(Color.Transparent, rim)), shape)
}
