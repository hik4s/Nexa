package com.nexa.finance.api

import com.nexa.finance.application.ai.AiService
import com.nexa.finance.domain.common.DomainValidationException
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
import java.time.YearMonth
import java.util.UUID

fun Route.aiRoutes(service: AiService, dailyLimit: Int = 20) {
    authenticate("session-bearer") {
        route("/api/v1/ai") {
            get("/analysis") {
                val month = parseMonth(call.request.queryParameters["month"])
                call.respond(service.analyze(call.principalUserId(), month).toResponse())
            }

            post("/questions") {
                val request = call.receive<AiQuestionRequest>()
                val month = parseMonth(request.month)
                call.respond(service.ask(call.principalUserId(), month, request.question).toResponse())
            }

            get("/usage") {
                call.respond(
                    AiUsageResponse(
                        usedToday = service.usageCount(call.principalUserId()),
                        dailyLimit = dailyLimit,
                    )
                )
            }
        }
    }
}

private fun parseMonth(value: String?): YearMonth =
    runCatching { if (value.isNullOrBlank()) YearMonth.now() else YearMonth.parse(value) }
        .getOrElse { throw DomainValidationException("AI month is invalid.") }

private fun io.ktor.server.application.ApplicationCall.principalUserId(): UserId =
    principal<UserIdPrincipal>()?.name?.let {
        runCatching { UserId(UUID.fromString(it)) }
            .getOrElse { throw DomainValidationException("Invalid authenticated principal.") }
    } ?: throw DomainValidationException("Invalid authenticated principal.")

private fun com.nexa.finance.application.ai.AiAnalysis.toResponse() = AiAnalysisResponse(
    month = month.toString(),
    provider = provider,
    monthlySummary = monthlySummary,
    contextFactCount = contextFactCount,
    insights = insights.map {
        AiInsightResponse(
            id = it.id,
            level = it.level.name,
            title = it.title,
            message = it.message,
            sourceFactIds = it.sourceFactIds,
        )
    },
)

private fun com.nexa.finance.application.ai.AiAnswer.toResponse() =
    AiAnswerResponse(answer = answer, sourceFactIds = sourceFactIds)
