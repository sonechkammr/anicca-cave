package com.lifeclock.widget.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lifeclock.widget.R

/** Every token and both depth modifiers on one page, for checking light, dark, Russian and large text. */
@Composable
internal fun TokenGallery(themeMode: Int = THEME_SYSTEM) {
    AniccaTheme(themeMode) {
        val sp = Anicca.spacing
        val type = Anicca.type
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            Column(
                Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(sp.xl),
                verticalArrangement = Arrangement.spacedBy(sp.l),
            ) {
                Text(stringResource(R.string.widgets_title), style = type.screenTitle)
                Text(
                    stringResource(R.string.widgets_subtitle), style = type.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Raised card with the hero number.
                Column(Modifier.fillMaxWidth().raised(Anicca.shapes.large).padding(sp.l)) {
                    Text(stringResource(R.string.age_label), style = type.caption)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("26", style = type.hero)
                        Text(".48913277", style = type.heroFraction)
                    }
                    // Inset track with a raised fill.
                    Box(Modifier.fillMaxWidth().height(sp.m).inset(Anicca.shapes.small)) {
                        Box(
                            Modifier.fillMaxWidth(0.62f).height(sp.m)
                                .raised(Anicca.shapes.small, elevation = 2.dp, color = MaterialTheme.colorScheme.primary),
                        )
                    }
                }

                // Two tiles and an inset field.
                Row(horizontalArrangement = Arrangement.spacedBy(sp.m)) {
                    for (value in listOf("9 713", "839 203 449")) {
                        Column(Modifier.weight(1f).raised().padding(sp.l)) {
                            Text(stringResource(R.string.stat_days), style = type.small)
                            Text(value, style = type.statValue)
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().inset().padding(horizontal = sp.l, vertical = sp.m)) {
                    Text(stringResource(R.string.birth_title), style = type.body)
                }

                // Flat text rows stay flat.
                Column {
                    Text(stringResource(R.string.settings_title), style = type.title)
                    Text(
                        stringResource(R.string.hint_expand), style = type.label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("1 2 3 4 5 6 7 8 9 0", style = type.number)
                    Text(stringResource(R.string.settings_footer), style = type.micro)
                }

                // Spacing scale.
                Row(horizontalArrangement = Arrangement.spacedBy(sp.s), verticalAlignment = Alignment.Bottom) {
                    for (step in listOf(sp.xs, sp.s, sp.m, sp.l, sp.xl, sp.xxl, sp.xxxl)) {
                        Box(Modifier.width(step).height(step).background(MaterialTheme.colorScheme.primary))
                    }
                }
            }
        }
    }
}

@Preview(name = "Light", widthDp = 380, heightDp = 900)
@Preview(name = "Dark", widthDp = 380, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Russian", widthDp = 380, heightDp = 900, locale = "ru")
@Preview(name = "200% font", widthDp = 380, heightDp = 1500, fontScale = 2f)
@Composable
private fun TokenGalleryPreview() = TokenGallery()
