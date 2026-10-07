package com.nexa.finance.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val LightPrimaryContainer = Color(0xFFD8EFE7)
private val LightSecondaryContainer = Color(0xFFF0DCE0)
private val LightTertiaryContainer = Color(0xFFF1E8D6)
private val LightSurfaceVariant = Color(0xFFE9ECE9)
private val LightOnSurfaceVariant = Color(0xFF46504B)
private val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
private val LightSurfaceContainerLow = Color(0xFFF7F7F4)
private val LightSurfaceContainer = Color(0xFFF1F2EF)
private val LightSurfaceContainerHigh = Color(0xFFECEEEB)
private val LightSurfaceContainerHighest = Color(0xFFE6E9E6)
private val LightSurfaceBright = Color(0xFFFAFAF7)
private val LightSurfaceDim = Color(0xFFD9DDD9)
private val LightErrorContainer = Color(0xFFFFDAD6)

private val DarkPrimaryContainer = Color(0xFF0C513B)
private val DarkSecondaryContainer = Color(0xFF5F1A28)
private val DarkTertiaryContainer = Color(0xFF5B4B2E)
private val DarkSurfaceVariant = Color(0xFF1D2924)
private val DarkSurfaceContainerLowest = Color(0xFF070B09)
private val DarkSurfaceContainerLow = Color(0xFF0B1210)
private val DarkSurfaceContainer = Color(0xFF101916)
private val DarkSurfaceContainerHigh = Color(0xFF17211E)
private val DarkSurfaceContainerHighest = Color(0xFF1E2925)
private val DarkSurfaceBright = Color(0xFF26332E)
private val DarkSurfaceDim = Color(0xFF050806)
private val DarkErrorContainer = Color(0xFF8C1D18)

private val ErrorRed = Color(0xFFBA1A1A)
private val ErrorLight = Color(0xFFFFB4AB)
private val ErrorDark = Color(0xFF93000A)

val NexaLightColorScheme: ColorScheme = lightColorScheme(
    primary = DeepGreen,
    onPrimary = Ivory,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = Obsidian,
    secondary = Burgundy,
    onSecondary = Ivory,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = Obsidian,
    tertiary = Champagne,
    onTertiary = Obsidian,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = Obsidian,
    background = Ivory,
    onBackground = Obsidian,
    surface = Ivory,
    onSurface = Obsidian,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceDim = LightSurfaceDim,
    surfaceBright = LightSurfaceBright,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = SoftGray,
    outlineVariant = Color(0xFFD5DDD9),
    error = ErrorRed,
    onError = Ivory,
    errorContainer = LightErrorContainer,
    onErrorContainer = ErrorDark,
)

val NexaDarkColorScheme: ColorScheme = darkColorScheme(
    primary = DeepGreen,
    onPrimary = Ivory,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = Ivory,
    secondary = Burgundy,
    onSecondary = Ivory,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = Ivory,
    tertiary = Champagne,
    onTertiary = Obsidian,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = Ivory,
    background = Obsidian,
    onBackground = Ivory,
    surface = Obsidian,
    onSurface = Ivory,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Ivory,
    surfaceDim = DarkSurfaceDim,
    surfaceBright = DarkSurfaceBright,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = SoftGray,
    outlineVariant = Color(0xFF42514B),
    error = ErrorLight,
    onError = Obsidian,
    errorContainer = DarkErrorContainer,
    onErrorContainer = Ivory,
)
