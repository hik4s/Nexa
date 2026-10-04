package com.nexa.finance.application.ai

import com.nexa.finance.infrastructure.budget.JdbcBudgetRepository
import com.nexa.finance.infrastructure.category.JdbcCategoryRepository
import com.nexa.finance.infrastructure.goal.JdbcGoalRepository
import com.nexa.finance.infrastructure.transaction.JdbcTransactionRepository
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.transaction.TransactionDirection
import com.nexa.finance.domain.transaction.TransactionStatus
import java.time.YearMonth

class AiContextBuilder(
    private val transactions: JdbcTransactionRepository,
    private val budgets: JdbcBudgetRepository,
    private val goals: JdbcGoalRepository,
    private val categories: JdbcCategoryRepository,
) {
    fun build(userId: UserId, month: YearMonth): AiFinancialContext {
        val all = transactions.findByOwner(userId)
            .filter { it.status == TransactionStatus.POSTED }

        val currency = all.groupingBy { it.amount.currency.name }
            .eachCount()
            .maxByOrNull { it.value }?.key ?: "BRL"

        val current = all.filter { YearMonth.from(it.occurredOn) == month && it.amount.currency.name == currency }
        val previousMonth = month.minusMonths(1)
        val previous = all.filter { YearMonth.from(it.occurredOn) == previousMonth && it.amount.currency.name == currency }

        val income = current.filter { it.direction == TransactionDirection.CREDIT }.sumOf { it.amount.amountInMinor }
        val expense = current.filter { it.direction == TransactionDirection.DEBIT }.sumOf { it.amount.amountInMinor }
        val previousExpense = previous.filter { it.direction == TransactionDirection.DEBIT }.sumOf { it.amount.amountInMinor }

        val categoryRecords = categories.listAvailable(userId.value)
            .associateBy { it.id }

        val categoryGroups = current
            .filter { it.direction == TransactionDirection.DEBIT }
            .groupBy { it.categoryId }
            .map { (id, txs) ->
                val amount = txs.sumOf { it.amount.amountInMinor }
                val share = if (expense == 0L) 0.0 else amount.toDouble() * 100.0 / expense.toDouble()
                AiCategoryFact(
                    categoryId = id?.value,
                    categoryName = id?.value?.let { categoryRecords[it]?.name } ?: "Sem categoria",
                    amountMinor = amount,
                    currency = currency,
                    sharePercentage = share,
                )
            }
            .sortedByDescending { it.amountMinor }

        val budgetFacts = budgets.findByOwner(userId)
            .filter { it.amount.currency.name == currency }
            .map { budget ->
                val consumed = budgets.consumedMinor(userId, budget).coerceAtLeast(0)
                val remaining = (budget.amount.amountInMinor - consumed).coerceAtLeast(0)
                val progress = ((consumed * 100) / budget.amount.amountInMinor).toInt().coerceIn(0, 100)
                val alert = when {
                    progress >= 100 -> "EXCEEDED"
                    progress >= 80 -> "WARNING"
                    else -> "OK"
                }
                AiBudgetFact(
                    id = budget.id,
                    description = budget.description,
                    amountMinor = budget.amount.amountInMinor,
                    consumedMinor = consumed,
                    remainingMinor = remaining,
                    progressPercent = progress,
                    alertLevel = alert,
                    currency = currency,
                )
            }

        val goalFacts = goals.findByOwner(userId)
            .filter { it.targetAmount.currency.name == currency }
            .map {
                val remaining = (it.targetAmount.amountInMinor - it.currentAmount.amountInMinor).coerceAtLeast(0)
                AiGoalFact(
                    id = it.id,
                    name = it.name,
                    targetAmountMinor = it.targetAmount.amountInMinor,
                    currentAmountMinor = it.currentAmount.amountInMinor,
                    remainingMinor = remaining,
                    progressPercent = (it.progressRatio * 100).toInt().coerceIn(0, 100),
                    status = it.status.name,
                    currency = currency,
                )
            }

        return AiFinancialContext(
            userId = userId.value,
            month = month,
            previousMonth = previousMonth,
            currency = currency,
            incomeMinor = income,
            expenseMinor = expense,
            previousExpenseMinor = previousExpense.takeIf { previous.isNotEmpty() },
            balanceDeltaMinor = income - expense,
            categoryFacts = categoryGroups,
            budgets = budgetFacts,
            goals = goalFacts,
            facts = listOf(
                AiFact("income-current", "Entradas do mês", income, currency, date = month.atEndOfMonth()),
                AiFact("expenses-current", "Despesas do mês", expense, currency, date = month.atEndOfMonth()),
                AiFact("expenses-previous", "Despesas do mês anterior", previousExpense, currency, date = previousMonth.atEndOfMonth()),
                AiFact("balance-delta-current", "Resultado do mês", income - expense, currency, date = month.atEndOfMonth()),
            ),
        )
    }
}
