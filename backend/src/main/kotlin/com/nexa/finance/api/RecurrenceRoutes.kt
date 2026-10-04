package com.nexa.finance.api

import com.nexa.finance.application.recurrence.RecurrenceService
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.category.CategoryId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.recurrence.Recurrence
import com.nexa.finance.domain.recurrence.RecurrenceFrequency
import com.nexa.finance.domain.transaction.TransactionType
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

fun Route.recurrenceRoutes(service: RecurrenceService) {
    authenticate("session-bearer") {
        route("/api/v1/recurrences") {
            post {
                val r = call.receive<CreateRecurrenceRequest>()
                val recurrence = service.create(
                    call.principalUserId(), AccountId(uuid(r.accountId, "Account ID is invalid.")),
                    Money(r.amountMinor, enum<CurrencyCode>(r.currency, "Currency is invalid.")),
                    enum<TransactionType>(r.transactionType, "Transaction type is invalid."),
                    enum<RecurrenceFrequency>(r.frequency, "Frequency is invalid."),
                    r.interval, date(r.startOn, "Start date is invalid."), r.endOn?.let { date(it, "End date is invalid.") },
                    r.maxOccurrences, r.description, r.categoryId?.let { CategoryId(uuid(it, "Category ID is invalid.")) },
                )
                call.respond(HttpStatusCode.Created, recurrence.response())
            }
            get { call.respond(service.list(call.principalUserId()).map { it.response() }) }
            get("/{id}") { call.respond(service.get(call.principalUserId(), uuid(call.parameters["id"] ?: "", "Recurrence ID is invalid.")).response()) }
            get("/{id}/occurrences") {
                val id = uuid(call.parameters["id"] ?: "", "Recurrence ID is invalid.")
                val from = date(call.request.queryParameters["from"] ?: throw DomainValidationException("from is required."), "from date is invalid.")
                val to = date(call.request.queryParameters["to"] ?: throw DomainValidationException("to is required."), "to date is invalid.")
                call.respond(RecurrenceOccurrencesResponse(service.occurrences(call.principalUserId(), id, from, to).map { RecurrenceOccurrenceResponse(it.number, it.scheduledOn.toString()) }))
            }
            post("/{id}/pause") { call.respond(service.pause(call.principalUserId(), uuid(call.parameters["id"] ?: "", "Recurrence ID is invalid.")).response()) }
            post("/{id}/resume") { call.respond(service.resume(call.principalUserId(), uuid(call.parameters["id"] ?: "", "Recurrence ID is invalid.")).response()) }
            post("/{id}/cancel") { call.respond(service.cancel(call.principalUserId(), uuid(call.parameters["id"] ?: "", "Recurrence ID is invalid.")).response()) }
            post("/{id}/occurrences/{number}/execute") {
                val id = uuid(call.parameters["id"] ?: "", "Recurrence ID is invalid.")
                val number = call.parameters["number"]?.toIntOrNull() ?: throw DomainValidationException("Occurrence number is invalid.")
                call.respond(HttpStatusCode.Created, service.execute(call.principalUserId(), id, number).toResponse())
            }
        }
    }
}

private fun Recurrence.response() = RecurrenceResponse(
    id.toString(), accountId.value.toString(), amount.amountInMinor, amount.currency.name, transactionType.name,
    frequency.name, interval, startOn.toString(), endOn?.toString(), maxOccurrences, description, categoryId?.value?.toString(), status.name
)

private fun com.nexa.finance.domain.transaction.Transaction.toResponse() = TransactionResponse(
    id.toString(), accountId.value.toString(), amount.amountInMinor, amount.currency.name, type.name, direction.name,
    status.name, occurredOn.toString(), description, categoryId?.value?.toString(), source.name
)

private fun io.ktor.server.application.ApplicationCall.principalUserId(): UserId =
    principal<UserIdPrincipal>()?.name?.let { UserId(runCatching { UUID.fromString(it) }.getOrElse { throw DomainValidationException("Invalid authenticated principal.") }) }
        ?: throw DomainValidationException("Invalid authenticated principal.")

private inline fun <reified T : Enum<T>> enum(value: String, message: String): T =
    runCatching { enumValueOf<T>(value.trim().uppercase()) }.getOrElse { throw DomainValidationException(message) }

private fun uuid(value: String, message: String): UUID =
    runCatching { UUID.fromString(value) }.getOrElse { throw DomainValidationException(message) }

private fun date(value: String, message: String): LocalDate =
    runCatching { LocalDate.parse(value) }.getOrElse { throw DomainValidationException(message) }


