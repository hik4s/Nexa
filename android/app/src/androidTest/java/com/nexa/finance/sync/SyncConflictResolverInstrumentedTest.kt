package com.nexa.finance.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncConflictResolverInstrumentedTest {
    private lateinit var database: NexaDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NexaDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun keepServerReplacesLocalTransactionAndClearsConflict() = runBlocking {
        database.localTransactionDao().upsert(
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
        database.syncOperationDao().upsert(
            SyncOperationEntity(
                id = OPERATION_ID,
                userId = USER_ID,
                entityType = "TRANSACTION",
                entityId = TRANSACTION_ID,
                operation = SyncWorker.OP_TRANSACTION_UPDATE,
                payloadJson = LOCAL_PAYLOAD,
                status = SyncOperationEntity.STATUS_CONFLICT,
                createdAtEpochMillis = 1_000L,
            ),
        )
        database.syncConflictDao().upsert(
            SyncConflictEntity(
                operationId = OPERATION_ID,
                userId = USER_ID,
                entityType = "TRANSACTION",
                entityId = TRANSACTION_ID,
                localPayloadJson = LOCAL_PAYLOAD,
                serverTransactionJson = SERVER_PAYLOAD,
                serverVersion = 2L,
                createdAtEpochMillis = 2_000L,
            ),
        )

        SyncConflictResolver(
            conflictDao = database.syncConflictDao(),
            operationDao = database.syncOperationDao(),
            transactionDao = database.localTransactionDao(),
            clock = { NOW },
            scheduleSync = {},
        ).keepServer(OPERATION_ID)

        val local = database.localTransactionDao().get(TRANSACTION_ID)
        assertEquals(2500L, local?.amountMinor)
        assertEquals("Estado do servidor", local?.description)
        assertEquals(2L, local?.version)
        assertNull(database.syncConflictDao().get(OPERATION_ID))
        assertEquals(0, database.syncOperationDao().count(USER_ID))
    }

    @Test
    fun resubmitMineRebasesOperationAndKeepsConflictUntilAck() = runBlocking {
        database.syncOperationDao().upsert(
            SyncOperationEntity(
                id = OPERATION_ID,
                userId = USER_ID,
                entityType = "TRANSACTION",
                entityId = TRANSACTION_ID,
                operation = SyncWorker.OP_TRANSACTION_UPDATE,
                payloadJson = LOCAL_PAYLOAD,
                status = SyncOperationEntity.STATUS_CONFLICT,
                createdAtEpochMillis = 1_000L,
                lastError = "CONFLICT",
            ),
        )
        database.syncConflictDao().upsert(
            SyncConflictEntity(
                operationId = OPERATION_ID,
                userId = USER_ID,
                entityType = "TRANSACTION",
                entityId = TRANSACTION_ID,
                localPayloadJson = LOCAL_PAYLOAD,
                serverTransactionJson = SERVER_PAYLOAD,
                serverVersion = 7L,
                createdAtEpochMillis = 2_000L,
            ),
        )

        var scheduled = false
        SyncConflictResolver(
            conflictDao = database.syncConflictDao(),
            operationDao = database.syncOperationDao(),
            transactionDao = database.localTransactionDao(),
            clock = { NOW },
            scheduleSync = { scheduled = true },
        ).resubmitMine(OPERATION_ID)

        val operation = database.syncOperationDao().pending(USER_ID, NOW, 10).single()
        assertEquals(SyncOperationEntity.STATUS_PENDING, operation.status)
        assertEquals(0, operation.attemptCount)
        assertEquals(NOW, operation.nextAttemptAtEpochMillis)
        assertEquals(TRANSACTION_ID, operation.payloadJson.substringAfter("\"id\":\"").substringBefore("\""))
        assertTrue(operation.payloadJson.contains("\"baseVersion\":7"))
        assertEquals(2700L, operation.payloadJson.substringAfter("\"amountMinor\":").substringBefore(",").toLong())
        assertTrue(scheduled)
        assertEquals(OPERATION_ID, database.syncConflictDao().get(OPERATION_ID)?.operationId)
    }

    companion object {
        private const val USER_ID = "user-1"
        private const val ACCOUNT_ID = "account-1"
        private const val TRANSACTION_ID = "tx-1"
        private const val OPERATION_ID = "operation-1"
        private const val NOW = 5_000L

        private const val LOCAL_PAYLOAD =
            "{\"id\":\"tx-1\",\"accountId\":\"account-1\",\"amountMinor\":2700,\"currency\":\"BRL\",\"type\":\"EXPENSE\",\"direction\":\"DEBIT\",\"status\":\"POSTED\",\"occurredOn\":\"2026-10-05\",\"description\":\"Meu lançamento\",\"source\":\"MANUAL\"}"

        private const val SERVER_PAYLOAD =
            "{\"id\":\"tx-1\",\"accountId\":\"account-1\",\"amountMinor\":2500,\"currency\":\"BRL\",\"type\":\"EXPENSE\",\"direction\":\"DEBIT\",\"status\":\"POSTED\",\"occurredOn\":\"2026-10-05\",\"description\":\"Estado do servidor\",\"source\":\"MANUAL\",\"version\":2}"
    }
}
