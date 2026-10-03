package com.nexa.finance.ui.components

import java.util.Locale
import org.junit.Test
import kotlin.test.assertEquals

class NexaMoneyTest {
    private val brLocale = Locale.forLanguageTag("pt-BR")

    @Test
    fun formatsBrlMinorUnits() {
        assertEquals(
            "R$ 18,90",
            formatNexaMoney(1890, "BRL", brLocale),
        )
    }

    @Test
    fun formatsNegativeValues() {
        assertEquals(
            "-R$ 18,90",
            formatNexaMoney(-1890, "BRL", brLocale),
        )
    }

    @Test
    fun addsPositiveSignWhenRequested() {
        assertEquals(
            "+R$ 18,90",
            formatNexaMoney(1890, "BRL", brLocale, showSign = true),
        )
    }
}
