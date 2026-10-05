package com.nexa.finance.api

import com.nexa.finance.application.openfinance.OpenFinanceConsentService
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.openfinance.OpenFinanceConsent
import com.nexa.finance.domain.openfinance.OpenFinanceConsentId
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.openFinanceConsentRoutes(service: OpenFinanceConsentService) {
    authenticate("session-bearer") {
        route("/api/v1/open-finance/consents") {
            post {
                val request = call.receive<CreateOpenFinanceConsentRequest>()
                val consent = service.request(
                    userId = call.principalUserId(),
                    providerId = request.providerId,
                    scopes = request.scopes,
                )
                call.respond(consent.toResponse())
            }

            get {
                call.respond(
                    service.list(call.principalUserId()).map { it.toResponse() }
                )
            }

            delete("/{id}") {
                val consentId = parseConsentId(call.parameters["id"])
                call.respond(
                    service.revoke(call.principalUserId(), consentId).toResponse()
                )
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.principalUserId(): UserId =
    principal<UserIdPrincipal>()?.name?.let {
        runCatching { UserId(UUID.fromString(it)) }
            .getOrElse { throw DomainValidationException("Invalid authenticated principal.") }
    } ?: throw DomainValidationException("Invalid authenticated principal.")

private fun parseConsentId(value: String?): OpenFinanceConsentId =
    runCatching { UUID.fromString(value ?: "") }
        .getOrElse { throw DomainValidationException("Open Finance consent ID is invalid.") }

private fun OpenFinanceConsent.toResponse(): OpenFinanceConsentResponse =
    OpenFinanceConsentResponse(
        id = id.toString(),
        providerId = providerId,
        externalConsentId = externalConsentId,
        scopes = scopes,
        status = status.name,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
        expiresAt = expiresAt?.toString(),
        revokedAt = revokedAt?.toString(),
    )
