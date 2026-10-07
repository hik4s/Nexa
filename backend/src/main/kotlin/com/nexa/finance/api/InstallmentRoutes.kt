package com.nexa.finance.api

import com.nexa.finance.application.installment.InstallmentService
import com.nexa.finance.domain.category.CategoryId
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.installment.Installment
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

fun Route.installmentRoutes(service: InstallmentService) {
    authenticate("session-bearer") {
        route("/api/v1/installments") {
            post {
                val r = call.receive<CreateInstallmentRequest>()
                val plan = service.create(
                    call.principalUserId(),
                    Money(r.totalAmountMinor, currency(r.currency)),
                    r.count,
                    date(r.firstDueOn, "First due date is invalid."),
                    r.description,
                    r.categoryId?.let { CategoryId(uuid(it, "Category ID is invalid.")) },
                )
                call.respond(HttpStatusCode.Created, plan.response())
            }
            get { call.respond(service.list(call.principalUserId()).map { it.response() }) }
            get("/{id}") {
                call.respond(service.get(call.principalUserId(), uuid(call.parameters["id"] ?: "", "Installment ID is invalid.")).response())
            }
            post("/{id}/items/{number}/complete") {
                val number = call.parameters["number"]?.toIntOrNull()
                    ?: throw DomainValidationException("Installment number is invalid.")
                call.respond(service.complete(call.principalUserId(), uuid(call.parameters["id"] ?: "", "Installment ID is invalid."), number).response())
            }
            post("/{id}/cancel") {
                call.respond(service.cancel(call.principalUserId(), uuid(call.parameters["id"] ?: "", "Installment ID is invalid.")).response())
            }
        }
    }
}

private fun Installment.response() = InstallmentResponse(
    id.toString(), totalAmount.amountInMinor, totalAmount.currency.name, installmentCount,
    firstDueOn.toString(), description, categoryId?.value?.toString(), status.name,
    items.map { InstallmentItemResponse(it.number, it.amount.amountInMinor, it.amount.currency.name, it.dueOn.toString(), it.status.name) }
)

private fun io.ktor.server.application.ApplicationCall.principalUserId(): UserId =
    principal<UserIdPrincipal>()?.name?.let {
        UserId(runCatching { UUID.fromString(it) }.getOrElse { throw DomainValidationException("Invalid authenticated principal.") })
    } ?: throw DomainValidationException("Invalid authenticated principal.")

private fun currency(value: String): com.nexa.finance.domain.common.CurrencyCode =
    runCatching { com.nexa.finance.domain.common.CurrencyCode.valueOf(value.trim().uppercase()) }
        .getOrElse { throw DomainValidationException("Currency is invalid.") }

private fun uuid(value: String, message: String): UUID =
    runCatching { UUID.fromString(value) }.getOrElse { throw DomainValidationException(message) }

private fun date(value: String, message: String): LocalDate =
    runCatching { LocalDate.parse(value) }.getOrElse { throw DomainValidationException(message) }

