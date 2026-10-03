package com.nexa.finance.domain.payment

import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.transaction.TransactionDirection
import com.nexa.finance.domain.transaction.TransactionStatus
import com.nexa.finance.domain.transaction.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PaymentTest {
    private val now = Instant.parse("2026-10-03T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val accountId = AccountId(UUID.fromString("00000000-0000-0000-0000-000000000101"))

    @Test
    fun createsDebitPayment() {
        val payment = Payment.create(
            accountId = accountId,
            amount = Money(10_000, CurrencyCode.BRL),
            occurredOn = LocalDate.of(2026, 10, 3),
            clock = clock,
        )
        assertEquals(TransactionType.PAYMENT, payment.transaction.type)
        assertEquals(TransactionDirection.DEBIT, payment.transaction.direction)
        assertEquals(TransactionStatus.POSTED, payment.transaction.status)
    }

    @Test
    fun rejectsNonPositiveAmount() {
        assertFailsWith<DomainValidationException> {
            Payment.create(
                accountId = accountId,
                amount = Money(0, CurrencyCode.BRL),
                occurredOn = LocalDate.of(2026, 10, 3),
                clock = clock,
            )
        }
    }

    @Test
    fun rejectsCancelledInitialStatus() {
        assertFailsWith<DomainValidationException> {
            Payment.create(
                accountId = accountId,
                amount = Money(10_000, CurrencyCode.BRL),
                occurredOn = LocalDate.of(2026, 10, 3),
                status = TransactionStatus.CANCELLED,
                clock = clock,
            )
        }
    }

    @Test
    fun supportsPendingStatus() {
        val payment = Payment.create(
            accountId = accountId,
            amount = Money(10_000, CurrencyCode.BRL),
            occurredOn = LocalDate.of(2026, 10, 3),
            status = TransactionStatus.PENDING,
            clock = clock,
        )
        assertEquals(TransactionStatus.PENDING, payment.transaction.status)
    }
}
