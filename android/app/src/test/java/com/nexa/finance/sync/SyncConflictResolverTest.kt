package com.nexa.finance.sync

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncConflictResolverTest {
    @Test
    fun keepServerReplacesLocalStateAndClearsConflict() = runBlocking {
        val conflicts = FakeConflictDao()
        val operations = FakeOperationDao(
            SyncOperationEntity(
                id = OPERATION_ID,
                userId = USER_ID,
                entityType = "TRANSACTION",
                entityId = TRANSACTION_ID,
                operation = SyncWorker.OP_TRANSACTION_UPDATE,
                payloadJson = """{"amountMinor":2700,"description":"Meu lançamento"}""",
                status = SyncOperationEntity.STATUS_CONFLICT,
                createdAtEpochMillis = 100L,
                lastError = "CONFLICT",
            ),
        )
        val transactions = FakeTransactionDao(
            LocalTransactionEntity(
                id = TRANSACTION_ID,
                accountId = ACCOUNT_ID,
                amountMinor = 2700L,
                currency = "BRL",
                type = "EXPENSE",
                direction = "DEBIT",
                status = "POSTED",
                occurredOn = "2026-10-05",
                description = "Meu lançamento",
                categoryId = null,
                source = "MANUAL",
                installmentId = null,
                installmentNumber = null,
                version = 1L,
            ),
        )
        val conflict = conflict(
            serverJson = """{"id":"$TRANSACTION_ID","accountId":"$ACCOUNT_ID","amountMinor":2600,"currency":"BRL","type":"EXPENSE","direction":"DEBIT","status":"POSTED","occurredOn":"2026-10-05","description":"Estado do servidor","source":"MANUAL","version":2}""",
        )
        conflicts.upsert(conflict)

        val resolver = resolver(conflicts, operations, transactions)

        resolver.keepServer(OPERATION_ID)

        assertEquals(2600L, transactions.rows[TRANSACTION_ID]?.amountMinor)
        assertEquals("Estado do servidor", transactions.rows[TRANSACTION_ID]?.description)
        assertEquals(2L, transactions.rows[TRANSACTION_ID]?.version)
        assertNull(operations.rows[OPERATION_ID])
        assertNull(conflicts.rows[OPERATION_ID])
    }

    @Test
    fun resubmitMineRebasesOperationAndSchedulesSync() = runBlocking {
        val conflicts = FakeConflictDao()
        val operations = FakeOperationDao(
            SyncOperationEntity(
                id = OPERATION_ID,
                userId = USER_ID,
                entityType = "TRANSACTION",
                entityId = TRANSACTION_ID,
                operation = SyncWorker.OP_TRANSACTION_UPDATE,
                payloadJson = """{"accountId":"$ACCOUNT_ID","amountMinor":2700,"description":"Meu lançamento"}""",
                status = SyncOperationEntity.STATUS_CONFLICT,
                createdAtEpochMillis = 100L,
            ),
        )
        val transactions = FakeTransactionDao()
        conflicts.upsert(
            conflict(
                serverVersion = 7L,
                serverJson = """{"id":"$TRANSACTION_ID","accountId":"$ACCOUNT_ID","amountMinor":2600,"currency":"BRL","type":"EXPENSE","direction":"DEBIT","status":"POSTED","occurredOn":"2026-10-05","description":"Servidor","source":"MANUAL","version":7}""",
            ),
        )

        var scheduled = false
        val resolver = resolver(conflicts, operations, transactions) {
            scheduled = true
        }

        resolver.resubmitMine(OPERATION_ID)

        val operation = operations.rows[OPERATION_ID] ?: error("Operation not found")
        val payload = JSONObject(operation.payloadJson)
        assertEquals(SyncOperationEntity.STATUS_PENDING, operation.status)
        assertEquals(0, operation.attemptCount)
        assertEquals(5_000L, operation.nextAttemptAtEpochMillis)
        assertEquals(TRANSACTION_ID, payload.getString("id"))
        assertEquals(7L, payload.getLong("baseVersion"))
        assertEquals(2700L, payload.getLong("amountMinor"))
        assertTrue(scheduled)
        assertTrue(conflicts.rows.containsKey(OPERATION_ID))
    }

    @Test
    fun pendingSummariesExposeLocalAndServerValues() = runBlocking {
        val conflicts = FakeConflictDao()
        val operations = FakeOperationDao()
        val transactions = FakeTransactionDao()
        conflicts.upsert(
            conflict(
                localJson = """{"amountMinor":2700,"description":"Meu lançamento"}""",
                serverVersion = 3L,
                serverJson = """{"id":"$TRANSACTION_ID","accountId":"$ACCOUNT_ID","amountMinor":2600,"currency":"BRL","type":"EXPENSE","direction":"DEBIT","status":"POSTED","occurredOn":"2026-10-05","description":"Servidor","source":"MANUAL","version":3}""",
            ),
        )

        val summaries = resolver(conflicts, operations, transactions).pending(USER_ID)

        assertEquals(1, summaries.size)
        assertEquals(2700L, summaries.single().localAmountMinor)
        assertEquals("Meu lançamento", summaries.single().localDescription)
        assertEquals(2600L, summaries.single().serverAmountMinor)
        assertEquals("Servidor", summaries.single().serverDescription)
        assertEquals(3L, summaries.single().serverVersion)
    }

    private fun resolver(
        conflicts: FakeConflictDao,
        operations: FakeOperationDao,
        transactions: FakeTransactionDao,
        scheduleSync: () -> Unit = {},
    ) = SyncConflictResolver(
        conflictDao = conflicts,
        operationDao = operations,
        transactionDao = transactions,
        clock = { 5_000L },
        scheduleSync = scheduleSync,
    )

    private fun conflict(
        localJson: String = """{"amountMinor":2700,"description":"Meu lançamento"}""",
        serverVersion: Long? = 2L,
        serverJson: String? = null,
    ) = SyncConflictEntity(
        operationId = OPERATION_ID,
        userId = USER_ID,
        entityType = "TRANSACTION",
        entityId = TRANSACTION_ID,
        localPayloadJson = localJson,
        serverTransactionJson = serverJson,
        serverVersion = serverVersion,
        createdAtEpochMillis = 2_000L,
    )

    private class FakeConflictDao : SyncConflictDao {
        val rows = linkedMapOf<String, SyncConflictEntity>()

        override suspend fun upsert(conflict: SyncConflictEntity) {
            rows[conflict.operationId] = conflict
        }

        override suspend fun pending(userId: String): List<SyncConflictEntity> =
            rows.values.filter { it.userId == userId }.sortedBy { it.createdAtEpochMillis }

        override suspend fun get(operationId: String): SyncConflictEntity? = rows[operationId]

        override suspend fun count(userId: String): Int = rows.values.count { it.userId == userId }

        override suspend fun delete(operationId: String) {
            rows.remove(operationId)
        }

        override suspend fun deleteForUser(userId: String) {
            rows.values.removeIf { it.userId == userId }
        }
    }

    private class FakeOperationDao(
        initial: SyncOperationEntity? = null,
    ) : SyncOperationDao {
        val rows = linkedMapOf<String, SyncOperationEntity>()

        init {
            initial?.let { rows[it.id] = it }
        }

        override suspend fun upsert(operation: SyncOperationEntity) {
            rows[operation.id] = operation
        }

        override suspend fun pending(userId: String, nowEpochMillis: Long, limit: Int): List<SyncOperationEntity> =
            rows.values.filter {
                it.userId == userId &&
                    it.status in setOf(SyncOperationEntity.STATUS_PENDING, SyncOperationEntity.STATUS_FAILED) &&
                    it.nextAttemptAtEpochMillis <= nowEpochMillis
            }.take(limit)

        override suspend fun updateStatus(id: String, status: String) {
            rows[id] = rows.getValue(id).copy(status = status)
        }

        override suspend fun updateLastError(id: String, lastError: String?) {
            rows[id] = rows.getValue(id).copy(lastError = lastError)
        }

        override suspend fun recoverStaleProcessing(nowEpochMillis: Long, cutoffEpochMillis: Long) = Unit

        override suspend fun markFailed(id: String, attemptCount: Int, nextAttemptAtEpochMillis: Long, lastError: String) {
            rows[id] = rows.getValue(id).copy(
                status = SyncOperationEntity.STATUS_FAILED,
                attemptCount = attemptCount,
                nextAttemptAtEpochMillis = nextAttemptAtEpochMillis,
                lastError = lastError,
            )
        }

        override suspend fun requeueConflict(id: String, payloadJson: String, nowEpochMillis: Long): Int {
            val current = rows[id] ?: return 0
            if (current.status != SyncOperationEntity.STATUS_CONFLICT) return 0
            rows[id] = current.copy(
                payloadJson = payloadJson,
                status = SyncOperationEntity.STATUS_PENDING,
                attemptCount = 0,
                nextAttemptAtEpochMillis = nowEpochMillis,
                lastError = null,
            )
            return 1
        }

        override suspend fun delete(id: String) {
            rows.remove(id)
        }

        override suspend fun count(userId: String): Int = rows.values.count { it.userId == userId }
    }

    private class FakeTransactionDao(
        initial: LocalTransactionEntity? = null,
    ) : LocalTransactionDao {
        val rows = linkedMapOf<String, LocalTransactionEntity>()

        init {
            initial?.let { rows[it.id] = it }
        }

        override suspend fun upsert(transaction: LocalTransactionEntity) {
            rows[transaction.id] = transaction
        }

        override suspend fun get(id: String): LocalTransactionEntity? = rows[id]

        override suspend fun listByAccount(accountId: String): List<LocalTransactionEntity> =
            rows.values.filter { it.accountId == accountId }
    }

    companion object {
        private const val USER_ID = "user-1"
        private const val ACCOUNT_ID = "account-1"
        private const val TRANSACTION_ID = "tx-1"
        private const val OPERATION_ID = "operation-1"
    }
}
