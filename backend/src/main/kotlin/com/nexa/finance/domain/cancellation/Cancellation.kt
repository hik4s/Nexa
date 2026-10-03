package com.nexa.finance.domain.cancellation

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.transaction.*
import java.time.Instant
import java.util.UUID

object Cancellation {
    fun cancelPending(transaction: Transaction, now: Instant = Instant.now()): Transaction {
        if (transaction.status != TransactionStatus.PENDING) {
            throw DomainValidationException("Only pending transactions can be cancelled by state transition.")
        }
        return transaction.cancel(now)
    }

    fun createForPosted(transaction: Transaction, now: Instant = Instant.now(), id: UUID = UUID.randomUUID()): Transaction {
        if (transaction.status != TransactionStatus.POSTED) throw DomainValidationException("Only posted transactions can receive a cancellation transaction.")
        if (transaction.type == TransactionType.CANCELLATION) throw DomainValidationException("A cancellation transaction cannot be cancelled again.")
        val oppositeDirection = when (transaction.direction) {
            TransactionDirection.DEBIT -> TransactionDirection.CREDIT
            TransactionDirection.CREDIT -> TransactionDirection.DEBIT
        }
        return Transaction.create(
            accountId = transaction.accountId,
            amount = transaction.amount,
            type = TransactionType.CANCELLATION,
            direction = oppositeDirection,
            occurredOn = transaction.occurredOn,
            description = transaction.description?.let { "Cancellation: $it" },
            source = TransactionSource.SYSTEM,
            status = TransactionStatus.POSTED,
            relatedTransactionId = transaction.id,
            id = id,
            createdAt = now,
            updatedAt = now,
        )
    }
}
