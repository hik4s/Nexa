package com.nexa.finance.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nexa.finance.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
                        "ACKNOWLEDGED" -> queue.complete(operation.id)
                        "CONFLICT" -> queue.conflict(operation.id, "Server detected a conflicting version of this transaction.")
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

    private fun isTransient(error: Exception): Boolean =
        error is IOException || error is java.net.SocketTimeoutException || error is java.net.ConnectException

    companion object { const val OP_TRANSACTION_CREATE = "TRANSACTION_CREATE" }
}
