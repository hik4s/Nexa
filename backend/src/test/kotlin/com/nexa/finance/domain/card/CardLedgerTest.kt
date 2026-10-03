package com.nexa.finance.domain.card

import com.nexa.finance.domain.account.Account
import com.nexa.finance.domain.account.AccountBalance
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.account.AccountType
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.invoice.InvoiceId
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CardLedgerTest {
    private val clock = Clock.fixed(java.time.Instant.parse("2026-10-03T12:00:00Z"), ZoneOffset.UTC)
    private val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private val accountId = AccountId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
    private val account = Account.create(userId, "Visa liability", AccountType.CREDIT_CARD, Money.zero(CurrencyCode.BRL), clock, accountId)
    private val card = Card.create(userId, account, "Visa", CardBrand.VISA, "1234", Money(500_000, CurrencyCode.BRL), 10, 20, clock)
    private val invoiceId = InvoiceId(UUID.fromString("00000000-0000-0000-0000-000000000003"))

    @Test
    fun purchaseIncreasesCardDebtAndConsumesLimit() {
        val purchase = CardLedger.purchase(card, Money(120_000, CurrencyCode.BRL), LocalDate.of(2026, 10, 3), invoiceId, clock = clock)
        val posted = purchase.post()
        val balance = AccountBalance.calculate(account, listOf(posted))
        assertEquals(120_000, balance.amountInMinor)
        assertEquals(380_000, CardLedger.availableLimit(card, balance).amountInMinor)
        assertEquals(com.nexa.finance.domain.transaction.TransactionDirection.CREDIT, posted.direction)
    }

    @Test
    fun refundReducesCardDebt() {
        val purchase = CardLedger.purchase(card, Money(120_000, CurrencyCode.BRL), LocalDate.of(2026, 10, 3), invoiceId, clock = clock).post()
        val refund = CardLedger.refund(card, Money(20_000, CurrencyCode.BRL), LocalDate.of(2026, 10, 4), invoiceId, clock = clock).post()
        val balance = AccountBalance.calculate(account, listOf(purchase, refund))
        assertEquals(100_000, balance.amountInMinor)
    }

    @Test
    fun paymentReducesCardDebtAndRestoresLimit() {
        val purchase = CardLedger.purchase(card, Money(120_000, CurrencyCode.BRL), LocalDate.of(2026, 10, 3), invoiceId, clock = clock).post()
        val payment = CardLedger.payment(card, Money(50_000, CurrencyCode.BRL), LocalDate.of(2026, 10, 20), invoiceId, clock = clock).post()
        val balance = AccountBalance.calculate(account, listOf(purchase, payment))
        assertEquals(70_000, balance.amountInMinor)
        assertEquals(430_000, CardLedger.availableLimit(card, balance).amountInMinor)
    }

    @Test
}

    @Test
    fun exhaustedLimitReportsZeroAvailable() {
        val purchase = CardLedger.purchase(card, Money(500_000, CurrencyCode.BRL), LocalDate.of(2026, 10, 3), invoiceId, clock = clock).post()
        val balance = AccountBalance.calculate(account, listOf(purchase))
        assertEquals(0, CardLedger.availableLimit(card, balance).amountInMinor)
    }

