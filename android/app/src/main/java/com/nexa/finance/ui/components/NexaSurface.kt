package com.nexa.finance.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.nexa.finance.ui.theme.NexaElevation
import com.nexa.finance.ui.theme.NexaRadii

/**
 * NEXA surface primitive.
 *
 * Material 3 Surface with NEXA defaults. Screens should prefer this wrapper
 * when they need an explicit surface instead of creating local styling.
 */
@Composable
fun NexaSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(NexaRadii.lg),
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = NexaElevation.none,
    shadowElevation: Dp = NexaElevation.none,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        content = content,
    )
}
