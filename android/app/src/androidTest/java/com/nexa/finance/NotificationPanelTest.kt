package com.nexa.finance

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationPanelTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun filtersAndMarksNotificationRead() {
        var readId: String? = null
        val notifications = listOf(
            NexaNotification("n1", "BUDGET_ALERT", "WARNING", "Orçamento ultrapassado", "Alimentação está em 120%.", null, "2026-10-04T12:00:00Z"),
            NexaNotification("n2", "GOAL", "INFO", "Meta Reserva", "Você já alcançou 80%.", "2026-10-04T11:00:00Z", "2026-10-04T11:00:00Z"),
        )

        compose.setContent {
            MaterialTheme {
                NotificationPanel(
                    notifications = notifications,
                    onRead = { readId = it },
                )
            }
        }

        compose.onNodeWithTag("notification_panel").assertIsDisplayed()
        compose.onNodeWithText("1 não lidos").assertIsDisplayed()
        compose.onNodeWithText("Orçamento ultrapassado").assertIsDisplayed()
        compose.onNodeWithTag("notifications_unread").performClick()
        compose.onNodeWithText("Meta Reserva").assertDoesNotExist()
        compose.onNodeWithTag("notification_read_n1").performClick()
        assert(readId == "n1")
    }
}
