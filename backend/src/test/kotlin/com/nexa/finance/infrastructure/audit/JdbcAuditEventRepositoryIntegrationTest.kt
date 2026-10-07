package com.nexa.finance.infrastructure.audit

import com.nexa.finance.application.audit.AuditEvent
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

class JdbcAuditEventRepositoryIntegrationTest {
    @Test
    fun persistsCriticalAuditEventWithoutSecretPayload() {
        assumeTrue(!System.getenv("POSTGRES_PASSWORD").isNullOrBlank())

        val connections = DatabaseConnectionProvider()
        val repository = JdbcAuditEventRepository(connections)
        val actor = UserId(UUID.randomUUID())
        val id = UUID.randomUUID()

        repository.record(
            AuditEvent(
                id = id,
                actorUserId = actor,
                eventType = "OPEN_FINANCE_CONSENT_CREATED",
                targetType = "OPEN_FINANCE_CONSENT",
                targetId = UUID.randomUUID(),
                metadata = """{"provider":"belvo","scopeCount":3}""",
                createdAt = Instant.parse("2026-10-06T02:00:00Z"),
            ),
        )

        try {
            connections.open().use { c ->
                c.prepareStatement(
                    """
                    SELECT actor_user_id, event_type, target_type, target_id, metadata, created_at
                    FROM audit_events
                    WHERE id = ?
                    """.trimIndent(),
                ).use { s ->
                    s.setObject(1, id)
                    s.executeQuery().use { r ->
                        assertTrue(r.next())
                        assertEquals(actor.value, r.getObject("actor_user_id", UUID::class.java))
                        assertEquals("OPEN_FINANCE_CONSENT_CREATED", r.getString("event_type"))
                        assertEquals("OPEN_FINANCE_CONSENT", r.getString("target_type"))
                        assertEquals("""{"provider":"belvo","scopeCount":3}""", r.getString("metadata"))
                        assertTrue(r.getTimestamp("created_at").toInstant() == Instant.parse("2026-10-06T02:00:00Z"))
                    }
                }
                c.prepareStatement("DELETE FROM audit_events WHERE id = ?").use { s ->
                    s.setObject(1, id)
                    s.executeUpdate()
                }
            }
        } finally {
            connections.close()
        }
    }
}
