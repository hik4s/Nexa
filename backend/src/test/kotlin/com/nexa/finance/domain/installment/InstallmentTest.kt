package com.nexa.finance.domain.installment

import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class InstallmentTest {
    private val fixedNow = Instant.parse("2026-10-03T12:00:00Z")
    private val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
    private val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))

    @Test
    fun createsExactScheduleAndPreservesTotal() {
        val plan = Installment.create(
            userId = userId,
            totalAmount = Money(1_000L, CurrencyCode.BRL),
            installmentCount = 3,
            firstDueOn = LocalDate.of(2026, 10, 15),
            clock = clock,
        )

        assertEquals(listOf(334L, 333L, 333L), plan.items.map { it.amount.amountInMinor })
        assertEquals(1_000L, plan.items.sumOf { it.amount.amountInMinor })
        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 15),
                LocalDate.of(2026, 11, 15),
                LocalDate.of(2026, 12, 15),
            ),
            plan.items.map { it.dueOn },
        )
        assertEquals(InstallmentPlanStatus.ACTIVE, plan.status)
    }

    @Test
    fun clampsMonthEndWithoutDriftingTheOriginalDay() {
        val plan = Installment.create(
            userId = userId,
            totalAmount = Money(900L, CurrencyCode.BRL),
            installmentCount = 3,
            firstDueOn = LocalDate.of(2027, 1, 31),
            clock = clock,
        )

        assertEquals(
            listOf(
                LocalDate.of(2027, 1, 31),
                LocalDate.of(2027, 2, 28),
                LocalDate.of(2027, 3, 31),
            ),
            plan.items.map { it.dueOn },
        )
    }

    @Test
    fun normalizesOptionalDescription() {
        val plan = Installment.create(
            userId = userId,
            totalAmount = Money(600L, CurrencyCode.BRL),
            installmentCount = 2,
            firstDueOn = LocalDate.of(2026, 10, 10),
            description = "  Notebook  ",
            clock = clock,
        )

        assertEquals("Notebook", plan.description)
    }

    @Test
    fun blankDescriptionBecomesNull() {
        val plan = Installment.create(
            userId = userId,
            totalAmount = Money(600L, CurrencyCode.BRL),
            installmentCount = 2,
            firstDueOn = LocalDate.of(2026, 10, 10),
            description = "   ",
            clock = clock,
        )

        assertNull(plan.description)
    }

    @Test
    fun completesItemsAndClosesPlanAfterLastCompletion() {
        val plan = Installment.create(
            userId = userId,
            totalAmount = Money(600L, CurrencyCode.BRL),
            installmentCount = 2,
            firstDueOn = LocalDate.of(2026, 10, 10),
            clock = clock,
        )

        val first = plan.completeItem(1, fixedNow.plusSeconds(60))
        assertEquals(InstallmentPlanStatus.ACTIVE, first.status)
        assertEquals(InstallmentItemStatus.COMPLETED, first.item(1).status)
        assertEquals(InstallmentItemStatus.SCHEDULED, first.item(2).status)

        val completed = first.completeItem(2, fixedNow.plusSeconds(120))
        assertEquals(InstallmentPlanStatus.COMPLETED, completed.status)
        assertEquals(InstallmentItemStatus.COMPLETED, completed.item(1).status)
        assertEquals(InstallmentItemStatus.COMPLETED, completed.item(2).status)
    }

    @Test
    fun cancellationCancelsAllScheduledItems() {
        val plan = Installment.create(
            userId = userId,
            totalAmount = Money(600L, CurrencyCode.BRL),
            installmentCount = 2,
            firstDueOn = LocalDate.of(2026, 10, 10),
            clock = clock,
        )

        val cancelled = plan.cancel(fixedNow.plusSeconds(60))
        assertEquals(InstallmentPlanStatus.CANCELLED, cancelled.status)
        assertEquals(
            listOf(InstallmentItemStatus.CANCELLED, InstallmentItemStatus.CANCELLED),
            cancelled.items.map { it.status },
        )
    }

    @Test
    fun rejectsCancellationAfterAnyCompletedItem() {
        val plan = Installment.create(
            userId = userId,
            totalAmount = Money(600L, CurrencyCode.BRL),
            installmentCount = 2,
            firstDueOn = LocalDate.of(2026, 10, 10),
            clock = clock,
        ).completeItem(1, fixedNow.plusSeconds(60))

        assertFailsWith<DomainValidationException> {
            plan.cancel(fixedNow.plusSeconds(120))
        }
    }

    @Test
    fun rejectsNonPositiveTotal() {
        assertFailsWith<DomainValidationException> {
            Installment.create(
                userId = userId,
                totalAmount = Money(0L, CurrencyCode.BRL),
                installmentCount = 2,
                firstDueOn = LocalDate.of(2026, 10, 10),
                clock = clock,
            )
        }
    }

    @Test
    fun rejectsTotalTooSmallToGiveEachInstallmentOneMinorUnit() {
        assertFailsWith<DomainValidationException> {
            Installment.create(
                userId = userId,
                totalAmount = Money(1L, CurrencyCode.BRL),
                installmentCount = 2,
                firstDueOn = LocalDate.of(2026, 10, 10),
                clock = clock,
            )
        }
    }

    @Test
    fun rejectsSingleInstallmentAndExcessiveCount() {
        assertFailsWith<DomainValidationException> {
            Installment.create(
                userId = userId,
                totalAmount = Money(600L, CurrencyCode.BRL),
                installmentCount = 1,
                firstDueOn = LocalDate.of(2026, 10, 10),
                clock = clock,
            )
        }

        assertFailsWith<DomainValidationException> {
            Installment.create(
                userId = userId,
                totalAmount = Money(600L, CurrencyCode.BRL),
                installmentCount = 361,
                firstDueOn = LocalDate.of(2026, 10, 10),
                clock = clock,
            )
        }
    }

    @Test
    fun laterInstallmentCannotBeCompletedTwice() {
        val plan = Installment.create(
            userId = userId,
            totalAmount = Money(600L, CurrencyCode.BRL),
            installmentCount = 2,
            firstDueOn = LocalDate.of(2026, 10, 10),
            clock = clock,
        )

        val completed = plan.completeItem(1, fixedNow.plusSeconds(60))

        assertFailsWith<DomainValidationException> {
            completed.completeItem(1, fixedNow.plusSeconds(120))
        }
    }
}
