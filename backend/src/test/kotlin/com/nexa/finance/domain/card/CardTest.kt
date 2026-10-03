package com.nexa.finance.domain.card

import com.nexa.finance.domain.account.Account
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.account.AccountType
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CardTest {
    private val now = Instant.parse("2026-10-03T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private val accountId = AccountId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
    private val liabilityAccount = Account.create(userId, "Visa liability", AccountType.CREDIT_CARD, Money.zero(CurrencyCode.BRL), clock, accountId)

    private fun card() = Card.create(
        ownerId = userId,
        liabilityAccount = liabilityAccount,
        name = "Nexa Visa",
        brand = CardBrand.VISA,
        lastFourDigits = "1234",
        creditLimit = Money(500_000, CurrencyCode.BRL),
        closingDay = 10,
        dueDay = 20,
        clock = clock,
    )

    @Test
    fun createsActiveCardWithLiabilityAccount() {
        val card = card()
        assertEquals(CardStatus.ACTIVE, card.status)
        assertEquals(accountId, card.liabilityAccountId)
        assertEquals("1234", card.lastFourDigits)
    }

    @Test
    fun blockedCardCannotBeUsed() {
        assertFailsWith<DomainValidationException> {
            card().block(now).ensurePurchasesAllowed()
        }
    }

    @Test
    fun cancellationCannotBeReversedIntoUsableCard() {
        assertFailsWith<DomainValidationException> {
            card().cancel(now).unblock(now.plusSeconds(1)).ensurePurchasesAllowed()
        }
    }

    @Test
    fun rejectsInvalidLastFourDigits() {
        assertFailsWith<DomainValidationException> {
            Card.create(userId, liabilityAccount, "Card", CardBrand.VISA, "12A4", Money(1000, CurrencyCode.BRL), 10, 20, clock)
        }
    }
}
