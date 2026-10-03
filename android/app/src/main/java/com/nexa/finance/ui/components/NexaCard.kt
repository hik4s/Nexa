package com.nexa.finance.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.nexa.finance.ui.theme.NexaElevation
import com.nexa.finance.ui.theme.NexaRadii

enum class NexaCardVariant {
    Standard,
    Elevated,
    Outlined,
}

/**
 * NEXA card primitive.
 *
 * Cards share the product's surface language while exposing only intentional
 * structural variants. Business-specific content remains owned by the caller.
 */
@Composable
fun NexaCard(
    modifier: Modifier = Modifier,
    variant: NexaCardVariant = NexaCardVariant.Standard,
    shape: Shape = RoundedCornerShape(NexaRadii.lg),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = when (variant) {
        NexaCardVariant.Standard -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
        NexaCardVariant.Elevated -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
        NexaCardVariant.Outlined -> CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    }

    val border = when (variant) {
        NexaCardVariant.Outlined -> CardDefaults.outlinedCardBorder()
        else -> null
    }

    val elevation = when (variant) {
        NexaCardVariant.Standard -> CardDefaults.cardElevation(
            defaultElevation = NexaElevation.none,
        )
        NexaCardVariant.Elevated -> CardDefaults.cardElevation(
            defaultElevation = NexaElevation.medium,
        )
        NexaCardVariant.Outlined -> CardDefaults.cardElevation(
            defaultElevation = NexaElevation.none,
        )
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            border = border,
            elevation = elevation,
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            border = border,
            elevation = elevation,
            content = content,
        )
    }
}
