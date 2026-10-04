package com.nexa.finance.domain.card

import com.nexa.finance.domain.account.Account
import com.nexa.finance.domain.account.AccountBalance
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.account.AccountType
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.invoice.Invoice
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class CardPaymentTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"), ZoneOffset.UTC)
    private val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private val cardAccountId = AccountId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
    private val bankAccountId = AccountId(UUID.fromString("00000000-0000-0000-0000-000000000004"))
    private val cardAccount = Account.create(userId, "Visa liability", AccountType.CREDIT_CARD, Money.zero(CurrencyCode.BRL), clock, cardAccountId)
    private val bankAccount = Account.create(userId, "Checking", AccountType.CHECKING, Money(500_000, CurrencyCode.BRL), clock, bankAccountId)
    private val card = Card.create(userId, cardAccount, "Visa", CardBrand.VISA, "1234", Money(500_000, CurrencyCode.BRL), 10, 20, clock)
    @Test
    fun blockedCardCanStillReceiveInvoicePayment() {
        val purchase = CardLedger.purchase(card, Money(120_000, CurrencyCode.BRL), LocalDate.of(2026, 10, 3), clock = clock).post()
        val invoice = Invoice.create(card.id, LocalDate.of(2026, 9, 11), LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 20), Money(120_000, CurrencyCode.BRL), listOf(purchase.id), clock).close(clock.instant())
        val blocked = card.block(clock.instant())
        val payment = CardPayment.create(userId, bankAccount, blocked, invoice, Money(120_000, CurrencyCode.BRL), LocalDate.of(2026, 10, 20), clock = clock)
        assertEquals(120_000, payment.amount.amountInMinor)
    }
    @Test
    fun paymentDecreasesCashAndCardDebt() {
        val purchase = CardLedger.purchase(card, Money(120_000, CurrencyCode.BRL), LocalDate.of(2026, 10, 3), clock = clock).post()
        val invoice = Invoice.create(card.id, LocalDate.of(2026, 9, 11), LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 20), Money(120_000, CurrencyCode.BRL), listOf(purchase.id), clock)
            .close(clock.instant())
        val payment = CardPayment.create(userId, bankAccount, card, invoice, Money(120_000, CurrencyCode.BRL), LocalDate.of(2026, 10, 20), clock = clock).post()

        val bankBalance = AccountBalance.calculate(bankAccount, listOf(payment.sourceTransaction))
        val cardBalance = AccountBalance.calculate(cardAccount, listOf(purchase, payment.liabilityTransaction))

        assertEquals(380_000, bankBalance.amountInMinor)
        assertEquals(0, cardBalance.amountInMinor)
    }
}
