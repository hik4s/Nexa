package com.nexa.finance.api

import kotlinx.serialization.Serializable

@Serializable
data class SyncPushRequest(
    val operations: List<SyncPushOperation>,
)

@Serializable
data class SyncPushOperation(
    val operationId: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payload: CreateTransactionRequest? = null,
)

@Serializable
data class SyncPushResponse(
    val acknowledgements: List<SyncAcknowledgement>,
    val cursor: Long,
)

@Serializable
data class SyncAcknowledgement(
    val operationId: String,
    val entityType: String,
    val entityId: String,
    val status: String,
)

@Serializable
data class SyncPullResponse(
    val cursor: Long,
    val changes: List<SyncChangeResponse>,
)

@Serializable
data class SyncChangeResponse(
    val cursor: Long,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val transaction: TransactionResponse? = null,
)
