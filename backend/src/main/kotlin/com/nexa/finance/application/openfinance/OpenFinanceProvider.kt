package com.nexa.finance.application.openfinance

import com.nexa.finance.domain.common.UserId
import java.time.Instant

interface OpenFinanceProvider {
    val providerId: String

    fun requestConsent(
        userId: UserId,
        scopes: Set<String>,
    ): ProviderConsentSession

    fun getConsentStatus(
        externalConsentId: String,
    ): ProviderConsentState

    fun revokeConsent(
        externalConsentId: String,
    )

    data class ProviderConsentSession(
        val externalConsentId: String,
        val authorizationUrl: String,
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
