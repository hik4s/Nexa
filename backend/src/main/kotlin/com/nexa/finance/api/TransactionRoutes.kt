package com.nexa.finance.api

import com.nexa.finance.application.transaction.TransactionService
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.category.CategoryId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.transaction.TransactionDirection
import com.nexa.finance.domain.transaction.TransactionId
import com.nexa.finance.domain.transaction.TransactionSource
import com.nexa.finance.domain.transaction.TransactionType
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

fun Route.transactionRoutes(service: TransactionService) {
    authenticate("session-bearer") {
        route("/api/v1/transactions") {
            post {
                val userId = call.principalUserId()
                val request = call.receiveValidated<CreateTransactionRequest>()
                val transaction = service.create(
                    ownerId = userId,
                    accountId = AccountId(parseUuid(request.accountId, "Account ID is invalid.")),
                    amount = Money(request.amountMinor, parseEnum(request.currency, "Currency is invalid.")),
                    type = parseEnum(request.type, "Transaction type is invalid."),
                    direction = parseEnum(request.direction, "Transaction direction is invalid."),
                    occurredOn = runCatching { LocalDate.parse(request.occurredOn) }
                        .getOrElse { throw DomainValidationException("Transaction date is invalid.") },
                    description = request.description,
                    categoryId = request.categoryId?.let { CategoryId(parseUuid(it, "Category ID is invalid.")) },
                    source = parseEnum(request.source, "Transaction source is invalid."),
                )
                call.respond(HttpStatusCode.Created, transaction.toResponse())
            }

            get {
                val userId = call.principalUserId()
                val accountId = call.request.queryParameters["accountId"]?.let {
                    AccountId(parseUuid(it, "Account ID is invalid."))
                }
                call.respond(service.list(userId, accountId).map { it.toResponse() })
            }

            get("/{id}") {
                val userId = call.principalUserId()
                val id = parseUuid(call.parameters["id"] ?: "", "Transaction ID is invalid.")
                call.respond(service.get(userId, id).toResponse())
            }

            post("/{id}/post") {
                val userId = call.principalUserId()
                val id = parseUuid(call.parameters["id"] ?: "", "Transaction ID is invalid.")
                call.respond(service.post(userId, id).toResponse())
            }

            post("/{id}/cancel") {
                val userId = call.principalUserId()
                val id = parseUuid(call.parameters["id"] ?: "", "Transaction ID is invalid.")
                call.respond(service.cancel(userId, id).toResponse())
            }
        }
    }
}

private fun com.nexa.finance.domain.transaction.Transaction.toResponse() =
    TransactionResponse(
        id = id.toString(),
        accountId = accountId.value.toString(),
        amountMinor = amount.amountInMinor,
        currency = amount.currency.name,
        type = type.name,
        direction = direction.name,
        status = status.name,
        occurredOn = occurredOn.toString(),
        description = description,
        categoryId = categoryId?.value?.toString(),
        source = source.name,
    )

private fun io.ktor.server.application.ApplicationCall.principalUserId(): UserId =
    principal<UserIdPrincipal>()?.name?.let {
        runCatching { UserId(UUID.fromString(it)) }
            .getOrElse { throw DomainValidationException("Invalid authenticated principal.") }
    } ?: throw DomainValidationException("Invalid authenticated principal.")

private suspend inline fun <reified T> io.ktor.server.application.ApplicationCall.receiveValidated(): T =
    try {
        receive()
    } catch (_: Exception) {
        throw DomainValidationException("Request body is invalid.")
    }

private inline fun <reified T : Enum<T>> parseEnum(value: String, message: String): T =
    runCatching { enumValueOf<T>(value.trim().uppercase()) }
        .getOrElse { throw DomainValidationException(message) }

private fun parseUuid(value: String, message: String): UUID =
    runCatching { UUID.fromString(value) }
        .getOrElse { throw DomainValidationException(message) }
