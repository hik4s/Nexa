package com.nexa.finance.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Burgundy,
    secondary = DeepGreen,
    background = Ivory,
    surface = Ivory,
    onBackground = Obsidian,
    onSurface = Obsidian,
)

private val DarkColors = darkColorScheme(
    primary = Champagne,
    secondary = DeepGreen,
    background = Obsidian,
    surface = Obsidian,
    onBackground = Ivory,
    onSurface = Ivory,
)

@Composable
fun NexaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
