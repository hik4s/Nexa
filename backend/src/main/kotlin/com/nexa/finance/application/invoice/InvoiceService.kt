package com.nexa.finance.application.invoice

import com.nexa.finance.application.audit.AuditEventRecorder
import com.nexa.finance.application.audit.AuditEventTypes
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.card.CardId
import com.nexa.finance.domain.card.CardPayment
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.invoice.Invoice
import com.nexa.finance.domain.invoice.InvoiceId
import com.nexa.finance.domain.invoice.InvoiceStatus
import com.nexa.finance.domain.transaction.TransactionSource
import com.nexa.finance.infrastructure.account.JdbcAccountRepository
import com.nexa.finance.infrastructure.card.JdbcCardRepository
import com.nexa.finance.infrastructure.invoice.JdbcInvoiceRepository
import java.time.LocalDate

class InvoiceService(
    private val invoices: JdbcInvoiceRepository,
    private val cards: JdbcCardRepository,
    private val accounts: JdbcAccountRepository,
    private val audit: AuditEventRecorder? = null,
) {
    fun create(ownerId: UserId, cardId: CardId, periodStart: LocalDate, periodEnd: LocalDate, dueOn: LocalDate): Invoice {
        if (periodEnd.isBefore(periodStart)) throw DomainValidationException("Invoice period end cannot precede its start.")
        val card = cards.findByOwnerAndId(ownerId, cardId) ?: throw DomainValidationException("Card was not found.")
        val invoice = invoices.createForPeriod(ownerId, card, periodStart, periodEnd, dueOn)
        audit?.record(ownerId, AuditEventTypes.INVOICE_CREATED, "INVOICE", invoice.id.value)
        return invoice
    }

    fun list(ownerId: UserId): List<Invoice> = invoices.findByOwner(ownerId)
    fun get(ownerId: UserId, id: InvoiceId): Invoice = invoices.findByOwnerAndId(ownerId, id) ?: throw DomainValidationException("Invoice was not found.")

    fun close(ownerId: UserId, id: InvoiceId): Invoice = changeStatus(ownerId, id, InvoiceStatus.CLOSED)
    fun overdue(ownerId: UserId, id: InvoiceId): Invoice = changeStatus(ownerId, id, InvoiceStatus.OVERDUE)

    private fun changeStatus(ownerId: UserId, id: InvoiceId, status: InvoiceStatus): Invoice {
        val current = get(ownerId, id)
        val next = when (status) {
            InvoiceStatus.CLOSED -> current.close()
            InvoiceStatus.OVERDUE -> current.markOverdue()
            else -> throw DomainValidationException("Unsupported invoice status transition.")
        }
        val updated = invoices.updateStatus(ownerId, id, next.status) ?: throw DomainValidationException("Invoice was not found.")
        audit?.record(ownerId, AuditEventTypes.INVOICE_STATUS_CHANGED, "INVOICE", updated.id.value)
        return updated
    }

    fun pay(ownerId: UserId, invoiceId: InvoiceId, sourceAccountId: AccountId, amount: Money, occurredOn: LocalDate, source: TransactionSource): Invoice {
        val invoice = get(ownerId, invoiceId)
        val card = cards.findByOwnerAndId(ownerId, invoice.cardId) ?: throw DomainValidationException("Card was not found.")
        val sourceAccount = accounts.findByOwnerAndId(ownerId, sourceAccountId) ?: throw DomainValidationException("Payment source account was not found.")
        val payment = CardPayment.create(ownerId, sourceAccount, card, invoice, amount, occurredOn, source)
        val updated = invoices.persistPayment(ownerId, payment.post())
        audit?.record(ownerId, AuditEventTypes.INVOICE_PAID, "INVOICE", updated.id.value)
        return updated
    }
}