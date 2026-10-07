package com.nexa.finance.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateTransferRequest(
    val sourceAccountId: String,
    val destinationAccountId: String,
    val amountMinor: Long,
    val currency: String,
    val occurredOn: String,
    val source: String = "MANUAL",
)

@Serializable
data class TransferResponse(
    val id: String,
    val sourceAccountId: String,
    val destinationAccountId: String,
    val amountMinor: Long,
    val currency: String,
    val occurredOn: String,
    val status: String,
    val sourceTransactionId: String,
    val destinationTransactionId: String,
)