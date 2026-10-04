package com.nexa.finance.api

import com.nexa.finance.application.card.CardService
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.card.CardBrand
import com.nexa.finance.domain.card.CardId
import com.nexa.finance.domain.card.CardStatus
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.transaction.TransactionSource
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
import java.time.LocalDate
import java.util.UUID

fun Route.cardRoutes(service: CardService) {
    authenticate("session-bearer") {
        route("/api/v1/cards") {
            post {
                val userId = call.cardPrincipalUserId()
                val r = call.receiveCard<CreateCardRequest>()
                val card = service.create(userId, AccountId(cardUuid(r.liabilityAccountId, "Liability account ID is invalid.")), r.name, cardEnum<CardBrand>(r.brand, "Card brand is invalid."), r.lastFourDigits, Money(r.creditLimitMinor, cardEnum<CurrencyCode>(r.currency, "Currency is invalid.")), r.closingDay, r.dueDay)
                call.respond(HttpStatusCode.Created, card.toCardResponse(service.availableLimit(userId, card.id)))
            }
            get {
                val userId = call.cardPrincipalUserId()
                call.respond(service.list(userId).map { it.toCardResponse(service.availableLimit(userId, it.id)) })
            }
            get("/{id}") {
                val userId = call.cardPrincipalUserId()
                val card = service.get(userId, CardId(cardUuid(call.parameters["id"] ?: "", "Card ID is invalid.")))
                call.respond(card.toCardResponse(service.availableLimit(userId, card.id)))
            }
            post("/{id}/status") {
                val userId = call.cardPrincipalUserId()
                val id = CardId(cardUuid(call.parameters["id"] ?: "", "Card ID is invalid."))
                val r = call.receiveCard<CardStatusRequest>()
                val card = service.updateStatus(userId, id, cardEnum<CardStatus>(r.status, "Card status is invalid."))
                call.respond(card.toCardResponse(service.availableLimit(userId, card.id)))
            }
            post("/{id}/purchases") {
                val userId = call.cardPrincipalUserId()
                val id = CardId(cardUuid(call.parameters["id"] ?: "", "Card ID is invalid."))
                val r = call.receiveCard<CardPurchaseRequest>()
                val tx = service.purchase(userId, id, Money(r.amountMinor, cardEnum<CurrencyCode>(r.currency, "Currency is invalid.")), runCatching { LocalDate.parse(r.occurredOn) }.getOrElse { throw DomainValidationException("Purchase date is invalid.") }, null, r.description, cardEnum<TransactionSource>(r.source, "Transaction source is invalid."), r.post, r.installmentCount)
                call.respond(HttpStatusCode.Created, tx.toCardTransactionResponse())
            }
        }
    }
}

private fun com.nexa.finance.domain.card.Card.toCardResponse(available: Money) = CardResponse(id.value.toString(), liabilityAccountId.value.toString(), name, brand.name, lastFourDigits, creditLimit.amountInMinor, available.amountInMinor, creditLimit.currency.name, closingDay, dueDay, status.name)
private fun com.nexa.finance.domain.transaction.Transaction.toCardTransactionResponse() = TransactionResponse(id.toString(), accountId.value.toString(), amount.amountInMinor, amount.currency.name, type.name, direction.name, status.name, occurredOn.toString(), description, categoryId?.value?.toString(), source.name, installmentId?.toString(), installmentNumber)
private fun io.ktor.server.application.ApplicationCall.cardPrincipalUserId(): UserId = principal<UserIdPrincipal>()?.name?.let { runCatching { UserId(UUID.fromString(it)) }.getOrElse { throw DomainValidationException("Invalid authenticated principal.") } } ?: throw DomainValidationException("Invalid authenticated principal.")
private suspend inline fun <reified T> io.ktor.server.application.ApplicationCall.receiveCard(): T = try { receive() } catch (_: Exception) { throw DomainValidationException("Request body is invalid.") }
private inline fun <reified T : Enum<T>> cardEnum(value: String, message: String): T = runCatching { enumValueOf<T>(value.trim().uppercase()) }.getOrElse { throw DomainValidationException(message) }
private fun cardUuid(value: String, message: String): UUID = runCatching { UUID.fromString(value) }.getOrElse { throw DomainValidationException(message) }