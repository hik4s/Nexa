package com.nexa.finance.domain.transfer

import com.nexa.finance.domain.account.Account
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.transaction.Transaction
import com.nexa.finance.domain.transaction.TransactionDirection
import com.nexa.finance.domain.transaction.TransactionStatus
import com.nexa.finance.domain.transaction.TransactionType
import com.nexa.finance.domain.transaction.TransactionSource
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

typealias TransferId = UUID

enum class TransferStatus { PENDING, POSTED, CANCELLED }

@ConsistentCopyVisibility
data class Transfer private constructor(
    val id: TransferId,
    val userId: UserId,
    val sourceAccountId: AccountId,
    val destinationAccountId: AccountId,
    val amount: Money,
    val occurredOn: LocalDate,
    val sourceTransaction: Transaction,
    val destinationTransaction: Transaction,
    val status: TransferStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        if (amount.amountInMinor <= 0) throw DomainValidationException("Transfer amount must be greater than zero.")
        if (sourceAccountId == destinationAccountId) throw DomainValidationException("Source and destination accounts must be different.")
        if (sourceTransaction.relatedTransactionId != destinationTransaction.id || destinationTransaction.relatedTransactionId != sourceTransaction.id) throw DomainValidationException("Transfer transactions must reference each other.")
        if (sourceTransaction.accountId != sourceAccountId || destinationTransaction.accountId != destinationAccountId) throw DomainValidationException("Transfer transactions must reference the correct accounts.")
        if (sourceTransaction.amount != amount || destinationTransaction.amount != amount) throw DomainValidationException("Transfer transactions must use the transfer amount.")
        if (sourceTransaction.type != TransactionType.TRANSFER || destinationTransaction.type != TransactionType.TRANSFER) throw DomainValidationException("Transfer transactions must have type TRANSFER.")
        if (sourceTransaction.direction != TransactionDirection.DEBIT || destinationTransaction.direction != TransactionDirection.CREDIT) throw DomainValidationException("Transfer must debit the source and credit the destination.")
        if (sourceTransaction.status != status.toTransactionStatus() || destinationTransaction.status != status.toTransactionStatus()) throw DomainValidationException("Transfer status must match both linked transactions.")
    }

    fun post(now: Instant = Instant.now()): Transfer {
        if (status != TransferStatus.PENDING) throw DomainValidationException("Only pending transfers can be posted.")
        return copy(sourceTransaction = sourceTransaction.post(now), destinationTransaction = destinationTransaction.post(now), status = TransferStatus.POSTED, updatedAt = now)
    }

    fun cancel(now: Instant = Instant.now()): Transfer {
        if (status != TransferStatus.PENDING) throw DomainValidationException("Only pending transfers can be cancelled.")
        return copy(sourceTransaction = sourceTransaction.cancel(now), destinationTransaction = destinationTransaction.cancel(now), status = TransferStatus.CANCELLED, updatedAt = now)
    }

    companion object {
        fun create(userId: UserId, sourceAccount: Account, destinationAccount: Account, amount: Money, occurredOn: LocalDate, source: TransactionSource = TransactionSource.MANUAL, clock: Clock = Clock.systemUTC(), id: TransferId = UUID.randomUUID()): Transfer {
            sourceAccount.ensureUsable()
            destinationAccount.ensureUsable()
            if (sourceAccount.ownerId != userId || destinationAccount.ownerId != userId) throw DomainValidationException("Both transfer accounts must belong to the same user.")
            if (sourceAccount.id == destinationAccount.id) throw DomainValidationException("Source and destination accounts must be different.")
            if (sourceAccount.currency != destinationAccount.currency) throw DomainValidationException("Transfers currently require the same currency.")
            if (amount.currency != sourceAccount.currency) throw DomainValidationException("Transfer amount currency must match the account currency.")
            if (amount.amountInMinor <= 0) throw DomainValidationException("Transfer amount must be greater than zero.")
            val now = Instant.now(clock)
            val sourceTransactionId = UUID.randomUUID()
            val destinationTransactionId = UUID.randomUUID()
            val sourceTransaction = Transaction.create(sourceAccount.id, amount, TransactionType.TRANSFER, TransactionDirection.DEBIT, occurredOn, source = source, status = TransactionStatus.PENDING, relatedTransactionId = destinationTransactionId, id = sourceTransactionId, createdAt = now, updatedAt = now)
            val destinationTransaction = Transaction.create(destinationAccount.id, amount, TransactionType.TRANSFER, TransactionDirection.CREDIT, occurredOn, source = source, status = TransactionStatus.PENDING, relatedTransactionId = sourceTransactionId, id = destinationTransactionId, createdAt = now, updatedAt = now)
            return Transfer(id, userId, sourceAccount.id, destinationAccount.id, amount, occurredOn, sourceTransaction, destinationTransaction, TransferStatus.PENDING, now, now)
        }

        fun restore(
            id: TransferId,
            userId: UserId,
            sourceAccountId: AccountId,
            destinationAccountId: AccountId,
            amount: Money,
            occurredOn: LocalDate,
            sourceTransaction: Transaction,
            destinationTransaction: Transaction,
            status: TransferStatus,
            createdAt: Instant,
            updatedAt: Instant,
        ): Transfer = Transfer(id, userId, sourceAccountId, destinationAccountId, amount, occurredOn, sourceTransaction, destinationTransaction, status, createdAt, updatedAt)

        private fun TransferStatus.toTransactionStatus(): TransactionStatus = when (this) {
            TransferStatus.PENDING -> TransactionStatus.PENDING
            TransferStatus.POSTED -> TransactionStatus.POSTED
            TransferStatus.CANCELLED -> TransactionStatus.CANCELLED
        }
    }
}
