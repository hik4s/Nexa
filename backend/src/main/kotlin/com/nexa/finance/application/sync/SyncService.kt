package com.nexa.finance.application.sync

import com.nexa.finance.api.SyncAcknowledgement
import com.nexa.finance.api.SyncChangeResponse
import com.nexa.finance.api.SyncPushOperation
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.category.CategoryId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.transaction.TransactionDirection
import com.nexa.finance.domain.transaction.TransactionId
import com.nexa.finance.domain.transaction.TransactionSource
import com.nexa.finance.domain.transaction.TransactionType
import com.nexa.finance.application.transaction.TransactionService
import com.nexa.finance.infrastructure.sync.JdbcSyncChangeRepository
import com.nexa.finance.infrastructure.transaction.JdbcTransactionRepository
import java.time.LocalDate
import java.util.UUID

class SyncService(private val transactions: TransactionService, private val transactionRepository: JdbcTransactionRepository, private val changes: JdbcSyncChangeRepository) {
    fun push(ownerId: UserId, operations: List<SyncPushOperation>): List<SyncAcknowledgement> = operations.map { operation ->
        if (operation.entityType != "TRANSACTION" || operation.operation != "UPSERT") throw DomainValidationException("Unsupported sync operation.")
        val payload = operation.payload ?: throw DomainValidationException("Sync transaction payload is required.")
        val entityId = runCatching { UUID.fromString(operation.entityId) }.getOrElse { throw DomainValidationException("Sync entity ID is invalid.") }
        if (payload.id != operation.entityId) throw DomainValidationException("Sync payload ID does not match entity ID.")
        val current = runCatching { transactions.get(ownerId, entityId) }.getOrNull()
        if (current != null) {
            if (!current.matchesSyncPayload(payload)) {
                return@map SyncAcknowledgement(operation.operationId, operation.entityType, operation.entityId, "CONFLICT")
            }
            return@map SyncAcknowledgement(operation.operationId, operation.entityType, operation.entityId, "ACKNOWLEDGED")
        }
        transactions.create(ownerId, AccountId(UUID.fromString(payload.accountId)), Money(payload.amountMinor, enumValue<CurrencyCode>(payload.currency)), enumValue<TransactionType>(payload.type), enumValue<TransactionDirection>(payload.direction), runCatching { LocalDate.parse(payload.occurredOn) }.getOrElse { throw DomainValidationException("Sync transaction date is invalid.") }, payload.description, payload.categoryId?.let { CategoryId(UUID.fromString(it)) }, enumValue<TransactionSource>(payload.source), entityId)
        SyncAcknowledgement(operation.operationId, operation.entityType, operation.entityId, "ACKNOWLEDGED")
    }

    fun latestCursor(ownerId: UserId): Long = changes.latestCursor(ownerId)

    private fun com.nexa.finance.domain.transaction.Transaction.matchesSyncPayload(payload: com.nexa.finance.api.CreateTransactionRequest): Boolean =
        accountId.value.toString() == payload.accountId &&
            amount.amountInMinor == payload.amountMinor &&
            amount.currency.name == payload.currency.trim().uppercase() &&
            type.name == payload.type.trim().uppercase() &&
            direction.name == payload.direction.trim().uppercase() &&
            occurredOn == runCatching { LocalDate.parse(payload.occurredOn) }.getOrNull() &&
            description == payload.description?.trim()?.takeIf { it.isNotEmpty() } &&
            categoryId?.value?.toString() == payload.categoryId &&
            source.name == payload.source.trim().uppercase()

    fun pull(ownerId: UserId, cursor: Long, limit: Int): Pair<Long, List<SyncChangeResponse>> {
        require(cursor >= 0 && limit in 1..100) { "Invalid sync cursor or limit." }
        val rows = changes.changesAfter(ownerId, cursor, limit)
        val responses = rows.map { change -> SyncChangeResponse(change.cursor, change.entityType, change.entityId.toString(), change.operation, if (change.entityType == "TRANSACTION" && change.operation == "UPSERT") transactionRepository.findByOwnerAndId(ownerId, change.entityId)?.toResponse() else null) }
        return (responses.maxOfOrNull { it.cursor } ?: cursor) to responses
    }

    private fun com.nexa.finance.domain.transaction.Transaction.toResponse() = com.nexa.finance.api.TransactionResponse(id.toString(), accountId.value.toString(), amount.amountInMinor, amount.currency.name, type.name, direction.name, status.name, occurredOn.toString(), description, categoryId?.value?.toString(), source.name, installmentId?.toString(), installmentNumber)
    private inline fun <reified T : Enum<T>> enumValue(value: String): T = runCatching { enumValueOf<T>(value.trim().uppercase()) }.getOrElse { throw DomainValidationException("Invalid sync enum value.") }
}
