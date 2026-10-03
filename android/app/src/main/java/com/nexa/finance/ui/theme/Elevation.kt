package com.nexa.finance.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized NEXA elevation scale.
 *
 * Elevation is intentionally restrained. Most surfaces should remain flat;
 * higher levels are reserved for floating content and transient layers.
 */
object NexaElevation {
    val none: Dp = 0.dp
    val low: Dp = 1.dp
    val medium: Dp = 3.dp
    val high: Dp = 6.dp
    val overlay: Dp = 12.dp
}
