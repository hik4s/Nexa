package com.nexa.finance.domain.openfinance

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OpenFinanceConsentTest {
    private val userId = UserId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
    private val now = Instant.parse("2026-10-05T15:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun createsRequestedConsentWithNormalizedProviderAndScopes() {
        val consent = OpenFinanceConsent.createRequested(
            userId = userId,
            providerId = " BELVO ",
            scopes = setOf("transactions", "accounts"),
            clock = clock,
        )

        assertEquals(OpenFinanceConsentStatus.REQUESTED, consent.status)
        assertEquals("belvo", consent.providerId)
        assertEquals(setOf("TRANSACTIONS", "ACCOUNTS"), consent.scopes)
        assertFalse(consent.isUsable(now))
    }

    @Test
    fun transitionsToActiveAndIsUsableUntilExpiration() {
        val consent = requested()
            .markAwaitingAuthorization("external-1", now)
            .activate(now.plusSeconds(3600), now)

        assertEquals(OpenFinanceConsentStatus.ACTIVE, consent.status)
        assertTrue(consent.isUsable(now.plusSeconds(1)))
        assertFalse(consent.isUsable(now.plusSeconds(3601)))
    }

    @Test
    fun revokeIsIdempotentAndRecordsRevocationTime() {
        val revoked = requested()
            .markAwaitingAuthorization("external-1", now)
            .revoke(now)

        val again = revoked.revoke(now.plusSeconds(30))

        assertEquals(OpenFinanceConsentStatus.REVOKED, again.status)
        assertEquals(now, again.revokedAt)
    }

    @Test
    fun rejectsUnsupportedScope() {
        assertFailsWith<DomainValidationException> {
            requested(setOf("PIX_KEYS"))
        }
    }

    @Test
    fun cannotActivateAfterRevocation() {
        val revoked = requested().revoke(now)

        assertFailsWith<DomainValidationException> {
            revoked.activate(now.plusSeconds(3600), now)
        }
    }

    private fun requested(scopes: Set<String> = setOf("TRANSACTIONS")) =
        OpenFinanceConsent.createRequested(
            userId = userId,
            providerId = "belvo",
            scopes = scopes,
            clock = clock,
        )
}
