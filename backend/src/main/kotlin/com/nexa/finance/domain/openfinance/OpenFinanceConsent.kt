package com.nexa.finance.domain.openfinance

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import java.time.Clock
import java.time.Instant
import java.util.UUID

typealias OpenFinanceConsentId = UUID

enum class OpenFinanceConsentStatus {
    REQUESTED,
    AWAITING_AUTHORIZATION,
    ACTIVE,
    EXPIRED,
    REVOKED,
    FAILED,
}

data class OpenFinanceConsent private constructor(
    val id: OpenFinanceConsentId,
    val userId: UserId,
    val providerId: String,
    val externalConsentId: String?,
    val scopes: Set<String>,
    val status: OpenFinanceConsentStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val expiresAt: Instant?,
    val revokedAt: Instant?,
) {
    fun markAwaitingAuthorization(
        externalConsentId: String,
        now: Instant,
    ): OpenFinanceConsent {
        requireTransition(
            allowed = setOf(OpenFinanceConsentStatus.REQUESTED, OpenFinanceConsentStatus.FAILED),
            target = OpenFinanceConsentStatus.AWAITING_AUTHORIZATION,
        )
        return copy(
            externalConsentId = normalizeExternalId(externalConsentId),
            status = OpenFinanceConsentStatus.AWAITING_AUTHORIZATION,
            updatedAt = now,
        )
    }

    fun activate(
        expiresAt: Instant?,
        now: Instant,
    ): OpenFinanceConsent {
        requireTransition(
            allowed = setOf(OpenFinanceConsentStatus.AWAITING_AUTHORIZATION),
            target = OpenFinanceConsentStatus.ACTIVE,
        )
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw DomainValidationException("Open Finance consent expiration must be in the future.")
        }
        return copy(
            status = OpenFinanceConsentStatus.ACTIVE,
            updatedAt = now,
            expiresAt = expiresAt,
            revokedAt = null,
        )
    }

    fun expire(now: Instant): OpenFinanceConsent {
        if (status != OpenFinanceConsentStatus.ACTIVE) {
            throw DomainValidationException("Only active Open Finance consents can expire.")
        }
        return copy(
            status = OpenFinanceConsentStatus.EXPIRED,
            updatedAt = now,
        )
    }

    fun revoke(now: Instant): OpenFinanceConsent {
        if (status in setOf(OpenFinanceConsentStatus.REVOKED, OpenFinanceConsentStatus.EXPIRED)) {
            return this
        }
        if (status == OpenFinanceConsentStatus.REQUESTED || status == OpenFinanceConsentStatus.AWAITING_AUTHORIZATION || status == OpenFinanceConsentStatus.ACTIVE) {
            return copy(
                status = OpenFinanceConsentStatus.REVOKED,
                updatedAt = now,
                revokedAt = now,
            )
        }
        throw DomainValidationException("Failed Open Finance consents cannot be revoked.")
    }

    fun fail(now: Instant): OpenFinanceConsent {
        if (status !in setOf(OpenFinanceConsentStatus.REQUESTED, OpenFinanceConsentStatus.AWAITING_AUTHORIZATION)) {
            throw DomainValidationException("Only pending Open Finance consents can fail.")
        }
        return copy(
            status = OpenFinanceConsentStatus.FAILED,
            updatedAt = now,
        )
    }

    fun isUsable(at: Instant): Boolean =
        status == OpenFinanceConsentStatus.ACTIVE &&
            (expiresAt == null || expiresAt.isAfter(at))

    private fun requireTransition(
        allowed: Set<OpenFinanceConsentStatus>,
        target: OpenFinanceConsentStatus,
    ) {
        if (status !in allowed) {
            throw DomainValidationException("Cannot change Open Finance consent from $status to $target.")
        }
    }

    companion object {
        private val ALLOWED_SCOPES = setOf(
            "IDENTITY",
            "ACCOUNTS",
            "BALANCES",
            "TRANSACTIONS",
            "CREDIT_CARDS",
            "INVESTMENTS",
        )

        fun createRequested(
            userId: UserId,
            providerId: String,
            scopes: Set<String>,
            clock: Clock = Clock.systemUTC(),
            id: OpenFinanceConsentId = UUID.randomUUID(),
        ): OpenFinanceConsent {
            val normalizedProvider = providerId.trim().lowercase()
            if (normalizedProvider.isBlank() || normalizedProvider.length > 80) {
                throw DomainValidationException("Open Finance provider ID is invalid.")
            }
            val normalizedScopes = scopes
                .map(String::trim)
                .filter(String::isNotBlank)
                .map(String::uppercase)
                .toSet()
            if (normalizedScopes.isEmpty()) {
                throw DomainValidationException("Open Finance consent requires at least one scope.")
            }
            val unsupported = normalizedScopes - ALLOWED_SCOPES
            if (unsupported.isNotEmpty()) {
                throw DomainValidationException("Unsupported Open Finance consent scope.")
            }

            val now = Instant.now(clock)
            return OpenFinanceConsent(
                id = id,
                userId = userId,
                providerId = normalizedProvider,
                externalConsentId = null,
                scopes = normalizedScopes,
                status = OpenFinanceConsentStatus.REQUESTED,
                createdAt = now,
                updatedAt = now,
                expiresAt = null,
                revokedAt = null,
            )
        }

        fun restore(
            id: OpenFinanceConsentId,
            userId: UserId,
            providerId: String,
            externalConsentId: String?,
            scopes: Set<String>,
            status: OpenFinanceConsentStatus,
            createdAt: Instant,
            updatedAt: Instant,
            expiresAt: Instant?,
            revokedAt: Instant?,
        ): OpenFinanceConsent =
            OpenFinanceConsent(
                id = id,
                userId = userId,
                providerId = providerId,
                externalConsentId = externalConsentId,
                scopes = scopes,
                status = status,
                createdAt = createdAt,
                updatedAt = updatedAt,
                expiresAt = expiresAt,
                revokedAt = revokedAt,
            )

        private fun normalizeExternalId(value: String): String {
            val normalized = value.trim()
            if (normalized.isBlank() || normalized.length > 255) {
                throw DomainValidationException("Open Finance external consent ID is invalid.")
            }
            return normalized
        }
    }
}
