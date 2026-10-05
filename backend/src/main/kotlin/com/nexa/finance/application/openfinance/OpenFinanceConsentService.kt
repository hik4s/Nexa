package com.nexa.finance.application.openfinance

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.openfinance.OpenFinanceConsent
import com.nexa.finance.domain.openfinance.OpenFinanceConsentId
import com.nexa.finance.infrastructure.openfinance.JdbcOpenFinanceConsentRepository
import java.time.Clock
import java.time.Instant

class OpenFinanceConsentService(
    private val repository: JdbcOpenFinanceConsentRepository,
    private val providers: OpenFinanceProviderRegistry,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun request(
        userId: UserId,
        providerId: String,
        scopes: Set<String>,
    ): OpenFinanceConsent {
        val provider = providers.get(providerId)
        val consent = OpenFinanceConsent.createRequested(
            userId = userId,
            providerId = provider.providerId,
            scopes = scopes,
            clock = clock,
        )
        repository.create(consent)
        return consent
    }

    fun beginAuthorization(
        userId: UserId,
        consentId: OpenFinanceConsentId,
    ): OpenFinanceProvider.ProviderConsentSession {
        val consent = get(userId, consentId)
        val provider = providers.get(consent.providerId)
        val session = provider.requestConsent(userId, consent.scopes)
        val updated = consent.markAwaitingAuthorization(
            externalConsentId = session.externalConsentId,
            now = Instant.now(clock),
        )
        repository.update(updated)
        return session
    }

    fun sync(
        userId: UserId,
        consentId: OpenFinanceConsentId,
    ): OpenFinanceConsent {
        val consent = get(userId, consentId)
        val externalId = consent.externalConsentId
            ?: throw DomainValidationException("Open Finance consent has no external provider ID.")
        val provider = providers.get(consent.providerId)
        val providerStatus = provider.getConsentStatus(externalId)
        val now = Instant.now(clock)

        val updated = when (providerStatus.status) {
            OpenFinanceProvider.ExternalConsentStatus.AWAITING_AUTHORIZATION ->
                if (consent.status == com.nexa.finance.domain.openfinance.OpenFinanceConsentStatus.AWAITING_AUTHORIZATION) {
                    consent
                } else {
                    consent.markAwaitingAuthorization(externalId, now)
                }
            OpenFinanceProvider.ExternalConsentStatus.ACTIVE ->
                consent.activate(providerStatus.expiresAt, now)
            OpenFinanceProvider.ExternalConsentStatus.EXPIRED ->
                consent.expire(now)
            OpenFinanceProvider.ExternalConsentStatus.REVOKED ->
                consent.revoke(now)
            OpenFinanceProvider.ExternalConsentStatus.FAILED ->
                consent.fail(now)
        }
        return repository.update(updated)
            ?: throw DomainValidationException("Open Finance consent was not found.")
    }

    fun revoke(
        userId: UserId,
        consentId: OpenFinanceConsentId,
    ): OpenFinanceConsent {
        val consent = get(userId, consentId)
        consent.externalConsentId?.let { providers.get(consent.providerId).revokeConsent(it) }
        val updated = consent.revoke(Instant.now(clock))
        return repository.update(updated)
            ?: throw DomainValidationException("Open Finance consent was not found.")
    }

    fun list(userId: UserId): List<OpenFinanceConsent> = repository.findByOwner(userId)

    private fun get(userId: UserId, consentId: OpenFinanceConsentId): OpenFinanceConsent =
        repository.findByOwnerAndId(userId, consentId)
            ?: throw DomainValidationException("Open Finance consent was not found.")
}
