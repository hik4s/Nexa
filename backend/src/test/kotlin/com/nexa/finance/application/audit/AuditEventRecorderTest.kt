package com.nexa.finance.application.audit

import com.nexa.finance.domain.common.UserId
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuditEventRecorderTest {
    @Test
    fun recordsEventWithoutIncludingSecrets() {
        val repository = FakeRepository()
        val recorder = AuditEventRecorder(
            repository = repository,
            now = { Instant.parse("2026-10-06T02:30:00Z") },
        )
        val userId = UserId(UUID.fromString("11111111-1111-1111-1111-111111111111"))

        recorder.record(
            actorUserId = userId,
            eventType = "OPEN_FINANCE_CONSENT_CREATED",
            targetType = "OPEN_FINANCE_CONSENT",
            targetId = UUID.fromString("22222222-2222-2222-2222-222222222222"),
            metadata = """{"provider":"belvo","scopeCount":3}""",
        )

        val event = repository.events.single()
        assertEquals(userId, event.actorUserId)
        assertEquals("OPEN_FINANCE_CONSENT_CREATED", event.eventType)
        assertEquals(Instant.parse("2026-10-06T02:30:00Z"), event.createdAt)
        assertTrue("token" !in (event.metadata ?: ""))
        assertTrue("password" !in (event.metadata ?: ""))
    }

    @Test
    fun auditStorageFailureDoesNotBreakPrimaryFlow() {
        var invoked = false
        val recorder = AuditEventRecorder(
            repository = object : AuditEventRepository {
                override fun record(event: AuditEvent) {
                    invoked = true
                    error("simulated audit outage")
                }
            },
        )

        recorder.record(
            actorUserId = null,
            eventType = "AUTH_LOGIN_FAILED",
            metadata = """{"reason":"invalid_credentials"}""",
        )

        assertTrue(invoked)
    }

    private class FakeRepository : AuditEventRepository {
        val events = mutableListOf<AuditEvent>()

        override fun record(event: AuditEvent) {
            events += event
        }
    }
}
