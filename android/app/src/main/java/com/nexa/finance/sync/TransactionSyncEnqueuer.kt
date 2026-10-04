package com.nexa.finance.sync

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

object TransactionSyncEnqueuer {
    suspend fun enqueue(
        context: Context,
        userId: String,
        accountId: String,
        amountMinor: Long,
        type: String,
        direction: String,
        description: String,
        categoryId: String?,
        occurredOn: String = LocalDate.now().toString(),
        currency: String = "BRL",
        source: String = "MANUAL",
    ): String {
        val id = UUID.randomUUID().toString()
        val payload = JSONObject().apply {
            put("accountId", accountId)
            put("amountMinor", amountMinor)
            put("type", type)
            put("direction", direction)
            put("description", description)
            put("categoryId", categoryId ?: JSONObject.NULL)
            put("occurredOn", occurredOn)
            put("currency", currency)
            put("source", source)
        }
        val queue = SyncQueue(LocalDatabaseProvider.get(context).syncOperationDao())
        queue.enqueue(userId, "TRANSACTION", id, SyncWorker.OP_TRANSACTION_CREATE, payload.toString())
        SyncScheduler.schedule(context)
        return id
    }
}
