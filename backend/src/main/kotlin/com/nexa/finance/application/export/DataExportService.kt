package com.nexa.finance.application.export

import com.nexa.finance.application.audit.AuditEventRecorder
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.infrastructure.export.JdbcDataExportRepository
import kotlinx.serialization.json.JsonObject

class DataExportService(
    private val repository: JdbcDataExportRepository,
    private val audit: AuditEventRecorder? = null,
) {
    fun export(userId: UserId): JsonObject {
        val exported = repository.export(userId)
            ?: throw DomainValidationException("User was not found.")
        audit?.record(
            actorUserId = userId,
            eventType = "DATA_EXPORT_REQUESTED",
            targetType = "USER",
            targetId = userId.value,
        )
        return exported
    }
}
