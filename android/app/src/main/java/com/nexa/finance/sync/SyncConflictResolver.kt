package com.nexa.finance.sync

import org.json.JSONObject

data class SyncConflictSummary(
    val operationId: String,
    val entityId: String,
    val localDescription: String,
    val localAmountMinor: Long?,
    val serverDescription: String?,
    val serverAmountMinor: Long?,
    val serverVersion: Long?,
)

class SyncConflictResolver(
    private val conflictDao: SyncConflictDao,
    private val operationDao: SyncOperationDao,
    private val transactionDao: LocalTransactionDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun pending(userId: String): List<SyncConflictSummary> =
        conflictDao.pending(userId).map(::summarize)

    suspend fun keepServer(conflict: SyncConflictEntity) {
        val server = conflict.serverTransactionJson
            ?.let { parseTransaction(JSONObject(it)) }
            ?: error("O conflito não contém um snapshot do servidor.")

        transactionDao.upsert(server.toLocalEntity())
        operationDao.delete(conflict.operationId)
        conflictDao.delete(conflict.operationId)
    }

    suspend fun resubmitMine(conflict: SyncConflictEntity) {
        val serverVersion = conflict.serverVersion
            ?: error("O conflito não possui a versão atual do servidor.")

        val payload = JSONObject(conflict.localPayloadJson).apply {
            put("id", conflict.entityId)
            put("baseVersion", serverVersion)
        }

        val updated = operationDao.requeueConflict(
            id = conflict.operationId,
            payloadJson = payload.toString(),
            nowEpochMillis = clock(),
        )
        check(updated == 1) { "A operação em conflito não está mais disponível para reenvio." }
        SyncScheduler.scheduleFromApplicationContext()
    }

    private fun summarize(conflict: SyncConflictEntity): SyncConflictSummary {
        val local = JSONObject(conflict.localPayloadJson)
        val server = conflict.serverTransactionJson?.let { parseTransaction(JSONObject(it)) }

        return SyncConflictSummary(
            operationId = conflict.operationId,
            entityId = conflict.entityId,
            localDescription = local.optString("description").ifBlank { "Lançamento" },
            localAmountMinor = local.optLong("amountMinor", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE },
            serverDescription = server?.description,
            serverAmountMinor = server?.amountMinor,
            serverVersion = conflict.serverVersion,
        )
    }

    private fun parseTransaction(o: JSONObject): SyncTransaction =
        SyncTransaction(
            id = o.getString("id"),
            accountId = o.getString("accountId"),
            amountMinor = o.getLong("amountMinor"),
            currency = o.getString("currency"),
            type = o.getString("type"),
            direction = o.getString("direction"),
            status = o.getString("status"),
            occurredOn = o.getString("occurredOn"),
            description = o.optString("description").ifBlank { null },
            categoryId = o.optString("categoryId").ifBlank { null },
            source = o.getString("source"),
            installmentId = o.optString("installmentId").ifBlank { null },
            installmentNumber = o.optInt("installmentNumber", 0).takeIf { it > 0 },
            version = o.optLong("version", 1L),
        )

    private fun SyncTransaction.toLocalEntity(): LocalTransactionEntity =
        LocalTransactionEntity(
            id = id,
            accountId = accountId,
            amountMinor = amountMinor,
            currency = currency,
            type = type,
            direction = direction,
            status = status,
            occurredOn = occurredOn,
            description = description,
            categoryId = categoryId,
            source = source,
            installmentId = installmentId,
            installmentNumber = installmentNumber,
            version = version,
        )
}

private object SyncScheduler {
    fun scheduleFromApplicationContext() {
        // Replaced at integration point by the Android-aware adapter below.
    }
}