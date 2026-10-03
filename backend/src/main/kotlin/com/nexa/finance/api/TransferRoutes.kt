package com.nexa.finance.api

import com.nexa.finance.application.transfer.TransferService
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.transaction.TransactionSource
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.LocalDate
import java.util.UUID

fun Route.transferRoutes(service: TransferService) {
    authenticate("session-bearer") {
        route("/api/v1/transfers") {
            post {
                val userId = call.principalUserId()
                val request = call.receiveValidated<CreateTransferRequest>()
                val transfer = service.create(
                    ownerId = userId,
                    sourceAccountId = AccountId(parseUuid(request.sourceAccountId, "Source account ID is invalid.")),
                    destinationAccountId = AccountId(parseUuid(request.destinationAccountId, "Destination account ID is invalid.")),
                    amount = Money(request.amountMinor, parseEnum(request.currency, "Currency is invalid.")),
                    occurredOn = runCatching { LocalDate.parse(request.occurredOn) }
                        .getOrElse { throw DomainValidationException("Transfer date is invalid.") },
                    source = parseEnum(request.source, "Transfer source is invalid."),
                )
                call.respond(HttpStatusCode.Created, transfer.toResponse())
            }

            get {
                call.respond(service.list(call.principalUserId()).map { it.toResponse() })
            }

            get("/{id}") {
                val id = parseUuid(call.parameters["id"] ?: "", "Transfer ID is invalid.")
                call.respond(service.get(call.principalUserId(), id).toResponse())
            }

            post("/{id}/post") {
                val id = parseUuid(call.parameters["id"] ?: "", "Transfer ID is invalid.")
                call.respond(service.post(call.principalUserId(), id).toResponse())
            }

            post("/{id}/cancel") {
                val id = parseUuid(call.parameters["id"] ?: "", "Transfer ID is invalid.")
                call.respond(service.cancel(call.principalUserId(), id).toResponse())
            }
        }
    }
}

private fun com.nexa.finance.domain.transfer.Transfer.toResponse() = TransferResponse(
    id = id.toString(),
    sourceAccountId = sourceAccountId.value.toString(),
    destinationAccountId = destinationAccountId.value.toString(),
    amountMinor = amount.amountInMinor,
    currency = amount.currency.name,
    occurredOn = occurredOn.toString(),
    status = status.name,
    sourceTransactionId = sourceTransaction.id.toString(),
    destinationTransactionId = destinationTransaction.id.toString(),
)

private fun io.ktor.server.application.ApplicationCall.principalUserId(): UserId =
    principal<UserIdPrincipal>()?.name?.let {
        runCatching { UserId(UUID.fromString(it)) }
            .getOrElse { throw DomainValidationException("Invalid authenticated principal.") }
    } ?: throw DomainValidationException("Invalid authenticated principal.")

private suspend inline fun <reified T> io.ktor.server.application.ApplicationCall.receiveValidated(): T =
    try { receive() } catch (_: Exception) { throw DomainValidationException("Request body is invalid.") }

private inline fun <reified T : Enum<T>> parseEnum(value: String, message: String): T =
    runCatching { enumValueOf<T>(value.trim().uppercase()) }
        .getOrElse { throw DomainValidationException(message) }

private fun parseUuid(value: String, message: String): UUID =
    runCatching { UUID.fromString(value) }
        .getOrElse { throw DomainValidationException(message) }
