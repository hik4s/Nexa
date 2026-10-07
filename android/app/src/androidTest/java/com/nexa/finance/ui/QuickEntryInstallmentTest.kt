package com.nexa.finance.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nexa.finance.ApiClient
import com.nexa.finance.Card
import com.nexa.finance.Tx
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickEntryInstallmentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun card_purchase_selection_reaches_purchase_callback() {
        val selectedCount = AtomicInteger(0)
        val selectedCardId = AtomicReference("")
        val selectedAmount = AtomicLong(0)
        val selectedDescription = AtomicReference("")

        val card = Card(
            id = "card-test",
            liabilityAccountId = "liability-test",
            name = "NEXA Visa",
            brand = "VISA",
            lastFourDigits = "1234",
            creditLimitMinor = 100_000,
            availableLimitMinor = 100_000,
            currency = "BRL",
            closingDay = 10,
            dueDay = 20,
            status = "ACTIVE",
        )

        compose.setContent {
            MaterialTheme {
                QuickEntryCard(
                    api = ApiClient("http://127.0.0.1:1"),
                    accounts = emptyList(),
                    cards = listOf(card),
                    categories = emptyList(),
                    onTransactionAdded = {},
                    onMessage = {},
                    onCardPurchase = { cardId, amountMinor, description, installmentCount ->
                        selectedCardId.set(cardId)
                        selectedAmount.set(amountMinor)
                        selectedDescription.set(description)
                        selectedCount.set(installmentCount)
                        Tx(
                            id = "tx-test",
                            accountId = "liability-test",
                            description = description,
                            amountMinor = amountMinor,
                            direction = "DEBIT",
                            date = "2026-10-04",
                            categoryId = null,
                            cardId = cardId,
                            invoiceId = null,
                            installmentId = "installment-test",
                            installmentNumber = installmentCount,
                        )
                    },
                    initialInput = "120,00 mercado",
                )
            }
        }

        compose.onNodeWithText("Despesa · mercado").assertIsDisplayed()

        compose.onNodeWithTag("quick_card_mode").performClick()
        compose.onNodeWithTag("quick_installments").performClick()
        compose.onNodeWithText("3 parcelas").performClick()

        compose.onNodeWithText("Parcelas: 3 x").assertIsDisplayed()
        compose.onNodeWithTag("quick_submit").performClick()

        compose.waitUntil(5_000) { selectedCount.get() == 3 }

        check(selectedCardId.get() == "card-test")
        check(selectedAmount.get() == 12_000L)
        check(selectedDescription.get() == "mercado")
        check(selectedCount.get() == 3)
    }
}
