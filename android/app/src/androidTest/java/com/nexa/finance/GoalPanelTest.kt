package com.nexa.finance

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoalPanelTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun rendersGoalProgressDeadlineContributionAndHistory() {
        val goal = Goal(
            id = "goal-test",
            name = "Viagem",
            targetAmountMinor = 200_000,
            currentAmountMinor = 80_000,
            remainingMinor = 120_000,
            progressPercent = 40,
            currency = "BRL",
            targetDate = "2027-01-15",
            description = "Férias",
            status = "ACTIVE",
            projectedDate = "2026-12-15",
            averageMonthlyContributionMinor = 40_000,
            projectionStatus = "ON_TRACK",
        )
        val historical = goal.copy(
            id = "goal-history",
            name = "Reserva antiga",
            currentAmountMinor = 200_000,
            remainingMinor = 0,
            progressPercent = 100,
            status = "ACHIEVED",
        )

        compose.setContent {
            MaterialTheme {
                GoalPanel(
                    api = ApiClient("http://127.0.0.1:1"),
                    goals = listOf(goal, historical),
                    onGoalsChanged = {},
                    onMessage = {},
                )
            }
        }

        compose.onNodeWithTag("dashboard_goals").assertIsDisplayed()
        compose.onNodeWithTag("goal_goal-test").assertIsDisplayed()
        compose.onNodeWithText("Viagem").assertIsDisplayed()
        compose.onNodeWithText("40%", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("goal_deadline_goal-test").assertIsDisplayed()
        compose.onNodeWithTag("goal_progress_goal-test").assertIsDisplayed()
        assertEquals(1, compose.onAllNodesWithTag("goal_contribution_amount").fetchSemanticsNodes().size)
        compose.onNodeWithTag("goal_contribute").assertIsDisplayed()
        compose.onNodeWithTag("goal_name").assertIsDisplayed()
        compose.onNodeWithTag("goal_amount").assertIsDisplayed()
        compose.onNodeWithTag("goal_target_date").assertIsDisplayed()
        compose.onNodeWithTag("goal_submit").assertIsDisplayed()
        compose.onNodeWithText("Histórico").assertIsDisplayed()
        compose.onNodeWithTag("goal_history_goal-history").assertIsDisplayed()
    }
}
