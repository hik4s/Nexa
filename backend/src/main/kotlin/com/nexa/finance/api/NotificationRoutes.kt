package com.nexa.finance.api

import com.nexa.finance.application.notification.NotificationService
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.notificationRoutes(service: NotificationService) {
    authenticate("session-bearer") {
        route("/api/v1/notifications") {
            get {
                val unreadOnly = call.request.queryParameters["unreadOnly"]?.toBooleanStrictOrNull() ?: false
                call.respond(service.list(call.principalUserId(), unreadOnly).map { it.toResponse() })
            }

            post("/{id}/read") {
                val id = runCatching { UUID.fromString(call.parameters["id"] ?: "") }
                    .getOrElse { throw DomainValidationException("Notification ID is invalid.") }
                service.markRead(call.principalUserId(), id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.principalUserId(): UserId =
    principal<UserIdPrincipal>()?.name?.let {
        runCatching { UserId(UUID.fromString(it)) }
            .getOrElse { throw DomainValidationException("Invalid authenticated principal.") }
    } ?: throw DomainValidationException("Invalid authenticated principal.")

private fun com.nexa.finance.infrastructure.notification.NotificationRecord.toResponse() =
    NotificationResponse(
        id = id.toString(),
        type = type,
        severity = severity,
        title = title,
        message = message,
        readAt = readAt?.toString(),
        createdAt = createdAt.toString(),
    )
