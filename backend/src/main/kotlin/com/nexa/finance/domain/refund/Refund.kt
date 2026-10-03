package com.nexa.finance.domain.refund

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.transaction.*
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

typealias RefundId = UUID

@ConsistentCopyVisibility
data class Refund private constructor(
    val id: RefundId,
    val originalTransactionId: UUID,
    val transaction: Transaction,
) {
    init {
        if (transaction.type != TransactionType.REFUND) throw DomainValidationException("Refund must use transaction type REFUND.")
        if (transaction.direction != TransactionDirection.CREDIT) throw DomainValidationException("Refund must be a credit.")
        if (transaction.relatedTransactionId != originalTransactionId) throw DomainValidationException("Refund must reference the original transaction.")
    }

    companion object {
        fun create(
            original: Transaction,
            amount: Money = original.amount,
            occurredOn: LocalDate = original.occurredOn,
            description: String? = null,
            source: TransactionSource = TransactionSource.MANUAL,
            status: TransactionStatus = TransactionStatus.POSTED,
            clock: Clock = Clock.systemUTC(),
            id: RefundId = UUID.randomUUID(),
        ): Refund {
            if (original.status != TransactionStatus.POSTED) throw DomainValidationException("Only posted transactions can be refunded.")
            if (original.type != TransactionType.EXPENSE && original.type != TransactionType.PURCHASE) throw DomainValidationException("Only expenses and purchases can be refunded.")
            if (original.direction != TransactionDirection.DEBIT) throw DomainValidationException("The original transaction must be a debit.")
            if (amount.amountInMinor <= 0) throw DomainValidationException("Refund amount must be greater than zero.")
            if (amount.currency != original.amount.currency) throw DomainValidationException("Refund currency must match the original transaction.")
            if (amount.amountInMinor > original.amount.amountInMinor) throw DomainValidationException("Refund amount cannot exceed the original transaction amount.")
            if (status == TransactionStatus.CANCELLED) throw DomainValidationException("A refund cannot start cancelled.")
            val now = Instant.now(clock)
            return Refund(
                id = id,
                originalTransactionId = original.id,
                transaction = Transaction.create(
                    accountId = original.accountId,
                    amount = amount,
                    type = TransactionType.REFUND,
                    direction = TransactionDirection.CREDIT,
                    occurredOn = occurredOn,
                    description = description,
                    source = source,
                    status = status,
                    relatedTransactionId = original.id,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }
}
