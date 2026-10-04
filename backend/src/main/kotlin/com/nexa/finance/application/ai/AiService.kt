package com.nexa.finance.application.ai

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.infrastructure.ai.JdbcAiUsageRepository
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

class AiService(
    private val contextBuilder: AiContextBuilder,
    private val provider: AiProvider,
    private val usage: JdbcAiUsageRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val dailyLimit: Int = 20,
) {
    fun analyze(userId: UserId, month: YearMonth): AiAnalysis {
        consume(userId)
        return provider.analyze(contextBuilder.build(userId, month))
    }

    fun ask(userId: UserId, month: YearMonth, question: String): AiAnswer {
        if (question.length > 500) throw DomainValidationException("AI question cannot exceed 500 characters.")
        consume(userId)
        return provider.answer(contextBuilder.build(userId, month), question)
    }

    fun usageCount(userId: UserId): Int =
        usage.count(userId, LocalDate.now(clock))

    private fun consume(userId: UserId) {
        val date = LocalDate.now(clock)
        if (!usage.tryConsume(userId, date, dailyLimit)) {
            throw DomainValidationException("Daily AI usage limit reached.")
        }
    }
}
