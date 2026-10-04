package com.nexa.finance.domain.invoice

import com.nexa.finance.domain.card.CardId
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.transaction.TransactionId
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@JvmInline
value class InvoiceId(val value: UUID) {
    companion object {
        fun new(): InvoiceId = InvoiceId(UUID.randomUUID())
    }
}

enum class InvoiceStatus { OPEN, CLOSED, OVERDUE, PAID, CANCELLED }

@ConsistentCopyVisibility
data class Invoice private constructor(
    val id: InvoiceId,
    val cardId: CardId,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val dueOn: LocalDate,
    val totalAmount: Money,
    val transactionIds: List<TransactionId>,
    val paidAmount: Money,
    val status: InvoiceStatus,
    val paymentTransactionIds: List<TransactionId>,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        if (periodEnd.isBefore(periodStart)) throw DomainValidationException("Invoice period end cannot precede its start.")
        if (dueOn.isBefore(periodEnd)) throw DomainValidationException("Invoice due date cannot precede period end.")
        if (totalAmount.amountInMinor <= 0) throw DomainValidationException("Invoice total must be greater than zero.")
        if (paidAmount.currency != totalAmount.currency) throw DomainValidationException("Invoice payment currency must match total currency.")
        if (paidAmount.amountInMinor < 0 || paidAmount.amountInMinor > totalAmount.amountInMinor) {
            throw DomainValidationException("Invoice paid amount must be between zero and the invoice total.")
        }
        if (transactionIds.distinct().size != transactionIds.size) throw DomainValidationException("Invoice transactions must be unique.")
        if (status == InvoiceStatus.PAID && paidAmount != totalAmount) throw DomainValidationException("Paid invoices must be fully paid.")
        if (paymentTransactionIds.distinct().size != paymentTransactionIds.size) throw DomainValidationException("Invoice payment transactions must be unique.")
        if (paymentTransactionIds.isNotEmpty() && paidAmount.amountInMinor == 0L) throw DomainValidationException("Payment transactions require a positive paid amount.")
    }

    fun close(now: Instant = Instant.now()): Invoice {
        if (status != InvoiceStatus.OPEN) throw DomainValidationException("Only open invoices can be closed.")
        return copy(status = InvoiceStatus.CLOSED, updatedAt = now)
    }

    fun markOverdue(now: Instant = Instant.now()): Invoice {
        if (status != InvoiceStatus.CLOSED) throw DomainValidationException("Only closed invoices can become overdue.")
        return copy(status = InvoiceStatus.OVERDUE, updatedAt = now)
    }

    fun registerPayment(
        amount: Money,
        paymentTransactionId: TransactionId,
        now: Instant = Instant.now(),
    ): Invoice {
        if (status !in setOf(InvoiceStatus.CLOSED, InvoiceStatus.OVERDUE)) {
            throw DomainValidationException("Only closed or overdue invoices can receive payments.")
        }
        if (amount.currency != totalAmount.currency) throw DomainValidationException("Payment currency must match invoice currency.")
        if (amount.amountInMinor <= 0) throw DomainValidationException("Invoice payment must be greater than zero.")
        val newPaid = paidAmount + amount
        if (newPaid.amountInMinor > totalAmount.amountInMinor) throw DomainValidationException("Invoice payment cannot exceed the remaining balance.")
        val newStatus = if (newPaid == totalAmount) InvoiceStatus.PAID else status
        return copy(
            paidAmount = newPaid,
            status = newStatus,
            paymentTransactionIds = paymentTransactionIds + paymentTransactionId,
            updatedAt = now,
        )
    }

    fun remainingAmount(): Money = totalAmount - paidAmount

    fun cancel(now: Instant = Instant.now()): Invoice {
        if (status == InvoiceStatus.PAID) throw DomainValidationException("Paid invoices cannot be cancelled.")
        if (paidAmount.amountInMinor > 0) throw DomainValidationException("Invoices with payments must be reversed rather than cancelled.")
        return copy(status = InvoiceStatus.CANCELLED, updatedAt = now)
    }

    companion object {
        fun restore(
            id: InvoiceId,
            cardId: CardId,
            periodStart: LocalDate,
            periodEnd: LocalDate,
            dueOn: LocalDate,
            totalAmount: Money,
            transactionIds: List<TransactionId>,
            paidAmount: Money,
            status: InvoiceStatus,
            paymentTransactionIds: List<TransactionId>,
            createdAt: Instant,
            updatedAt: Instant,
        ): Invoice = Invoice(id, cardId, periodStart, periodEnd, dueOn, totalAmount, transactionIds, paidAmount, status, paymentTransactionIds, createdAt, updatedAt)

        fun create(
            cardId: CardId,
            periodStart: LocalDate,
            periodEnd: LocalDate,
            dueOn: LocalDate,
            totalAmount: Money,
            transactionIds: List<TransactionId>,
            clock: Clock = Clock.systemUTC(),
            id: InvoiceId = InvoiceId.new(),
        ): Invoice {
            val now = Instant.now(clock)
            return Invoice(
                id = id,
                cardId = cardId,
                periodStart = periodStart,
                periodEnd = periodEnd,
                dueOn = dueOn,
                totalAmount = totalAmount,
                transactionIds = transactionIds.distinct(),
                paidAmount = Money.zero(totalAmount.currency),
                status = InvoiceStatus.OPEN,
                paymentTransactionIds = emptyList(),
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
