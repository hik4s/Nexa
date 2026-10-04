package com.nexa.finance.infrastructure.budget

import com.nexa.finance.domain.account.Account
import com.nexa.finance.domain.account.AccountType
import com.nexa.finance.domain.budget.Budget
import com.nexa.finance.domain.category.CategoryId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.transaction.Transaction
import com.nexa.finance.domain.transaction.TransactionDirection
import com.nexa.finance.domain.transaction.TransactionStatus
import com.nexa.finance.domain.transaction.TransactionType
import com.nexa.finance.infrastructure.account.JdbcAccountRepository
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import com.nexa.finance.infrastructure.transaction.JdbcTransactionRepository
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue

class JdbcBudgetRepositoryIntegrationTest {
    @Test
    fun calculatesNetConsumptionFromPostedLedgerEventsWithIsolation() {
        assumeTrue(!System.getenv("POSTGRES_PASSWORD").isNullOrBlank())

        val connections = DatabaseConnectionProvider()
        val budgetRepository = JdbcBudgetRepository(connections)
        val accountRepository = JdbcAccountRepository(connections)
        val transactionRepository = JdbcTransactionRepository(connections)

        val userId = createUser(connections)
        val otherUserId = createUser(connections)
        val account = Account.create(userId, "Budget Test Account", AccountType.CHECKING, Money.zero(CurrencyCode.BRL))
        val otherAccount = Account.create(otherUserId, "Other Budget Test Account", AccountType.CHECKING, Money.zero(CurrencyCode.BRL))
        accountRepository.create(account)
        accountRepository.create(otherAccount)

        val foodCategory = CategoryId(UUID.fromString("10000000-0000-0000-0000-000000000001"))
        try {
            val budget = Budget.create(
                userId = userId,
                amount = Money(50_000, CurrencyCode.BRL),
                startOn = LocalDate.of(2026, 10, 1),
                endOn = LocalDate.of(2026, 10, 31),
                categoryId = foodCategory,
            )
            budgetRepository.create(budget)

            transactionRepository.create(
                userId,
                Transaction.create(
                    accountId = account.id,
                    amount = Money(30_000, CurrencyCode.BRL),
                    type = TransactionType.EXPENSE,
                    direction = TransactionDirection.DEBIT,
                    status = TransactionStatus.POSTED,
                    occurredOn = LocalDate.of(2026, 10, 5),
                    categoryId = foodCategory,
                )
            )
            transactionRepository.create(
                userId,
                Transaction.create(
                    accountId = account.id,
                    amount = Money(20_000, CurrencyCode.BRL),
                    type = TransactionType.EXPENSE,
                    direction = TransactionDirection.DEBIT,
                    status = TransactionStatus.POSTED,
                    occurredOn = LocalDate.of(2026, 10, 10),
                    categoryId = CategoryId(UUID.fromString("10000000-0000-0000-0000-000000000002")),
                )
            )
            transactionRepository.create(
                userId,
                Transaction.create(
                    accountId = account.id,
                    amount = Money(10_000, CurrencyCode.BRL),
                    type = TransactionType.EXPENSE,
                    direction = TransactionDirection.DEBIT,
                    status = TransactionStatus.PENDING,
                    occurredOn = LocalDate.of(2026, 10, 11),
                    categoryId = foodCategory,
                )
            )
            transactionRepository.create(
                userId,
                Transaction.create(
                    accountId = account.id,
                    amount = Money(10_000, CurrencyCode.BRL),
                    type = TransactionType.PURCHASE,
                    direction = TransactionDirection.CREDIT,
                    status = TransactionStatus.POSTED,
                    occurredOn = LocalDate.of(2026, 10, 12),
                    categoryId = foodCategory,
                )
            )
            transactionRepository.create(
                userId,
                Transaction.create(
                    accountId = account.id,
                    amount = Money(5_000, CurrencyCode.BRL),
                    type = TransactionType.REFUND,
                    direction = TransactionDirection.DEBIT,
                    status = TransactionStatus.POSTED,
                    occurredOn = LocalDate.of(2026, 10, 13),
                    categoryId = foodCategory,
                )
            )
            transactionRepository.create(
                userId,
                Transaction.create(
                    accountId = account.id,
                    amount = Money(8_000, CurrencyCode.BRL),
                    type = TransactionType.EXPENSE,
                    direction = TransactionDirection.DEBIT,
                    status = TransactionStatus.POSTED,
                    occurredOn = LocalDate.of(2026, 11, 1),
                    categoryId = foodCategory,
                )
            )
            transactionRepository.create(
                otherUserId,
                Transaction.create(
                    accountId = otherAccount.id,
                    amount = Money(40_000, CurrencyCode.BRL),
                    type = TransactionType.EXPENSE,
                    direction = TransactionDirection.DEBIT,
                    status = TransactionStatus.POSTED,
                    occurredOn = LocalDate.of(2026, 10, 15),
                    categoryId = foodCategory,
                )
            )

            assertEquals(35_000L, budgetRepository.consumedMinor(userId, budget))
        } finally {
            deleteUser(connections, userId)
            deleteUser(connections, otherUserId)
        }
    }

    private fun createUser(connections: DatabaseConnectionProvider): UserId {
        val id = UUID.randomUUID()
        connections.open().use { c ->
            c.prepareStatement(
                """
                INSERT INTO users
                    (id, email, display_name, time_zone, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """.trimIndent()
            ).use { s ->
                s.setObject(1, id)
                s.setString(2, "budget-${id}@example.com")
                s.setString(3, "Budget Integration")
                s.setString(4, "UTC")
                s.executeUpdate()
            }
        }
        return UserId(id)
    }

    private fun deleteUser(connections: DatabaseConnectionProvider, id: UserId) {
        connections.open().use { c ->
            c.prepareStatement(
                "DELETE FROM transactions WHERE account_id IN (SELECT id FROM accounts WHERE owner_id = ?)"
            ).use { s ->
                s.setObject(1, id.value)
                s.executeUpdate()
            }
            c.prepareStatement("DELETE FROM budgets WHERE user_id = ?").use { s ->
                s.setObject(1, id.value)
                s.executeUpdate()
            }
            c.prepareStatement("DELETE FROM accounts WHERE owner_id = ?").use { s ->
                s.setObject(1, id.value)
                s.executeUpdate()
            }
            c.prepareStatement("DELETE FROM users WHERE id = ?").use { s ->
                s.setObject(1, id.value)
                s.executeUpdate()
            }
        }
    }
}
