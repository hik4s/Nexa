package com.nexa.finance.sync

data class SyncPushAck(val operationId: String, val entityId: String, val status: String)
data class SyncPushResult(val acknowledgements: List<SyncPushAck>, val cursor: Long)
data class SyncTransaction(val id: String, val accountId: String, val amountMinor: Long, val currency: String, val type: String, val direction: String, val status: String, val occurredOn: String, val description: String?, val categoryId: String?, val source: String, val installmentId: String?, val installmentNumber: Int?)
data class SyncPullChange(val cursor: Long, val entityId: String, val operation: String, val transaction: SyncTransaction?)
data class SyncPullResult(val cursor: Long, val changes: List<SyncPullChange>)
