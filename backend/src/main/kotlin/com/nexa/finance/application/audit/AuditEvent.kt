package com.nexa.finance.application.audit

import com.nexa.finance.domain.common.UserId
import java.time.Instant
import java.util.UUID

data class AuditEvent(
    val id: UUID = UUID.randomUUID(),
    val actorUserId: UserId?,
    val eventType: String,
    val targetType: String?,
    val targetId: UUID?,
    val metadata: String?,
    val createdAt: Instant,
) {
    init {
        require(eventType.length in 1..80) { "Audit event type is invalid." }
        require(targetType == null || targetType.length in 1..80) { "Audit target type is invalid." }
        require(metadata == null || metadata.length <= 4_000) { "Audit metadata is too large." }
    }
}

interface AuditEventRepository {
    fun record(event: AuditEvent)
}
