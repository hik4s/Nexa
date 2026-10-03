package com.nexa.finance.domain.account

import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class AccountTest {
    private val fixedClock = Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"), ZoneOffset.UTC)
    private val owner = UserId.new()

    @Test
    fun createsNormalizedActiveAccount() {
        val account = Account.create(
            ownerId = owner,
            name = "  Nubank  ",
            type = AccountType.CHECKING,
            openingBalance = Money.fromMinorUnits(150_000, CurrencyCode.BRL),
            clock = fixedClock
        )

        assertEquals("Nubank", account.name)
        assertEquals(AccountStatus.ACTIVE, account.status)
        assertEquals(CurrencyCode.BRL, account.currency)
        assertEquals(150_000, account.openingBalance.amountInMinor)
        assertEquals(Instant.parse("2026-10-03T12:00:00Z"), account.createdAt)
    }

    @Test
    fun rejectsBlankName() {
        assertFailsWith<DomainValidationException> {
            Account.create(owner, "   ", AccountType.CHECKING, Money.zero(CurrencyCode.BRL), clock = fixedClock)
        }
    }

    @Test
    fun archivePreventsRenamingUntilRestored() {
        val account = Account.create(
            owner,
            "Conta",
            AccountType.CHECKING,
            Money.zero(CurrencyCode.BRL),
            clock = fixedClock
        )
        account.archive(Instant.parse("2026-10-03T13:00:00Z"))

        assertEquals(AccountStatus.ARCHIVED, account.status)
        assertFailsWith<DomainValidationException> {
            account.rename("Nova conta", Instant.parse("2026-10-03T14:00:00Z"))
        }

        account.restore(Instant.parse("2026-10-03T15:00:00Z"))
        account.rename("Nova conta", Instant.parse("2026-10-03T16:00:00Z"))
        assertEquals("Nova conta", account.name)
    }
}
