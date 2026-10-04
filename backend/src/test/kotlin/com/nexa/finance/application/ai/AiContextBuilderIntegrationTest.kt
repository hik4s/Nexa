package com.nexa.finance.application.ai

import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.category.CategoryId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.transaction.Transaction
import com.nexa.finance.domain.transaction.TransactionDirection
import com.nexa.finance.domain.transaction.TransactionSource
import com.nexa.finance.domain.transaction.TransactionStatus
import com.nexa.finance.domain.transaction.TransactionType
import com.nexa.finance.infrastructure.budget.JdbcBudgetRepository
import com.nexa.finance.infrastructure.category.JdbcCategoryRepository
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import com.nexa.finance.infrastructure.goal.JdbcGoalRepository
import com.nexa.finance.infrastructure.transaction.JdbcTransactionRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

class AiContextBuilderIntegrationTest {
    @Test
    fun contextAggregatesPostedDataAndDoesNotExposeDescriptions() {
        assumeTrue(!System.getenv("POSTGRES_PASSWORD").isNullOrBlank())

        val connections = DatabaseConnectionProvider()
        val txRepository = JdbcTransactionRepository(connections)
        val builder = AiContextBuilder(
            transactions = txRepository,
            budgets = JdbcBudgetRepository(connections),
            goals = JdbcGoalRepository(connections),
            categories = JdbcCategoryRepository(connections),
        )
        val userId = createUser(connections)
        val accountId = createAccount(connections, userId)
        val categoryId = createCategory(connections, userId)

        try {
            val now = LocalDate.of(2026, 10, 4)
            txRepository.create(
                userId,
                Transaction.create(
                    accountId = AccountId(accountId),
                    amount = Money(500_000, CurrencyCode.BRL),
                    type = TransactionType.INCOME,
                    direction = TransactionDirection.CREDIT,
                    occurredOn = now,
                    description = "Salário secreto de Kauan",
                    source = TransactionSource.MANUAL,
                )
            )
            txRepository.create(
                userId,
                Transaction.create(
                    accountId = AccountId(accountId),
                    amount = Money(120_000, CurrencyCode.BRL),
                    type = TransactionType.EXPENSE,
                    direction = TransactionDirection.DEBIT,
                    occurredOn = now,
                    description = "Compra privada do usuário",
                    categoryId = CategoryId(categoryId),
                    source = TransactionSource.MANUAL,
                )
            )
            txRepository.create(
                userId,
                Transaction.create(
                    accountId = AccountId(accountId),
                    amount = Money(900_000, CurrencyCode.BRL),
                    type = TransactionType.EXPENSE,
                    direction = TransactionDirection.DEBIT,
                    occurredOn = now,
                    description = "Pendente não deve entrar",
                    status = TransactionStatus.PENDING,
                    source = TransactionSource.MANUAL,
                )
            )
            txRepository.create(
                userId,
                Transaction.create(
                    accountId = AccountId(accountId),
                    amount = Money(80_000, CurrencyCode.BRL),
                    type = TransactionType.EXPENSE,
                    direction = TransactionDirection.DEBIT,
                    occurredOn = now.minusMonths(1),
                    description = "Despesa anterior",
                    categoryId = CategoryId(categoryId),
                    source = TransactionSource.MANUAL,
                )
            )

            val context = builder.build(userId, YearMonth.of(2026, 10))
            assertEquals(500_000, context.incomeMinor)
            assertEquals(120_000, context.expenseMinor)
            assertEquals(80_000, context.previousExpenseMinor)
            assertEquals(1, context.categoryFacts.size)
            assertEquals(120_000, context.categoryFacts.first().amountMinor)
            assertTrue(context.facts.none { it.label.contains("secreto", ignoreCase = true) })
            assertTrue(context.facts.none { it.label.contains("privada", ignoreCase = true) })
        } finally {
            deleteUser(connections, userId)
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
                s.setString(2, "ai-context-" + id + "@example.com")
                s.setString(3, "AI Context")
                s.setString(4, "UTC")
                s.executeUpdate()
            }
        }
        return UserId(id)
    }

    private fun createAccount(connections: DatabaseConnectionProvider, userId: UserId): UUID {
        val id = UUID.randomUUID()
        connections.open().use { c ->
            c.prepareStatement(
                """
                INSERT INTO accounts
                    (id, owner_id, name, type, currency, opening_balance_minor, status, created_at, updated_at)
                VALUES (?, ?, 'AI Context Account', 'CHECKING', 'BRL', 0, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """.trimIndent()
            ).use { s ->
                s.setObject(1, id)
                s.setObject(2, userId.value)
                s.executeUpdate()
            }
        }
        return id
    }

    private fun createCategory(connections: DatabaseConnectionProvider, userId: UserId): UUID {
        val id = UUID.randomUUID()
        connections.open().use { c ->
            c.prepareStatement(
                """
                INSERT INTO categories
                    (id, owner_id, name, type, sort_order, status, created_at, updated_at)
                VALUES (?, ?, 'Context Test', 'EXPENSE', 999, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """.trimIndent()
            ).use { s ->
                s.setObject(1, id)
                s.setObject(2, userId.value)
                s.executeUpdate()
            }
        }
        return id
    }

    private fun deleteUser(connections: DatabaseConnectionProvider, id: UserId) {
        connections.open().use { c ->
            c.prepareStatement(
                "DELETE FROM transactions WHERE account_id IN (SELECT id FROM accounts WHERE owner_id = ?)"
            ).use { s ->
                s.setObject(1, id.value)
                s.executeUpdate()
            }
            c.prepareStatement("DELETE FROM accounts WHERE owner_id = ?").use { s ->
                s.setObject(1, id.value)
                s.executeUpdate()
            }
            c.prepareStatement("DELETE FROM categories WHERE owner_id = ?").use { s ->
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
