package com.nexa.finance.domain.invoice

import com.nexa.finance.domain.card.CardId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.transaction.TransactionId
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InvoiceTest {
    private val now = Instant.parse("2026-10-03T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val cardId = CardId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
    private val txId = TransactionId(UUID.fromString("00000000-0000-0000-0000-000000000003"))

    private fun invoice() = Invoice.create(
        cardId = cardId,
        periodStart = LocalDate.of(2026, 9, 11),
        periodEnd = LocalDate.of(2026, 10, 10),
        dueOn = LocalDate.of(2026, 10, 20),
        totalAmount = Money(100_000, CurrencyCode.BRL),
        transactionIds = listOf(txId),
        clock = clock,
    )

    @Test
    fun startsOpenAndTracksRemainingBalance() {
        val invoice = invoice()
        assertEquals(InvoiceStatus.OPEN, invoice.status)
        assertEquals(100_000, invoice.remainingAmount().amountInMinor)
    }

    @Test
    fun closesThenAcceptsPartialPaymentWithoutCallingItPaid() {
        val closed = invoice().close(now)
        val partiallyPaid = closed.registerPayment(Money(40_000, CurrencyCode.BRL), txId, now.plusSeconds(1))
        assertEquals(InvoiceStatus.CLOSED, partiallyPaid.status)
        assertEquals(60_000, partiallyPaid.remainingAmount().amountInMinor)
    }

    @Test
    fun fullPaymentMarksInvoicePaid() {
        val paid = invoice().close(now).registerPayment(Money(100_000, CurrencyCode.BRL), txId, now.plusSeconds(1))
        assertEquals(InvoiceStatus.PAID, paid.status)
        assertEquals(0, paid.remainingAmount().amountInMinor)
    }

    @Test
    fun paymentCannotExceedRemainingBalance() {
        assertFailsWith<DomainValidationException> {
            invoice().close(now).registerPayment(Money(100_001, CurrencyCode.BRL), txId, now.plusSeconds(1))
        }
    }

    @Test
    fun overdueCanBePaid() {
        val overdue = invoice().close(now).markOverdue(now.plusSeconds(1))
        val paid = overdue.registerPayment(Money(100_000, CurrencyCode.BRL), txId, now.plusSeconds(2))
        assertEquals(InvoiceStatus.PAID, paid.status)
    }
}
