package com.nexa.finance.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized NEXA corner-radius scale.
 *
 * The scale keeps the interface restrained: small radii for compact controls,
 * medium radii for standard surfaces, and one semantic pill radius.
 */
object NexaRadii {
    val none: Dp = 0.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val pill: Dp = 999.dp
}
