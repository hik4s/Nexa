package com.nexa.finance.application.recurrence

import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.recurrence.Recurrence
import com.nexa.finance.domain.recurrence.RecurrenceFrequency
import com.nexa.finance.domain.transaction.Transaction
import com.nexa.finance.domain.transaction.TransactionDirection
import com.nexa.finance.domain.transaction.TransactionSource
import com.nexa.finance.domain.transaction.TransactionStatus
import com.nexa.finance.domain.transaction.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecurrenceInsightsTest {
    private val today = LocalDate.of(2026, 10, 20)
    private val clock = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC)
    private val userId = UserId(UUID.randomUUID())
    private val accountId = AccountId(UUID.randomUUID())

    @Test
    fun summaryMarksRecurrenceDueSoonAndListsUpcomingOccurrences() {
        val recurrence = Recurrence.create(
            userId = userId,
            accountId = accountId,
            amount = Money(9_900, CurrencyCode.BRL),
            transactionType = TransactionType.EXPENSE,
            frequency = RecurrenceFrequency.WEEKLY,
            startOn = LocalDate.of(2026, 10, 20),
            maxOccurrences = 5,
            clock = clock,
        )

        val summary = RecurrenceInsights.summary(recurrence, clock)

        assertEquals(RecurrenceAlertLevel.DUE_SOON, summary.alertLevel)
        assertEquals(LocalDate.of(2026, 10, 20), summary.nextOccurrenceOn)
        assertEquals(listOf(1, 2, 3, 4, 5), summary.nextOccurrences.map { it.number })
    }

    @Test
    fun detectsWeeklyCandidateFromPostedTransactions() {
        val transactions = listOf(
            transaction(LocalDate.of(2026, 10, 1)),
            transaction(LocalDate.of(2026, 10, 8)),
            transaction(LocalDate.of(2026, 10, 15)),
        )

        val candidates = RecurrenceInsights.candidates(transactions, emptyList(), today)

        assertEquals(1, candidates.size)
        val candidate = candidates.single()
        assertEquals(RecurrenceFrequency.WEEKLY, candidate.frequency)
        assertEquals(3, candidate.occurrenceCount)
        assertTrue(candidate.confidencePercent >= 80)
        assertEquals(LocalDate.of(2026, 10, 22), candidate.nextOccurrenceOn)
    }

    private fun transaction(date: LocalDate): Transaction =
        Transaction.create(
            accountId = accountId,
            amount = Money(12_000, CurrencyCode.BRL),
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.DEBIT,
            status = TransactionStatus.POSTED,
            occurredOn = date,
            description = "Internet",
            source = TransactionSource.MANUAL,
        )
}
