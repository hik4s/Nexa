package com.nexa.finance.application.notification

import com.nexa.finance.application.ai.AiContextBuilder
import com.nexa.finance.application.budget.BudgetService
import com.nexa.finance.application.goal.GoalService
import com.nexa.finance.application.recurrence.RecurrenceService
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.invoice.InvoiceStatus
import com.nexa.finance.infrastructure.invoice.JdbcInvoiceRepository
import com.nexa.finance.infrastructure.notification.JdbcNotificationRepository
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.YearMonth
import java.util.UUID

class NotificationService(
    private val repository: JdbcNotificationRepository,
    private val budgets: BudgetService,
    private val goals: GoalService,
    private val recurrences: RecurrenceService,
    private val invoices: JdbcInvoiceRepository,
    private val aiContext: AiContextBuilder,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun refresh(userId: UserId) {
        val today = LocalDate.now(clock)
        budgets.list(userId).filter { it.alertLevel.name != "OK" }.forEach { budget ->
            repository.createIfAbsent(
                userId = userId,
                type = "BUDGET_ALERT",
                severity = if (budget.alertLevel.name == "EXCEEDED") "WARNING" else "INFO",
                title = if (budget.alertLevel.name == "EXCEEDED") "Orçamento ultrapassado" else "Orçamento em atenção",
                message = (budget.budget.description ?: "Um orçamento") + " está em " + budget.progressPercent + "% do limite.",
                dedupeKey = "budget:" + budget.budget.id + ":" + budget.progressPercent / 10,
            )
        }

        goals.list(userId).filter { it.goal.status.name == "ACTIVE" }.forEach { goal ->
            val dueSoon = goal.goal.targetDate?.let {
                !it.isBefore(today) && ChronoUnit.DAYS.between(today, it) <= 30
            } == true
            if (dueSoon || goal.progressPercent >= 80) {
                repository.createIfAbsent(
                    userId,
                    "GOAL",
                    if (dueSoon && goal.progressPercent < 80) "WARNING" else "INFO",
                    "Meta: " + goal.goal.name,
                    if (dueSoon) "Prazo em " + ChronoUnit.DAYS.between(today, goal.goal.targetDate) + " dias, com " +
                        goal.progressPercent + "% concluído." else "Você já alcançou " + goal.progressPercent + "% da meta.",
                    "goal:" + goal.goal.id + ":" + if (dueSoon) "deadline" else "progress",
                )
            }
        }

        recurrences.summaries(userId).filter { it.nextOccurrenceOn != null }.forEach { summary ->
            val days = ChronoUnit.DAYS.between(today, summary.nextOccurrenceOn)
            if (days in 0..7 && summary.recurrence.status.name == "ACTIVE") {
                repository.createIfAbsent(
                    userId,
                    "RECURRENCE",
                    if (days <= 1) "WARNING" else "INFO",
                    "Recorrência próxima",
                    (summary.recurrence.description ?: "Lançamento recorrente") +
                        " está programado para " + summary.nextOccurrenceOn + ".",
                    "recurrence:" + summary.recurrence.id + ":" + summary.nextOccurrenceOn,
                )
            }
        }

        invoices.findByOwner(userId).filter { it.status in setOf(InvoiceStatus.CLOSED, InvoiceStatus.OVERDUE) }.forEach { invoice ->
            val days = ChronoUnit.DAYS.between(today, invoice.dueOn)
            if (invoice.status == InvoiceStatus.OVERDUE || days in 0..7) {
                repository.createIfAbsent(
                    userId,
                    "INVOICE",
                    if (invoice.status == InvoiceStatus.OVERDUE || days <= 1) "WARNING" else "INFO",
                    if (invoice.status == InvoiceStatus.OVERDUE) "Fatura vencida" else "Fatura próxima",
                    if (invoice.status == InvoiceStatus.OVERDUE) "A fatura está vencida e possui saldo em aberto."
                    else "A fatura vence em " + days + " dias.",
                    "invoice:" + invoice.id.value + ":" + invoice.status.name,
                )
            }
        }

        val context = aiContext.build(userId, YearMonth.from(today))
        context.previousExpenseMinor?.let { previous ->
            if (previous > 0 && context.expenseMinor > previous * 1.20) {
                repository.createIfAbsent(
                    userId,
                    "SPENDING_CHANGE",
                    "WARNING",
                    "Despesas em alta",
                    "As despesas deste mês estão mais de 20% acima do mês anterior.",
                    "spending:" + YearMonth.from(today),
                )
            }
        }

        if (today.dayOfWeek.value == 1) {
            val result = context.balanceDeltaMinor
            val direction = if (result >= 0) "positivo" else "negativo"
            repository.createIfAbsent(
                userId,
                "PERIODIC_INSIGHT",
                "INFO",
                "Resumo financeiro da semana",
                "Até agora, o resultado do mês está " + direction + ": " +
                    context.expenseMinor + " em despesas e " + context.incomeMinor + " em entradas.",
                "periodic-insight:" + YearMonth.from(today) + ":" + today.toString(),
            )
        }
    }

    fun list(userId: UserId, unreadOnly: Boolean = false): List<com.nexa.finance.infrastructure.notification.NotificationRecord> {
        refresh(userId)
        return repository.list(userId, unreadOnly)
    }

    fun markRead(userId: UserId, id: UUID) {
        repository.markRead(userId, id)
    }
}
