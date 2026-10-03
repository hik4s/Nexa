package com.nexa.finance.domain.goal

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.ConsistentCopyVisibility

typealias GoalId = UUID

enum class GoalStatus {
    ACTIVE,
    PAUSED,
    ACHIEVED,
    CANCELLED,
}

@ConsistentCopyVisibility
data class Goal private constructor(
    val id: GoalId,
    val userId: UserId,
    val name: String,
    val targetAmount: Money,
    val currentAmount: Money,
    val targetDate: LocalDate?,
    val description: String?,
    val status: GoalStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        if (targetAmount.amountInMinor <= 0) {
            throw DomainValidationException("Goal target amount must be greater than zero.")
        }
        if (currentAmount.amountInMinor < 0) {
            throw DomainValidationException("Goal current amount cannot be negative.")
        }
        if (currentAmount.currency != targetAmount.currency) {
            throw DomainValidationException("Goal amounts must use the same currency.")
        }
        if (currentAmount > targetAmount) {
            throw DomainValidationException("Goal current amount cannot exceed the target amount.")
        }
        if (description?.isBlank() == true) {
            throw DomainValidationException("Goal description cannot be blank.")
        }
        if (description != null && description.length > MAX_DESCRIPTION_LENGTH) {
            throw DomainValidationException("Goal description cannot exceed $MAX_DESCRIPTION_LENGTH characters.")
        }
    }

    val progressRatio: Double
        get() = currentAmount.amountInMinor.toDouble() / targetAmount.amountInMinor.toDouble()

    fun addContribution(amount: Money, now: Instant = Instant.now()): Goal {
        requireContribution(amount)
        val next = currentAmount + amount
        if (next > targetAmount) {
            throw DomainValidationException("Goal contribution cannot exceed the target amount.")
        }
        val nextStatus = if (next == targetAmount) GoalStatus.ACHIEVED else status
        return copy(currentAmount = next, status = nextStatus, updatedAt = now)
    }

    fun removeContribution(amount: Money, now: Instant = Instant.now()): Goal {
        requireContribution(amount)
        val next = currentAmount - amount
        if (next.amountInMinor < 0) {
            throw DomainValidationException("Goal contribution cannot reduce the current amount below zero.")
        }
        val nextStatus = if (status == GoalStatus.ACHIEVED) GoalStatus.ACTIVE else status
        return copy(currentAmount = next, status = nextStatus, updatedAt = now)
    }

    fun pause(now: Instant = Instant.now()): Goal {
        if (status != GoalStatus.ACTIVE) {
            throw DomainValidationException("Only active goals can be paused.")
        }
        return copy(status = GoalStatus.PAUSED, updatedAt = now)
    }

    fun resume(now: Instant = Instant.now()): Goal {
        if (status != GoalStatus.PAUSED) {
            throw DomainValidationException("Only paused goals can be resumed.")
        }
        return copy(status = GoalStatus.ACTIVE, updatedAt = now)
    }

    fun cancel(now: Instant = Instant.now()): Goal {
        if (status == GoalStatus.ACHIEVED) {
            throw DomainValidationException("Achieved goals cannot be cancelled.")
        }
        return copy(status = GoalStatus.CANCELLED, updatedAt = now)
    }

    private fun requireContribution(amount: Money) {
        if (status == GoalStatus.CANCELLED) {
            throw DomainValidationException("Cancelled goals cannot receive contribution changes.")
        }
        if (status == GoalStatus.PAUSED) {
            throw DomainValidationException("Paused goals cannot receive contribution changes.")
        }
        if (amount.amountInMinor <= 0) {
            throw DomainValidationException("Contribution amount must be greater than zero.")
        }
        if (amount.currency != targetAmount.currency) {
            throw DomainValidationException("Contribution currency must match the goal currency.")
        }
    }

    companion object {
        const val MAX_DESCRIPTION_LENGTH = 160

        fun create(
            userId: UserId,
            name: String,
            targetAmount: Money,
            targetDate: LocalDate? = null,
            description: String? = null,
            currentAmount: Money = Money.zero(targetAmount.currency),
            clock: Clock = Clock.systemUTC(),
            id: GoalId = UUID.randomUUID(),
        ): Goal {
            val normalizedName = name.trim()
            if (normalizedName.length !in 1..80) {
                throw DomainValidationException("Goal name must contain between 1 and 80 characters.")
            }
            if (targetDate != null && targetDate.isBefore(LocalDate.now(clock))) {
                throw DomainValidationException("Goal target date cannot be in the past.")
            }
            val normalizedDescription = description?.trim()?.takeIf { it.isNotEmpty() }
            val now = Instant.now(clock)
            return Goal(
                id = id,
                userId = userId,
                name = normalizedName,
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                targetDate = targetDate,
                description = normalizedDescription,
                status = if (currentAmount == targetAmount) GoalStatus.ACHIEVED else GoalStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
