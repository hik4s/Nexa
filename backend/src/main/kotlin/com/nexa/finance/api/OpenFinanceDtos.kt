package com.nexa.finance.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateOpenFinanceConsentRequest(
    val providerId: String,
    val scopes: Set<String>,
)

@Serializable
data class OpenFinanceConsentAuthorizationSessionRequest(
    val documentType: String,
    val documentNumber: String,
    val holderName: String,
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

@Serializable
data class CompleteOpenFinanceConsentAuthorizationRequest(
    val externalConsentId: String,
    val institutionId: String,
)

@Serializable
data class OpenFinanceConsentAuthorizationResponse(
    val externalConsentId: String?,
    val authorizationUrl: String?,
    val widgetAccessToken: String? = null,
    val sessionExpiresAt: String? = null,
)

@Serializable
data class OpenFinanceConsentSyncResponse(
    val id: String,
    val status: String,
    val expiresAt: String?,
)
