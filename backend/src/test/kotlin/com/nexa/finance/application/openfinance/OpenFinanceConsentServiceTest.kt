package com.nexa.finance.application.openfinance

import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.openfinance.OpenFinanceConsent
import com.nexa.finance.domain.openfinance.OpenFinanceConsentStatus
import java.time.Clock
import java.time.Instant
import java.io.IOException
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OpenFinanceConsentServiceTest {
    private val now = Instant.parse("2026-10-05T22:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val userId = UserId(UUID.fromString("11111111-1111-1111-1111-111111111111"))

    @Test
    fun requestRejectsUnconfiguredProviderBeforePersistence() {
        val repository = FakeConsentRepository()
        val service = OpenFinanceConsentService(
            repository = repository,
            providers = ConfiguredOpenFinanceProviderRegistry(
                providers = emptyMap(),
            ),
            clock = clock,
        )

        assertFailsWith<RuntimeException> {
            service.request(
                userId = userId,
                providerId = "belvo",
                scopes = setOf("ACCOUNTS"),
            )
        }
        assertEquals(0, repository.values.size)
    }

    @Test
    fun syncRetriesTransientProviderFailureAndEventuallySucceeds() {
        val repository = FakeConsentRepository()
        val provider = RetryingProvider()
        val delays = mutableListOf<Long>()
        val service = OpenFinanceConsentService(
            repository = repository,
            providers = ConfiguredOpenFinanceProviderRegistry(
                providers = mapOf("fake" to provider),
            ),
            clock = clock,
            retryPolicy = com.nexa.finance.infrastructure.resilience.RetryPolicy(
                settings = com.nexa.finance.RetrySettings(
                    maxAttempts = 4,
                    initialBackoffMillis = 10,
                    maxBackoffMillis = 100,
                ),
                sleeper = delays::add,
            ),
        )

        val consent = service.request(
            userId = userId,
            providerId = "fake",
            scopes = setOf("ACCOUNTS"),
        )
        service.beginAuthorization(userId, consent.id)

        provider.transientFailuresRemaining = 2
        val synced = service.sync(userId, consent.id)

        assertEquals(OpenFinanceConsentStatus.AWAITING_AUTHORIZATION, synced.status)
        assertEquals(3, provider.statusCalls)
        assertEquals(listOf(10L, 20L), delays)
    }

    @Test
    fun externalProviderFailureExhaustsRetriesWithoutCorruptingConsentState() {
        val repository = FakeConsentRepository()
        val provider = RetryingProvider().apply { transientFailuresRemaining = 10 }
        val delays = mutableListOf<Long>()
        val service = OpenFinanceConsentService(
            repository = repository,
            providers = ConfiguredOpenFinanceProviderRegistry(
                providers = mapOf("fake" to provider),
            ),
            clock = clock,
            retryPolicy = com.nexa.finance.infrastructure.resilience.RetryPolicy(
                settings = com.nexa.finance.RetrySettings(
                    maxAttempts = 3,
                    initialBackoffMillis = 10,
                    maxBackoffMillis = 50,
                ),
                sleeper = delays::add,
            ),
        )

        val consent = service.request(userId, "fake", setOf("ACCOUNTS"))
        service.beginAuthorization(userId, consent.id)

        assertFailsWith<IOException> {
            service.sync(userId, consent.id)
        }

        val persisted = repository.findByOwnerAndId(userId, consent.id)
        assertEquals(OpenFinanceConsentStatus.AWAITING_AUTHORIZATION, persisted?.status)
        assertEquals(3, provider.statusCalls)
        assertEquals(listOf(10L, 20L), delays)
    }

    @Test
    fun recoveredExternalProviderAllowsSyncToActiveState() {
        val repository = FakeConsentRepository()
        val provider = RetryingProvider()
        val service = OpenFinanceConsentService(
            repository = repository,
            providers = ConfiguredOpenFinanceProviderRegistry(
                providers = mapOf("fake" to provider),
            ),
            clock = clock,
            retryPolicy = com.nexa.finance.infrastructure.resilience.RetryPolicy(
                settings = com.nexa.finance.RetrySettings(
                    maxAttempts = 4,
                    initialBackoffMillis = 10,
                    maxBackoffMillis = 100,
                ),
                sleeper = {},
            ),
        )

        val consent = service.request(userId, "fake", setOf("ACCOUNTS"))
        service.beginAuthorization(userId, consent.id)

        provider.transientFailuresRemaining = 1
        provider.recoveredStatus = OpenFinanceProvider.ExternalConsentStatus.ACTIVE
        provider.recoveredExpiresAt = now.plusSeconds(3600)

        val synced = service.sync(userId, consent.id)

        assertEquals(OpenFinanceConsentStatus.ACTIVE, synced.status)
        assertEquals(now.plusSeconds(3600), synced.expiresAt)
        assertEquals(2, provider.statusCalls)
    }

    @Test
    fun requestPersistsNormalizedConsentForConfiguredProvider() {
        val repository = FakeConsentRepository()
        val provider = FakeProvider("BELVO")
        val service = OpenFinanceConsentService(
            repository = repository,
            providers = ConfiguredOpenFinanceProviderRegistry(
                providers = mapOf("belvo" to provider),
            ),
            clock = clock,
        )

        val consent = service.request(
            userId = userId,
            providerId = " BELVO ",
            scopes = setOf("accounts", "transactions"),
        )

        assertEquals("belvo", consent.providerId)
        assertEquals(OpenFinanceConsentStatus.REQUESTED, consent.status)
        assertEquals(consent, repository.findByOwnerAndId(userId, consent.id))
    }

    private class FakeConsentRepository : OpenFinanceConsentRepository {
        val values = linkedMapOf<UUID, OpenFinanceConsent>()

        override fun create(consent: OpenFinanceConsent) {
            values[consent.id] = consent
        }

        override fun findByOwner(ownerId: UserId): List<OpenFinanceConsent> =
            values.values.filter { it.userId == ownerId }

        override fun findByOwnerAndId(ownerId: UserId, id: UUID): OpenFinanceConsent? =
            values[id]?.takeIf { it.userId == ownerId }

        override fun update(consent: OpenFinanceConsent): OpenFinanceConsent? {
            if (!values.containsKey(consent.id)) return null
            values[consent.id] = consent
            return consent
        }

        override fun listUsable(ownerId: UserId, at: Instant): List<OpenFinanceConsent> =
            values.values.filter { it.userId == ownerId && it.isUsable(at) }
    }

    private class RetryingProvider : OpenFinanceProvider {
        override val providerId: String = "fake"

        var transientFailuresRemaining = 0
        var statusCalls = 0
        var recoveredStatus = OpenFinanceProvider.ExternalConsentStatus.AWAITING_AUTHORIZATION
        var recoveredExpiresAt: Instant? = null

        override fun requestConsent(
            userId: UserId,
            scopes: Set<String>,
        ) = OpenFinanceProvider.ProviderConsentSession(
            externalConsentId = "external-retry",
            authorizationUrl = "https://provider.example/authorize",
        )

        override fun getConsentStatus(externalConsentId: String) : OpenFinanceProvider.ProviderConsentState {
            statusCalls += 1
            if (transientFailuresRemaining > 0) {
                transientFailuresRemaining -= 1
                throw IOException("temporary provider outage")
            }
            return OpenFinanceProvider.ProviderConsentState(
                status = recoveredStatus,
                expiresAt = recoveredExpiresAt,
            )
        }

        override fun revokeConsent(externalConsentId: String) = Unit
    }

    private class FakeProvider(
        override val providerId: String,
    ) : OpenFinanceProvider {
        override fun requestConsent(
            userId: UserId,
            scopes: Set<String>,
        ) = OpenFinanceProvider.ProviderConsentSession(
            externalConsentId = "external-1",
            authorizationUrl = "https://provider.example/authorize",
        )

        override fun getConsentStatus(externalConsentId: String) =
            OpenFinanceProvider.ProviderConsentState(
                status = OpenFinanceProvider.ExternalConsentStatus.AWAITING_AUTHORIZATION,
                expiresAt = null,
            )

        override fun revokeConsent(externalConsentId: String) = Unit
    }
}
