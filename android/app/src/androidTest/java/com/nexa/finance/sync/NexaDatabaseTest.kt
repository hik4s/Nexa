package com.nexa.finance.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NexaDatabaseTest {
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
    fun persistsSyncOperationAndCheckpoint() = runBlocking {
        val now = TimeUnit.SECONDS.toMillis(1)
        val operation = SyncOperationEntity(
            id = "operation-1",
            userId = "user-1",
            entityType = "TRANSACTION",
            entityId = "tx-1",
            operation = "UPSERT",
            payloadJson = """{"id":"tx-1"}""",
            createdAtEpochMillis = now,
        )
        database.syncOperationDao().upsert(operation)
        database.syncCheckpointDao().upsert(
            SyncCheckpointEntity(
                userId = "user-1",
                serverCursor = "cursor-1",
                lastSyncedAtEpochMillis = now,
            ),
        )

        val pending = database.syncOperationDao().pending("user-1", now, 20)
        val checkpoint = database.syncCheckpointDao().get("user-1")

        assertEquals(listOf(operation), pending)
        assertNotNull(checkpoint)
        assertEquals("cursor-1", checkpoint?.serverCursor)


    @Test
    fun persistsAndClearsSyncConflict() = runBlocking {
        val conflict = SyncConflictEntity(
            operationId = "operation-conflict-1",
            userId = "user-1",
            entityType = "TRANSACTION",
            entityId = "tx-1",
            localPayloadJson = """{"id":"tx-1","amountMinor":2600}""",
            serverTransactionJson = """{"id":"tx-1","amountMinor":2500,"version":2}""",
            serverVersion = 2L,
            createdAtEpochMillis = 2_000L,
        )

        database.syncConflictDao().upsert(conflict)

        val pending = database.syncConflictDao().pending("user-1")

        assertEquals(listOf(conflict), pending)
        assertEquals(1, database.syncConflictDao().count("user-1"))

        database.syncConflictDao().delete(conflict.operationId)

        assertEquals(0, database.syncConflictDao().count("user-1"))
    }
    }
}
