package com.lifeclock.widget.ui.widgets

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeclock.widget.Prefs
import com.lifeclock.widget.R
import com.lifeclock.widget.WidgetPreview
import com.lifeclock.widget.ui.components.EmptyPrompt
import com.lifeclock.widget.ui.components.Panel
import com.lifeclock.widget.ui.components.Screen
import com.lifeclock.widget.ui.components.numberStyle
import com.lifeclock.widget.widget.WidgetRefresh
import com.lifeclock.widget.widget.WidgetRegistry
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import kotlin.math.roundToInt

@Composable
fun WidgetsScreen(birth: ZonedDateTime?, onGoSettings: () -> Unit) {
    if (birth == null) { EmptyPrompt(onGoSettings); return }
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var opacity by remember { mutableFloatStateOf(Prefs.opacityDefault(ctx).toFloat()) }
    var saved by remember { mutableStateOf(false) }
    LaunchedEffect(saved) { if (saved) { delay(1600); saved = false } }

    Screen {
        Column {
            Text(stringResource(R.string.widgets_title), fontSize = 26.sp, fontWeight = FontWeight.Medium)
            Text(stringResource(R.string.widgets_subtitle), fontSize = 13.sp, color = cs.onSurfaceVariant)
        }

        val pinUnsupported = stringResource(R.string.pin_unsupported)
        val groups = WidgetRegistry.all.groupBy { it.group }
        groups.forEach { (group, specs) ->
            if (groups.size > 1) {
                Text(stringResource(group.title), fontSize = 14.sp, color = cs.onSurfaceVariant)
            }
            specs.forEach { spec ->
                Panel(Modifier.fillMaxWidth()) {
                    Text(stringResource(spec.label), fontWeight = FontWeight.Medium, fontSize = 17.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(spec.blurb), fontSize = 12.sp, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    val preview = Prefs.widgetConfig(ctx, 0, spec.kind).copy(scrim = 1)
                    WidgetPreview(0, spec.kind, spec.previewWidthDp, spec.previewHeightDp, preview)
                    spec.extraPreview?.let { (w, h) ->
                        Spacer(Modifier.height(8.dp))
                        WidgetPreview(0, spec.kind, w, h, preview)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (!WidgetRefresh.requestPin(ctx, spec.kind)) {
                                Toast.makeText(ctx, pinUnsupported, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.widget_add)) }
                }
            }
        }

        Panel(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.opacity_title), fontWeight = FontWeight.Medium)
                Text("${opacity.roundToInt()}%", style = numberStyle)
            }
            Slider(
                value = opacity,
                onValueChange = { opacity = it },
                onValueChangeFinished = {
                    WidgetRefresh.setOpacityAll(ctx, opacity.roundToInt())
                    saved = true
                },
                valueRange = 0f..100f,
                steps = 9,
            )
            Text(
                stringResource(if (saved) R.string.opacity_done else R.string.opacity_hint),
                fontSize = 12.sp, color = cs.onSurfaceVariant,
            )
        }
        Text(stringResource(R.string.widgets_footer), fontSize = 12.sp, color = cs.onSurfaceVariant)
    }
}
