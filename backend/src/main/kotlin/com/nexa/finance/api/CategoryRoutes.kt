package com.nexa.finance.api

import com.nexa.finance.infrastructure.category.JdbcCategoryRepository
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.util.UUID

fun Route.categoryRoutes(repository: JdbcCategoryRepository) {
    authenticate("session-bearer") {
        route("/api/v1/categories") {
            get {
                val userId = UUID.fromString(
                    call.principal<UserIdPrincipal>()?.name ?: throw IllegalStateException("Missing authenticated user.")
                )
                val type = call.request.queryParameters["type"]?.trim()?.uppercase()
                    ?.takeIf { it.isNotBlank() }
                call.respond(repository.listAvailable(userId, type).map {
                    CategoryResponse(it.id.toString(), it.name, it.type, it.iconKey, it.sortOrder)
                })
            }
        }
    }
}
