package com.nexa.finance.domain.budget

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

class BudgetTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-03T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun createsActiveBudget() {
        val budget = Budget.create(
            userId = UserId.new(),
            amount = Money(100_00, CurrencyCode.BRL),
            startOn = LocalDate.of(2026, 10, 1),
            endOn = LocalDate.of(2026, 10, 31),
            clock = clock,
        )
        assertEquals(BudgetStatus.ACTIVE, budget.status)
    }

    @Test
    fun rejectsInvalidPeriod() {
        assertFailsWith<DomainValidationException> {
            Budget.create(
                userId = UserId.new(),
                amount = Money(100_00, CurrencyCode.BRL),
                startOn = LocalDate.of(2026, 10, 31),
                endOn = LocalDate.of(2026, 10, 1),
                clock = clock,
            )
        }
    }

    @Test
    fun pausesAndResumes() {
        val budget = Budget.create(
            userId = UserId.new(),
            amount = Money(100_00, CurrencyCode.BRL),
            startOn = LocalDate.of(2026, 10, 1),
            endOn = LocalDate.of(2026, 10, 31),
            clock = clock,
        )
        assertEquals(BudgetStatus.PAUSED, budget.pause().status)
        assertEquals(BudgetStatus.ACTIVE, budget.pause().resume().status)
    }
}
