package com.nexa.finance.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateAccountRequest(
    val name: String,
    val type: String,
    val openingBalanceMinor: Long,
    val currency: String,
)

@Serializable
data class AccountResponse(
    val id: String,
    val name: String,
    val type: String,
    val currency: String,
    val openingBalanceMinor: Long,
    val balanceMinor: Long,
    val status: String,
    val isPrimary: Boolean,
)
