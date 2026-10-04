package com.nexa.finance.application.goal

import com.nexa.finance.domain.goal.Goal
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.max

enum class GoalProjectionStatus {
    NO_CONTRIBUTION,
    ON_TRACK,
    AT_RISK,
    NO_DEADLINE,
    ACHIEVED,
    PAUSED,
}

data class GoalProjection(
    val projectedDate: LocalDate?,
    val averageMonthlyContributionMinor: Long?,
    val status: GoalProjectionStatus,
)

object GoalProjectionCalculator {
    fun calculate(goal: Goal, today: LocalDate): GoalProjection {
        if (goal.status.name == "ACHIEVED" || goal.currentAmount == goal.targetAmount) {
            return GoalProjection(today, null, GoalProjectionStatus.ACHIEVED)
        }
        if (goal.status.name == "PAUSED") {
            return GoalProjection(null, null, GoalProjectionStatus.PAUSED)
        }
        if (goal.currentAmount.amountInMinor <= 0) {
            return GoalProjection(null, null, GoalProjectionStatus.NO_CONTRIBUTION)
        }

        val createdDate = goal.createdAt.atZone(ZoneOffset.UTC).toLocalDate()
        val monthsElapsed = max(
            1,
            ChronoUnit.MONTHS.between(createdDate.withDayOfMonth(1), today.withDayOfMonth(1)).toInt() + 1,
        )
        val averageMonthly = max(
            1L,
            goal.currentAmount.amountInMinor / monthsElapsed,
        )
        val remaining = goal.targetAmount.amountInMinor - goal.currentAmount.amountInMinor
        val monthsNeeded = (remaining + averageMonthly - 1) / averageMonthly
        val projectedDate = today.plusMonths(monthsNeeded)

        val status = when {
            goal.targetDate == null -> GoalProjectionStatus.NO_DEADLINE
            projectedDate <= goal.targetDate -> GoalProjectionStatus.ON_TRACK
            else -> GoalProjectionStatus.AT_RISK
        }
        return GoalProjection(
            projectedDate = projectedDate,
            averageMonthlyContributionMinor = averageMonthly,
            status = status,
        )
    }
}
