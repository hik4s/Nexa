package com.nexa.finance.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateRecurrenceRequest(
    val accountId: String,
    val amountMinor: Long,
    val currency: String,
    val transactionType: String,
    val frequency: String,
    val interval: Int = 1,
    val startOn: String,
    val endOn: String? = null,
    val maxOccurrences: Int? = null,
    val description: String? = null,
    val categoryId: String? = null,
)

@Serializable
data class RecurrenceResponse(
    val id: String,
    val accountId: String,
    val amountMinor: Long,
    val currency: String,
    val transactionType: String,
    val frequency: String,
    val interval: Int,
    val startOn: String,
    val endOn: String?,
    val maxOccurrences: Int?,
    val description: String?,
    val categoryId: String?,
    val status: String,
)

@Serializable
data class RecurrenceOccurrenceResponse(val number: Int, val scheduledOn: String)

@Serializable
data class RecurrenceOccurrencesResponse(val occurrences: List<RecurrenceOccurrenceResponse>)
