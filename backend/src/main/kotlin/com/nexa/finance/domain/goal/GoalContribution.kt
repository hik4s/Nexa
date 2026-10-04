package com.nexa.finance.domain.goal

import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class GoalContribution(
    val id: UUID,
    val goalId: GoalId,
    val userId: UserId,
    val amount: Money,
    val occurredOn: LocalDate,
    val createdAt: Instant,
) {
    init {
        if (amount.amountInMinor <= 0) {
            throw DomainValidationException("Goal contribution amount must be greater than zero.")
        }
    }

    companion object {
        fun create(
            goalId: GoalId,
            userId: UserId,
            amount: Money,
            occurredOn: LocalDate,
            now: Instant = Instant.now(),
            id: UUID = UUID.randomUUID(),
        ): GoalContribution = GoalContribution(
            id = id,
            goalId = goalId,
            userId = userId,
            amount = amount,
            occurredOn = occurredOn,
            createdAt = now,
        )

        fun restore(
            id: UUID,
            goalId: GoalId,
            userId: UserId,
            amountMinor: Long,
            currency: CurrencyCode,
            occurredOn: LocalDate,
            createdAt: Instant,
        ): GoalContribution = create(
            id = id,
            goalId = goalId,
            userId = userId,
            amount = Money(amountMinor, currency),
            occurredOn = occurredOn,
            now = createdAt,
        )
    }
}
