package com.nexa.finance.application.ai

import java.time.YearMonth
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RuleBasedAiProviderTest {
    private val provider = RuleBasedAiProvider()
    private val context = AiFinancialContext(
        userId = UUID.randomUUID(),
        month = YearMonth.of(2026, 10),
        previousMonth = YearMonth.of(2026, 9),
        currency = "BRL",
        incomeMinor = 500_000,
        expenseMinor = 350_000,
        previousExpenseMinor = 300_000,
        balanceDeltaMinor = 150_000,
        categoryFacts = listOf(
            AiCategoryFact(UUID.randomUUID(), "Moradia", 180_000, "BRL", 51.43),
            AiCategoryFact(UUID.randomUUID(), "Alimentação", 70_000, "BRL", 20.0),
        ),
        budgets = listOf(
            AiBudgetFact(UUID.randomUUID(), "Alimentação", 80_000, 72_000, 8_000, 90, "WARNING", "BRL"),
        ),
        goals = listOf(
            AiGoalFact(UUID.randomUUID(), "Reserva", 1_000_000, 800_000, 200_000, 80, "ACTIVE", "BRL"),
        ),
        facts = listOf(
            AiFact("income-current", "Entradas do mês", 500_000, "BRL"),
            AiFact("expenses-current", "Despesas do mês", 350_000, "BRL"),
            AiFact("expenses-previous", "Despesas anteriores", 300_000, "BRL"),
        ),
    )

    @Test
    fun analysisUsesOnlyKnownFactsForQuantitativeInsights() {
        val analysis = provider.analyze(context)
        assertTrue(analysis.insights.isNotEmpty())
        assertContains(analysis.insights.flatMap { it.sourceFactIds }, "expenses-current")
        assertContains(analysis.insights.flatMap { it.sourceFactIds }, "expenses-previous")
        assertEquals("rules-v1", analysis.provider)
    }

    @Test
    fun unsupportedQuestionRefusesToInventAnAnswer() {
        val answer = provider.answer(context, "Qual será meu gasto daqui a cinco anos?")
        assertEquals("Não tenho dados suficientes para responder isso com segurança.", answer.answer)
        assertTrue(answer.sourceFactIds.isEmpty())
    }

    @Test
    fun supportedQuestionReturnsProvenance() {
        val answer = provider.answer(context, "Quanto eu gastei este mês?")
        assertContains(answer.answer, "3.500")
        assertEquals(listOf("expenses-current"), answer.sourceFactIds)
    }
}
