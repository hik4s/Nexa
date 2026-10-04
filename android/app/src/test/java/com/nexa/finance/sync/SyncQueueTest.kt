package com.nexa.finance.sync

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncQueueTest {
    @Test
    fun enqueueClaimCompleteLifecycle() = runBlocking {
        val dao = FakeSyncOperationDao()
        val queue = SyncQueue(
            operationDao = dao,
            clock = { 1_000L },
            idFactory = { "operation-1" },
        )

        val id = queue.enqueue(
            userId = "user-1",
            entityType = "TRANSACTION",
            entityId = "tx-1",
            operation = "UPSERT",
            payloadJson = """{"id":"tx-1"}""",
        )

        assertEquals("operation-1", id)
        assertEquals(1, dao.count("user-1"))

        val claimed = queue.claimBatch("user-1")
        assertEquals(1, claimed.size)
        assertEquals(SyncOperationEntity.STATUS_PROCESSING, claimed.single().status)
        assertEquals(SyncOperationEntity.STATUS_PROCESSING, dao.items.single().status)

        queue.complete(id)
        assertEquals(0, dao.count("user-1"))
    }

    @Test
    fun failedOperationUsesCappedExponentialBackoff() = runBlocking {
        val dao = FakeSyncOperationDao()
        val queue = SyncQueue(dao, clock = { 10_000L }, idFactory = { "operation-2" })

        queue.enqueue("user-1", "GOAL", "goal-1", "UPDATE", "{}")
        queue.fail("operation-2", attemptCount = 4, lastError = "network timeout")

        val failed = dao.items.single()
        assertEquals(SyncOperationEntity.STATUS_FAILED, failed.status)
        assertEquals(4, failed.attemptCount)
        assertEquals(18_000L, failed.nextAttemptAtEpochMillis)
        assertEquals("network timeout", failed.lastError)
    }

    @Test
    fun backoffCapsAtThirtyTwoSeconds() {
        assertEquals(1_000L, SyncQueue.backoffMillis(1))
        assertEquals(2_000L, SyncQueue.backoffMillis(2))
        assertEquals(4_000L, SyncQueue.backoffMillis(3))
        assertEquals(32_000L, SyncQueue.backoffMillis(6))
        assertEquals(32_000L, SyncQueue.backoffMillis(100))
    }

    @Test
    fun invalidBatchLimitIsRejected() = runBlocking {
        val queue = SyncQueue(FakeSyncOperationDao())
        val exception = runCatching { queue.claimBatch("user-1", 0) }.exceptionOrNull()

        assertTrue(exception is IllegalArgumentException)
    }

    private class FakeSyncOperationDao : SyncOperationDao {
        val items = mutableListOf<SyncOperationEntity>()

        override suspend fun upsert(operation: SyncOperationEntity) {
            items.removeAll { it.id == operation.id }
            items += operation
        }

        override suspend fun pending(
            userId: String,
            nowEpochMillis: Long,
            limit: Int,
        ): List<SyncOperationEntity> =
            items
                .filter {
                    it.userId == userId &&
                        it.status in setOf(SyncOperationEntity.STATUS_PENDING, SyncOperationEntity.STATUS_FAILED) &&
                        it.nextAttemptAtEpochMillis <= nowEpochMillis
                }
                .sortedBy(SyncOperationEntity::createdAtEpochMillis)
                .take(limit)

        override suspend fun updateStatus(id: String, status: String) {
            val index = items.indexOfFirst { it.id == id }
            items[index] = items[index].copy(status = status)
        }

        override suspend fun markFailed(
            id: String,
            attemptCount: Int,
            nextAttemptAtEpochMillis: Long,
            lastError: String,
        ) {
            val index = items.indexOfFirst { it.id == id }
            items[index] = items[index].copy(
                status = SyncOperationEntity.STATUS_FAILED,
                attemptCount = attemptCount,
                nextAttemptAtEpochMillis = nextAttemptAtEpochMillis,
                lastError = lastError,
            )
        }

        override suspend fun delete(id: String) {
            items.removeAll { it.id == id }
        }

        override suspend fun count(userId: String): Int =
            items.count { it.userId == userId }
    }
}
