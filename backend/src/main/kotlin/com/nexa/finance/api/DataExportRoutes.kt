package com.nexa.finance.api

import com.nexa.finance.application.export.DataExportService
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.util.UUID

fun Route.dataExportRoutes(service: DataExportService) {
    authenticate("session-bearer") {
        get("/api/v1/auth/me/export") {
            val principal = call.principal<UserIdPrincipal>()
                ?: throw DomainValidationException("Invalid authenticated principal.")
            val userId = runCatching { UserId(UUID.fromString(principal.name)) }
                .getOrElse { throw DomainValidationException("Invalid authenticated principal.") }

            call.response.header("Content-Disposition", "attachment; filename=nexa-data-export.json")
            call.respond(service.export(userId))
        }
    }
}
