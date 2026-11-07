package com.lifeclock.widget.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeclock.widget.R
import com.lifeclock.widget.ui.components.BirthForm
import com.lifeclock.widget.ui.components.Panel
import com.lifeclock.widget.ui.components.Screen
import java.time.ZonedDateTime

@Composable
fun SettingsScreen(
    birth: ZonedDateTime?,
    themeMode: Int,
    onBirth: (ZonedDateTime) -> Unit,
    onTheme: (Int) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Screen {
        Column {
            Text(stringResource(R.string.settings_title), fontSize = 26.sp, fontWeight = FontWeight.Medium)
            if (birth == null) {
                Text(stringResource(R.string.settings_start), fontSize = 13.sp, color = cs.onSurfaceVariant)
            }
        }
        Panel(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.birth_title), fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            BirthForm(initial = birth, onSave = onBirth)
        }
        Panel(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.theme_title), fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark).forEachIndexed { i, label ->
                    FilterChip(selected = themeMode == i, onClick = { onTheme(i) }, label = { Text(stringResource(label)) })
                }
            }
        }
        Text(stringResource(R.string.settings_footer), fontSize = 12.sp, color = cs.onSurfaceVariant)
    }
}
