package com.lifeclock.widget.ui.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * True when the person turned animations off (Settings > Accessibility > Remove animations, or the animation scale in
 * developer options). Decorative motion, like the confetti, stays still then. The widgets keep running: their moving
 * digits are the age itself, and the ECG sweep is what the pulse widget shows.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
