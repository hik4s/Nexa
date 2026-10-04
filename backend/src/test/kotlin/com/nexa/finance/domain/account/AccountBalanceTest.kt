package com.nexa.finance.domain.account

import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.transaction.Transaction
import com.nexa.finance.domain.transaction.TransactionDirection
import com.nexa.finance.domain.transaction.TransactionStatus
import com.nexa.finance.domain.transaction.TransactionType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountBalanceTest {
    private val currency = CurrencyCode.BRL
    private val userId = UserId.new()

    @Test
    fun calculatesPostedTransactionsForAssetAccount() {
        val account = Account.create(
            ownerId = userId,
            name = "Conta",
            type = AccountType.CHECKING,
            openingBalance = Money(1_000_00, currency),
        )
        val income = Transaction.create(
            accountId = account.id,
            amount = Money(250_00, currency),
            type = TransactionType.INCOME,
            direction = TransactionDirection.CREDIT,
            occurredOn = LocalDate.of(2026, 10, 1),
        )
        val expense = Transaction.create(
            accountId = account.id,
            amount = Money(80_50, currency),
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.DEBIT,
            occurredOn = LocalDate.of(2026, 10, 2),
        )

        assertEquals(Money(1_169_50, currency), AccountBalance.calculate(account, listOf(income, expense)))
    }

    @Test
    fun ignoresPendingTransactions() {
        val account = Account.create(
            ownerId = userId,
            name = "Conta",
            type = AccountType.CHECKING,
            openingBalance = Money(500_00, currency),
        )
        val pendingIncome = Transaction.create(
            accountId = account.id,
            amount = Money(100_00, currency),
            type = TransactionType.INCOME,
            direction = TransactionDirection.CREDIT,
            occurredOn = LocalDate.of(2026, 10, 1),
            status = TransactionStatus.PENDING,
        )

        assertEquals(Money(500_00, currency), AccountBalance.calculate(account, listOf(pendingIncome)))
    }

    @Test
    fun calculatesCreditCardLiabilityWithAccountingDirection() {
        val account = Account.create(
            ownerId = userId,
            name = "Cartão",
            type = AccountType.CREDIT_CARD,
            openingBalance = Money.zero(currency),
        )
        val purchase = Transaction.create(
            accountId = account.id,
            amount = Money(300_00, currency),
            type = TransactionType.PURCHASE,
            direction = TransactionDirection.CREDIT,
            occurredOn = LocalDate.of(2026, 10, 1),
        )
        val payment = Transaction.create(
            accountId = account.id,
            amount = Money(100_00, currency),
            type = TransactionType.PAYMENT,
            direction = TransactionDirection.DEBIT,
            occurredOn = LocalDate.of(2026, 10, 5),
        )

        assertEquals(Money(200_00, currency), AccountBalance.calculate(account, listOf(purchase, payment)))
    }
}
