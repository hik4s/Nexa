package com.nexa.finance.domain.refund

import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.common.*
import com.nexa.finance.domain.transaction.*
import java.time.*
import java.util.UUID
import kotlin.test.*

class RefundTest {
    private val now = Instant.parse("2026-10-03T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val accountId = AccountId(UUID.fromString("00000000-0000-0000-0000-000000000102"))

    private fun purchase(status: TransactionStatus = TransactionStatus.POSTED): Transaction = Transaction.create(
        accountId = accountId, amount = Money(20_000, CurrencyCode.BRL),
        type = TransactionType.PURCHASE, direction = TransactionDirection.DEBIT,
        occurredOn = LocalDate.of(2026, 10, 1), status = status,
        createdAt = now, updatedAt = now,
    )

    @Test fun createsFullRefundAsCreditLinkedToOriginal() {
        val original = purchase()
        val refund = Refund.create(original, clock = clock)
        assertEquals(TransactionType.REFUND, refund.transaction.type)
        assertEquals(TransactionDirection.CREDIT, refund.transaction.direction)
        assertEquals(original.id, refund.transaction.relatedTransactionId)
        assertEquals(original.amount, refund.transaction.amount)
    }

    @Test fun supportsPartialRefund() {
        val refund = Refund.create(purchase(), Money(5_000, CurrencyCode.BRL), clock = clock)
        assertEquals(5_000, refund.transaction.amount.amountInMinor)
    }

    @Test fun rejectsAmountAboveOriginal() {
        assertFailsWith<DomainValidationException> { Refund.create(purchase(), Money(20_001, CurrencyCode.BRL), clock = clock) }
    }

    @Test fun rejectsCurrencyMismatch() {
        assertFailsWith<DomainValidationException> { Refund.create(purchase(), Money(5_000, CurrencyCode.USD), clock = clock) }
    }

    @Test fun rejectsPendingOriginal() {
        assertFailsWith<DomainValidationException> { Refund.create(purchase(TransactionStatus.PENDING), clock = clock) }
    }
}
