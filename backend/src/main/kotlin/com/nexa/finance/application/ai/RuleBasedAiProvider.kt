package com.nexa.finance.application.ai

import java.text.NumberFormat
import java.util.Locale

class RuleBasedAiProvider : AiProvider {
    override val name: String = "rules-v1"

    override fun analyze(context: AiFinancialContext): AiAnalysis {
        val insights = buildList {
            val top = context.categoryFacts.firstOrNull()
            if (top != null && top.sharePercentage >= 30.0) {
                add(
                    AiInsight(
                        id = "top-category",
                        level = AiInsightLevel.WARNING,
                        title = "Categoria concentrada",
                        message = top.categoryName + " representa " + percent(top.sharePercentage) + " das despesas do mês.",
                        sourceFactIds = listOf("category-" + (top.categoryId ?: "uncategorized")),
                    )
                )
            }

            context.budgets.filter { it.alertLevel != "OK" }.forEach { budget ->
                add(
                    AiInsight(
                        id = "budget-" + budget.id,
                        level = if (budget.alertLevel == "EXCEEDED") AiInsightLevel.WARNING else AiInsightLevel.INFO,
                        title = if (budget.alertLevel == "EXCEEDED") "Orçamento ultrapassado" else "Orçamento em atenção",
                        message = (budget.description?.takeIf { it.isNotBlank() } ?: "Um orçamento") +
                            " está em " + budget.progressPercent + "% do limite.",
                        sourceFactIds = listOf("budget-" + budget.id),
                    )
                )
            }

            context.goals.filter { it.status == "ACTIVE" }.forEach { goal ->
                add(
                    AiInsight(
                        id = "goal-" + goal.id,
                        level = if (goal.progressPercent >= 80) AiInsightLevel.POSITIVE else AiInsightLevel.INFO,
                        title = "Meta: " + goal.name,
                        message = "Progresso atual de " + goal.progressPercent + "%, faltando " +
                            money(goal.remainingMinor, goal.currency) + ".",
                        sourceFactIds = listOf("goal-" + goal.id),
                    )
                )
            }

            context.previousExpenseMinor?.let { previous ->
                if (previous > 0) {
                    val delta = (context.expenseMinor - previous).toDouble() / previous * 100.0
                    if (kotlin.math.abs(delta) >= 10.0) {
                        add(
                            AiInsight(
                                id = "expense-trend",
                                level = if (delta > 0) AiInsightLevel.WARNING else AiInsightLevel.POSITIVE,
                                title = if (delta > 0) "Despesas em alta" else "Despesas em queda",
                                message = "As despesas variaram " + percent(kotlin.math.abs(delta)) +
                                    " em relação ao mês anterior.",
                                sourceFactIds = listOf("expenses-current", "expenses-previous"),
                            )
                        )
                    }
                }
            }
        }.take(8)

        return AiAnalysis(
            month = context.month,
            insights = insights,
            monthlySummary = "No mês " + context.month + ", entradas de " +
                money(context.incomeMinor, context.currency) + " e despesas de " +
                money(context.expenseMinor, context.currency) + ".",
            contextFactCount = context.facts.size + context.categoryFacts.size + context.budgets.size + context.goals.size,
            provider = name,
        )
    }

    override fun answer(context: AiFinancialContext, question: String): AiAnswer {
        val normalized = question.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return AiAnswer("Faça uma pergunta sobre seus dados financeiros disponíveis.", emptyList())

        return when {
            normalized.contains("quanto") && (normalized.contains("gastei") || normalized.contains("despesa")) ->
                AiAnswer(
                    "Você teve " + money(context.expenseMinor, context.currency) +
                        " em despesas em " + context.month + ".",
                    listOf("expenses-current"),
                )

            normalized.contains("receita") || normalized.contains("entrada") || normalized.contains("salário") ->
                AiAnswer(
                    "As entradas registradas em " + context.month + " somam " +
                        money(context.incomeMinor, context.currency) + ".",
                    listOf("income-current"),
                )

            normalized.contains("categoria") || normalized.contains("onde") ->
                context.categoryFacts.firstOrNull()?.let {
                    AiAnswer(
                        "A maior categoria de despesas foi " + it.categoryName + ", com " +
                            money(it.amountMinor, it.currency) + ".",
                        listOf("category-" + (it.categoryId ?: "uncategorized")),
                    )
                } ?: unsupported()

            normalized.contains("meta") ->
                context.goals.firstOrNull()?.let {
                    AiAnswer(
                        "A meta " + it.name + " está em " + it.progressPercent + "% e faltam " +
                            money(it.remainingMinor, it.currency) + ".",
                        listOf("goal-" + it.id),
                    )
                } ?: unsupported()

            normalized.contains("orçamento") || normalized.contains("orcamento") ->
                context.budgets.firstOrNull()?.let {
                    AiAnswer(
                        "Seu orçamento " + (it.description ?: "principal") + " está em " +
                            it.progressPercent + "% de uso.",
                        listOf("budget-" + it.id),
                    )
                } ?: unsupported()

            else -> unsupported()
        }
    }

    private fun unsupported() = AiAnswer(
        "Não tenho dados suficientes para responder isso com segurança.",
        emptyList(),
    )

    private fun money(minor: Long, currency: String): String =
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).apply {
            this.currency = java.util.Currency.getInstance(currency)
        }.format(minor / 100.0)

    private fun percent(value: Double): String =
        String.format(Locale.forLanguageTag("pt-BR"), "%.1f%%", value)
}
