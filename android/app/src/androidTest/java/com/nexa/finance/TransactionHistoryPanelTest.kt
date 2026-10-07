package com.nexa.finance

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TransactionHistoryPanelTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun filtersSearchTypePeriodAndShowsTransactionDetails() {
        val today = LocalDate.now()
        val account = Account("account-1", "Conta principal", 100_000, true, "CHECKING", "BRL")
        val category = Category("category-1", "Moradia", "EXPENSE", null)
        val txExpense = Tx(
            id = "tx-expense",
            accountId = account.id,
            description = "Aluguel",
            amountMinor = 150_000,
            direction = "DEBIT",
            date = today.toString(),
            categoryId = category.id,
            cardId = null,
        )
        val txIncome = Tx(
            id = "tx-income",
            accountId = account.id,
            description = "Salário",
            amountMinor = 300_000,
            direction = "CREDIT",
            date = today.toString(),
            categoryId = null,
            cardId = null,
        )
        val txOld = txExpense.copy(
            id = "tx-old",
            description = "Aluguel antigo",
            date = today.minusDays(120).toString(),
        )

        compose.setContent {
            MaterialTheme {
                TransactionHistoryPanel(
                    transactions = listOf(txExpense, txIncome, txOld),
                    accounts = listOf(account),
                    cards = emptyList(),
                    categories = listOf(category),
                )
            }
        }

        compose.onNodeWithTag("transaction_history").assertIsDisplayed()
        compose.onNodeWithTag("history_count").assertIsDisplayed()
        compose.onNodeWithTag("history_type_debit").performClick()
        compose.onNodeWithTag("history_count").assertIsDisplayed()

        compose.onNodeWithTag("history_search").performTextInput("Aluguel")
        compose.onNodeWithTag("history_period_LAST_90_DAYS").performClick()
        compose.onNodeWithTag("history_tx_tx-expense").assertIsDisplayed()
        compose.onNodeWithTag("history_tx_tx-old").assertDoesNotExist()

        compose.onNodeWithTag("history_details_tx-expense").performClick()
        compose.onNodeWithText("Tipo: DEBIT").assertIsDisplayed()
        compose.onNodeWithText("Conta: Conta principal").assertIsDisplayed()
        compose.onNodeWithText("Categoria: Moradia").assertIsDisplayed()

        compose.onNodeWithTag("history_clear_filters").performClick()
        compose.onNodeWithText("Salário").assertIsDisplayed()
    }
}
