package com.nexa.finance.domain.recurrence

import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.transaction.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RecurrenceTest {
    private val fixedNow = Instant.parse("2026-10-03T12:00:00Z")
    private val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
    private val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private val accountId = AccountId(UUID.fromString("00000000-0000-0000-0000-000000000101"))

    @Test
    fun createsDailyRecurrenceWithDeterministicOccurrences() {
        val recurrence = recurrence(
            frequency = RecurrenceFrequency.DAILY,
            interval = 2,
            startOn = LocalDate.of(2026, 10, 3),
            maxOccurrences = 4,
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 3),
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 7),
                LocalDate.of(2026, 10, 9),
            ),
            (1..4).map { recurrence.occurrence(it).scheduledOn },
        )
    }

    @Test
    fun createsWeeklyRecurrence() {
        val recurrence = recurrence(
            frequency = RecurrenceFrequency.WEEKLY,
            interval = 2,
            startOn = LocalDate.of(2026, 10, 3),
            maxOccurrences = 3,
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 3),
                LocalDate.of(2026, 10, 17),
                LocalDate.of(2026, 10, 31),
            ),
            recurrence.occurrencesBetween(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 11, 1),
            ).map { it.scheduledOn },
        )
    }

    @Test
    fun monthlyEndOfMonthDoesNotDrift() {
        val recurrence = recurrence(
            frequency = RecurrenceFrequency.MONTHLY,
            startOn = LocalDate.of(2027, 1, 31),
            maxOccurrences = 4,
        )

        assertEquals(
            listOf(
                LocalDate.of(2027, 1, 31),
                LocalDate.of(2027, 2, 28),
                LocalDate.of(2027, 3, 31),
                LocalDate.of(2027, 4, 30),
            ),
            (1..4).map { recurrence.occurrence(it).scheduledOn },
        )
    }

    @Test
    fun yearlyLeapDayDoesNotPermanentlyDrift() {
        val recurrence = recurrence(
            frequency = RecurrenceFrequency.YEARLY,
            startOn = LocalDate.of(2028, 2, 29),
            maxOccurrences = 5,
        )

        assertEquals(
            listOf(
                LocalDate.of(2028, 2, 29),
                LocalDate.of(2029, 2, 28),
                LocalDate.of(2030, 2, 28),
                LocalDate.of(2031, 2, 28),
                LocalDate.of(2032, 2, 29),
            ),
            (1..5).map { recurrence.occurrence(it).scheduledOn },
        )
    }

    @Test
    fun nextOccurrenceRespectsEndDateAndMaximumCount() {
        val recurrence = recurrence(
            frequency = RecurrenceFrequency.MONTHLY,
            startOn = LocalDate.of(2026, 10, 15),
            endOn = LocalDate.of(2026, 12, 20),
            maxOccurrences = 10,
        )

        assertEquals(
            LocalDate.of(2026, 11, 15),
            recurrence.nextOccurrenceOnOrAfter(LocalDate.of(2026, 11, 1))?.scheduledOn,
        )
        assertEquals(
            3,
            recurrence.nextOccurrenceOnOrAfter(LocalDate.of(2026, 12, 1))?.number,
        )
        assertNull(
            recurrence.nextOccurrenceOnOrAfter(LocalDate.of(2027, 1, 1)),
        )
    }

    @Test
    fun rangeIsInclusiveAndDoesNotReturnOutsideBounds() {
        val recurrence = recurrence(
            frequency = RecurrenceFrequency.DAILY,
            startOn = LocalDate.of(2026, 10, 3),
            maxOccurrences = 5,
        )

        val occurrences = recurrence.occurrencesBetween(
            LocalDate.of(2026, 10, 5),
            LocalDate.of(2026, 10, 7),
        )

        assertEquals(listOf(3, 4, 5), occurrences.map { it.number })
        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 6),
                LocalDate.of(2026, 10, 7),
            ),
            occurrences.map { it.scheduledOn },
        )
    }

    @Test
    fun pauseAndResumeControlFutureExecutionWithoutChangingTheRule() {
        val recurrence = recurrence(
            frequency = RecurrenceFrequency.WEEKLY,
            startOn = LocalDate.of(2026, 10, 3),
        )

        val paused = recurrence.pause(fixedNow.plusSeconds(60))
        assertEquals(RecurrenceStatus.PAUSED, paused.status)
        assertNull(paused.nextOccurrenceOnOrAfter(LocalDate.of(2026, 10, 10)))

        val resumed = paused.resume(fixedNow.plusSeconds(120))
        assertEquals(RecurrenceStatus.ACTIVE, resumed.status)
        assertEquals(
            LocalDate.of(2026, 10, 10),
            resumed.nextOccurrenceOnOrAfter(LocalDate.of(2026, 10, 10))?.scheduledOn,
        )
    }

    @Test
    fun cancelStopsFutureExecution() {
        val recurrence = recurrence(
            frequency = RecurrenceFrequency.DAILY,
            startOn = LocalDate.of(2026, 10, 3),
        )

        val cancelled = recurrence.cancel(fixedNow.plusSeconds(60))
        assertEquals(RecurrenceStatus.CANCELLED, cancelled.status)
        assertNull(cancelled.nextOccurrenceOnOrAfter(LocalDate.of(2026, 10, 4)))
        assertFalse(cancelled.isScheduledOn(LocalDate.of(2026, 10, 4)))
    }

    @Test
    fun isScheduledOnMatchesOnlyExactOccurrenceDates() {
        val recurrence = recurrence(
            frequency = RecurrenceFrequency.WEEKLY,
            startOn = LocalDate.of(2026, 10, 3),
        )

        assertTrue(recurrence.isScheduledOn(LocalDate.of(2026, 10, 3)))
        assertTrue(recurrence.isScheduledOn(LocalDate.of(2026, 10, 10)))
        assertFalse(recurrence.isScheduledOn(LocalDate.of(2026, 10, 9)))
    }

    @Test
    fun blankDescriptionBecomesNull() {
        val recurrence = recurrence(description = "   ")
        assertNull(recurrence.description)
    }

    @Test
    fun trimsDescription() {
        val recurrence = recurrence(description = "  Internet  ")
        assertEquals("Internet", recurrence.description)
    }

    @Test
    fun rejectsUnsupportedTransactionType() {
        assertFailsWith<DomainValidationException> {
            recurrence(transactionType = TransactionType.PAYMENT)
        }
    }

    @Test
    fun rejectsNonPositiveAmount() {
        assertFailsWith<DomainValidationException> {
            recurrence(amount = Money(0, CurrencyCode.BRL))
        }
    }

    @Test
    fun rejectsInvalidIntervalAndBounds() {
        assertFailsWith<DomainValidationException> {
            recurrence(interval = 0)
        }
        assertFailsWith<DomainValidationException> {
            recurrence(interval = Recurrence.MAX_INTERVAL + 1)
        }
        assertFailsWith<DomainValidationException> {
            recurrence(
                startOn = LocalDate.of(2026, 10, 10),
                endOn = LocalDate.of(2026, 10, 9),
            )
        }
        assertFailsWith<DomainValidationException> {
            recurrence(maxOccurrences = Recurrence.MAX_OCCURRENCES + 1)
        }
    }

    @Test
    fun rejectsInvalidOccurrenceQueries() {
        val recurrence = recurrence(maxOccurrences = 3)

        assertFailsWith<DomainValidationException> {
            recurrence.occurrence(0)
        }
        assertFailsWith<DomainValidationException> {
            recurrence.occurrence(4)
        }
        assertFailsWith<DomainValidationException> {
            recurrence.occurrencesBetween(
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 9),
            )
        }
    }

    private fun recurrence(
        amount: Money = Money(1_990, CurrencyCode.BRL),
        transactionType: TransactionType = TransactionType.EXPENSE,
        frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
        interval: Int = 1,
        startOn: LocalDate = LocalDate.of(2026, 10, 3),
        endOn: LocalDate? = null,
        maxOccurrences: Int? = null,
        description: String? = null,
    ): Recurrence =
        Recurrence.create(
            userId = userId,
            accountId = accountId,
            amount = amount,
            transactionType = transactionType,
            frequency = frequency,
            interval = interval,
            startOn = startOn,
            endOn = endOn,
            maxOccurrences = maxOccurrences,
            description = description,
            clock = clock,
        )
}
