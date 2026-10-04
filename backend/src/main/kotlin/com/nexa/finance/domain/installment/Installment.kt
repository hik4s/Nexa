package com.nexa.finance.domain.installment

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.category.CategoryId
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

typealias InstallmentId = UUID

enum class InstallmentPlanStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED,
}

enum class InstallmentItemStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED,
}

data class InstallmentItem(
    val number: Int,
    val amount: Money,
    val dueOn: LocalDate,
    val status: InstallmentItemStatus = InstallmentItemStatus.SCHEDULED,
) {
    init {
        if (number <= 0) {
            throw DomainValidationException("Installment number must be greater than zero.")
        }
        if (amount.amountInMinor <= 0) {
            throw DomainValidationException("Installment amount must be greater than zero.")
        }
    }
}

@ConsistentCopyVisibility
data class Installment private constructor(
    val id: InstallmentId,
    val userId: UserId,
    val totalAmount: Money,
    val installmentCount: Int,
    val firstDueOn: LocalDate,
    val description: String?,
    val categoryId: CategoryId?,
    val items: List<InstallmentItem>,
    val status: InstallmentPlanStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        if (totalAmount.amountInMinor <= 0) {
            throw DomainValidationException("Installment total amount must be greater than zero.")
        }
        if (installmentCount !in MIN_INSTALLMENTS..MAX_INSTALLMENTS) {
            throw DomainValidationException(
                "Installment count must be between $MIN_INSTALLMENTS and $MAX_INSTALLMENTS."
            )
        }
        if (items.size != installmentCount) {
            throw DomainValidationException("Installment item count must match installmentCount.")
        }
        if (items.map { it.number } != (1..installmentCount).toList()) {
            throw DomainValidationException("Installment numbers must be sequential and start at one.")
        }
        if (items.any { it.amount.currency != totalAmount.currency }) {
            throw DomainValidationException("All installments must use the total amount currency.")
        }
        if (items.sumOf { it.amount.amountInMinor } != totalAmount.amountInMinor) {
            throw DomainValidationException("Installment amounts must add up exactly to the total amount.")
        }
        if (description?.isBlank() == true) {
            throw DomainValidationException("Installment description cannot be blank.")
        }
        if (description != null && description.length > MAX_DESCRIPTION_LENGTH) {
            throw DomainValidationException(
                "Installment description cannot exceed $MAX_DESCRIPTION_LENGTH characters."
            )
        }
    }

    fun completeItem(number: Int, now: Instant = Instant.now()): Installment {
        val item = item(number)
        if (status != InstallmentPlanStatus.ACTIVE) {
            throw DomainValidationException("Only active installment plans can complete an installment.")
        }
        if (item.status != InstallmentItemStatus.SCHEDULED) {
            throw DomainValidationException("Installment item is not scheduled.")
        }

        val updatedItems = replaceItem(item.copy(status = InstallmentItemStatus.COMPLETED))
        val nextStatus = if (updatedItems.all { it.status == InstallmentItemStatus.COMPLETED }) {
            InstallmentPlanStatus.COMPLETED
        } else {
            InstallmentPlanStatus.ACTIVE
        }

        return copy(items = updatedItems, status = nextStatus, updatedAt = now)
    }

    fun cancel(now: Instant = Instant.now()): Installment {
        if (status == InstallmentPlanStatus.CANCELLED) {
            throw DomainValidationException("Installment plan is already cancelled.")
        }
        if (items.any { it.status == InstallmentItemStatus.COMPLETED }) {
            throw DomainValidationException(
                "An installment plan with completed installments must be reversed rather than cancelled directly."
            )
        }

        val cancelledItems = items.map { it.copy(status = InstallmentItemStatus.CANCELLED) }
        return copy(items = cancelledItems, status = InstallmentPlanStatus.CANCELLED, updatedAt = now)
    }

    fun item(number: Int): InstallmentItem =
        items.firstOrNull { it.number == number }
            ?: throw DomainValidationException("Installment item $number does not exist.")

    private fun replaceItem(updated: InstallmentItem): List<InstallmentItem> =
        items.map { if (it.number == updated.number) updated else it }

    companion object {
        const val MIN_INSTALLMENTS = 2
        const val MAX_INSTALLMENTS = 360
        const val MAX_DESCRIPTION_LENGTH = 160

        fun create(
            userId: UserId,
            totalAmount: Money,
            installmentCount: Int,
            firstDueOn: LocalDate,
            description: String? = null,
            categoryId: CategoryId? = null,
            clock: Clock = Clock.systemUTC(),
            id: InstallmentId = UUID.randomUUID(),
        ): Installment {
            if (totalAmount.amountInMinor <= 0) {
                throw DomainValidationException("Installment total amount must be greater than zero.")
            }
            if (installmentCount !in MIN_INSTALLMENTS..MAX_INSTALLMENTS) {
                throw DomainValidationException(
                    "Installment count must be between $MIN_INSTALLMENTS and $MAX_INSTALLMENTS."
                )
            }
            if (totalAmount.amountInMinor < installmentCount) {
                throw DomainValidationException(
                    "Total amount must contain at least one minor unit per installment."
                )
            }

            val normalizedDescription = description?.trim()?.takeIf { it.isNotEmpty() }
            val itemAmounts = splitAmount(totalAmount.amountInMinor, installmentCount)
            val items = itemAmounts.mapIndexed { index, amount ->
                InstallmentItem(
                    number = index + 1,
                    amount = Money(amount, totalAmount.currency),
                    dueOn = monthlyDueDate(firstDueOn, index),
                )
            }
            val now = Instant.now(clock)

            return Installment(
                id = id,
                userId = userId,
                totalAmount = totalAmount,
                installmentCount = installmentCount,
                firstDueOn = firstDueOn,
                description = normalizedDescription,
                categoryId = categoryId,
                items = items,
                status = InstallmentPlanStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            )
        }

        fun restore(id: InstallmentId, userId: UserId, totalAmount: Money, installmentCount: Int, firstDueOn: LocalDate, description: String?, categoryId: CategoryId?, items: List<InstallmentItem>, status: InstallmentPlanStatus, createdAt: Instant, updatedAt: Instant,
        ): Installment = Installment(id, userId, totalAmount, installmentCount, firstDueOn, description, categoryId, items, status, createdAt, updatedAt)

        private fun splitAmount(totalMinor: Long, count: Int): List<Long> {
            val base = totalMinor / count
            val remainder = totalMinor % count

            return List(count) { index ->
                base + if (index < remainder) 1 else 0
            }
        }

        private fun monthlyDueDate(firstDueOn: LocalDate, monthOffset: Int): LocalDate {
            val targetMonth = YearMonth.from(firstDueOn).plusMonths(monthOffset.toLong())
            val day = minOf(firstDueOn.dayOfMonth, targetMonth.lengthOfMonth())
            return targetMonth.atDay(day)
        }
    }
}
