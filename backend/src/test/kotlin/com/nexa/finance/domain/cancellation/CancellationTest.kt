package com.nexa.finance.domain.cancellation

import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.transaction.Transaction
import com.nexa.finance.domain.transaction.TransactionDirection
import com.nexa.finance.domain.transaction.TransactionStatus
import com.nexa.finance.domain.transaction.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CancellationTest {
    private val now = Instant.parse("2026-10-03T12:00:00Z")
    private val accountId = AccountId(UUID.fromString("00000000-0000-0000-0000-000000000103"))

    private fun transaction(
        status: TransactionStatus,
        direction: TransactionDirection = TransactionDirection.DEBIT,
    ): Transaction =
        Transaction.create(
            accountId = accountId,
            amount = Money(12_000, CurrencyCode.BRL),
            type = if (direction == TransactionDirection.DEBIT) TransactionType.EXPENSE else TransactionType.INCOME,
            direction = direction,
            occurredOn = LocalDate.of(2026, 10, 3),
            status = status,
            createdAt = now,
            updatedAt = now,
        )

    @Test
    fun cancelsPendingByStatusTransition() {
        val cancelled = Cancellation.cancelPending(transaction(TransactionStatus.PENDING), now)
        assertEquals(TransactionStatus.CANCELLED, cancelled.status)
    }

    @Test
    fun createsCreditCancellationForPostedDebit() {
        val posted = transaction(TransactionStatus.POSTED)
        val cancellation = Cancellation.createForPosted(posted, now)
        assertEquals(TransactionType.CANCELLATION, cancellation.type)
        assertEquals(TransactionDirection.CREDIT, cancellation.direction)
        assertEquals(posted.amount, cancellation.amount)
        assertEquals(posted.id, cancellation.relatedTransactionId)
        assertEquals(TransactionStatus.POSTED, cancellation.status)
    }

    @Test
    fun createsDebitCancellationForPostedCredit() {
        val posted = transaction(TransactionStatus.POSTED, TransactionDirection.CREDIT)
        val cancellation = Cancellation.createForPosted(posted, now)
        assertEquals(TransactionDirection.DEBIT, cancellation.direction)
    }

    @Test
    fun rejectsPostedStateTransitionCancellation() {
        assertFailsWith<DomainValidationException> {
            Cancellation.cancelPending(transaction(TransactionStatus.POSTED), now)
        }
    }

    @Test
    fun rejectsCancellationOfCancellationTransaction() {
        val cancellation = Cancellation.createForPosted(transaction(TransactionStatus.POSTED), now)
        assertFailsWith<DomainValidationException> {
            Cancellation.createForPosted(cancellation, now)
        }
    }

    @Test
    fun rejectsPendingPostedCancellation() {
        assertFailsWith<DomainValidationException> {
            Cancellation.createForPosted(transaction(TransactionStatus.PENDING), now)
        }
    }
}
