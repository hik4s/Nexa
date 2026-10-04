package com.nexa.finance.application.sync

import com.nexa.finance.api.CreateTransactionRequest
import com.nexa.finance.api.SyncAcknowledgement
import com.nexa.finance.api.SyncChangeResponse
import com.nexa.finance.api.SyncPushOperation
import com.nexa.finance.api.TransactionResponse
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.category.CategoryId
import com.nexa.finance.domain.common.ConflictException
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.transaction.TransactionDirection
import com.nexa.finance.domain.transaction.TransactionSource
import com.nexa.finance.domain.transaction.TransactionType
import com.nexa.finance.infrastructure.sync.JdbcSyncChangeRepository
import com.nexa.finance.infrastructure.transaction.JdbcTransactionRepository
import com.nexa.finance.application.transaction.TransactionService
import java.time.LocalDate
import java.util.UUID

class SyncService(
    private val transactions: TransactionService,
    private val transactionRepository: JdbcTransactionRepository,
    private val changes: JdbcSyncChangeRepository,
) {
    fun push(ownerId: UserId, operations: List<SyncPushOperation>): List<SyncAcknowledgement> =
        operations.map { operation ->
            if (operation.entityType != "TRANSACTION" || operation.operation != "UPSERT") {
                throw DomainValidationException("Unsupported sync operation.")
            }

            val payload = operation.payload
                ?: throw DomainValidationException("Sync transaction payload is required.")
            val entityId = runCatching { UUID.fromString(operation.entityId) }
                .getOrElse { throw DomainValidationException("Sync entity ID is invalid.") }

            if (payload.id != operation.entityId) {
                throw DomainValidationException("Sync payload ID does not match entity ID.")
            }
            if (payload.baseVersion != null && payload.baseVersion <= 0) {
                throw DomainValidationException("Sync base version must be positive.")
            }

            val current = transactionRepository.findByOwnerAndId(ownerId, entityId)
            when {
                current == null -> {
                    if (payload.baseVersion != null) {
                        return@map conflict(operation)
                    }

                    transactions.create(
                        ownerId = ownerId,
                        accountId = AccountId(UUID.fromString(payload.accountId)),
                        amount = Money(payload.amountMinor, enumValue<CurrencyCode>(payload.currency)),
                        type = enumValue<TransactionType>(payload.type),
                        direction = enumValue<TransactionDirection>(payload.direction),
                        occurredOn = parseDate(payload.occurredOn),
                        description = payload.description,
                        categoryId = payload.categoryId?.let { CategoryId(UUID.fromString(it)) },
                        source = enumValue<TransactionSource>(payload.source),
                        id = entityId,
                    )
                    acknowledgement(operation)
                }

                payload.baseVersion == null -> {
                    if (current.matchesSyncPayload(payload)) acknowledgement(operation)
                    else conflict(operation)
                }

                current.matchesSyncPayload(payload) && current.version == payload.baseVersion -> {
                    acknowledgement(operation, current.version)
                }

                current.matchesSyncPayload(payload) && current.version == payload.baseVersion + 1 -> {
                    acknowledgement(operation, current.version)
                }

                current.version != payload.baseVersion -> {
                    conflict(operation)
                }

                else -> {
                    val updated = transactions.update(
                        ownerId = ownerId,
                        id = entityId,
                        baseVersion = payload.baseVersion,
                        accountId = AccountId(UUID.fromString(payload.accountId)),
                        amount = Money(payload.amountMinor, enumValue<CurrencyCode>(payload.currency)),
                        type = enumValue<TransactionType>(payload.type),
                        direction = enumValue<TransactionDirection>(payload.direction),
                        occurredOn = parseDate(payload.occurredOn),
                        description = payload.description,
                        categoryId = payload.categoryId?.let { CategoryId(UUID.fromString(it)) },
                        source = enumValue<TransactionSource>(payload.source),
                    )
                    acknowledgement(operation, updated.version)
                }
            }
        }

    fun latestCursor(ownerId: UserId): Long = changes.latestCursor(ownerId)

    fun pull(ownerId: UserId, cursor: Long, limit: Int): Pair<Long, List<SyncChangeResponse>> {
        require(cursor >= 0 && limit in 1..100) { "Invalid sync cursor or limit." }
        val rows = changes.changesAfter(ownerId, cursor, limit)
        val responses = rows.map { change ->
            SyncChangeResponse(
                cursor = change.cursor,
                entityType = change.entityType,
                entityId = change.entityId.toString(),
                operation = change.operation,
                transaction = if (change.entityType == "TRANSACTION" && change.operation == "UPSERT") {
                    transactionRepository.findByOwnerAndId(ownerId, change.entityId)?.toResponse()
                } else {
                    null
                },
            )
        }
        return (responses.maxOfOrNull { it.cursor } ?: cursor) to responses
    }

    private fun acknowledgement(
        operation: SyncPushOperation,
        version: Long? = null,
    ) = SyncAcknowledgement(operation.operationId, operation.entityType, operation.entityId, "ACKNOWLEDGED", version)

    private fun conflict(operation: SyncPushOperation) =
        SyncAcknowledgement(operation.operationId, operation.entityType, operation.entityId, "CONFLICT", null)

    private fun com.nexa.finance.domain.transaction.Transaction.toResponse() =
        TransactionResponse(
            id = id.toString(),
            accountId = accountId.value.toString(),
            amountMinor = amount.amountInMinor,
            currency = amount.currency.name,
            type = type.name,
            direction = direction.name,
            status = status.name,
            occurredOn = occurredOn.toString(),
            description = description,
            categoryId = categoryId?.value?.toString(),
            source = source.name,
            installmentId = installmentId?.toString(),
            installmentNumber = installmentNumber,
            version = version,
        )

    private fun com.nexa.finance.domain.transaction.Transaction.matchesSyncPayload(
        payload: CreateTransactionRequest,
    ): Boolean =
        accountId.value.toString() == payload.accountId &&
            amount.amountInMinor == payload.amountMinor &&
            amount.currency.name == payload.currency.trim().uppercase() &&
            type.name == payload.type.trim().uppercase() &&
            direction.name == payload.direction.trim().uppercase() &&
            occurredOn == runCatching { LocalDate.parse(payload.occurredOn) }.getOrNull() &&
            description == payload.description?.trim()?.takeIf { it.isNotEmpty() } &&
            categoryId?.value?.toString() == payload.categoryId &&
            source.name == payload.source.trim().uppercase()

    private fun parseDate(value: String): LocalDate =
        runCatching { LocalDate.parse(value) }
            .getOrElse { throw DomainValidationException("Sync transaction date is invalid.") }

    private inline fun <reified T : Enum<T>> enumValue(value: String): T =
        runCatching { enumValueOf<T>(value.trim().uppercase()) }
            .getOrElse { throw DomainValidationException("Invalid sync enum value.") }
}
