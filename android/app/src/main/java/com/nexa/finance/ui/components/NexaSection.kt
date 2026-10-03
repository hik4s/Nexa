package com.nexa.finance.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nexa.finance.ui.theme.NexaSpacing
import androidx.compose.material3.MaterialTheme

/**
 * NEXA content section primitive.
 *
 * Provides the default vertical rhythm for titled content blocks without
 * encoding business-specific layout decisions.
 */
@Composable
fun NexaSection(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = NexaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(NexaSpacing.sm),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        content()
    }
}
