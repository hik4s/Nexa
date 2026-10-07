package com.nexa.finance.api

import com.nexa.finance.application.auth.AuthenticationService
import com.nexa.finance.application.auth.ProfileService
import com.nexa.finance.application.auth.RecoveryService
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
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.time.ZoneId

fun Route.authRoutes(
    authentication: AuthenticationService,
    recovery: RecoveryService,
    profile: ProfileService,
) {
    route("/api/v1/auth") {
        post("/register") {
            val request = call.receiveValidated<RegisterRequest>()
            val timeZone = parseTimeZone(request.timeZone)
            val password = request.password.toCharArray()
            try {
                val user = authentication.register(request.email, request.displayName, timeZone, password)
                call.respond(
                    HttpStatusCode.Created,
                    UserResponse(user.id.value.toString(), user.email, user.displayName, user.timeZone.id, user.status.name),
                )
            } finally {
                password.fill('\u0000')
            }
        }

        post("/login") {
            val request = call.receiveValidated<LoginRequest>()
            val password = request.password.toCharArray()
            try {
                val session = authentication.login(request.email, password)
                call.respond(
                    SessionResponse(session.userId.value.toString(), session.token, session.expiresAt.toString()),
                )
            } finally {
                password.fill('\u0000')
            }
        }

        post("/recovery/request") {
            val request = call.receiveValidated<RecoveryRequestDto>()
            val result = recovery.requestRecovery(request.email)
            call.respond(RecoveryResponse(result.token, result.expiresAt.toString()))
        }

        post("/recovery/reset") {
            val request = call.receiveValidated<ResetPasswordRequest>()
            val password = request.newPassword.toCharArray()
            try {
                recovery.resetPassword(request.token, password)
                call.respond(HttpStatusCode.NoContent)
            } finally {
                password.fill('\u0000')
            }
        }

        authenticate("session-bearer") {
            post("/logout") {
                val token = bearerToken(call)
                authentication.logout(token)
                call.respond(HttpStatusCode.NoContent)
            }

            get("/me") {
                val userId = call.principalUserId()
                call.respond(profile.getProfile(userId).toResponse())
            }

            put("/me") {
                val request = call.receiveValidated<UpdateProfileRequest>()
                val userId = call.principalUserId()
                val timeZone = request.timeZone?.let(::parseTimeZone)
                call.respond(profile.updateProfile(userId, request.displayName, request.email, timeZone).toResponse())
            }
        }
    }
}

private suspend inline fun <reified T> io.ktor.server.application.ApplicationCall.receiveValidated(): T =
    try {
        receive()
    } catch (_: Exception) {
        throw DomainValidationException("Request body is invalid.")
    }

private fun io.ktor.server.application.ApplicationCall.principalUserId(): UserId =
    principal<UserIdPrincipal>()?.name?.let { UserId(java.util.UUID.fromString(it)) }
        ?: throw DomainValidationException("Invalid authenticated principal.")

private fun bearerToken(call: io.ktor.server.application.ApplicationCall): String =
    call.request.headers["Authorization"]
        ?.removePrefix("Bearer ")
        ?.takeIf { it.isNotBlank() }
        ?: throw DomainValidationException("Invalid session.")

private fun parseTimeZone(value: String): ZoneId =
    runCatching { ZoneId.of(value.trim()) }
        .getOrElse { throw DomainValidationException("Time zone is invalid.") }

private fun com.nexa.finance.application.auth.UserProfile.toResponse() =
    UserResponse(userId.value.toString(), email, displayName, timeZone.id, status)
