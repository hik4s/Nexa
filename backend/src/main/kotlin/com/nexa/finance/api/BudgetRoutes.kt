package com.nexa.finance.api

import com.nexa.finance.application.budget.BudgetProgress
import com.nexa.finance.application.budget.BudgetService
import com.nexa.finance.domain.category.CategoryId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
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

fun Route.budgetRoutes(service: BudgetService) {
    authenticate("session-bearer") {
        route("/api/v1/budgets") {
            post {
                val userId = call.principalUserId()
                val request = call.receive<CreateBudgetRequest>()
                val created = service.create(
                    userId = userId,
                    amountMinor = request.amountMinor,
                    currency = parseCurrency(request.currency),
                    startOn = parseDate(request.startOn, "startOn"),
                    endOn = parseDate(request.endOn, "endOn"),
                    categoryId = request.categoryId?.let(::parseCategoryId),
                    description = request.description,
                )
                call.respond(HttpStatusCode.Created, service.get(userId, created.id).toResponse())
            }

            get {
                call.respond(service.list(call.principalUserId()).map { it.toResponse() })
            }

            get("/{id}") {
                val userId = call.principalUserId()
                call.respond(service.get(userId, parseUuid(call.parameters["id"])).toResponse())
            }

            put("/{id}") {
                val userId = call.principalUserId()
                val request = call.receive<UpdateBudgetRequest>()
                call.respond(
                    service.update(
                        userId = userId,
                        budgetId = parseUuid(call.parameters["id"]),
                        amountMinor = request.amountMinor,
                        currency = parseCurrency(request.currency),
                        startOn = parseDate(request.startOn, "startOn"),
                        endOn = parseDate(request.endOn, "endOn"),
                        categoryId = request.categoryId?.let(::parseCategoryId),
                        description = request.description,
                    ).toResponse()
                )
            }

            post("/{id}/pause") {
                call.respond(service.pause(call.principalUserId(), parseUuid(call.parameters["id"])).toStatusResponse())
            }
            post("/{id}/resume") {
                call.respond(service.resume(call.principalUserId(), parseUuid(call.parameters["id"])).toStatusResponse())
            }
            post("/{id}/complete") {
                call.respond(service.complete(call.principalUserId(), parseUuid(call.parameters["id"])).toStatusResponse())
            }
            delete("/{id}") {
                call.respond(service.cancel(call.principalUserId(), parseUuid(call.parameters["id"])).toStatusResponse())
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.principalUserId(): UserId =
    principal<UserIdPrincipal>()?.name?.let {
        runCatching { UserId(UUID.fromString(it)) }
            .getOrElse { throw DomainValidationException("Invalid authenticated principal.") }
    } ?: throw DomainValidationException("Invalid authenticated principal.")

private fun parseUuid(value: String?): UUID =
    runCatching { UUID.fromString(value ?: "") }
        .getOrElse { throw DomainValidationException("Budget ID is invalid.") }

private fun parseCategoryId(value: String): CategoryId =
    runCatching { CategoryId(UUID.fromString(value)) }
        .getOrElse { throw DomainValidationException("Budget category ID is invalid.") }

private fun parseDate(value: String, field: String): LocalDate =
    runCatching { LocalDate.parse(value) }
        .getOrElse { throw DomainValidationException("Budget $field is invalid.") }

private fun parseCurrency(value: String): CurrencyCode =
    runCatching { enumValueOf<CurrencyCode>(value.trim().uppercase()) }
        .getOrElse { throw DomainValidationException("Budget currency is invalid.") }

private fun BudgetProgress.toResponse(): BudgetResponse =
    BudgetResponse(
        id = budget.id.toString(),
        amountMinor = budget.amount.amountInMinor,
        consumedMinor = consumedMinor,
        remainingMinor = remainingMinor,
        progressPercent = progressPercent,
        alertLevel = alertLevel.name,
        currency = budget.amount.currency.name,
        startOn = budget.startOn.toString(),
        endOn = budget.endOn.toString(),
        categoryId = budget.categoryId?.value?.toString(),
        description = budget.description,
        status = budget.status.name,
    )

private fun BudgetProgress.toStatusResponse(): BudgetStatusResponse =
    BudgetStatusResponse(toResponse())
