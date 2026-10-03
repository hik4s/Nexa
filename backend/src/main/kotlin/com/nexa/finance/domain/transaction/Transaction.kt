package com.nexa.finance.domain.transaction

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.category.CategoryId
import com.nexa.finance.domain.card.CardId
import com.nexa.finance.domain.invoice.InvoiceId
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

 typealias TransactionId = UUID

/**
 * Core financial event recorded against a financial account.
 *
 * The transaction amount is always positive. Direction carries the accounting
 * effect (debit/credit), avoiding ambiguous signed transaction amounts.
 */
@ConsistentCopyVisibility
data class Transaction private constructor(
    val id: TransactionId,
    val accountId: AccountId,
    val amount: Money,
    val type: TransactionType,
    val direction: TransactionDirection,
    val status: TransactionStatus,
    val occurredOn: LocalDate,
    val description: String?,
    val categoryId: CategoryId?,
    val cardId: CardId?,
    val invoiceId: InvoiceId?,
    val source: TransactionSource,
    val relatedTransactionId: TransactionId?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        if (amount.amountInMinor <= 0) {
            throw DomainValidationException("Transaction amount must be greater than zero.")
        }

        validateTypeAndDirection(type, direction)

        description?.let {
            if (it.isBlank()) {
                throw DomainValidationException("Transaction description cannot be blank.")
            }
            if (it.length > MAX_DESCRIPTION_LENGTH) {
                throw DomainValidationException(
                    "Transaction description cannot exceed $MAX_DESCRIPTION_LENGTH characters."
                )
            }
        }

        if (type == TransactionType.CANCELLATION && relatedTransactionId == null) {
            throw DomainValidationException("A cancellation must reference the transaction it cancels.")
        }
    }

    /** Transition a pending transaction into a posted state. */
    fun post(now: Instant = Instant.now()): Transaction {
        if (status != TransactionStatus.PENDING) {
            throw DomainValidationException("Only pending transactions can be posted.")
        }
        return copy(status = TransactionStatus.POSTED, updatedAt = now)
    }

    /** Mark a transaction as cancelled. A separate cancellation event should be used for accounting reversal. */
    fun cancel(now: Instant = Instant.now()): Transaction {
        if (status == TransactionStatus.CANCELLED) {
            throw DomainValidationException("Transaction is already cancelled.")
        }
        return copy(status = TransactionStatus.CANCELLED, updatedAt = now)
    }

    companion object {
        const val MAX_DESCRIPTION_LENGTH = 160

        fun create(
            accountId: AccountId,
            amount: Money,
            type: TransactionType,
            direction: TransactionDirection,
            occurredOn: LocalDate,
            description: String? = null,
            categoryId: CategoryId? = null,
            cardId: CardId? = null,
            invoiceId: InvoiceId? = null,
            source: TransactionSource = TransactionSource.MANUAL,
            status: TransactionStatus = TransactionStatus.POSTED,
            relatedTransactionId: TransactionId? = null,
            id: TransactionId = UUID.randomUUID(),
            createdAt: Instant = Instant.now(),
            updatedAt: Instant = createdAt,
        ): Transaction {
            if (accountId.value == UUID(0, 0)) {
                throw DomainValidationException("Account ID cannot be the zero UUID.")
            }

            val normalizedDescription = description?.trim()?.takeIf { it.isNotEmpty() }

            return Transaction(
                id = id,
                accountId = accountId,
                amount = amount,
                type = type,
                direction = direction,
                status = status,
                occurredOn = occurredOn,
                description = normalizedDescription,
                categoryId = categoryId,
                source = source,
                relatedTransactionId = relatedTransactionId,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
        }

        private fun validateTypeAndDirection(
            type: TransactionType,
            direction: TransactionDirection,
        ) {
            val expectedDirection = when (type) {
                TransactionType.INCOME -> TransactionDirection.CREDIT

                TransactionType.EXPENSE -> TransactionDirection.DEBIT

                // PURCHASE, PAYMENT and REFUND are account-contextual:
                // assets and liabilities have opposite accounting effects.
                TransactionType.PURCHASE,
                TransactionType.PAYMENT,
                TransactionType.REFUND,
                -> null

                TransactionType.TRANSFER,
                TransactionType.CANCELLATION,
                TransactionType.ADJUSTMENT,
                -> null
            }

            if (expectedDirection != null && direction != expectedDirection) {
                throw DomainValidationException(
                    "Transaction type $type must use direction $expectedDirection."
                )
            }
        }
    }
}
