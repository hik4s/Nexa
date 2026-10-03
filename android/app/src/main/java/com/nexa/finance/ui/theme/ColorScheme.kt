package com.nexa.finance.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val ColorLightPrimaryContainer = Color(0xFFE9D9DE)
private val ColorLightSecondaryContainer = Color(0xFFDCE8E0)
private val ColorLightTertiaryContainer = Color(0xFFE9E0C8)
private val ColorLightSurfaceVariant = Color(0xFFE9E5DC)
private val ColorLightOnSurfaceVariant = Color(0xFF4B4744)
private val ColorLightSurfaceContainerLowest = Color(0xFFFFFFFF)
private val ColorLightSurfaceContainerLow = Color(0xFFF2EEE6)
private val ColorLightSurfaceContainer = Color(0xFFEDE9E0)
private val ColorLightSurfaceContainerHigh = Color(0xFFE7E3DA)
private val ColorLightSurfaceContainerHighest = Color(0xFFE1DDD4)
private val ColorLightSurfaceBright = Color(0xFFFBF8F1)
private val ColorLightSurfaceDim = Color(0xFFD8D4CC)
private val ColorLightErrorContainer = Color(0xFFF9DEDC)

private val ColorDarkPrimaryContainer = Color(0xFF7B3445)
private val ColorDarkSecondaryContainer = Color(0xFF2A5540)
private val ColorDarkTertiaryContainer = Color(0xFF544A2E)
private val ColorDarkSurfaceVariant = Color(0xFF242426)
private val ColorDarkSurfaceContainerLowest = Color(0xFF070708)
private val ColorDarkSurfaceContainerLow = Color(0xFF0D0D0E)
private val ColorDarkSurfaceContainer = Color(0xFF141415)
private val ColorDarkSurfaceContainerHigh = Color(0xFF1A1A1B)
private val ColorDarkSurfaceContainerHighest = Color(0xFF202021)
private val ColorDarkSurfaceBright = Color(0xFF2A2A2B)
private val ColorDarkSurfaceDim = Color(0xFF050506)
private val ColorDarkErrorContainer = Color(0xFF8C1D18)

private val ErrorRed = Color(0xFFB3261E)
private val ErrorLight = Color(0xFFFFB4AB)
private val ErrorDark = Color(0xFF8C1D18)

val NexaLightColorScheme: ColorScheme = lightColorScheme(
    primary = Burgundy,
    onPrimary = Ivory,
    primaryContainer = ColorLightPrimaryContainer,
    onPrimaryContainer = Obsidian,
    secondary = DeepGreen,
    onSecondary = Ivory,
    secondaryContainer = ColorLightSecondaryContainer,
    onSecondaryContainer = Obsidian,
    tertiary = Champagne,
    onTertiary = Obsidian,
    tertiaryContainer = ColorLightTertiaryContainer,
    onTertiaryContainer = Obsidian,
    background = Ivory,
    onBackground = Obsidian,
    surface = Ivory,
    onSurface = Obsidian,
    surfaceVariant = ColorLightSurfaceVariant,
    onSurfaceVariant = ColorLightOnSurfaceVariant,
    surfaceDim = ColorLightSurfaceDim,
    surfaceBright = ColorLightSurfaceBright,
    surfaceContainerLowest = ColorLightSurfaceContainerLowest,
    surfaceContainerLow = ColorLightSurfaceContainerLow,
    surfaceContainer = ColorLightSurfaceContainer,
    surfaceContainerHigh = ColorLightSurfaceContainerHigh,
    surfaceContainerHighest = ColorLightSurfaceContainerHighest,
    outline = SoftGray,
    outlineVariant = Color(0xFFD2CEC5),
    error = ErrorRed,
    onError = Ivory,
    errorContainer = ColorLightErrorContainer,
    onErrorContainer = ErrorDark,
)

val NexaDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Burgundy,
    onPrimary = Ivory,
    primaryContainer = ColorDarkPrimaryContainer,
    onPrimaryContainer = Ivory,
    secondary = DeepGreen,
    onSecondary = Ivory,
    secondaryContainer = ColorDarkSecondaryContainer,
    onSecondaryContainer = Ivory,
    tertiary = Champagne,
    onTertiary = Obsidian,
    tertiaryContainer = ColorDarkTertiaryContainer,
    onTertiaryContainer = Ivory,
    background = Obsidian,
    onBackground = Ivory,
    surface = Obsidian,
    onSurface = Ivory,
    surfaceVariant = ColorDarkSurfaceVariant,
    onSurfaceVariant = Ivory,
    surfaceDim = ColorDarkSurfaceDim,
    surfaceBright = ColorDarkSurfaceBright,
    surfaceContainerLowest = ColorDarkSurfaceContainerLowest,
    surfaceContainerLow = ColorDarkSurfaceContainerLow,
    surfaceContainer = ColorDarkSurfaceContainer,
    surfaceContainerHigh = ColorDarkSurfaceContainerHigh,
    surfaceContainerHighest = ColorDarkSurfaceContainerHighest,
    outline = SoftGray,
    outlineVariant = Color(0xFF48484A),
    error = ErrorLight,
    onError = Obsidian,
    errorContainer = ColorDarkErrorContainer,
    onErrorContainer = Ivory,
)
