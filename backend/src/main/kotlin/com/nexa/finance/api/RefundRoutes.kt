package com.nexa.finance.api

import com.nexa.finance.application.refund.RefundService
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.LocalDate
import java.util.UUID

fun Route.refundRoutes(service: RefundService) {
    authenticate("session-bearer") {
        route("/api/v1/transactions/{id}") {
            post("/refunds") {
                val userId = call.principalRefundUserId()
                val id = parseRefundUuid(call.parameters["id"] ?: "", "Transaction ID is invalid.")
                val request = call.receive<CreateRefundRequest>()
                val tx = service.refund(
                    ownerId = userId,
                    originalId = id,
                    amount = Money(request.amountMinor, parseRefundEnum<CurrencyCode>(request.currency, "Currency is invalid.")),
                    occurredOn = parseRefundDate(request.occurredOn, "Refund date is invalid."),
                    description = request.description,
                )
                call.respond(HttpStatusCode.Created, tx.toRefundTransactionResponse())
            }
            post("/cancellation") {
                val userId = call.principalRefundUserId()
                val id = parseRefundUuid(call.parameters["id"] ?: "", "Transaction ID is invalid.")
                val request = call.receive<CreateCancellationRequest>()
                val tx = service.cancelPosted(
                    ownerId = userId,
                    originalId = id,
                    occurredOn = parseRefundDate(request.occurredOn, "Cancellation date is invalid."),
                    description = request.description,
                )
                call.respond(HttpStatusCode.Created, tx.toRefundTransactionResponse())
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.principalRefundUserId(): UserId =
    principal<UserIdPrincipal>()?.name?.let {
        runCatching { UserId(UUID.fromString(it)) }
            .getOrElse { throw DomainValidationException("Invalid authenticated principal.") }
    } ?: throw DomainValidationException("Invalid authenticated principal.")

private fun parseRefundUuid(value: String, message: String): UUID =
    runCatching { UUID.fromString(value) }.getOrElse { throw DomainValidationException(message) }

private fun parseRefundDate(value: String, message: String): LocalDate =
    runCatching { LocalDate.parse(value) }.getOrElse { throw DomainValidationException(message) }

private inline fun <reified T : Enum<T>> parseRefundEnum(value: String, message: String): T =
    runCatching { enumValueOf<T>(value.trim().uppercase()) }.getOrElse { throw DomainValidationException(message) }

private fun com.nexa.finance.domain.transaction.Transaction.toRefundTransactionResponse() =
    TransactionResponse(
        id.toString(),
        accountId.value.toString(),
        amount.amountInMinor,
        amount.currency.name,
        type.name,
        direction.name,
        status.name,
        occurredOn.toString(),
        description,
        categoryId?.value?.toString(),
        source.name,
    )
