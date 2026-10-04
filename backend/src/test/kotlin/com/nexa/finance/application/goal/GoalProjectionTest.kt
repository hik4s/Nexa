package com.nexa.finance.application.goal

import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.goal.Goal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class GoalProjectionTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-04T12:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 10, 4)

    @Test
    fun withoutContributionProjectionIsPending() {
        val goal = Goal.create(
            userId = UserId.new(),
            name = "Viagem",
            targetAmount = Money(100_000, CurrencyCode.BRL),
            targetDate = LocalDate.of(2027, 1, 15),
            clock = clock,
        )
        val projection = GoalProjectionCalculator.calculate(goal, today)
        assertEquals(GoalProjectionStatus.NO_CONTRIBUTION, projection.status)
        assertEquals(null, projection.projectedDate)
    }

    @Test
    fun projectsDateFromAverageMonthlyPace() {
        val goal = Goal.create(
            userId = UserId.new(),
            name = "Viagem",
            targetAmount = Money(100_000, CurrencyCode.BRL),
            currentAmount = Money(40_000, CurrencyCode.BRL),
            targetDate = LocalDate.of(2027, 1, 15),
            clock = clock,
        )
        val projection = GoalProjectionCalculator.calculate(goal, today)
        assertEquals(40_000L, projection.averageMonthlyContributionMinor)
        assertEquals(LocalDate.of(2026, 12, 4), projection.projectedDate)
        assertEquals(GoalProjectionStatus.ON_TRACK, projection.status)
    }

    @Test
    fun flagsProjectedDateAfterDeadlineAsAtRisk() {
        val goal = Goal.create(
            userId = UserId.new(),
            name = "Viagem",
            targetAmount = Money(100_000, CurrencyCode.BRL),
            currentAmount = Money(40_000, CurrencyCode.BRL),
            targetDate = LocalDate.of(2026, 11, 30),
            clock = clock,
        )
        val projection = GoalProjectionCalculator.calculate(goal, today)
        assertEquals(LocalDate.of(2026, 12, 4), projection.projectedDate)
        assertEquals(GoalProjectionStatus.AT_RISK, projection.status)
    }

    @Test
    fun supportsGoalWithoutDeadline() {
        val goal = Goal.create(
            userId = UserId.new(),
            name = "Viagem",
            targetAmount = Money(100_000, CurrencyCode.BRL),
            currentAmount = Money(40_000, CurrencyCode.BRL),
            clock = clock,
        )
        val projection = GoalProjectionCalculator.calculate(goal, today)
        assertEquals(GoalProjectionStatus.NO_DEADLINE, projection.status)
    }

    @Test
    fun achievedGoalHasAchievedProjection() {
        val goal = Goal.create(
            userId = UserId.new(),
            name = "Viagem",
            targetAmount = Money(100_000, CurrencyCode.BRL),
            currentAmount = Money(100_000, CurrencyCode.BRL),
            clock = clock,
        )
        val projection = GoalProjectionCalculator.calculate(goal, today)
        assertEquals(GoalProjectionStatus.ACHIEVED, projection.status)
        assertEquals(today, projection.projectedDate)
    }

    @Test
    fun pausedGoalDoesNotProject() {
        val goal = Goal.create(
            userId = UserId.new(),
            name = "Viagem",
            targetAmount = Money(100_000, CurrencyCode.BRL),
            currentAmount = Money(40_000, CurrencyCode.BRL),
            targetDate = LocalDate.of(2027, 1, 15),
            clock = clock,
        ).pause()
        val projection = GoalProjectionCalculator.calculate(goal, today)
        assertEquals(GoalProjectionStatus.PAUSED, projection.status)
        assertEquals(null, projection.projectedDate)
    }
}
