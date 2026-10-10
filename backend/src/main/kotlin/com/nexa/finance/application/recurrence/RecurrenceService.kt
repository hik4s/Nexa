package com.nexa.finance.application.recurrence

import com.nexa.finance.application.audit.AuditEventRecorder
import com.nexa.finance.application.audit.AuditEventTypes
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.account.AccountStatus
import com.nexa.finance.domain.account.AccountType
import com.nexa.finance.domain.category.CategoryId
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.recurrence.Recurrence
import com.nexa.finance.domain.recurrence.RecurrenceFrequency
import com.nexa.finance.domain.recurrence.RecurrenceId
import com.nexa.finance.domain.recurrence.RecurrenceOccurrence
import com.nexa.finance.domain.recurrence.RecurrenceStatus
import com.nexa.finance.domain.transaction.Transaction
import com.nexa.finance.domain.transaction.TransactionDirection
import com.nexa.finance.domain.transaction.TransactionSource
import com.nexa.finance.domain.transaction.TransactionType
import com.nexa.finance.infrastructure.account.JdbcAccountRepository
import com.nexa.finance.infrastructure.recurrence.JdbcRecurrenceRepository
import com.nexa.finance.infrastructure.transaction.JdbcTransactionRepository
import java.time.Clock
import java.time.LocalDate

class RecurrenceService(
    private val recurrences: JdbcRecurrenceRepository,
    private val accounts: JdbcAccountRepository,
    private val transactions: JdbcTransactionRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val audit: AuditEventRecorder? = null,
) {
    fun create(
        ownerId: UserId,
        accountId: AccountId,
        amount: Money,
        transactionType: TransactionType,
        frequency: RecurrenceFrequency,
        interval: Int,
        startOn: LocalDate,
        endOn: LocalDate?,
        maxOccurrences: Int?,
        description: String?,
        categoryId: CategoryId?,
    ): Recurrence {
        validateAccountAndCategory(ownerId, accountId, amount, categoryId)
        val recurrence = Recurrence.create(
            ownerId,
            accountId,
            amount,
            transactionType,
            frequency,
            interval,
            startOn,
            endOn,
            maxOccurrences,
            description,
            categoryId,
            clock = clock,
        )
        recurrences.create(recurrence)
        audit?.record(ownerId, AuditEventTypes.RECURRENCE_CREATED, "RECURRENCE", recurrence.id)
        return recurrence
    }

    fun update(
        ownerId: UserId,
        id: RecurrenceId,
        accountId: AccountId,
        amount: Money,
        transactionType: TransactionType,
        frequency: RecurrenceFrequency,
        interval: Int,
        startOn: LocalDate,
        endOn: LocalDate?,
        maxOccurrences: Int?,
        description: String?,
        categoryId: CategoryId?,
    ): Recurrence {
        val current = get(ownerId, id)
        validateAccountAndCategory(ownerId, accountId, amount, categoryId)
        val updated = recurrences.update(
            current.update(
                accountId = accountId,
                amount = amount,
                transactionType = transactionType,
                frequency = frequency,
                interval = interval,
                startOn = startOn,
                endOn = endOn,
                maxOccurrences = maxOccurrences,
                description = description,
                categoryId = categoryId,
                now = java.time.Instant.now(clock),
            )
        ) ?: throw DomainValidationException("Recurrence was not found.")
        audit?.record(ownerId, AuditEventTypes.RECURRENCE_UPDATED, "RECURRENCE", updated.id)
        return updated
    }

    fun list(ownerId: UserId): List<Recurrence> = recurrences.list(ownerId)

    fun summaries(ownerId: UserId): List<RecurrenceSummary> =
        list(ownerId).map { RecurrenceInsights.summary(it, clock) }

    fun get(ownerId: UserId, id: RecurrenceId): Recurrence =
        recurrences.findByOwnerAndId(ownerId, id)
            ?: throw DomainValidationException("Recurrence was not found.")

    fun summary(ownerId: UserId, id: RecurrenceId): RecurrenceSummary =
        RecurrenceInsights.summary(get(ownerId, id), clock)

    fun candidates(ownerId: UserId): List<RecurrenceCandidate> =
        RecurrenceInsights.candidates(
            transactions = transactions.findByOwner(ownerId),
            recurrences = recurrences.list(ownerId),
            today = LocalDate.now(clock),
        )

    fun occurrences(
        ownerId: UserId,
        id: RecurrenceId,
        from: LocalDate,
        to: LocalDate,
    ): List<RecurrenceOccurrence> =
        get(ownerId, id).occurrencesBetween(from, to)

    fun pause(ownerId: UserId, id: RecurrenceId): Recurrence =
        updateStatus(ownerId, id, get(ownerId, id).pause(java.time.Instant.now(clock)).status)

    fun resume(ownerId: UserId, id: RecurrenceId): Recurrence =
        updateStatus(ownerId, id, get(ownerId, id).resume(java.time.Instant.now(clock)).status)

    fun cancel(ownerId: UserId, id: RecurrenceId): Recurrence =
        updateStatus(ownerId, id, get(ownerId, id).cancel(java.time.Instant.now(clock)).status)

    private fun updateStatus(ownerId: UserId, id: RecurrenceId, status: RecurrenceStatus): Recurrence {
        val updated = recurrences.updateStatus(ownerId, id, status)
            ?: throw DomainValidationException("Recurrence was not found.")
        audit?.record(ownerId, AuditEventTypes.RECURRENCE_STATUS_CHANGED, "RECURRENCE", updated.id)
        return updated
    }

    fun execute(ownerId: UserId, id: RecurrenceId, occurrenceNumber: Int): Transaction {
        val recurrence = get(ownerId, id)
        val occurrence = recurrence.occurrence(occurrenceNumber)
        recurrences.findExecution(ownerId, id, occurrenceNumber)?.let { existingId ->
            return transactions.findByOwnerAndId(ownerId, existingId)
                ?: throw DomainValidationException("Recurring execution record is inconsistent.")
        }
        if (recurrence.status != RecurrenceStatus.ACTIVE) {
            throw DomainValidationException("Only active recurrences can be executed.")
        }
        val account = accounts.findByOwnerAndId(ownerId, recurrence.accountId)
            ?: throw DomainValidationException("Account was not found.")
        if (account.status != AccountStatus.ACTIVE) {
            throw DomainValidationException("Archived accounts cannot receive recurring transactions.")
        }
        if (account.type == AccountType.CREDIT_CARD) {
            throw DomainValidationException("Credit-card accounts require card purchase recurrence semantics.")
        }
        val direction = if (recurrence.transactionType == TransactionType.INCOME) {
            TransactionDirection.CREDIT
        } else {
            TransactionDirection.DEBIT
        }
        val transaction = Transaction.create(
            accountId = recurrence.accountId,
            amount = recurrence.amount,
            type = recurrence.transactionType,
            direction = direction,
            occurredOn = occurrence.scheduledOn,
            description = recurrence.description,
            categoryId = recurrence.categoryId,
            source = TransactionSource.SYSTEM,
            recurrenceId = recurrence.id,
            recurrenceOccurrenceNumber = occurrence.number,
        )
        transactions.create(ownerId, transaction)
        audit?.record(ownerId, AuditEventTypes.RECURRENCE_EXECUTED, "TRANSACTION", transaction.id)
        return transaction
    }

    private fun validateAccountAndCategory(
        ownerId: UserId,
        accountId: AccountId,
        amount: Money,
        categoryId: CategoryId?,
    ) {
        val account = accounts.findByOwnerAndId(ownerId, accountId)
            ?: throw DomainValidationException("Account was not found.")
        if (account.status != AccountStatus.ACTIVE) {
            throw DomainValidationException("Archived accounts cannot receive recurring transactions.")
        }
        if (account.type == AccountType.CREDIT_CARD) {
            throw DomainValidationException("Credit-card accounts require card purchase recurrence semantics.")
        }
        if (amount.currency != account.currency) {
            throw DomainValidationException("Recurrence currency must match account currency.")
        }
        if (categoryId != null && !transactions.categoryBelongsToOwner(ownerId, categoryId)) {
            throw DomainValidationException("Category was not found.")
        }
    }
}
