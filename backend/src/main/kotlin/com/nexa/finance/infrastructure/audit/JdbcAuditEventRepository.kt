package com.nexa.finance.infrastructure.audit

import com.nexa.finance.application.audit.AuditEvent
import com.nexa.finance.application.audit.AuditEventRepository
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import java.sql.Timestamp

class JdbcAuditEventRepository(
    private val connections: DatabaseConnectionProvider = DatabaseConnectionProvider(),
) : AuditEventRepository {
    override fun record(event: AuditEvent) {
        connections.open().use { c ->
            c.prepareStatement(
                """
                INSERT INTO audit_events
                    (id, actor_user_id, event_type, target_type, target_id, metadata, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
            ).use { s ->
                s.setObject(1, event.id)
                if (event.actorUserId == null) s.setNull(2, java.sql.Types.OTHER) else s.setObject(2, event.actorUserId.value)
                s.setString(3, event.eventType)
                s.setString(4, event.targetType)
                if (event.targetId == null) s.setNull(5, java.sql.Types.OTHER) else s.setObject(5, event.targetId)
                s.setString(6, event.metadata)
                s.setTimestamp(7, Timestamp.from(event.createdAt))
                s.executeUpdate()
            }
        }
    }
}