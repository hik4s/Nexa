package com.nexa.finance

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BudgetPanelTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun rendersProgressAlertCategoryAndHistory() {
        val category = Category("food", "Alimentação", "EXPENSE", "food")
        val budget = Budget(
            id = "budget-test",
            amountMinor = 100_000,
            consumedMinor = 80_000,
            remainingMinor = 20_000,
            progressPercent = 80,
            alertLevel = "WARNING",
            currency = "BRL",
            startOn = "2026-10-01",
            endOn = "2026-10-31",
            categoryId = "food",
            description = "Alimentação do mês",
            status = "ACTIVE",
        )
        val historical = budget.copy(
            id = "budget-history",
            startOn = "2026-09-01",
            endOn = "2026-09-30",
            status = "COMPLETED",
        )

        compose.setContent {
            MaterialTheme {
                BudgetPanel(
                    api = ApiClient("http://127.0.0.1:1"),
                    budgets = listOf(budget, historical),
                    categories = listOf(category),
                    onBudgetsChanged = {},
                    onMessage = {},
                )
            }
        }

        compose.onNodeWithTag("dashboard_budget").assertIsDisplayed()
        compose.onNodeWithTag("budget_budget-test").assertIsDisplayed()
        compose.onNodeWithTag("budget_progress").assertIsDisplayed()
        compose.onNodeWithText("WARNING").assertIsDisplayed()
        compose.onNodeWithText("Alimentação do mês").assertIsDisplayed()
        compose.onNodeWithText("80%", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("budget_amount").assertIsDisplayed()
        compose.onNodeWithTag("budget_category").assertIsDisplayed()
        compose.onNodeWithTag("budget_submit").assertIsDisplayed()
        compose.onNodeWithText("Histórico").assertIsDisplayed()
        compose.onNodeWithTag("budget_history_budget-history").assertIsDisplayed()
    }
}
