package com.nexa.finance

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.UUID
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinanceE2ETest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

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

        compose.onNodeWithTag("account_name").performTextInput("Conta E2E")
        compose.onNodeWithTag("account_submit").performClick()

        compose.waitUntil(15_000) {
            compose.onAllNodes(hasText("Conta E2E")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("dashboard_list").performScrollToIndex(2)
        compose.onNodeWithText("Conta E2E").assertIsDisplayed()
        compose.onNodeWithTag("dashboard_list").performScrollToIndex(5)

        compose.onNodeWithTag("tx_amount").performTextInput("123,45")
        compose.onNodeWithTag("tx_description").performTextInput("Teste E2E")
        compose.onNodeWithTag("tx_submit").performClick()

        compose.waitUntil(15_000) {
            compose.onAllNodes(hasText("Transação registrada.")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("transaction_status").assertIsDisplayed()
    }
}
