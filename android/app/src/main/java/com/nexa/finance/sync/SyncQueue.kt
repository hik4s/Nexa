package com.nexa.finance.sync

import java.util.UUID

class SyncQueue(
    private val operationDao: SyncOperationDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) {
    suspend fun enqueue(
        userId: String,
        entityType: String,
        entityId: String,
        operation: String,
        payloadJson: String,
    ): String {
        val id = idFactory()
        operationDao.upsert(
            SyncOperationEntity(
                id = id,
                userId = userId,
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                payloadJson = payloadJson,
                createdAtEpochMillis = clock(),
            ),
        )
        return id
    }

    suspend fun claimBatch(userId: String, limit: Int = 20): List<SyncOperationEntity> {
        require(limit in 1..100) { "Sync batch limit must be between 1 and 100." }
        val now = clock()
        val pending = operationDao.pending(userId, now, limit)
        pending.forEach { operationDao.updateStatus(it.id, SyncOperationEntity.STATUS_PROCESSING) }
        return pending.map { it.copy(status = SyncOperationEntity.STATUS_PROCESSING) }
    }

    suspend fun complete(id: String) {
        operationDao.delete(id)
    }

    suspend fun fail(id: String, attemptCount: Int, lastError: String) {
        require(attemptCount >= 1) { "Sync attempt count must be at least 1." }
        operationDao.markFailed(
            id = id,
            attemptCount = attemptCount,
            nextAttemptAtEpochMillis = clock() + backoffMillis(attemptCount),
            lastError = lastError.take(500),
        )
    }

    companion object {
        fun backoffMillis(attemptCount: Int): Long {
            require(attemptCount >= 1) { "Sync attempt count must be at least 1." }
            val exponent = (attemptCount - 1).coerceAtMost(5)
            return 1_000L * (1L shl exponent)
        }
    }
}
