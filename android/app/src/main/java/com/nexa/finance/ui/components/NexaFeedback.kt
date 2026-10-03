package com.nexa.finance.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nexa.finance.ui.theme.NexaSpacing

enum class NexaFeedbackTone {
    Informative,
    Positive,
    Negative,
    Neutral,
}

/**
 * Reusable loading state for screen-level asynchronous work.
 */
@Composable
fun NexaLoadingState(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(NexaSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NexaSpacing.sm),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(NexaSpacing.lg),
            color = MaterialTheme.colorScheme.primary,
        )
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Reusable error state with an optional retry action.
 */
@Composable
fun NexaErrorState(
    message: String,
    modifier: Modifier = Modifier,
    retryLabel: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(NexaSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NexaSpacing.md),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        if (retryLabel != null && onRetry != null) {
            NexaButton(
                onClick = onRetry,
                variant = NexaButtonVariant.Secondary,
            ) {
                Text(retryLabel)
            }
        }
    }
}

/**
 * Compact feedback message for contextual success, warning-like information
 * and neutral confirmations without introducing custom screen colors.
 */
@Composable
fun NexaFeedback(
    message: String,
    modifier: Modifier = Modifier,
    tone: NexaFeedbackTone = NexaFeedbackTone.Informative,
) {
    val color = when (tone) {
        NexaFeedbackTone.Informative -> MaterialTheme.colorScheme.primary
        NexaFeedbackTone.Positive -> MaterialTheme.colorScheme.secondary
        NexaFeedbackTone.Negative -> MaterialTheme.colorScheme.error
        NexaFeedbackTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = message,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
    )
}
