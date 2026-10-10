package com.nexa.finance.application.budget

import com.nexa.finance.application.audit.AuditEventRecorder
import com.nexa.finance.application.audit.AuditEventTypes
import com.nexa.finance.domain.budget.Budget
import com.nexa.finance.domain.budget.BudgetId
import com.nexa.finance.domain.category.CategoryId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.infrastructure.budget.JdbcBudgetRepository
import java.time.LocalDate

enum class BudgetAlertLevel { OK, WARNING, EXCEEDED }

data class BudgetProgress(
    val budget: Budget,
    val consumedMinor: Long,
    val remainingMinor: Long,
    val progressPercent: Int,
    val alertLevel: BudgetAlertLevel,
)

class BudgetService(
    private val repository: JdbcBudgetRepository,
    private val audit: AuditEventRecorder? = null,
) {
    fun create(
        userId: UserId,
        amountMinor: Long,
        currency: CurrencyCode,
        startOn: LocalDate,
        endOn: LocalDate,
        categoryId: CategoryId?,
        description: String?,
    ): Budget {
        validateCategory(userId, categoryId)
        val budget = Budget.create(
            userId = userId,
            amount = Money(amountMinor, currency),
            startOn = startOn,
            endOn = endOn,
            categoryId = categoryId,
            description = description,
        )
        repository.create(budget)
        audit?.record(userId, AuditEventTypes.BUDGET_CREATED, "BUDGET", budget.id)
        return budget
    }

    fun list(userId: UserId): List<BudgetProgress> =
        repository.findByOwner(userId).map(::progressFor)

    fun get(userId: UserId, budgetId: BudgetId): BudgetProgress =
        progressFor(repository.findByOwnerAndId(userId, budgetId)
            ?: throw DomainValidationException("Budget was not found."))

    fun update(
        userId: UserId,
        budgetId: BudgetId,
        amountMinor: Long,
        currency: CurrencyCode,
        startOn: LocalDate,
        endOn: LocalDate,
        categoryId: CategoryId?,
        description: String?,
    ): BudgetProgress {
        val current = repository.findByOwnerAndId(userId, budgetId)
            ?: throw DomainValidationException("Budget was not found.")
        validateCategory(userId, categoryId)
        val next = current.update(
            amount = Money(amountMinor, currency),
            startOn = startOn,
            endOn = endOn,
            categoryId = categoryId,
            description = description,
        )
        val updated = repository.update(next)
            ?: throw DomainValidationException("Budget was not found.")
        audit?.record(userId, AuditEventTypes.BUDGET_UPDATED, "BUDGET", updated.id)
        return progressFor(updated)
    }

    fun pause(userId: UserId, budgetId: BudgetId): BudgetProgress =
        change(userId, budgetId) { it.pause() }

    fun resume(userId: UserId, budgetId: BudgetId): BudgetProgress =
        change(userId, budgetId) { it.resume() }

    fun complete(userId: UserId, budgetId: BudgetId): BudgetProgress =
        change(userId, budgetId) { it.complete() }

    fun cancel(userId: UserId, budgetId: BudgetId): BudgetProgress =
        change(userId, budgetId) { it.cancel() }

    private fun change(userId: UserId, budgetId: BudgetId, transform: (Budget) -> Budget): BudgetProgress {
        val current = repository.findByOwnerAndId(userId, budgetId)
            ?: throw DomainValidationException("Budget was not found.")
        val updated = repository.update(transform(current))
            ?: throw DomainValidationException("Budget was not found.")
        audit?.record(userId, AuditEventTypes.BUDGET_STATUS_CHANGED, "BUDGET", updated.id)
        return progressFor(updated)
    }

    private fun progressFor(budget: Budget): BudgetProgress {
        val consumed = repository.consumedMinor(budget.userId, budget).coerceAtLeast(0)
        val remaining = (budget.amount.amountInMinor - consumed).coerceAtLeast(0)
        val progress = ((consumed * 100) / budget.amount.amountInMinor)
            .toInt()
            .coerceIn(0, 100)
        val alert = when {
            progress >= 100 -> BudgetAlertLevel.EXCEEDED
            progress >= 80 -> BudgetAlertLevel.WARNING
            else -> BudgetAlertLevel.OK
        }
        return BudgetProgress(budget, consumed, remaining, progress, alert)
    }

    private fun validateCategory(userId: UserId, categoryId: CategoryId?) {
        if (categoryId != null && !repository.isCategoryAvailable(userId, categoryId)) {
            throw DomainValidationException("Budget category was not found.")
        }
    }
}
