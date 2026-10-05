package com.nexa.finance.sync

import org.json.JSONObject

interface SyncConflictPayloadCodec {
    fun parseTransaction(payloadJson: String): SyncTransaction
    fun summarizeLocal(payloadJson: String): Pair<String, Long?>
    fun rebasePayload(payloadJson: String, entityId: String, serverVersion: Long): String
}

object JsonSyncConflictPayloadCodec : SyncConflictPayloadCodec {
    override fun parseTransaction(payloadJson: String): SyncTransaction {
        val o = JSONObject(payloadJson)
        return SyncTransaction(
            id = o.getString("id"),
            accountId = o.getString("accountId"),
            amountMinor = o.getLong("amountMinor"),
            currency = o.getString("currency"),
            type = o.getString("type"),
            direction = o.getString("direction"),
            status = o.getString("status"),
            occurredOn = o.getString("occurredOn"),
            description = o.optString("description").ifBlank { null },
            categoryId = o.optString("categoryId").ifBlank { null },
            source = o.getString("source"),
            installmentId = o.optString("installmentId").ifBlank { null },
            installmentNumber = o.optInt("installmentNumber", 0).takeIf { it > 0 },
            version = o.optLong("version", 1L),
        )
    }

    override fun summarizeLocal(payloadJson: String): Pair<String, Long?> {
        val local = JSONObject(payloadJson)
        val description = local.optString("description").ifBlank { "Lançamento" }
        val amount = local.optLong("amountMinor", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
        return description to amount
    }

    override fun rebasePayload(payloadJson: String, entityId: String, serverVersion: Long): String =
        JSONObject(payloadJson).apply {
            put("id", entityId)
            put("baseVersion", serverVersion)
        }.toString()
}
