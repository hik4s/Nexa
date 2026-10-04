package com.nexa.finance.api

import com.nexa.finance.application.account.AccountService
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.account.AccountType
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
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.accountRoutes(service: AccountService) {
    authenticate("session-bearer") {
        route("/api/v1/accounts") {
            post {
                val userId = call.principalUserId()
                val request = call.receiveValidated<CreateAccountRequest>()
                val type = parseEnum<AccountType>(request.type, "Account type is invalid.")
                val currency = parseEnum<CurrencyCode>(request.currency, "Currency is invalid.")
                val account = service.create(
                    userId,
                    request.name,
                    type,
                    Money(request.openingBalanceMinor, currency),
                )
                call.respond(
                    HttpStatusCode.Created,
                    account.toResponse(service.balance(userId, account.id)),
                )
            }

            get {
                val userId = call.principalUserId()
                call.respond(
                    service.list(userId).map { account ->
                        account.toResponse(service.balance(userId, account.id))
                    },
                )
            }

            post("/{id}/primary") {
                val userId = call.principalUserId()
                val accountId = call.parameters["id"]?.let(::parseUuid)
                    ?: throw DomainValidationException("Account ID is invalid.")
                val account = service.setPrimary(userId, AccountId(accountId))
                call.respond(account.toResponse(service.balance(userId, account.id)))
            }

            get("/{id}") {
                val userId = call.principalUserId()
                val accountId = call.parameters["id"]?.let(::parseUuid)
                    ?: throw DomainValidationException("Account ID is invalid.")
                val account = service.get(userId, AccountId(accountId))
                call.respond(account.toResponse(service.balance(userId, account.id)))
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
    principal<UserIdPrincipal>()?.name?.let {
        runCatching { UserId(UUID.fromString(it)) }
            .getOrElse { throw DomainValidationException("Invalid authenticated principal.") }
    } ?: throw DomainValidationException("Invalid authenticated principal.")

private inline fun <reified T : Enum<T>> parseEnum(value: String, message: String): T =
    runCatching { enumValueOf<T>(value.trim().uppercase()) }
        .getOrElse { throw DomainValidationException(message) }

private fun parseUuid(value: String): UUID =
    runCatching { UUID.fromString(value) }
        .getOrElse { throw DomainValidationException("Account ID is invalid.") }

private fun com.nexa.finance.domain.account.Account.toResponse(balance: Money) =
    AccountResponse(
        id = id.value.toString(),
        name = name,
        type = type.name,
        currency = currency.name,
        openingBalanceMinor = openingBalance.amountInMinor,
        balanceMinor = balance.amountInMinor,
        status = status.name,
        isPrimary = isPrimary,
    )
