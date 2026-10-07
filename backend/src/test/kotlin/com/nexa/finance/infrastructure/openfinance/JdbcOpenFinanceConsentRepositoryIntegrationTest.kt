package com.nexa.finance.infrastructure.openfinance

import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.openfinance.OpenFinanceConsent
import com.nexa.finance.domain.openfinance.OpenFinanceConsentStatus
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

class JdbcOpenFinanceConsentRepositoryIntegrationTest {
    @Test
    fun roundTripsConsentAndPreservesOwnershipAndStatus() {
        assumeTrue(!System.getenv("POSTGRES_PASSWORD").isNullOrBlank())

        val connections = DatabaseConnectionProvider()
        val repository = JdbcOpenFinanceConsentRepository(connections)
        val userId = createUser(connections)
        val fixed = Instant.parse("2026-10-05T15:00:00Z")
        val clock = Clock.fixed(fixed, ZoneOffset.UTC)

        try {
            val consent = OpenFinanceConsent.createRequested(
                userId = UserId(userId),
                providerId = "belvo",
                scopes = setOf("transactions", "accounts"),
                clock = clock,
            ).markAwaitingAuthorization("external-123", fixed)

            repository.create(consent)

            val stored = repository.findByOwnerAndId(UserId(userId), consent.id)
            assertEquals(OpenFinanceConsentStatus.AWAITING_AUTHORIZATION, stored?.status)
            assertEquals(setOf("TRANSACTIONS", "ACCOUNTS"), stored?.scopes)
            assertEquals("belvo", stored?.providerId)
            assertEquals("external-123", stored?.externalConsentId)

            val active = stored!!.activate(fixed.plusSeconds(3600), fixed)
            repository.update(active)

            val usable = repository.listUsable(UserId(userId), fixed.plusSeconds(1))
            assertTrue(usable.any { it.id == consent.id && it.status == OpenFinanceConsentStatus.ACTIVE })
        } finally {
            deleteUser(connections, userId)
        }
    }

    private fun createUser(connections: DatabaseConnectionProvider): UUID {
        val id = UUID.randomUUID()
        connections.open().use { c ->
            c.prepareStatement(
                """
                INSERT INTO users
                    (id, email, display_name, time_zone, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """.trimIndent(),
            ).use { s ->
                s.setObject(1, id)
                s.setString(2, "openfinance-$id@example.com")
                s.setString(3, "Open Finance Integration")
                s.setString(4, "UTC")
                s.executeUpdate()
            }
        }
        return id
    }

    private fun deleteUser(connections: DatabaseConnectionProvider, id: UUID) {
        connections.open().use { c ->
            c.prepareStatement("DELETE FROM open_finance_consents WHERE user_id = ?").use { s ->
                s.setObject(1, id)
                s.executeUpdate()
            }
            c.prepareStatement("DELETE FROM users WHERE id = ?").use { s ->
                s.setObject(1, id)
                s.executeUpdate()
            }
        }
    }
}
