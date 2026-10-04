package com.nexa.finance

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinanceE2ETest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun scrollToDashboard(tag: String) {
        compose.onNodeWithTag("dashboard_list").performScrollToNode(hasTestTag(tag))
    }

    @Test
    fun quick_entry_parses_natural_language_and_registers_expense() {
        val suffix = UUID.randomUUID().toString().take(8)
        val name = "Kauan Quick E2E"
        val email = "quick.$suffix@example.com"
        val password = "NexaE2e1234"

        compose.onNodeWithText("Criar uma conta").performClick()
        compose.onNodeWithTag("auth_name").performTextInput(name)
        compose.onNodeWithTag("auth_email").performTextInput(email)
        compose.onNodeWithTag("auth_password").performTextInput(password)
        compose.onNodeWithTag("auth_submit").performClick()

        compose.waitUntil(15_000) {
            compose.onAllNodes(hasText("Conta criada. Faça login.")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Já tenho uma conta").performClick()
        compose.onNodeWithTag("auth_submit").performClick()

        compose.waitUntil(15_000) {
            compose.onAllNodes(hasText("NEXA Finance")).fetchSemanticsNodes().isNotEmpty()
        }
        scrollToDashboard("account_name")

        compose.onNodeWithTag("account_name").performTextInput("Conta Quick E2E")
        compose.onNodeWithTag("account_submit").performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasText("Conta Quick E2E")).fetchSemanticsNodes().isNotEmpty()
        }
        scrollToDashboard("quick_input")
        compose.onNodeWithTag("quick_input").performTextInput("18,90 almoço")
        compose.onNodeWithText("Despesa · almoço").assertIsDisplayed()
        compose.onNodeWithText("R$ 18,90").assertIsDisplayed()
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasText("Categoria: Alimentação")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Categoria: Alimentação").assertIsDisplayed()
        compose.onNodeWithTag("quick_submit").performClick()

        compose.waitUntil(15_000) {
            compose.onAllNodes(hasText("Lançamento registrado.")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("quick_status").assertIsDisplayed()
        scrollToDashboard("dashboard_expenses")
        compose.onNodeWithTag("dashboard_expenses").assertIsDisplayed()
        scrollToDashboard("dashboard_result")
        compose.onNodeWithTag("dashboard_result").assertIsDisplayed()
        scrollToDashboard("dashboard_categories")
        compose.onNodeWithTag("dashboard_categories").assertIsDisplayed()
        scrollToDashboard("dashboard_evolution")
        compose.onNodeWithTag("dashboard_evolution").assertIsDisplayed()
        scrollToDashboard("dashboard_budget")
        compose.onNodeWithTag("dashboard_budget").assertIsDisplayed()
        scrollToDashboard("dashboard_insight")
        compose.onNodeWithTag("dashboard_insight").assertIsDisplayed()
    }

    @Test
    fun register_login_account_and_transaction_flow() {
        val suffix = UUID.randomUUID().toString().take(8)
        val name = "Kauan E2E"
        val email = "e2e.$suffix@example.com"
        val password = "NexaE2e1234"

        compose.onNodeWithText("Criar uma conta").performClick()
        compose.onNodeWithTag("auth_name").performTextInput(name)
        compose.onNodeWithTag("auth_email").performTextInput(email)
        compose.onNodeWithTag("auth_password").performTextInput(password)
        compose.onNodeWithTag("auth_submit").performClick()

        compose.waitUntil(15_000) {
            compose.onAllNodes(hasTestTag("auth_message")).fetchSemanticsNodes().isNotEmpty()
        }
        val authText = compose.onNodeWithTag("auth_message").fetchSemanticsNode().config[SemanticsProperties.Text].joinToString()
        println("NEXA_E2E_AUTH=$authText")
        compose.onNodeWithText("Conta criada. Faça login.").assertIsDisplayed()

        compose.onNodeWithText("Já tenho uma conta").performClick()
        compose.onNodeWithTag("auth_submit").performClick()

        compose.waitUntil(15_000) {
            compose.onAllNodes(hasText("NEXA Finance")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("NEXA Finance").assertIsDisplayed()
        scrollToDashboard("account_name")

        compose.onNodeWithTag("account_name").performTextInput("Conta E2E")
        compose.onNodeWithTag("account_submit").performClick()

        compose.waitUntil(15_000) {
            compose.onAllNodes(hasText("Conta E2E")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Conta E2E").assertIsDisplayed()
        scrollToDashboard("quick_input")

        compose.onNodeWithTag("quick_input").performTextInput("123,45 teste e2e")
        compose.onNodeWithTag("quick_submit").performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasText("Lançamento registrado.")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("quick_status").assertIsDisplayed()

        scrollToDashboard("goal_name")
        compose.onNodeWithTag("goal_name").performTextInput("Meta E2E")
        compose.onNodeWithTag("goal_amount").performTextInput("100,00")
        scrollToDashboard("goal_submit")
        val goalSubmitSemantics = compose.onNodeWithTag("goal_submit").fetchSemanticsNode()
        assert(!goalSubmitSemantics.config.contains(SemanticsProperties.Disabled))
        compose.onNodeWithTag("goal_submit").assertIsDisplayed()
        compose.onNodeWithTag("goal_submit").performTouchInput { click() }

        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("goal_form_message").fetchSemanticsNodes().isNotEmpty()
        }
        val createMessage = compose.onNodeWithTag("goal_form_message")
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .joinToString()
        assertEquals("Meta criada.", createMessage)

        scrollToDashboard("goal_contribution_amount")
        compose.onNodeWithTag("goal_contribution_amount").performTextInput("100,00")
        compose.onNodeWithTag("goal_contribute").performClick()

        compose.waitUntil(15_000) {
            compose.onNodeWithTag("goal_form_message")
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .joinToString() == "Meta atingida."
        }
        assertEquals(
            "Meta atingida.",
            compose.onNodeWithTag("goal_form_message")
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .joinToString()
        )
        scrollToDashboard("dashboard_expenses")
        compose.onNodeWithTag("dashboard_expenses").assertIsDisplayed()
    }
}
