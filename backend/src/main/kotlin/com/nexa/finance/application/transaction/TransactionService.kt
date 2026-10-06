package com.nexa.finance.application.transaction

import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.account.AccountStatus
import com.nexa.finance.application.audit.AuditEventRecorder
import com.nexa.finance.application.audit.AuditEventTypes
import com.nexa.finance.domain.category.CategoryId
import com.nexa.finance.domain.common.ConflictException
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.transaction.Transaction
import com.nexa.finance.domain.transaction.TransactionDirection
import com.nexa.finance.domain.transaction.TransactionId
import com.nexa.finance.domain.transaction.TransactionSource
import com.nexa.finance.domain.transaction.TransactionStatus
import com.nexa.finance.domain.transaction.TransactionType
import com.nexa.finance.infrastructure.account.JdbcAccountRepository
import com.nexa.finance.infrastructure.transaction.JdbcTransactionRepository
import java.time.LocalDate

class TransactionService(
    private val transactions: JdbcTransactionRepository,
    private val accounts: JdbcAccountRepository,
    private val audit: AuditEventRecorder? = null,
) {
    fun create(
        ownerId: UserId,
        accountId: AccountId,
        amount: Money,
        type: TransactionType,
        direction: TransactionDirection,
        occurredOn: LocalDate,
        description: String?,
        categoryId: CategoryId?,
        source: TransactionSource,
        id: TransactionId? = null,
    ): Transaction {
        val existing = id?.let { transactions.findByOwnerAndId(ownerId, it) }
        if (existing != null) return existing

        val account = accounts.findByOwnerAndId(ownerId, accountId)
            ?: throw DomainValidationException("Account was not found.")
        if (account.status != AccountStatus.ACTIVE) {
            throw DomainValidationException("Archived accounts cannot receive new transactions.")
        }
        if (amount.currency != account.currency) {
            throw DomainValidationException("Transaction currency must match account currency.")
        }
        val transaction = Transaction.create(
            accountId = accountId,
            amount = amount,
            type = type,
            direction = direction,
            occurredOn = occurredOn,
            description = description,
            categoryId = categoryId,
            source = source,
            id = id ?: java.util.UUID.randomUUID(),
        )
        transactions.create(ownerId, transaction)
        audit?.record(ownerId, AuditEventTypes.TRANSACTION_CREATED, "TRANSACTION", transaction.id)
        return transaction
    }

    fun update(
        ownerId: UserId,
        id: TransactionId,
        baseVersion: Long,
        accountId: AccountId,
        amount: Money,
        type: TransactionType,
        direction: TransactionDirection,
        occurredOn: LocalDate,
        description: String?,
        categoryId: CategoryId?,
        source: TransactionSource,
    ): Transaction {
        require(baseVersion > 0) { "Base version must be positive." }
        val current = get(ownerId, id)
        if (current.version != baseVersion) {
            throw ConflictException("Transaction has changed since this client last read it.")
        }
        if (amount.currency != current.amount.currency) {
            val targetAccount = accounts.findByOwnerAndId(ownerId, accountId)
                ?: throw DomainValidationException("Account was not found.")
            if (amount.currency != targetAccount.currency) {
                throw DomainValidationException("Transaction currency must match account currency.")
            }
        } else {
            val targetAccount = accounts.findByOwnerAndId(ownerId, accountId)
                ?: throw DomainValidationException("Account was not found.")
            if (amount.currency != targetAccount.currency) {
                throw DomainValidationException("Transaction currency must match account currency.")
            }
        }
        val updated = transactions.updateEditableFields(
            ownerId = ownerId,
            id = id,
            expectedVersion = baseVersion,
            accountId = accountId,
            amount = amount,
            type = type,
            direction = direction,
            occurredOn = occurredOn,
            description = description,
            categoryId = categoryId,
            source = source,
        ) ?: run {
            val latest = transactions.findByOwnerAndId(ownerId, id)
            if (latest == null) throw DomainValidationException("Transaction was not found.")
            throw ConflictException("Transaction has changed since this client last read it.")
        }
        audit?.record(ownerId, AuditEventTypes.TRANSACTION_UPDATED, "TRANSACTION", updated.id)
        return updated
    }

    fun list(ownerId: UserId, accountId: AccountId?, limit: Int = 100, offset: Int = 0): List<Transaction> =
        transactions.findByOwner(ownerId, accountId, limit, offset)

    fun get(ownerId: UserId, id: TransactionId): Transaction =
        transactions.findByOwnerAndId(ownerId, id)
            ?: throw DomainValidationException("Transaction was not found.")

    fun post(ownerId: UserId, id: TransactionId): Transaction =
        get(ownerId, id).let {
            if (it.status != TransactionStatus.PENDING) {
                throw DomainValidationException("Only pending transactions can be posted.")
            }
            transactions.updateStatus(ownerId, id, TransactionStatus.POSTED, it.version)
                ?: run {
                    val latest = transactions.findByOwnerAndId(ownerId, id)
                    if (latest == null) throw DomainValidationException("Transaction was not found.")
                    throw ConflictException("Transaction has changed since this client last read it.")
                }
            } .also { updated ->
                audit?.record(ownerId, AuditEventTypes.TRANSACTION_POSTED, "TRANSACTION", updated.id)
            }
        }

    fun cancel(ownerId: UserId, id: TransactionId): Transaction =
        get(ownerId, id).let {
            if (it.status == TransactionStatus.CANCELLED) {
                throw DomainValidationException("Transaction is already cancelled.")
            }
            transactions.updateStatus(ownerId, id, TransactionStatus.CANCELLED, it.version)
                ?: run {
                    val latest = transactions.findByOwnerAndId(ownerId, id)
                    if (latest == null) throw DomainValidationException("Transaction was not found.")
                    throw ConflictException("Transaction has changed since this client last read it.")
                }
            }.also { updated ->
                audit?.record(ownerId, AuditEventTypes.TRANSACTION_CANCELLED, "TRANSACTION", updated.id)
            }
        }
}
