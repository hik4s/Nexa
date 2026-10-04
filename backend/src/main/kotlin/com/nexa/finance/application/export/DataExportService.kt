package com.nexa.finance.application.export

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.infrastructure.export.JdbcDataExportRepository
import kotlinx.serialization.json.JsonObject

class DataExportService(private val repository: JdbcDataExportRepository) {
    fun export(userId: UserId): JsonObject =
        repository.export(userId)
            ?: throw DomainValidationException("User was not found.")
}
