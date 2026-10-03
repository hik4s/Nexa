package com.nexa.finance.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun NexaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) NexaDarkColorScheme else NexaLightColorScheme,
        typography = NexaTypography,
        content = content,
    )
}
