package com.nexa.finance.api

import com.nexa.finance.application.goal.GoalProgress
import com.nexa.finance.application.goal.GoalService
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.goal.GoalId
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
import io.ktor.server.routing.route
import java.time.LocalDate
import java.util.UUID

fun Route.goalRoutes(service: GoalService) {
    authenticate("session-bearer") {
        route("/api/v1/goals") {
            post {
                val userId = call.principalUserId()
                val request = call.receive<CreateGoalRequest>()
                val created = service.create(
                    userId = userId,
                    name = request.name,
                    targetAmountMinor = request.targetAmountMinor,
                    currency = parseCurrency(request.currency),
                    targetDate = request.targetDate?.let { parseDate(it, "targetDate") },
                    description = request.description,
                )
                call.respond(HttpStatusCode.Created, created.toResponse())
            }

            get {
                call.respond(service.list(call.principalUserId()).map { it.toResponse() })
            }

            get("/{id}") {
                val userId = call.principalUserId()
                val goalId = parseGoalId(call.parameters["id"])
                call.respond(
                    GoalDetailResponse(
                        goal = service.get(userId, goalId).toResponse(),
                        contributions = service.contributions(userId, goalId).map { it.toResponse() },
                    )
                )
            }

            post("/{id}/contributions") {
                val userId = call.principalUserId()
                val request = call.receive<CreateGoalContributionRequest>()
                val (progress, contribution) = service.contribute(
                    userId = userId,
                    goalId = parseGoalId(call.parameters["id"]),
                    amountMinor = request.amountMinor,
                    currency = parseCurrency(request.currency),
                    occurredOn = parseDate(request.occurredOn, "occurredOn"),
                )
                call.respond(
                    GoalDetailResponse(
                        goal = progress.toResponse(),
                        contributions = service.contributions(userId, progress.goal.id).map { it.toResponse() },
                    )
                )
            }

            post("/{id}/pause") {
                call.respond(service.pause(call.principalUserId(), parseGoalId(call.parameters["id"])).toResponse())
            }

            post("/{id}/resume") {
                call.respond(service.resume(call.principalUserId(), parseGoalId(call.parameters["id"])).toResponse())
            }

            delete("/{id}") {
                call.respond(service.cancel(call.principalUserId(), parseGoalId(call.parameters["id"])).toResponse())
            }
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.principalUserId(): UserId =
    principal<UserIdPrincipal>()?.name?.let {
        runCatching { UserId(UUID.fromString(it)) }
            .getOrElse { throw DomainValidationException("Invalid authenticated principal.") }
    } ?: throw DomainValidationException("Invalid authenticated principal.")

private fun parseGoalId(value: String?): GoalId =
    runCatching { UUID.fromString(value ?: "") }
        .getOrElse { throw DomainValidationException("Goal ID is invalid.") }

private fun parseDate(value: String, field: String): LocalDate =
    runCatching { LocalDate.parse(value) }
        .getOrElse { throw DomainValidationException("Goal $field is invalid.") }

private fun parseCurrency(value: String): CurrencyCode =
    runCatching { enumValueOf<CurrencyCode>(value.trim().uppercase()) }
        .getOrElse { throw DomainValidationException("Goal currency is invalid.") }

private fun GoalProgress.toResponse(): GoalResponse =
    GoalResponse(
        id = goal.id.toString(),
        name = goal.name,
        targetAmountMinor = goal.targetAmount.amountInMinor,
        currentAmountMinor = goal.currentAmount.amountInMinor,
        remainingMinor = remainingMinor,
        progressPercent = progressPercent,
        currency = goal.targetAmount.currency.name,
        targetDate = goal.targetDate?.toString(),
        description = goal.description,
        status = goal.status.name,
        projectedDate = projection.projectedDate?.toString(),
        averageMonthlyContributionMinor = projection.averageMonthlyContributionMinor,
        projectionStatus = projection.status.name,
    )

private fun com.nexa.finance.domain.goal.GoalContribution.toResponse(): GoalContributionResponse =
    GoalContributionResponse(
        id = id.toString(),
        goalId = goalId.toString(),
        amountMinor = amount.amountInMinor,
        currency = amount.currency.name,
        occurredOn = occurredOn.toString(),
        createdAt = createdAt.toString(),
    )
