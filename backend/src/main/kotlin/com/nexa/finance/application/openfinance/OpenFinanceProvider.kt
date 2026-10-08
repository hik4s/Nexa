package com.nexa.finance.application.openfinance

import com.nexa.finance.domain.common.UserId
import java.time.Instant

interface OpenFinanceProvider {
    val providerId: String

    /**
     * Legacy provider contract kept for adapters that can return a provider-native authorization URL
     * and external consent identifier in one operation.
     */
    fun requestConsent(
        userId: UserId,
        scopes: Set<String>,
    ): ProviderConsentSession

    /**
     * Provider-neutral authorization-session contract.
     *
     * Adapters such as Hosted Widget providers may not know the final external consent identifier
     * when the session is created. In that case the identifier is resolved by the provider callback
     * and the returned session only contains the short-lived launch material.
     */
    fun requestAuthorizationSession(
        userId: UserId,
        scopes: Set<String>,
        context: ProviderAuthorizationContext,
    ): ProviderConsentSession = requestConsent(userId, scopes)

    fun getConsentStatus(
        externalConsentId: String,
    ): ProviderConsentState

    fun revokeConsent(
        externalConsentId: String,
    )

    data class ProviderAuthorizationContext(
        val externalId: String,
        val documentType: String? = null,
        val documentNumber: String? = null,
        val holderName: String? = null,
    )

    data class ProviderConsentSession(
        val externalConsentId: String?,
        val authorizationUrl: String?,
        val widgetAccessToken: String? = null,
    )

    data class ProviderConsentState(
        val status: ExternalConsentStatus,
        val expiresAt: Instant?,
    )

    enum class ExternalConsentStatus {
        AWAITING_AUTHORIZATION,
        ACTIVE,
        EXPIRED,
        REVOKED,
        FAILED,
    }
}

interface OpenFinanceProviderRegistry {
    fun get(providerId: String): OpenFinanceProvider
}
