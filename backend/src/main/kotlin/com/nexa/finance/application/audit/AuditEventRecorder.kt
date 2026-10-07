package com.nexa.finance.application.audit

import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class AuditEventRecorder(
    private val repository: AuditEventRepository,
    private val now: () -> Instant = { Instant.now() },
) {
    private val logger = LoggerFactory.getLogger(AuditEventRecorder::class.java)

    fun record(
        actorUserId: com.nexa.finance.domain.common.UserId?,
        eventType: String,
        targetType: String? = null,
        targetId: UUID? = null,
        metadata: String? = null,
    ) {
        runCatching {
            repository.record(
                AuditEvent(
                    actorUserId = actorUserId,
                    eventType = eventType,
                    targetType = targetType,
                    targetId = targetId,
                    metadata = metadata,
                    createdAt = now(),
                ),
            )
        }.onFailure {
            logger.warn("Audit event could not be persisted: type={}, targetType={}", eventType, targetType, it)
        }
    }
}
