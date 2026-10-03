package com.nexa.finance.domain.transfer

import com.nexa.finance.domain.account.Account
import com.nexa.finance.domain.account.AccountType
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
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

class TransferTest {
    private val fixedNow = Instant.parse("2026-10-03T12:00:00Z")
    private val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
    private val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))

    private fun account(name: String, id: UUID = UUID.randomUUID()): Account =
        Account.create(userId, name, AccountType.CHECKING, Money(100_000L, CurrencyCode.BRL), clock, com.nexa.finance.domain.account.AccountId(id))

    @Test fun createsLinkedPendingTransactions() {
        val transfer = Transfer.create(userId, account("Origem"), account("Destino"), Money(18_900L, CurrencyCode.BRL), LocalDate.of(2026, 10, 3), clock = clock)
        assertEquals(TransferStatus.PENDING, transfer.status)
        assertEquals(TransactionDirection.DEBIT, transfer.sourceTransaction.direction)
        assertEquals(TransactionDirection.CREDIT, transfer.destinationTransaction.direction)
        assertEquals(TransactionType.TRANSFER, transfer.sourceTransaction.type)
        assertEquals(TransactionType.TRANSFER, transfer.destinationTransaction.type)
        assertEquals(TransactionStatus.PENDING, transfer.sourceTransaction.status)
        assertEquals(TransactionStatus.PENDING, transfer.destinationTransaction.status)
        assertEquals(transfer.destinationTransaction.id, transfer.sourceTransaction.relatedTransactionId)
        assertEquals(transfer.sourceTransaction.id, transfer.destinationTransaction.relatedTransactionId)
    }

    @Test fun postPostsBothTransactions() {
        val transfer = Transfer.create(userId, account("Origem"), account("Destino"), Money(5_000L, CurrencyCode.BRL), LocalDate.of(2026, 10, 3), clock = clock)
        val posted = transfer.post(fixedNow.plusSeconds(60))
        assertEquals(TransferStatus.POSTED, posted.status)
        assertEquals(TransactionStatus.POSTED, posted.sourceTransaction.status)
        assertEquals(TransactionStatus.POSTED, posted.destinationTransaction.status)
    }

    @Test fun cancelCancelsBothPendingTransactions() {
        val transfer = Transfer.create(userId, account("Origem"), account("Destino"), Money(5_000L, CurrencyCode.BRL), LocalDate.of(2026, 10, 3), clock = clock)
        val cancelled = transfer.cancel(fixedNow.plusSeconds(60))
        assertEquals(TransferStatus.CANCELLED, cancelled.status)
        assertEquals(TransactionStatus.CANCELLED, cancelled.sourceTransaction.status)
        assertEquals(TransactionStatus.CANCELLED, cancelled.destinationTransaction.status)
    }

    @Test fun postedTransferCannotBeCancelled() {
        val transfer = Transfer.create(userId, account("Origem"), account("Destino"), Money(5_000L, CurrencyCode.BRL), LocalDate.of(2026, 10, 3), clock = clock).post(fixedNow.plusSeconds(60))
        assertFailsWith<DomainValidationException> { transfer.cancel(fixedNow.plusSeconds(120)) }
    }

    @Test fun cancelledTransferCannotBePosted() {
        val transfer = Transfer.create(userId, account("Origem"), account("Destino"), Money(5_000L, CurrencyCode.BRL), LocalDate.of(2026, 10, 3), clock = clock).cancel(fixedNow.plusSeconds(60))
        assertFailsWith<DomainValidationException> { transfer.post(fixedNow.plusSeconds(120)) }
    }

    @Test fun rejectsDifferentCurrencies() {
        val source = account("Origem")
        val destination = Account.create(userId, "Destino", AccountType.CHECKING, Money(100_000L, CurrencyCode.USD), clock)
        assertFailsWith<DomainValidationException> { Transfer.create(userId, source, destination, Money(5_000L, CurrencyCode.BRL), LocalDate.of(2026, 10, 3), clock = clock) }
    }

    @Test fun rejectsAccountsOwnedByDifferentUsers() {
        val otherUser = UserId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
        val destination = Account.create(otherUser, "Outro", AccountType.CHECKING, Money(100_000L, CurrencyCode.BRL), clock)
        assertFailsWith<DomainValidationException> { Transfer.create(userId, account("Origem"), destination, Money(5_000L, CurrencyCode.BRL), LocalDate.of(2026, 10, 3), clock = clock) }
    }

    @Test fun rejectsArchivedAccounts() {
        val source = account("Origem")
        source.archive(fixedNow)
        assertFailsWith<DomainValidationException> { Transfer.create(userId, source, account("Destino"), Money(5_000L, CurrencyCode.BRL), LocalDate.of(2026, 10, 3), clock = clock) }
    }

    @Test fun rejectsSameAccount() {
        val source = account("Origem")
        assertFailsWith<DomainValidationException> { Transfer.create(userId, source, source, Money(5_000L, CurrencyCode.BRL), LocalDate.of(2026, 10, 3), clock = clock) }
    }

    @Test fun rejectsNonPositiveAmount() {
        assertFailsWith<DomainValidationException> { Transfer.create(userId, account("Origem"), account("Destino"), Money(0L, CurrencyCode.BRL), LocalDate.of(2026, 10, 3), clock = clock) }
    }

    @Test fun rejectsAmountCurrencyMismatch() {
        assertFailsWith<DomainValidationException> { Transfer.create(userId, account("Origem"), account("Destino"), Money(5_000L, CurrencyCode.USD), LocalDate.of(2026, 10, 3), clock = clock) }
    }
}
