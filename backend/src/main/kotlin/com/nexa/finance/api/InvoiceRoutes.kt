package com.nexa.finance.api

import com.nexa.finance.application.invoice.InvoiceService
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.card.CardId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.invoice.InvoiceId
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

fun Route.invoiceRoutes(service: InvoiceService) {
    authenticate("session-bearer") {
        route("/api/v1/invoices") {
            post {
                val userId = call.invoicePrincipalUserId()
                val r = call.receiveInvoice<CreateInvoiceRequest>()
                val invoice = service.create(userId, CardId(invoiceUuid(r.cardId, "Card ID is invalid.")), invoiceDate(r.periodStart), invoiceDate(r.periodEnd), invoiceDate(r.dueOn))
                call.respond(HttpStatusCode.Created, invoice.toInvoiceResponse())
            }
            get {
                val userId = call.invoicePrincipalUserId()
                call.respond(service.list(userId).map { it.toInvoiceResponse() })
            }
            get("/{id}") {
                val userId = call.invoicePrincipalUserId()
                call.respond(service.get(userId, InvoiceId(invoiceUuid(call.parameters["id"] ?: "", "Invoice ID is invalid."))).toInvoiceResponse())
            }
            post("/{id}/close") {
                val userId = call.invoicePrincipalUserId()
                call.respond(service.close(userId, InvoiceId(invoiceUuid(call.parameters["id"] ?: "", "Invoice ID is invalid."))).toInvoiceResponse())
            }
            post("/{id}/overdue") {
                val userId = call.invoicePrincipalUserId()
                call.respond(service.overdue(userId, InvoiceId(invoiceUuid(call.parameters["id"] ?: "", "Invoice ID is invalid."))).toInvoiceResponse())
            }
            post("/{id}/payments") {
                val userId = call.invoicePrincipalUserId()
                val r = call.receiveInvoice<InvoicePaymentRequest>()
                val invoice = service.pay(userId, InvoiceId(invoiceUuid(call.parameters["id"] ?: "", "Invoice ID is invalid.")), AccountId(invoiceUuid(r.sourceAccountId, "Source account ID is invalid.")), Money(r.amountMinor, invoiceEnum<CurrencyCode>(r.currency, "Currency is invalid.")), invoiceDate(r.occurredOn), invoiceEnum<TransactionSource>(r.source, "Transaction source is invalid."))
                call.respond(invoice.toInvoiceResponse())
            }
        }
    }
}

private fun com.nexa.finance.domain.invoice.Invoice.toInvoiceResponse() = InvoiceResponse(id.value.toString(), cardId.value.toString(), periodStart.toString(), periodEnd.toString(), dueOn.toString(), totalAmount.amountInMinor, paidAmount.amountInMinor, remainingAmount().amountInMinor, totalAmount.currency.name, status.name, transactionIds.map(UUID::toString), paymentTransactionIds.map(UUID::toString))
private fun io.ktor.server.application.ApplicationCall.invoicePrincipalUserId(): UserId = principal<UserIdPrincipal>()?.name?.let { runCatching { UserId(UUID.fromString(it)) }.getOrElse { throw DomainValidationException("Invalid authenticated principal.") } } ?: throw DomainValidationException("Invalid authenticated principal.")
private suspend inline fun <reified T> io.ktor.server.application.ApplicationCall.receiveInvoice(): T = try { receive() } catch (_: Exception) { throw DomainValidationException("Request body is invalid.") }
private fun invoiceDate(value: String): LocalDate = runCatching { LocalDate.parse(value) }.getOrElse { throw DomainValidationException("Date is invalid.") }
private inline fun <reified T : Enum<T>> invoiceEnum(value: String, message: String): T = runCatching { enumValueOf<T>(value.trim().uppercase()) }.getOrElse { throw DomainValidationException(message) }
private fun invoiceUuid(value: String, message: String): UUID = runCatching { UUID.fromString(value) }.getOrElse { throw DomainValidationException(message) }