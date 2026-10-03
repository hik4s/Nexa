package com.nexa.finance.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

enum class NexaMoneyTone {
    Neutral,
    Income,
    Expense,
    Accent,
}

internal fun formatNexaMoney(
    amountInMinorUnits: Long,
    currencyCode: String = "BRL",
    locale: Locale = Locale.forLanguageTag("pt-BR"),
    showSign: Boolean = false,
): String {
    val currency = Currency.getInstance(currencyCode)
    val fractionDigits = currency.defaultFractionDigits.coerceAtLeast(0)
    val amount = BigDecimal.valueOf(amountInMinorUnits).movePointLeft(fractionDigits)
    val formatter = NumberFormat.getCurrencyInstance(locale).apply {
        this.currency = currency
        minimumFractionDigits = fractionDigits
        maximumFractionDigits = fractionDigits
    }
    val formatted = formatter.format(amount)

    return if (showSign && amount.signum() > 0) {
        "+$formatted"
    } else {
        formatted
    }
}

/**
 * NEXA monetary value primitive.
 *
 * Amounts are supplied in minor currency units so formatting never relies on
 * floating-point arithmetic. Semantic tone is explicit instead of inferred.
 */
@Composable
fun NexaMoneyText(
    amountInMinorUnits: Long,
    modifier: Modifier = Modifier,
    currencyCode: String = "BRL",
    tone: NexaMoneyTone = NexaMoneyTone.Neutral,
    showSign: Boolean = false,
    style: TextStyle = MaterialTheme.typography.headlineSmall,
    locale: Locale = Locale.forLanguageTag("pt-BR"),
) {
    val formatted = remember(amountInMinorUnits, currencyCode, showSign, locale) {
        formatNexaMoney(
            amountInMinorUnits = amountInMinorUnits,
            currencyCode = currencyCode,
            locale = locale,
            showSign = showSign,
        )
    }

    val color = when (tone) {
        NexaMoneyTone.Neutral -> MaterialTheme.colorScheme.onSurface
        NexaMoneyTone.Income -> MaterialTheme.colorScheme.secondary
        NexaMoneyTone.Expense -> MaterialTheme.colorScheme.error
        NexaMoneyTone.Accent -> MaterialTheme.colorScheme.tertiary
    }

    Text(
        text = formatted,
        modifier = modifier,
        style = style,
        color = color,
    )
}
