package com.nexa.finance.application.openfinance

import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.openfinance.OpenFinanceConsent
import com.nexa.finance.domain.openfinance.OpenFinanceConsentStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class OpenFinanceConsentServiceTest {
    private val now = Instant.parse("2026-10-05T22:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val userId = UserId(UUID.fromString("11111111-1111-1111-1111-111111111111"))

    @Test
    fun requestsConsentOnlyForConfiguredProvider() {
        val repository = FakeConsentRepository()
        val provider = FakeProvider("belvo")
        val service = OpenFinanceConsentService(
            repository = repository,
            providers = ConfiguredOpenFinanceProviderRegistry(mapOf("belvo" to provider)),
            clock = clock,
        )

        val consent = service.request(userId, " BELVO ", setOf("accounts", "transactions"))

        assertEquals(OpenFinanceConsentStatus.REQUESTED, consent.status)
        assertEquals("belvo", consent.providerId)
        assertEquals(consent, repository.findByOwnerAndId(userId, consent.id))
        assertFailsWith<RuntimeException> {
            service.request(userId, "pluggy", setOf("ACCOUNTS"))
        }
    }

    @Test
    fun beginsAuthorizationAndStoresExternalConsentId() {
        val repository = FakeConsentRepository()
        val provider = FakeProvider("belvo")
        val service = service(repository, provider)

        val consent = service.request(userId, "belvo", setOf("ACCOUNTS"))
        val session = service.beginAuthorization(userId, consent.id)

        assertEquals("external-1", session.externalConsentId)
        val stored = repository.findByOwnerAndId(userId, consent.id)!!
        assertEquals(OpenFinanceConsentStatus.AWAITING_AUTHORIZATION, stored.status)
        assertEquals("external-1", stored.externalConsentId)
    }

    @Test
    fun syncActivatesConsentAndPreservesProviderExpiration() {
        val repository = FakeConsentRepository()
        val provider = FakeProvider("belvo").apply {
            state = OpenFinanceProvider.ProviderConsentState(
                status = OpenFinanceProvider.ExternalConsentStatus.ACTIVE,
                expiresAt = now.plusSeconds(3600),
            )
        }
        val service = service(repository, provider)

        val consent = service.request(userId, "belvo", setOf("BALANCES"))
        service.beginAuthorization(userId, consent.id)
        val updated = service.sync(userId, consent.id)

        assertEquals(OpenFinanceConsentStatus.ACTIVE, updated.status)
        assertEquals(now.plusSeconds(3600), updated.expiresAt)
    }

    @Test
    fun revokeCallsProviderAndStoresRevokedState() {
        val repository = FakeConsentRepository()
        val provider = FakeProvider("belvo")
        val service = service(repository, provider)

        val consent = service.request(userId, "belvo", setOf("TRANSACTIONS"))
        service.beginAuthorization(userId, consent.id)
        val revoked = service.revoke(userId, consent.id)

        assertEquals(OpenFinanceConsentStatus.REVOKED, revoked.status)
        assertEquals("external-1", provider.revokedExternalConsentId)
        assertEquals(now, revoked.revokedAt)
    }

    private fun service(
        repository: FakeConsentRepository,
        provider: FakeProvider,
    ) = OpenFinanceConsentService(
        repository = repository,
        providers = ConfiguredOpenFinanceProviderRegistry(mapOf(provider.providerId to provider)),
        clock = clock,
    )

    private class FakeConsentRepository : OpenFinanceConsentRepository {
        private val values = linkedMapOf<UUID, OpenFinanceConsent>()

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

    private class FakeProvider(
        override val providerId: String,
    ) : OpenFinanceProvider {
        var state = OpenFinanceProvider.ProviderConsentState(
            status = OpenFinanceProvider.ExternalConsentStatus.AWAITING_AUTHORIZATION,
            expiresAt = null,
        )
        var revokedExternalConsentId: String? = null

        override fun requestConsent(
            userId: UserId,
            scopes: Set<String>,
        ): OpenFinanceProvider.ProviderConsentSession =
            OpenFinanceProvider.ProviderConsentSession(
                externalConsentId = "external-1",
                authorizationUrl = "https://provider.example/authorize?consent=external-1",
            )

        override fun getConsentStatus(externalConsentId: String): OpenFinanceProvider.ProviderConsentState = state

        override fun revokeConsent(externalConsentId: String) {
            revokedExternalConsentId = externalConsentId
        }
    }
}
