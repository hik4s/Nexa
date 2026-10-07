package com.nexa.finance.api

import com.nexa.finance.application.recurrence.RecurrenceService
import com.nexa.finance.application.recurrence.RecurrenceSummary
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
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.time.LocalDate
import java.util.UUID

fun Route.recurrenceRoutes(service: RecurrenceService) {
    authenticate("session-bearer") {
        route("/api/v1/recurrences") {
            post {
                val r = call.receive<CreateRecurrenceRequest>()
                val recurrence = service.create(
                    call.principalUserId(),
                    AccountId(uuid(r.accountId, "Account ID is invalid.")),
                    Money(r.amountMinor, enumValue<CurrencyCode>(r.currency, "Currency is invalid.")),
                    enumValue<TransactionType>(r.transactionType, "Transaction type is invalid."),
                    enumValue<RecurrenceFrequency>(r.frequency, "Frequency is invalid."),
                    r.interval,
                    date(r.startOn, "Start date is invalid."),
                    r.endOn?.let { date(it, "End date is invalid.") },
                    r.maxOccurrences,
                    r.description,
                    r.categoryId?.let { CategoryId(uuid(it, "Category ID is invalid.")) },
                )
                call.respond(HttpStatusCode.Created, service.summary(call.principalUserId(), recurrence.id).toResponse())
            }

            get {
                call.respond(service.summaries(call.principalUserId()).map { it.toResponse() })
            }

            get("/candidates") {
                val response = service.candidates(call.principalUserId()).map {
                    RecurrenceCandidateResponse(
                        accountId = it.accountId,
                        amountMinor = it.amountMinor,
                        currency = it.currency,
                        transactionType = it.transactionType.name,
                        frequency = it.frequency.name,
                        description = it.description,
                        categoryId = it.categoryId,
                        occurrenceCount = it.occurrenceCount,
                        confidencePercent = it.confidencePercent,
                        nextOccurrenceOn = it.nextOccurrenceOn.toString(),
                    )
                }
                call.respond(response)
            }

            get("/{id}") {
                call.respond(
                    service.summary(
                        call.principalUserId(),
                        uuid(call.parameters["id"] ?: "", "Recurrence ID is invalid."),
                    ).toResponse()
                )
            }

            put("/{id}") {
                val r = call.receive<UpdateRecurrenceRequest>()
                val updated = service.update(
                    ownerId = call.principalUserId(),
                    id = uuid(call.parameters["id"] ?: "", "Recurrence ID is invalid."),
                    accountId = AccountId(uuid(r.accountId, "Account ID is invalid.")),
                    amount = Money(r.amountMinor, enumValue<CurrencyCode>(r.currency, "Currency is invalid.")),
                    transactionType = enumValue<TransactionType>(r.transactionType, "Transaction type is invalid."),
                    frequency = enumValue<RecurrenceFrequency>(r.frequency, "Frequency is invalid."),
                    interval = r.interval,
                    startOn = date(r.startOn, "Start date is invalid."),
                    endOn = r.endOn?.let { date(it, "End date is invalid.") },
                    maxOccurrences = r.maxOccurrences,
                    description = r.description,
                    categoryId = r.categoryId?.let { CategoryId(uuid(it, "Category ID is invalid.")) },
                )
                call.respond(service.summary(call.principalUserId(), updated.id).toResponse())
            }

            get("/{id}/occurrences") {
                val id = uuid(call.parameters["id"] ?: "", "Recurrence ID is invalid.")
                val from = date(
                    call.request.queryParameters["from"]
                        ?: throw DomainValidationException("from is required."),
                    "from date is invalid.",
                )
                val to = date(
                    call.request.queryParameters["to"]
                        ?: throw DomainValidationException("to is required."),
                    "to date is invalid.",
                )
                call.respond(
                    RecurrenceOccurrencesResponse(
                        service.occurrences(call.principalUserId(), id, from, to)
                            .map { RecurrenceOccurrenceResponse(it.number, it.scheduledOn.toString()) }
                    )
                )
            }

            post("/{id}/pause") {
                val userId = call.principalUserId()
                val id = uuid(call.parameters["id"] ?: "", "Recurrence ID is invalid.")
                call.respond(service.summary(userId, service.pause(userId, id).id).toResponse())
            }

            post("/{id}/resume") {
                val userId = call.principalUserId()
                val id = uuid(call.parameters["id"] ?: "", "Recurrence ID is invalid.")
                call.respond(service.summary(userId, service.resume(userId, id).id).toResponse())
            }

            delete("/{id}") {
                val userId = call.principalUserId()
                val id = uuid(call.parameters["id"] ?: "", "Recurrence ID is invalid.")
                call.respond(service.summary(userId, service.cancel(userId, id).id).toResponse())
            }

            route("/{id}/occurrences/{number}") {
                post("/execute") {
                    val id = uuid(call.parameters["id"] ?: "", "Recurrence ID is invalid.")
                    val number = call.parameters["number"]?.toIntOrNull()
                        ?: throw DomainValidationException("Occurrence number is invalid.")
                    call.respond(
                        HttpStatusCode.Created,
                        service.execute(call.principalUserId(), id, number).toResponse()
                    )
                }
            }
        }
    }
}

private fun RecurrenceSummary.toResponse() = RecurrenceResponse(
    id = recurrence.id.toString(),
    accountId = recurrence.accountId.value.toString(),
    amountMinor = recurrence.amount.amountInMinor,
    currency = recurrence.amount.currency.name,
    transactionType = recurrence.transactionType.name,
    frequency = recurrence.frequency.name,
    interval = recurrence.interval,
    startOn = recurrence.startOn.toString(),
    endOn = recurrence.endOn?.toString(),
    maxOccurrences = recurrence.maxOccurrences,
    description = recurrence.description,
    categoryId = recurrence.categoryId?.value?.toString(),
    status = recurrence.status.name,
    nextOccurrenceNumber = nextOccurrenceNumber,
    nextOccurrenceOn = nextOccurrenceOn?.toString(),
    nextOccurrences = nextOccurrences.map { RecurrenceOccurrenceResponse(it.number, it.scheduledOn.toString()) },
    alertLevel = alertLevel.name,
)

private fun com.nexa.finance.domain.transaction.Transaction.toResponse() = TransactionResponse(
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

private fun io.ktor.server.application.ApplicationCall.principalUserId(): UserId =
    principal<UserIdPrincipal>()?.name?.let {
        UserId(
            runCatching { UUID.fromString(it) }
                .getOrElse { throw DomainValidationException("Invalid authenticated principal.") }
        )
    } ?: throw DomainValidationException("Invalid authenticated principal.")

private inline fun <reified T : Enum<T>> enumValue(value: String, message: String): T =
    runCatching { enumValueOf<T>(value.trim().uppercase()) }
        .getOrElse { throw DomainValidationException(message) }

private fun uuid(value: String, message: String): UUID =
    runCatching { UUID.fromString(value) }
        .getOrElse { throw DomainValidationException(message) }

private fun date(value: String, message: String): LocalDate =
    runCatching { LocalDate.parse(value) }
        .getOrElse { throw DomainValidationException(message) }
