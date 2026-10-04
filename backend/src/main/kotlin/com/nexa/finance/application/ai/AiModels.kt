package com.nexa.finance.application.ai

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

enum class AiInsightLevel { INFO, WARNING, POSITIVE }

data class AiFact(
    val id: String,
    val label: String,
    val valueMinor: Long? = null,
    val currency: String? = null,
    val percentage: Double? = null,
    val date: LocalDate? = null,
)

data class AiCategoryFact(
    val categoryId: UUID?,
    val categoryName: String,
    val amountMinor: Long,
    val currency: String,
    val sharePercentage: Double,
)

data class AiBudgetFact(
    val id: UUID,
    val description: String?,
    val amountMinor: Long,
    val consumedMinor: Long,
    val remainingMinor: Long,
    val progressPercent: Int,
    val alertLevel: String,
    val currency: String,
)

data class AiGoalFact(
    val id: UUID,
    val name: String,
    val targetAmountMinor: Long,
    val currentAmountMinor: Long,
    val remainingMinor: Long,
    val progressPercent: Int,
    val status: String,
    val currency: String,
)

data class AiFinancialContext(
    val userId: UUID,
    val month: YearMonth,
    val previousMonth: YearMonth?,
    val currency: String,
    val incomeMinor: Long,
    val expenseMinor: Long,
    val previousExpenseMinor: Long?,
    val balanceDeltaMinor: Long,
    val categoryFacts: List<AiCategoryFact>,
    val budgets: List<AiBudgetFact>,
    val goals: List<AiGoalFact>,
    val facts: List<AiFact>,
)

data class AiInsight(
    val id: String,
    val level: AiInsightLevel,
    val title: String,
    val message: String,
    val sourceFactIds: List<String>,
)

data class AiAnswer(
    val answer: String,
    val sourceFactIds: List<String>,
)

data class AiAnalysis(
    val month: YearMonth,
    val insights: List<AiInsight>,
    val monthlySummary: String,
    val contextFactCount: Int,
    val provider: String,
)
