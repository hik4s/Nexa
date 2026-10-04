package com.nexa.finance

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.UUID
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecurrencePanelTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun rendersScheduleAlertNextChargesAndCandidate() {
        val recurrenceId = UUID.randomUUID().toString()
        val recurrence = Recurrence(
            id = recurrenceId,
            accountId = "account-1",
            amountMinor = 12_000,
            transactionType = "EXPENSE",
            frequency = "WEEKLY",
            interval = 1,
            startOn = "2026-10-20",
            endOn = null,
            maxOccurrences = null,
            description = "Internet",
            categoryId = null,
            status = "ACTIVE",
            nextOccurrenceNumber = 1,
            nextOccurrenceOn = "2026-10-20",
            nextOccurrences = listOf("1|2026-10-20", "2|2026-10-27"),
            alertLevel = "DUE_SOON",
        )
        val candidate = RecurrenceCandidate(
            accountId = "account-1",
            amountMinor = 12_000,
            currency = "BRL",
            transactionType = "EXPENSE",
            frequency = "WEEKLY",
            description = "Internet",
            categoryId = null,
            occurrenceCount = 3,
            confidencePercent = 88,
            nextOccurrenceOn = "2026-10-22",
        )

        compose.setContent {
            MaterialTheme {
                RecurrencePanel(
                    api = ApiClient("http://127.0.0.1:1"),
                    recurrences = listOf(recurrence),
                    candidates = listOf(candidate),
                    accounts = listOf(Account("account-1", "Conta", 100_000)),
                    categories = emptyList(),
                    onRecurrencesChanged = {},
                    onCandidatesChanged = {},
                    onMessage = {},
                )
            }
        }

        compose.onNodeWithTag("dashboard_recurring").assertIsDisplayed()
        compose.onNodeWithTag("recurrence_$recurrenceId").assertIsDisplayed()
        compose.onNodeWithTag("recurrence_alert_$recurrenceId").assertIsDisplayed()
        compose.onNodeWithTag("recurrence_next_$recurrenceId").assertIsDisplayed()
        compose.onNodeWithText("DUE_SOON").assertIsDisplayed()
        compose.onNodeWithTag("recurrence_candidate_0").assertIsDisplayed()
        compose.onNodeWithTag("recurrence_candidate_confidence_0").assertIsDisplayed()
        compose.onNodeWithTag("recurrence_submit").assertIsDisplayed()
    }
}
