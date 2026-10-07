package com.nexa.finance.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateTransactionRequest(
    val id: String? = null,
    val accountId: String,
    val amountMinor: Long,
    val currency: String,
    val type: String,
    val direction: String,
    val occurredOn: String,
    val description: String? = null,
    val categoryId: String? = null,
    val source: String = "MANUAL",
    val baseVersion: Long? = null,
)

@Serializable
data class UpdateTransactionRequest(
    val baseVersion: Long,
    val accountId: String,
    val amountMinor: Long,
    val currency: String,
    val type: String,
    val direction: String,
    val occurredOn: String,
    val description: String? = null,
    val categoryId: String? = null,
    val source: String = "MANUAL",
)

@Serializable
data class TransactionResponse(
    val id: String,
    val accountId: String,
    val amountMinor: Long,
    val currency: String,
    val type: String,
    val direction: String,
    val status: String,
    val occurredOn: String,
    val description: String?,
    val categoryId: String?,
    val source: String,
    val installmentId: String? = null,
    val installmentNumber: Int? = null,
    val version: Long = 1,
)

@Serializable
data class TransactionStatusResponse(
    val transaction: TransactionResponse,
)
