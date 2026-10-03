package com.nexa.finance.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexa.finance.ui.theme.NexaRadii
import com.nexa.finance.ui.theme.NexaSpacing

enum class NexaButtonVariant {
    Primary,
    Secondary,
    Tonal,
    Text,
}

private val NexaButtonContentPadding = PaddingValues(
    horizontal = NexaSpacing.lg,
    vertical = NexaSpacing.sm,
)

/**
 * NEXA button primitive.
 *
 * Variants share the same shape, minimum touch size and loading behavior.
 * Screen code should select a variant instead of creating local button styling.
 */
@Composable
fun NexaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NexaButtonVariant = NexaButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(NexaRadii.md)
    val effectiveEnabled = enabled && !loading
    val buttonModifier = modifier.defaultMinSize(minHeight = NexaSpacing.huge)

    when (variant) {
        NexaButtonVariant.Primary -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = effectiveEnabled,
            shape = shape,
            contentPadding = NexaButtonContentPadding,
        ) {
            NexaButtonContent(loading, MaterialTheme.colorScheme.onPrimary, content)
        }

        NexaButtonVariant.Secondary -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = effectiveEnabled,
            shape = shape,
            contentPadding = NexaButtonContentPadding,
        ) {
            NexaButtonContent(loading, MaterialTheme.colorScheme.primary, content)
        }

        NexaButtonVariant.Tonal -> FilledTonalButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = effectiveEnabled,
            shape = shape,
            contentPadding = NexaButtonContentPadding,
        ) {
            NexaButtonContent(loading, MaterialTheme.colorScheme.onSecondaryContainer, content)
        }

        NexaButtonVariant.Text -> TextButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = effectiveEnabled,
            shape = shape,
            contentPadding = NexaButtonContentPadding,
        ) {
            NexaButtonContent(loading, MaterialTheme.colorScheme.primary, content)
        }
    }
}

@Composable
private fun RowScope.NexaButtonContent(
    loading: Boolean,
    indicatorColor: Color,
    content: @Composable RowScope.() -> Unit,
) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = indicatorColor,
            strokeWidth = 2.dp,
        )
    } else {
        content()
    }
}
