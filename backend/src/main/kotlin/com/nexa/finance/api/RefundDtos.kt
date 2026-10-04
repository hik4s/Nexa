package com.nexa.finance.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateRefundRequest(
    val amountMinor: Long,
    val currency: String,
    val occurredOn: String,
    val description: String? = null,
)

@Serializable
data class CreateCancellationRequest(
    val occurredOn: String,
    val description: String? = null,
)
