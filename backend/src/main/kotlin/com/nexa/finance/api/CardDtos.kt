package com.nexa.finance.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateCardRequest(
    val liabilityAccountId: String,
    val name: String,
    val brand: String,
    val lastFourDigits: String,
    val creditLimitMinor: Long,
    val currency: String,
    val closingDay: Int,
    val dueDay: Int,
)

@Serializable
data class CardPurchaseRequest(
    val amountMinor: Long,
    val currency: String,
    val occurredOn: String,
    val description: String? = null,
    val source: String = "MANUAL",
    val post: Boolean = true,
)

@Serializable
data class CardStatusRequest(val status: String)

@Serializable
data class CardResponse(
    val id: String,
    val liabilityAccountId: String,
    val name: String,
    val brand: String,
    val lastFourDigits: String,
    val creditLimitMinor: Long,
    val availableLimitMinor: Long,
    val currency: String,
    val closingDay: Int,
    val dueDay: Int,
    val status: String,
)