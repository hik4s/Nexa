package com.nexa.finance.api

import com.nexa.finance.application.sync.SyncService
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.syncRoutes(service: SyncService) {
    authenticate("session-bearer") {
        route("/api/v1/sync") {
            post("/push") {
                val userId = call.principalUserId()
                val request = call.receive<SyncPushRequest>()
                if (request.operations.size > 50) throw DomainValidationException("Sync batch is too large.")
                val acknowledgements = service.push(userId, request.operations)
                call.respond(HttpStatusCode.OK, SyncPushResponse(acknowledgements, service.latestCursor(userId)))
            }
            get("/pull") {
                val userId = call.principalUserId()
                val cursor = call.request.queryParameters["cursor"]?.toLongOrNull() ?: 0L
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val (nextCursor, changes) = service.pull(userId, cursor, limit)
                call.respond(HttpStatusCode.OK, SyncPullResponse(nextCursor, changes))
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.principalUserId(): UserId =
    principal<UserIdPrincipal>()?.name?.let { runCatching { UserId(UUID.fromString(it)) }.getOrElse { throw DomainValidationException("Invalid authenticated principal.") } }
        ?: throw DomainValidationException("Invalid authenticated principal.")
