package com.nexa.finance.domain.payment

import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.transaction.Transaction
import com.nexa.finance.domain.transaction.TransactionDirection
import com.nexa.finance.domain.transaction.TransactionSource
import com.nexa.finance.domain.transaction.TransactionStatus
import com.nexa.finance.domain.transaction.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

typealias PaymentId = UUID

@ConsistentCopyVisibility
data class Payment private constructor(
    val id: PaymentId,
    val transaction: Transaction,
) {
    init {
        if (transaction.type != TransactionType.PAYMENT) {
            throw DomainValidationException("Payment must use transaction type PAYMENT.")
        }
        if (transaction.direction != TransactionDirection.DEBIT) {
            throw DomainValidationException("Payment must be a debit.")
        }
    }

    companion object {
        fun create(
            accountId: AccountId,
            amount: Money,
            occurredOn: LocalDate,
            description: String? = null,
            relatedTransactionId: UUID? = null,
            source: TransactionSource = TransactionSource.MANUAL,
            status: TransactionStatus = TransactionStatus.POSTED,
            clock: Clock = Clock.systemUTC(),
            id: PaymentId = UUID.randomUUID(),
        ): Payment {
            if (amount.amountInMinor <= 0) {
                throw DomainValidationException("Payment amount must be greater than zero.")
            }
            if (status == TransactionStatus.CANCELLED) {
                throw DomainValidationException("A payment cannot start cancelled.")
            }

            val now = Instant.now(clock)
            val transaction = Transaction.create(
                accountId = accountId,
                amount = amount,
                type = TransactionType.PAYMENT,
                direction = TransactionDirection.DEBIT,
                occurredOn = occurredOn,
                description = description,
                source = source,
                status = status,
                relatedTransactionId = relatedTransactionId,
                createdAt = now,
                updatedAt = now,
            )
            return Payment(id = id, transaction = transaction)
        }
    }
}
