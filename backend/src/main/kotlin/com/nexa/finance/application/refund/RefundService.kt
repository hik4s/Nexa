package com.nexa.finance.application.refund

import com.nexa.finance.application.audit.AuditEventRecorder
import com.nexa.finance.application.audit.AuditEventTypes
import com.nexa.finance.domain.cancellation.Cancellation
import com.nexa.finance.domain.card.CardLedger
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.transaction.Transaction
import com.nexa.finance.domain.transaction.TransactionDirection
import com.nexa.finance.domain.transaction.TransactionSource
import com.nexa.finance.domain.transaction.TransactionStatus
import com.nexa.finance.domain.transaction.TransactionType
import com.nexa.finance.infrastructure.card.JdbcCardRepository
import com.nexa.finance.infrastructure.transaction.JdbcTransactionRepository
import java.time.LocalDate
import java.util.UUID

class RefundService(
    private val transactions: JdbcTransactionRepository,
    private val cards: JdbcCardRepository,
    private val audit: AuditEventRecorder? = null,
) {
    fun refund(
        ownerId: UserId,
        originalId: UUID,
        amount: Money,
        occurredOn: LocalDate,
        description: String?,
    ): Transaction {
        val original = transactions.findByOwnerAndId(ownerId, originalId)
            ?: throw DomainValidationException("Transaction was not found.")
        if (original.status != TransactionStatus.POSTED) {
            throw DomainValidationException("Only posted transactions can be refunded.")
        }
        if (original.type != TransactionType.EXPENSE && original.type != TransactionType.PURCHASE) {
            throw DomainValidationException("Only expenses and purchases can be refunded.")
        }
        val alreadyRefunded = transactions.sumRelatedAmount(ownerId, original.id, TransactionType.REFUND)
        val remaining = original.amount.amountInMinor - alreadyRefunded
        if (amount.amountInMinor > remaining) {
            throw DomainValidationException("Refund amount exceeds the remaining refundable amount.")
        }

        val refund = if (original.cardId != null) {
            if (original.direction != TransactionDirection.CREDIT) {
                throw DomainValidationException("Card purchases must be credits before they can be refunded.")
            }
            val card = cards.findByOwnerAndId(ownerId, original.cardId)
                ?: throw DomainValidationException("Card was not found for this transaction.")
            if (card.liabilityAccountId != original.accountId) {
                throw DomainValidationException("Card transaction account does not match the card liability account.")
            }
            CardLedger.refund(
                card = card,
                amount = amount,
                occurredOn = occurredOn,
                invoiceId = null,
                description = description,
                source = TransactionSource.MANUAL,
            ).post().let { tx ->
                Transaction.create(
                    id = tx.id,
                    accountId = tx.accountId,
                    amount = tx.amount,
                    type = tx.type,
                    direction = tx.direction,
                    status = tx.status,
                    occurredOn = tx.occurredOn,
                    description = tx.description,
                    categoryId = tx.categoryId,
                    cardId = tx.cardId,
                    invoiceId = tx.invoiceId,
                    source = tx.source,
                    relatedTransactionId = original.id,
                    createdAt = tx.createdAt,
                    updatedAt = tx.updatedAt,
                )
            }
        } else {
            com.nexa.finance.domain.refund.Refund.create(
                original = original,
                amount = amount,
                occurredOn = occurredOn,
                description = description,
                source = TransactionSource.MANUAL,
            ).transaction
        }

        transactions.create(ownerId, refund)
        audit?.record(ownerId, AuditEventTypes.REFUND_CREATED, "TRANSACTION", refund.id)
        return refund
    }

    fun cancelPosted(
        ownerId: UserId,
        originalId: UUID,
        occurredOn: LocalDate,
        description: String?,
    ): Transaction {
        val original = transactions.findByOwnerAndId(ownerId, originalId)
            ?: throw DomainValidationException("Transaction was not found.")
        if (original.status != TransactionStatus.POSTED) {
            throw DomainValidationException("Only posted transactions can receive an accounting cancellation.")
        }
        if (original.type == TransactionType.CANCELLATION) {
            throw DomainValidationException("A cancellation transaction cannot be cancelled again.")
        }
        if (original.type == TransactionType.TRANSFER) {
            throw DomainValidationException("Transfer transactions must be reversed through the transfer aggregate.")
        }
        if (original.type == TransactionType.PAYMENT && transactions.isInvoicePaymentTransaction(ownerId, original.id)) {
            throw DomainValidationException("Invoice payment transactions must be reversed through the invoice payment aggregate.")
        }
        if (transactions.existsRelated(ownerId, original.id, TransactionType.CANCELLATION)) {
            throw DomainValidationException("Transaction already has an accounting cancellation.")
        }
        val cancellation = Cancellation.createForPosted(original).let {
            Transaction.create(
                id = it.id,
                accountId = it.accountId,
                amount = it.amount,
                type = it.type,
                direction = it.direction,
                status = it.status,
                occurredOn = occurredOn,
                description = description?.trim()?.takeIf(String::isNotEmpty) ?: it.description,
                categoryId = it.categoryId,
                cardId = it.cardId,
                invoiceId = null,
                source = it.source,
                relatedTransactionId = it.relatedTransactionId,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
            )
        }
        transactions.create(ownerId, cancellation)
        audit?.record(ownerId, AuditEventTypes.CANCELLATION_CREATED, "TRANSACTION", cancellation.id)
        return cancellation
    }
}
