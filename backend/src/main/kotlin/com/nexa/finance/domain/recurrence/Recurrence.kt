package com.nexa.finance.domain.recurrence

import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.category.CategoryId
import com.nexa.finance.domain.transaction.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.math.max

typealias RecurrenceId = UUID

enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

enum class RecurrenceStatus {
    ACTIVE,
    PAUSED,
    CANCELLED,
}

/**
 * Planned occurrence of a recurrence.
 *
 * It is a scheduling object only; it does not create a ledger Transaction by itself.
 */
data class RecurrenceOccurrence(
    val number: Int,
    val scheduledOn: LocalDate,
) {
    init {
        if (number <= 0) {
            throw DomainValidationException("Recurrence occurrence number must be greater than zero.")
        }
    }
}

@ConsistentCopyVisibility
data class Recurrence private constructor(
    val id: RecurrenceId,
    val userId: UserId,
    val accountId: AccountId,
    val amount: Money,
    val transactionType: TransactionType,
    val frequency: RecurrenceFrequency,
    val interval: Int,
    val startOn: LocalDate,
    val endOn: LocalDate?,
    val maxOccurrences: Int?,
    val description: String?,
    val categoryId: CategoryId?,
    val status: RecurrenceStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        if (amount.amountInMinor <= 0) {
            throw DomainValidationException("Recurring amount must be greater than zero.")
        }
        if (accountId.value == UUID(0, 0)) {
            throw DomainValidationException("Account ID cannot be the zero UUID.")
        }
        if (transactionType !in SUPPORTED_TRANSACTION_TYPES) {
            throw DomainValidationException(
                "Recurrence supports only INCOME and EXPENSE transaction types."
            )
        }
        if (interval !in MIN_INTERVAL..MAX_INTERVAL) {
            throw DomainValidationException(
                "Recurrence interval must be between $MIN_INTERVAL and $MAX_INTERVAL."
            )
        }
        if (endOn != null && endOn.isBefore(startOn)) {
            throw DomainValidationException("Recurrence end date cannot be before the start date.")
        }
        if (maxOccurrences != null && maxOccurrences !in 1..MAX_OCCURRENCES) {
            throw DomainValidationException(
                "Maximum occurrences must be between 1 and $MAX_OCCURRENCES."
            )
        }
        if (description?.isBlank() == true) {
            throw DomainValidationException("Recurrence description cannot be blank.")
        }
        if (description != null && description.length > MAX_DESCRIPTION_LENGTH) {
            throw DomainValidationException(
                "Recurrence description cannot exceed $MAX_DESCRIPTION_LENGTH characters."
            )
        }
    }

    fun pause(now: Instant = Instant.now()): Recurrence {
        if (status != RecurrenceStatus.ACTIVE) {
            throw DomainValidationException("Only active recurrences can be paused.")
        }
        return copy(status = RecurrenceStatus.PAUSED, updatedAt = now)
    }

    fun resume(now: Instant = Instant.now()): Recurrence {
        if (status != RecurrenceStatus.PAUSED) {
            throw DomainValidationException("Only paused recurrences can be resumed.")
        }
        return copy(status = RecurrenceStatus.ACTIVE, updatedAt = now)
    }

    fun cancel(now: Instant = Instant.now()): Recurrence {
        if (status == RecurrenceStatus.CANCELLED) {
            throw DomainValidationException("Recurrence is already cancelled.")
        }
        return copy(status = RecurrenceStatus.CANCELLED, updatedAt = now)
    }

    fun update(
        accountId: AccountId = this.accountId,
        amount: Money = this.amount,
        transactionType: TransactionType = this.transactionType,
        frequency: RecurrenceFrequency = this.frequency,
        interval: Int = this.interval,
        startOn: LocalDate = this.startOn,
        endOn: LocalDate? = this.endOn,
        maxOccurrences: Int? = this.maxOccurrences,
        description: String? = this.description,
        categoryId: CategoryId? = this.categoryId,
        now: Instant = Instant.now(),
    ): Recurrence {
        if (status == RecurrenceStatus.CANCELLED) {
            throw DomainValidationException("Cancelled recurrences cannot be edited.")
        }
        return Recurrence(
            id = id,
            userId = userId,
            accountId = accountId,
            amount = amount,
            transactionType = transactionType,
            frequency = frequency,
            interval = interval,
            startOn = startOn,
            endOn = endOn,
            maxOccurrences = maxOccurrences,
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            categoryId = categoryId,
            status = status,
            createdAt = createdAt,
            updatedAt = now,
        )
    }

    /**
     * Returns the deterministic date for an occurrence number, independent of the current status.
     * This allows historical/audit queries without turning the schedule into ledger entries.
     */
    fun occurrence(number: Int): RecurrenceOccurrence {
        validateOccurrenceNumber(number)
        return RecurrenceOccurrence(number, dateForOccurrence(number))
    }

    /**
     * Returns the next occurrence on or after the supplied date while the recurrence is active.
     *
     * Paused and cancelled recurrences do not produce executable future occurrences.
     */
    fun nextOccurrenceOnOrAfter(date: LocalDate): RecurrenceOccurrence? {
        if (status != RecurrenceStatus.ACTIVE) {
            return null
        }

        val number = firstOccurrenceNumberOnOrAfter(date)
        if (!isOccurrenceAllowed(number)) {
            return null
        }

        val scheduledOn = dateForOccurrence(number)
        return RecurrenceOccurrence(number, scheduledOn)
    }

    /**
     * Lists scheduled occurrences inside an inclusive date range.
     *
     * The returned dates are generated from the original anchor date, preventing
     * month-end/year-end clamping from permanently drifting future occurrences.
     */
    fun occurrencesBetween(
        fromInclusive: LocalDate,
        toInclusive: LocalDate,
    ): List<RecurrenceOccurrence> {
        if (toInclusive.isBefore(fromInclusive)) {
            throw DomainValidationException("Occurrence range end cannot be before the start.")
        }

        if (status != RecurrenceStatus.ACTIVE) {
            return emptyList()
        }

        var number = firstOccurrenceNumberOnOrAfter(fromInclusive)
        val result = mutableListOf<RecurrenceOccurrence>()

        while (isOccurrenceAllowed(number)) {
            val scheduledOn = dateForOccurrence(number)
            if (scheduledOn.isAfter(toInclusive)) {
                break
            }
            if (!scheduledOn.isBefore(fromInclusive)) {
                result += RecurrenceOccurrence(number, scheduledOn)
            }
            number++
        }

        return result
    }

    fun isScheduledOn(date: LocalDate): Boolean =
        nextOccurrenceOnOrAfter(date)?.scheduledOn == date

    private fun validateOccurrenceNumber(number: Int) {
        if (number <= 0) {
            throw DomainValidationException("Recurrence occurrence number must be greater than zero.")
        }
        if (!isOccurrenceAllowed(number)) {
            throw DomainValidationException("Recurrence occurrence $number is outside the recurrence bounds.")
        }
    }

    private fun isOccurrenceAllowed(number: Int): Boolean {
        if (number <= 0) {
            return false
        }
        if (maxOccurrences != null && number > maxOccurrences) {
            return false
        }
        val date = dateForOccurrence(number)
        return endOn == null || !date.isAfter(endOn)
    }

    private fun firstOccurrenceNumberOnOrAfter(date: LocalDate): Int {
        if (!date.isAfter(startOn)) {
            return 1
        }

        val estimated = when (frequency) {
            RecurrenceFrequency.DAILY -> {
                val elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(startOn, date)
                max(1, (elapsedDays / interval).toInt() + 1)
            }

            RecurrenceFrequency.WEEKLY -> {
                val elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(startOn, date)
                max(1, (elapsedDays / (interval * 7L)).toInt() + 1)
            }

            RecurrenceFrequency.MONTHLY -> {
                val elapsedMonths =
                    (date.year.toLong() - startOn.year.toLong()) * 12L +
                        (date.monthValue - startOn.monthValue)
                max(1, (elapsedMonths / interval).toInt() + 1)
            }

            RecurrenceFrequency.YEARLY -> {
                val elapsedYears = date.year.toLong() - startOn.year.toLong()
                max(1, (elapsedYears / interval).toInt() + 1)
            }
        }

        var number = estimated
        while (number > 1 && dateForOccurrence(number - 1).compareTo(date) >= 0) {
            number--
        }
        while (dateForOccurrence(number).isBefore(date)) {
            number++
        }
        return number
    }

    private fun dateForOccurrence(number: Int): LocalDate {
        val offset = number - 1
        return when (frequency) {
            RecurrenceFrequency.DAILY ->
                startOn.plusDays(Math.multiplyExact(offset.toLong(), interval.toLong()))

            RecurrenceFrequency.WEEKLY ->
                startOn.plusWeeks(Math.multiplyExact(offset.toLong(), interval.toLong()))

            RecurrenceFrequency.MONTHLY -> {
                val monthOffset = Math.multiplyExact(offset.toLong(), interval.toLong())
                val targetMonth = YearMonth.from(startOn).plusMonths(monthOffset)
                val day = minOf(startOn.dayOfMonth, targetMonth.lengthOfMonth())
                targetMonth.atDay(day)
            }

            RecurrenceFrequency.YEARLY -> {
                val yearOffset = Math.multiplyExact(offset.toLong(), interval.toLong())
                val targetYear = Math.addExact(startOn.year.toLong(), yearOffset)
                val targetMonth = YearMonth.of(Math.toIntExact(targetYear), startOn.month)
                val day = minOf(startOn.dayOfMonth, targetMonth.lengthOfMonth())
                targetMonth.atDay(day)
            }
        }
    }

    companion object {
        const val MIN_INTERVAL = 1
        const val MAX_INTERVAL = 365
        const val MAX_OCCURRENCES = 10_000
        const val MAX_DESCRIPTION_LENGTH = 160

        private val SUPPORTED_TRANSACTION_TYPES = setOf(
            TransactionType.INCOME,
            TransactionType.EXPENSE,
        )

        fun restore(
            id: RecurrenceId,
            userId: UserId,
            accountId: AccountId,
            amount: Money,
            transactionType: TransactionType,
            frequency: RecurrenceFrequency,
            interval: Int,
            startOn: LocalDate,
            endOn: LocalDate?,
            maxOccurrences: Int?,
            description: String?,
            categoryId: CategoryId?,
            status: RecurrenceStatus,
            createdAt: Instant,
            updatedAt: Instant,
        ): Recurrence = Recurrence(
            id = id,
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
            categoryId = categoryId,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

        fun create(
            userId: UserId,
            accountId: AccountId,
            amount: Money,
            transactionType: TransactionType,
            frequency: RecurrenceFrequency,
            interval: Int = 1,
            startOn: LocalDate,
            endOn: LocalDate? = null,
            maxOccurrences: Int? = null,
            description: String? = null,
            categoryId: CategoryId? = null,
            clock: Clock = Clock.systemUTC(),
            id: RecurrenceId = UUID.randomUUID(),
        ): Recurrence {
            val normalizedDescription = description?.trim()?.takeIf { it.isNotEmpty() }
            val now = Instant.now(clock)

            return Recurrence(
                id = id,
                userId = userId,
                accountId = accountId,
                amount = amount,
                transactionType = transactionType,
                frequency = frequency,
                interval = interval,
                startOn = startOn,
                endOn = endOn,
                maxOccurrences = maxOccurrences,
                description = normalizedDescription,
                categoryId = categoryId,
                status = RecurrenceStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
