package com.nexa.finance.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateOpenFinanceConsentRequest(
    val providerId: String,
    val scopes: Set<String>,
)

@Serializable
data class OpenFinanceConsentResponse(
    val id: String,
    val providerId: String,
    val externalConsentId: String?,
    val scopes: Set<String>,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val expiresAt: String?,
    val revokedAt: String?,
)
