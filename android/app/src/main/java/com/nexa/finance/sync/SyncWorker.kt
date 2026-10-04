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
        val operations = queue.claimBatch(userId)
        if (operations.isEmpty()) return@withContext Result.success()

        var transientFailure = false
        for (operation in operations) {
            try {
                execute(operation, ApiClient().withToken(token))
                queue.complete(operation.id)
            } catch (error: Exception) {
                queue.fail(operation.id, operation.attemptCount + 1, error.message ?: error::class.simpleName.orEmpty())
                if (isTransient(error)) transientFailure = true
            }
        }
        if (transientFailure) Result.retry() else Result.success()
    }

    private fun execute(operation: SyncOperationEntity, api: ApiClient) {
        when (operation.operation) {
            OP_TRANSACTION_CREATE -> {
                val payload = JSONObject(operation.payloadJson)
                api.createTransaction(
                    accountId = payload.getString("accountId"),
                    amountMinor = payload.getLong("amountMinor"),
                    type = payload.getString("type"),
                    direction = payload.getString("direction"),
                    description = payload.optString("description").ifBlank { "Lançamento" },
                    categoryId = payload.optString("categoryId").ifBlank { null },
                    id = operation.entityId,
                    occurredOn = payload.getString("occurredOn"),
                )
            }
            else -> throw IllegalArgumentException("Unsupported sync operation: " + operation.operation)
        }
    }

    private fun isTransient(error: Exception): Boolean =
        error is IOException || error is java.net.SocketTimeoutException || error is java.net.ConnectException

    companion object {
        const val OP_TRANSACTION_CREATE = "TRANSACTION_CREATE"
    }
}
