package com.nexa.finance.sync


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
    private val scheduleSync: () -> Unit,
    private val payloadCodec: SyncConflictPayloadCodec = JsonSyncConflictPayloadCodec,
) {
    suspend fun pending(userId: String): List<SyncConflictSummary> =
        conflictDao.pending(userId).map(::summarize)

    suspend fun keepServer(operationId: String) {
        val conflict = conflictDao.get(operationId)
            ?: error("Conflito não encontrado.")
        val server = conflict.serverTransactionJson
            ?.let(payloadCodec::parseTransaction)
            ?: error("O conflito não contém um snapshot do servidor.")

        transactionDao.upsert(server.toLocalEntity())
        operationDao.delete(conflict.operationId)
        conflictDao.delete(conflict.operationId)
    }

    suspend fun resubmitMine(operationId: String) {
        val conflict = conflictDao.get(operationId)
            ?: error("Conflito não encontrado.")
        val serverVersion = conflict.serverVersion
            ?: error("O conflito não possui a versão atual do servidor.")

        val payload = payloadCodec.rebasePayload(
            payloadJson = conflict.localPayloadJson,
            entityId = conflict.entityId,
            serverVersion = serverVersion,
        )

        val updated = operationDao.requeueConflict(
            id = conflict.operationId,
            payloadJson = payload,
            nowEpochMillis = clock(),
        )
        check(updated == 1) { "A operação em conflito não está mais disponível para reenvio." }
        scheduleSync()
    }

    private fun summarize(conflict: SyncConflictEntity): SyncConflictSummary {
        val (localDescription, localAmountMinor) = payloadCodec.summarizeLocal(conflict.localPayloadJson)
        val server = conflict.serverTransactionJson?.let(payloadCodec::parseTransaction)

        return SyncConflictSummary(
            operationId = conflict.operationId,
            entityId = conflict.entityId,
            localDescription = localDescription,
            localAmountMinor = localAmountMinor,
            serverDescription = server?.description,
            serverAmountMinor = server?.amountMinor,
            serverVersion = conflict.serverVersion,
        )
    }

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
