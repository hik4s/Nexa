package com.nexa.finance.domain.goal

import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GoalTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-03T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun contributionAchievesGoalAtTarget() {
        val goal = Goal.create(
            userId = UserId.new(),
            name = "Reserva",
            targetAmount = Money(100_00, CurrencyCode.BRL),
            clock = clock,
        )
        val achieved = goal.addContribution(Money(100_00, CurrencyCode.BRL))
        assertEquals(GoalStatus.ACHIEVED, achieved.status)
        assertEquals(1.0, achieved.progressRatio)
    }

    @Test
    fun cannotOverfundGoal() {
        val goal = Goal.create(
            userId = UserId.new(),
            name = "Reserva",
            targetAmount = Money(100_00, CurrencyCode.BRL),
            clock = clock,
        )
        assertFailsWith<DomainValidationException> {
            goal.addContribution(Money(100_01, CurrencyCode.BRL))
        }
    }

    @Test
    fun pausedGoalRejectsContribution() {
        val goal = Goal.create(
            userId = UserId.new(),
            name = "Reserva",
            targetAmount = Money(100_00, CurrencyCode.BRL),
            clock = clock,
        ).pause()
        assertFailsWith<DomainValidationException> {
            goal.addContribution(Money(10_00, CurrencyCode.BRL))
        }
    }

    @Test
    fun removingContributionReopensAchievedGoal() {
        val goal = Goal.create(
            userId = UserId.new(),
            name = "Reserva",
            targetAmount = Money(100_00, CurrencyCode.BRL),
            currentAmount = Money(100_00, CurrencyCode.BRL),
            clock = clock,
        )
        val reopened = goal.removeContribution(Money(10_00, CurrencyCode.BRL))
        assertEquals(GoalStatus.ACTIVE, reopened.status)
        assertEquals(Money(90_00, CurrencyCode.BRL), reopened.currentAmount)
    }
}
