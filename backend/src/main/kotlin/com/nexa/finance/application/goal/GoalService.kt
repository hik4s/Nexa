package com.nexa.finance.application.goal

import com.nexa.finance.application.audit.AuditEventRecorder
import com.nexa.finance.application.audit.AuditEventTypes
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.goal.Goal
import com.nexa.finance.domain.goal.GoalContribution
import com.nexa.finance.domain.goal.GoalId
import com.nexa.finance.infrastructure.goal.JdbcGoalRepository
import java.time.Clock
import java.time.LocalDate

data class GoalProgress(
    val goal: Goal,
    val remainingMinor: Long,
    val progressPercent: Int,
    val projection: GoalProjection,
)

class GoalService(
    private val repository: JdbcGoalRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val audit: AuditEventRecorder? = null,
) {
    fun create(
        userId: UserId,
        name: String,
        targetAmountMinor: Long,
        currency: CurrencyCode,
        targetDate: LocalDate?,
        description: String?,
    ): GoalProgress {
        val goal = Goal.create(
            userId = userId,
            name = name,
            targetAmount = Money(targetAmountMinor, currency),
            targetDate = targetDate,
            description = description,
            clock = clock,
        )
        repository.create(goal)
        audit?.record(userId, AuditEventTypes.GOAL_CREATED, "GOAL", goal.id)
        return progressFor(goal)
    }

    fun list(userId: UserId): List<GoalProgress> =
        repository.findByOwner(userId).map(::progressFor)

    fun get(userId: UserId, goalId: GoalId): GoalProgress =
        progressFor(repository.findByOwnerAndId(userId, goalId)
            ?: throw DomainValidationException("Goal was not found."))

    fun contribute(
        userId: UserId,
        goalId: GoalId,
        amountMinor: Long,
        currency: CurrencyCode,
        occurredOn: LocalDate,
    ): Pair<GoalProgress, GoalContribution> {
        val result = repository.contribute(
            ownerId = userId,
            goalId = goalId,
            amount = Money(amountMinor, currency),
            occurredOn = occurredOn,
        )
        audit?.record(userId, AuditEventTypes.GOAL_CONTRIBUTION_ADDED, "GOAL", result.first.goal.id)
        return progressFor(result.first) to result.second
    }

    fun contributions(userId: UserId, goalId: GoalId): List<GoalContribution> {
        get(userId, goalId)
        return repository.listContributions(userId, goalId)
    }

    fun pause(userId: UserId, goalId: GoalId): GoalProgress =
        change(userId, goalId) { it.pause() }

    fun resume(userId: UserId, goalId: GoalId): GoalProgress =
        change(userId, goalId) { it.resume() }

    fun cancel(userId: UserId, goalId: GoalId): GoalProgress =
        change(userId, goalId) { it.cancel() }

    private fun change(userId: UserId, goalId: GoalId, transform: (Goal) -> Goal): GoalProgress {
        val current = repository.findByOwnerAndId(userId, goalId)
            ?: throw DomainValidationException("Goal was not found.")
        val updated = repository.update(transform(current))
            ?: throw DomainValidationException("Goal was not found.")
        audit?.record(userId, AuditEventTypes.GOAL_STATUS_CHANGED, "GOAL", updated.id)
        return progressFor(updated)
    }

    private fun progressFor(goal: Goal): GoalProgress {
        val remaining = (goal.targetAmount.amountInMinor - goal.currentAmount.amountInMinor).coerceAtLeast(0)
        val percent = ((goal.currentAmount.amountInMinor * 100) / goal.targetAmount.amountInMinor)
            .toInt()
            .coerceIn(0, 100)
        val today = LocalDate.now(clock)
        return GoalProgress(
            goal = goal,
            remainingMinor = remaining,
            progressPercent = percent,
            projection = GoalProjectionCalculator.calculate(goal, today),
        )
    }
}
