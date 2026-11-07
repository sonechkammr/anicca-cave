package com.lifeclock.widget.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lifeclock.widget.ui.theme.Anicca
import com.lifeclock.widget.ui.theme.raised

/** A titled, raised block that holds rows or controls. The title and the text rows inside stay flat. */
@Composable
fun GroupCard(title: String?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Anicca.spacing.s)) {
        if (title != null) Text(title, style = Anicca.type.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.fillMaxWidth().raised(Anicca.shapes.medium).padding(Anicca.spacing.l), content = content)
    }
}

/**
 * Label on the left, value on the right. Tabular figures keep live digits from jittering. At large font sizes, when
 * both no longer fit on one line, the value drops under its label instead of breaking mid-number.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ValueRow(label: String, value: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, accent: Boolean = false) {
    FlowRow(
        modifier
            .fillMaxWidth()
            // Rows that do something keep the 48 dp touch target; plain readouts can be tighter.
            .heightIn(min = if (onClick != null) Anicca.spacing.touch else 36.dp)
            .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
            .padding(vertical = Anicca.spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Anicca.type.body, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = Anicca.spacing.m))
        Text(
            value,
            style = Anicca.type.number,
            softWrap = false,
            color = if (accent || onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
