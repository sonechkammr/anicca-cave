package com.lifeclock.widget.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The one spacing scale. Screens use these instead of loose dp numbers. */
data class Spacing(
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 28.dp,
    val xxxl: Dp = 40.dp,
    /** Smallest touch target. */
    val touch: Dp = 48.dp,
)

/** Three corner sizes, taken from what the app already uses (panel 20, age card 28). */
data class AniccaShapes(
    val small: RoundedCornerShape = RoundedCornerShape(12.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(20.dp),
    val large: RoundedCornerShape = RoundedCornerShape(28.dp),
)

/**
 * Type scale in sp, so it follows the system font size. System font; sizes match what the screens set by hand
 * today. Numeric styles use tabular figures so digits don't jitter as they change.
 */
data class AniccaType(
    val hero: TextStyle = TextStyle(
        fontSize = 88.sp, fontWeight = FontWeight.Medium, letterSpacing = (-3).sp, fontFeatureSettings = TNUM,
    ),
    val heroFraction: TextStyle = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TNUM),
    val screenTitle: TextStyle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Medium),
    val headline: TextStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium),
    val cellValue: TextStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TNUM),
    val statValue: TextStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TNUM),
    val title: TextStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium),
    val label: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    val body: TextStyle = TextStyle(fontSize = 16.sp),
    val number: TextStyle = TextStyle(fontSize = 16.sp, fontFeatureSettings = TNUM),
    val caption: TextStyle = TextStyle(fontSize = 13.sp),
    val small: TextStyle = TextStyle(fontSize = 12.sp),
    val micro: TextStyle = TextStyle(fontSize = 11.sp),
)

private const val TNUM = "tnum"

internal val LocalSpacing = staticCompositionLocalOf { Spacing() }
internal val LocalAniccaShapes = staticCompositionLocalOf { AniccaShapes() }
internal val LocalAniccaType = staticCompositionLocalOf { AniccaType() }

/** Entry point to the tokens: `Anicca.spacing.l`, `Anicca.type.hero`, `Anicca.shapes.medium`. */
object Anicca {
    val spacing: Spacing @Composable @ReadOnlyComposable get() = LocalSpacing.current
    val shapes: AniccaShapes @Composable @ReadOnlyComposable get() = LocalAniccaShapes.current
    val type: AniccaType @Composable @ReadOnlyComposable get() = LocalAniccaType.current
}
