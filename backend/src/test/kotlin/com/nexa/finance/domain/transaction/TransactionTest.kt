package com.nexa.finance.domain.transaction

import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.category.CategoryId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class TransactionTest {
    private val accountId = AccountId(UUID.randomUUID())
    private val occurredOn = LocalDate.of(2026, 10, 3)
    private val createdAt = Instant.parse("2026-10-03T12:00:00Z")

    @Test
    fun createsPostedExpenseByDefault() {
        val tx = Transaction.create(
            accountId = accountId,
            amount = Money.fromMinorUnits(1_890, CurrencyCode.BRL),
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.DEBIT,
            occurredOn = occurredOn,
            description = "  Almoço   de hoje  ",
            createdAt = createdAt,
        )

        assertEquals(TransactionStatus.POSTED, tx.status)
        assertEquals("Almoço   de hoje", tx.description)
        assertEquals(1_890, tx.amount.amountInMinor)
        assertEquals(CurrencyCode.BRL, tx.amount.currency)
        assertEquals(createdAt, tx.createdAt)
        assertEquals(createdAt, tx.updatedAt)
    }

    @Test
    fun normalizesDescriptionWhitespaceAroundValue() {
        val tx = Transaction.create(
            accountId = accountId,
            amount = Money.fromMinorUnits(100, CurrencyCode.BRL),
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.DEBIT,
            occurredOn = occurredOn,
            description = "  Compra do mercado  ",
            createdAt = createdAt,
        )

        assertEquals("Compra do mercado", tx.description)
    }

    @Test
    fun allowsOptionalDescription() {
        val tx = Transaction.create(
            accountId = accountId,
            amount = Money.fromMinorUnits(100, CurrencyCode.BRL),
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.DEBIT,
            occurredOn = occurredOn,
            createdAt = createdAt,
        )

        assertEquals(null, tx.description)
    }

    @Test
    fun rejectsZeroAmount() {
        assertFailsWith<DomainValidationException> {
            Transaction.create(
                accountId = accountId,
                amount = Money.zero(CurrencyCode.BRL),
                type = TransactionType.EXPENSE,
                direction = TransactionDirection.DEBIT,
                occurredOn = occurredOn,
                createdAt = createdAt,
            )
        }
    }

    @Test
    fun rejectsNegativeAmount() {
        assertFailsWith<DomainValidationException> {
            Transaction.create(
                accountId = accountId,
                amount = Money.fromMinorUnits(-100, CurrencyCode.BRL),
                type = TransactionType.EXPENSE,
                direction = TransactionDirection.DEBIT,
                occurredOn = occurredOn,
                createdAt = createdAt,
            )
        }
    }

    @Test
    fun rejectsIncompatibleDirection() {
        assertFailsWith<DomainValidationException> {
            Transaction.create(
                accountId = accountId,
                amount = Money.fromMinorUnits(100, CurrencyCode.BRL),
                type = TransactionType.INCOME,
                direction = TransactionDirection.DEBIT,
                occurredOn = occurredOn,
                createdAt = createdAt,
            )
        }
    }

    @Test
    fun acceptsStronglyTypedCategoryId() {
        val categoryId = CategoryId.new()
        val tx = Transaction.create(
            accountId = accountId,
            amount = Money.fromMinorUnits(100, CurrencyCode.BRL),
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.DEBIT,
            occurredOn = occurredOn,
            categoryId = categoryId,
            createdAt = createdAt,
        )

        assertEquals(categoryId, tx.categoryId)
    }

    @Test
    fun cancellationRequiresRelatedTransaction() {
        assertFailsWith<DomainValidationException> {
            Transaction.create(
                accountId = accountId,
                amount = Money.fromMinorUnits(100, CurrencyCode.BRL),
                type = TransactionType.CANCELLATION,
                direction = TransactionDirection.CREDIT,
                occurredOn = occurredOn,
                createdAt = createdAt,
            )
        }
    }

    @Test
    fun cancellationAcceptsRelatedTransaction() {
        val originalId = UUID.randomUUID()

        val tx = Transaction.create(
            accountId = accountId,
            amount = Money.fromMinorUnits(100, CurrencyCode.BRL),
            type = TransactionType.CANCELLATION,
            direction = TransactionDirection.CREDIT,
            occurredOn = occurredOn,
            relatedTransactionId = originalId,
            createdAt = createdAt,
        )

        assertEquals(TransactionType.CANCELLATION, tx.type)
        assertEquals(originalId, tx.relatedTransactionId)
    }

    @Test
    fun postingPendingTransactionReturnsPostedCopy() {
        val tx = Transaction.create(
            accountId = accountId,
            amount = Money.fromMinorUnits(100, CurrencyCode.BRL),
            type = TransactionType.PURCHASE,
            direction = TransactionDirection.DEBIT,
            occurredOn = occurredOn,
            status = TransactionStatus.PENDING,
            createdAt = createdAt,
            updatedAt = createdAt,
        )

        val postedAt = Instant.parse("2026-10-03T13:00:00Z")
        val posted = tx.post(postedAt)

        assertNotSame(tx, posted)
        assertEquals(TransactionStatus.PENDING, tx.status)
        assertEquals(TransactionStatus.POSTED, posted.status)
        assertEquals(createdAt, posted.createdAt)
        assertEquals(postedAt, posted.updatedAt)
    }

    @Test
    fun cannotPostAlreadyPostedTransaction() {
        val tx = Transaction.create(
            accountId = accountId,
            amount = Money.fromMinorUnits(100, CurrencyCode.BRL),
            type = TransactionType.PURCHASE,
            direction = TransactionDirection.DEBIT,
            occurredOn = occurredOn,
            status = TransactionStatus.POSTED,
            createdAt = createdAt,
        )

        assertFailsWith<DomainValidationException> {
            tx.post(createdAt.plusSeconds(60))
        }
    }

    @Test
    fun cancellingTransactionReturnsCancelledCopy() {
        val tx = Transaction.create(
            accountId = accountId,
            amount = Money.fromMinorUnits(100, CurrencyCode.BRL),
            type = TransactionType.PURCHASE,
            direction = TransactionDirection.DEBIT,
            occurredOn = occurredOn,
            status = TransactionStatus.PENDING,
            createdAt = createdAt,
        )

        val cancelledAt = Instant.parse("2026-10-03T14:00:00Z")
        val cancelled = tx.cancel(cancelledAt)

        assertNotSame(tx, cancelled)
        assertEquals(TransactionStatus.PENDING, tx.status)
        assertEquals(TransactionStatus.CANCELLED, cancelled.status)
        assertEquals(cancelledAt, cancelled.updatedAt)
    }
}
