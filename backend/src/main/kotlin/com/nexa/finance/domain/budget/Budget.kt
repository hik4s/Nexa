package com.nexa.finance.domain.budget

import com.nexa.finance.domain.category.CategoryId
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.ConsistentCopyVisibility

typealias BudgetId = UUID

enum class BudgetStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    CANCELLED,
}

@ConsistentCopyVisibility
data class Budget private constructor(
    val id: BudgetId,
    val userId: UserId,
    val categoryId: CategoryId?,
    val amount: Money,
    val startOn: LocalDate,
    val endOn: LocalDate,
    val description: String?,
    val status: BudgetStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        if (amount.amountInMinor <= 0) {
            throw DomainValidationException("Budget amount must be greater than zero.")
        }
        if (endOn.isBefore(startOn)) {
            throw DomainValidationException("Budget end date cannot be before the start date.")
        }
        if (description?.isBlank() == true) {
            throw DomainValidationException("Budget description cannot be blank.")
        }
        if (description != null && description.length > MAX_DESCRIPTION_LENGTH) {
            throw DomainValidationException("Budget description cannot exceed $MAX_DESCRIPTION_LENGTH characters.")
        }
    }

    fun pause(now: Instant = Instant.now()): Budget {
        if (status != BudgetStatus.ACTIVE) {
            throw DomainValidationException("Only active budgets can be paused.")
        }
        return copy(status = BudgetStatus.PAUSED, updatedAt = now)
    }

    fun resume(now: Instant = Instant.now()): Budget {
        if (status != BudgetStatus.PAUSED) {
            throw DomainValidationException("Only paused budgets can be resumed.")
        }
        return copy(status = BudgetStatus.ACTIVE, updatedAt = now)
    }

    fun complete(now: Instant = Instant.now()): Budget {
        if (status == BudgetStatus.CANCELLED) {
            throw DomainValidationException("Cancelled budgets cannot be completed.")
        }
        return copy(status = BudgetStatus.COMPLETED, updatedAt = now)
    }

    fun cancel(now: Instant = Instant.now()): Budget {
        if (status == BudgetStatus.CANCELLED) return this
        return copy(status = BudgetStatus.CANCELLED, updatedAt = now)
    }

    companion object {
        const val MAX_DESCRIPTION_LENGTH = 160

        fun create(
            userId: UserId,
            amount: Money,
            startOn: LocalDate,
            endOn: LocalDate,
            categoryId: CategoryId? = null,
            description: String? = null,
            clock: Clock = Clock.systemUTC(),
            id: BudgetId = UUID.randomUUID(),
        ): Budget {
            val normalizedDescription = description?.trim()?.takeIf { it.isNotEmpty() }
            val now = Instant.now(clock)
            return Budget(
                id = id,
                userId = userId,
                categoryId = categoryId,
                amount = amount,
                startOn = startOn,
                endOn = endOn,
                description = normalizedDescription,
                status = BudgetStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
