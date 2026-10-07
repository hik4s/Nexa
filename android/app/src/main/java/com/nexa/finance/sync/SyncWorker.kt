package com.nexa.finance.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nexa.finance.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

class SyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sessionStore = SessionStore(applicationContext)
        val userId = sessionStore.userId() ?: return@withContext Result.success()
        val token = sessionStore.token() ?: return@withContext Result.success()
        val database = LocalDatabaseProvider.get(applicationContext)
        val queue = SyncQueue(database.syncOperationDao())
        val api = ApiClient().withToken(token)
        try {
            val operations = queue.claimBatch(userId)
            if (operations.isNotEmpty()) {
                val push = api.pushSync(operations)
                val acknowledged = push.acknowledgements.associateBy { it.operationId }
                operations.forEach { operation ->
                    when (acknowledged[operation.id]?.status) {
                        "ACKNOWLEDGED" -> {
                            queue.complete(operation.id)
                            database.syncConflictDao().delete(operation.id)
                        }
                        "CONFLICT" -> {
                            queue.conflict(operation.id, "Server detected a conflicting version of this transaction.")
                            database.syncConflictDao().upsert(
                                SyncConflictEntity(
                                    operationId = operation.id,
                                    userId = userId,
                                    entityType = operation.entityType,
                                    entityId = operation.entityId,
                                    localPayloadJson = operation.payloadJson,
                                    serverTransactionJson = acknowledged[operation.id]?.serverTransaction?.let(::syncTransactionToJson),
                                    serverVersion = acknowledged[operation.id]?.version,
                                    createdAtEpochMillis = System.currentTimeMillis(),
                                ),
                            )
                        }
                        else -> queue.fail(operation.id, operation.attemptCount + 1, "Server did not acknowledge operation.")
                    }
                }
            }

            var cursor = database.syncCheckpointDao().get(userId)?.serverCursor?.toLongOrNull() ?: 0L
            var page: SyncPullResult
            do {
                page = api.pullSync(cursor)
                page.changes.forEach { change ->
                    if (change.operation == "UPSERT" && change.transaction != null) {
                        val tx = change.transaction
                        database.localTransactionDao().upsert(
                            LocalTransactionEntity(
                                id = tx.id,
                                accountId = tx.accountId,
                                amountMinor = tx.amountMinor,
                                currency = tx.currency,
                                type = tx.type,
                                direction = tx.direction,
                                status = tx.status,
                                occurredOn = tx.occurredOn,
                                description = tx.description,
                                categoryId = tx.categoryId,
                                source = tx.source,
                                installmentId = tx.installmentId,
                                installmentNumber = tx.installmentNumber,
                                version = tx.version,
                            ),
                        )
                    }
                }
                if (page.cursor > cursor) {
                    cursor = page.cursor
                    database.syncCheckpointDao().upsert(SyncCheckpointEntity(userId, cursor.toString(), System.currentTimeMillis()))
                }
            } while (page.changes.size >= 50)
            Result.success()
        } catch (error: Exception) {
            if (isTransient(error)) Result.retry() else throw error
        }
    }

    private fun syncTransactionToJson(transaction: SyncTransaction): String =
        JSONObject().apply {
            put("id", transaction.id)
            put("accountId", transaction.accountId)
            put("amountMinor", transaction.amountMinor)
            put("currency", transaction.currency)
            put("type", transaction.type)
            put("direction", transaction.direction)
            put("status", transaction.status)
            put("occurredOn", transaction.occurredOn)
            put("description", transaction.description ?: JSONObject.NULL)
            put("categoryId", transaction.categoryId ?: JSONObject.NULL)
            put("source", transaction.source)
            put("installmentId", transaction.installmentId ?: JSONObject.NULL)
            put("installmentNumber", transaction.installmentNumber ?: JSONObject.NULL)
            put("version", transaction.version)
        }.toString()

    private fun isTransient(error: Exception): Boolean =
        error is IOException || error is java.net.SocketTimeoutException || error is java.net.ConnectException

    companion object {
        const val OP_TRANSACTION_CREATE = "TRANSACTION_CREATE"
        const val OP_TRANSACTION_UPDATE = "TRANSACTION_UPDATE"
    }
}
