package com.nexa.finance.application.openfinance

import com.nexa.finance.application.audit.AuditEventRecorder
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.openfinance.OpenFinanceConsent
import com.nexa.finance.domain.openfinance.OpenFinanceConsentId
import com.nexa.finance.infrastructure.resilience.RetryPolicy
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.time.Clock
import java.time.Instant
import java.util.concurrent.TimeoutException

class OpenFinanceConsentService(
    private val repository: OpenFinanceConsentRepository,
    private val providers: OpenFinanceProviderRegistry,
    private val clock: Clock = Clock.systemUTC(),
    private val audit: AuditEventRecorder? = null,
    private val retryPolicy: RetryPolicy = RetryPolicy(com.nexa.finance.RetrySettings.fromEnvironment()),
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
        audit?.record(
            actorUserId = userId,
            eventType = "OPEN_FINANCE_CONSENT_CREATED",
            targetType = "OPEN_FINANCE_CONSENT",
            targetId = consent.id,
            metadata = "{\"provider\":\"" + consent.providerId + "\",\"scopeCount\":" + consent.scopes.size + "}",
        )
        return consent
    }

    fun beginAuthorization(
        userId: UserId,
        consentId: OpenFinanceConsentId,
    ): OpenFinanceProvider.ProviderConsentSession {
        val consent = get(userId, consentId)
        val provider = providers.get(consent.providerId)

        // Do not retry consent creation automatically: providers may create a new external
        // consent on each call unless they explicitly guarantee idempotency.
        val session = provider.requestConsent(userId, consent.scopes)
        val updated = consent.markAwaitingAuthorization(
            externalConsentId = session.externalConsentId,
            now = Instant.now(clock),
        )
        repository.update(updated)
        audit?.record(
            actorUserId = userId,
            eventType = "OPEN_FINANCE_AUTHORIZATION_STARTED",
            targetType = "OPEN_FINANCE_CONSENT",
            targetId = consent.id,
            metadata = "{\"provider\":\"" + consent.providerId + "\"}",
        )
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
        val providerStatus = retryPolicy.execute(
            retryable = ::isRetryableProviderFailure,
        ) {
            provider.getConsentStatus(externalId)
        }
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
        val persisted = repository.update(updated)
            ?: throw DomainValidationException("Open Finance consent was not found.")
        audit?.record(
            actorUserId = userId,
            eventType = "OPEN_FINANCE_CONSENT_STATUS_CHANGED",
            targetType = "OPEN_FINANCE_CONSENT",
            targetId = consent.id,
            metadata = "{\"status\":\"" + persisted.status.name + "\"}",
        )
        return persisted
    }

    fun revoke(
        userId: UserId,
        consentId: OpenFinanceConsentId,
    ): OpenFinanceConsent {
        val consent = get(userId, consentId)
        consent.externalConsentId?.let { externalId ->
            retryPolicy.execute(
                retryable = ::isRetryableProviderFailure,
            ) {
                providers.get(consent.providerId).revokeConsent(externalId)
            }
        }
        val updated = consent.revoke(Instant.now(clock))
        val persisted = repository.update(updated)
            ?: throw DomainValidationException("Open Finance consent was not found.")
        audit?.record(
            actorUserId = userId,
            eventType = "OPEN_FINANCE_CONSENT_REVOKED",
            targetType = "OPEN_FINANCE_CONSENT",
            targetId = consent.id,
        )
        return persisted
    }

    fun list(userId: UserId): List<OpenFinanceConsent> = repository.findByOwner(userId)

    private fun get(userId: UserId, consentId: OpenFinanceConsentId): OpenFinanceConsent =
        repository.findByOwnerAndId(userId, consentId)
            ?: throw DomainValidationException("Open Finance consent was not found.")

    private fun isRetryableProviderFailure(error: Throwable): Boolean =
        error is IOException ||
            error is ConnectException ||
            error is SocketTimeoutException ||
            error is TimeoutException
}
