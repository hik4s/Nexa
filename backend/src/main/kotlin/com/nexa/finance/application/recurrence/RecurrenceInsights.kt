package com.nexa.finance.application.recurrence

import com.nexa.finance.domain.recurrence.Recurrence
import com.nexa.finance.domain.recurrence.RecurrenceFrequency
import com.nexa.finance.domain.transaction.Transaction
import com.nexa.finance.domain.transaction.TransactionStatus
import com.nexa.finance.domain.transaction.TransactionType
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

enum class RecurrenceAlertLevel {
    NONE,
    UPCOMING,
    DUE_SOON,
}

data class RecurrenceSummary(
    val recurrence: Recurrence,
    val nextOccurrenceNumber: Int?,
    val nextOccurrenceOn: LocalDate?,
    val nextOccurrences: List<com.nexa.finance.domain.recurrence.RecurrenceOccurrence>,
    val alertLevel: RecurrenceAlertLevel,
)

data class RecurrenceCandidate(
    val accountId: String,
    val amountMinor: Long,
    val currency: String,
    val transactionType: TransactionType,
    val frequency: RecurrenceFrequency,
    val description: String,
    val categoryId: String?,
    val occurrenceCount: Int,
    val confidencePercent: Int,
    val nextOccurrenceOn: LocalDate,
)

object RecurrenceInsights {
    fun summary(recurrence: Recurrence, clock: Clock = Clock.systemUTC()): RecurrenceSummary {
        val today = LocalDate.now(clock)
        val next = recurrence.nextOccurrenceOnOrAfter(today)
        val upcoming = if (next == null) {
            emptyList()
        } else {
            recurrence.occurrencesBetween(today, today.plusDays(60)).take(5)
        }
        val alert = when {
            next == null -> RecurrenceAlertLevel.NONE
            !next.scheduledOn.isAfter(today.plusDays(2)) -> RecurrenceAlertLevel.DUE_SOON
            !next.scheduledOn.isAfter(today.plusDays(7)) -> RecurrenceAlertLevel.UPCOMING
            else -> RecurrenceAlertLevel.NONE
        }
        return RecurrenceSummary(
            recurrence = recurrence,
            nextOccurrenceNumber = next?.number,
            nextOccurrenceOn = next?.scheduledOn,
            nextOccurrences = upcoming,
            alertLevel = alert,
        )
    }

    fun candidates(
        transactions: List<Transaction>,
        recurrences: List<Recurrence>,
        today: LocalDate,
    ): List<RecurrenceCandidate> {
        val existing = recurrences.map {
            ExistingSignature(
                accountId = it.accountId.value.toString(),
                amountMinor = it.amount.amountInMinor,
                transactionType = it.transactionType,
                description = normalize(it.description),
                frequency = it.frequency,
            )
        }.toSet()

        val cutoff = today.minusDays(120)
        return transactions
            .asSequence()
            .filter { it.status == TransactionStatus.POSTED }
            .filter { it.type == TransactionType.INCOME || it.type == TransactionType.EXPENSE }
            .filter { it.source.name != "SYSTEM" }
            .filter { !it.occurredOn.isBefore(cutoff) }
            .filter { !it.description.isNullOrBlank() }
            .groupBy {
                CandidateKey(
                    accountId = it.accountId.value.toString(),
                    amountMinor = it.amount.amountInMinor,
                    currency = it.amount.currency.name,
                    transactionType = it.type,
                    description = normalize(it.description),
                    categoryId = it.categoryId?.value?.toString(),
                )
            }
            .mapNotNull { (key, values) ->
                val dates = values.map(Transaction::occurredOn).distinct().sorted()
                if (dates.size < 3) return@mapNotNull null
                val gaps = dates.zipWithNext { a, b -> java.time.temporal.ChronoUnit.DAYS.between(a, b) }
                val frequency = inferFrequency(gaps) ?: return@mapNotNull null
                if (existing.contains(ExistingSignature(key.accountId, key.amountMinor, key.transactionType, key.description, frequency))) {
                    return@mapNotNull null
                }

                val medianGap = gaps.sorted()[gaps.size / 2]
                val stability = gaps.count { abs(it - medianGap) <= tolerance(frequency) }
                val confidence = (70 + (dates.size - 3).coerceAtMost(3) * 5 + stability * 5)
                    .coerceIn(70, 96)

                RecurrenceCandidate(
                    accountId = key.accountId,
                    amountMinor = key.amountMinor,
                    currency = key.currency,
                    transactionType = key.transactionType,
                    frequency = frequency,
                    description = values.last().description!!.trim(),
                    categoryId = key.categoryId,
                    occurrenceCount = dates.size,
                    confidencePercent = confidence,
                    nextOccurrenceOn = nextDate(dates.last(), frequency),
                )
            }
            .sortedWith(compareByDescending<RecurrenceCandidate> { it.confidencePercent }.thenBy { it.nextOccurrenceOn })
            .take(20)
            .toList()
    }

    private data class CandidateKey(
        val accountId: String,
        val amountMinor: Long,
        val currency: String,
        val transactionType: TransactionType,
        val description: String,
        val categoryId: String?,
    )

    private data class ExistingSignature(
        val accountId: String,
        val amountMinor: Long,
        val transactionType: TransactionType,
        val description: String,
        val frequency: RecurrenceFrequency,
    )

    private fun normalize(value: String?): String =
        value?.trim()?.lowercase() ?: ""

    private fun inferFrequency(gaps: List<Long>): RecurrenceFrequency? {
        if (gaps.isEmpty()) return null
        val median = gaps.sorted()[gaps.size / 2]
        return when (median) {
            in 1..2 -> if (gaps.all { it in 1..2 }) RecurrenceFrequency.DAILY else null
            in 6..8 -> if (gaps.all { it in 6..8 }) RecurrenceFrequency.WEEKLY else null
            in 25..35 -> if (gaps.all { it in 25..35 }) RecurrenceFrequency.MONTHLY else null
            in 330..400 -> if (gaps.all { it in 330..400 }) RecurrenceFrequency.YEARLY else null
            else -> null
        }
    }

    private fun tolerance(frequency: RecurrenceFrequency): Long = when (frequency) {
        RecurrenceFrequency.DAILY -> 1
        RecurrenceFrequency.WEEKLY -> 1
        RecurrenceFrequency.MONTHLY -> 3
        RecurrenceFrequency.YEARLY -> 7
    }

    private fun nextDate(last: LocalDate, frequency: RecurrenceFrequency): LocalDate = when (frequency) {
        RecurrenceFrequency.DAILY -> last.plusDays(1)
        RecurrenceFrequency.WEEKLY -> last.plusWeeks(1)
        RecurrenceFrequency.MONTHLY -> {
            val month = YearMonth.from(last).plusMonths(1)
            month.atDay(minOf(last.dayOfMonth, month.lengthOfMonth()))
        }
        RecurrenceFrequency.YEARLY -> {
            val month = YearMonth.of(last.year + 1, last.month)
            month.atDay(minOf(last.dayOfMonth, month.lengthOfMonth()))
        }
    }
}
