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

@RunWith(AndroidJUnit4::class)
class AiPanelTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsInsightSourcesAndSendsQuestion() {
        var asked: String? = null
        val analysis = AiAnalysis(
            month = "2026-10",
            provider = "rules-v1",
            monthlySummary = "Entradas de R$ 5.000,00 e despesas de R$ 3.500,00.",
            contextFactCount = 4,
            insights = listOf(
                AiInsight(
                    id = "expense-trend",
                    level = "WARNING",
                    title = "Despesas em alta",
                    message = "As despesas variaram 16,7% em relação ao mês anterior.",
                    sourceFactIds = listOf("expenses-current", "expenses-previous"),
                )
            ),
        )

        compose.setContent {
            MaterialTheme {
                AiPanel(
                    analysis = analysis,
                    answer = AiAnswer("Você gastou R$ 3.500,00.", listOf("expenses-current")),
                    onAsk = { asked = it },
                )
            }
        }

        compose.onNodeWithTag("ai_panel").assertIsDisplayed()
        compose.onNodeWithText("Despesas em alta").assertIsDisplayed()
        compose.onNodeWithText("Fonte: expenses-current, expenses-previous").assertIsDisplayed()
        compose.onNodeWithTag("ai_question").performTextInput("Quanto eu gastei?")
        compose.onNodeWithTag("ai_ask").performClick()
        assert(asked == "Quanto eu gastei?")
        compose.onNodeWithTag("ai_answer").assertIsDisplayed()
    }
}
