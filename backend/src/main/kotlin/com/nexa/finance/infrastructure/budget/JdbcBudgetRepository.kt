package com.nexa.finance.infrastructure.budget

import com.nexa.finance.domain.budget.Budget
import com.nexa.finance.domain.budget.BudgetId
import com.nexa.finance.domain.budget.BudgetStatus
import com.nexa.finance.domain.category.CategoryId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import java.sql.Timestamp
import java.time.LocalDate
import java.util.UUID

class JdbcBudgetRepository(
    private val connections: DatabaseConnectionProvider = DatabaseConnectionProvider(),
) {
    fun create(budget: Budget) {
        connections.open().use { c ->
            c.prepareStatement(
                """
                INSERT INTO budgets
                    (id, user_id, category_id, amount_minor, currency, start_on, end_on, description, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { s ->
                s.setObject(1, budget.id)
                s.setObject(2, budget.userId.value)
                setUuidOrNull(s, 3, budget.categoryId?.value)
                s.setLong(4, budget.amount.amountInMinor)
                s.setString(5, budget.amount.currency.name)
                s.setObject(6, budget.startOn)
                s.setObject(7, budget.endOn)
                s.setString(8, budget.description)
                s.setString(9, budget.status.name)
                s.setTimestamp(10, Timestamp.from(budget.createdAt))
                s.setTimestamp(11, Timestamp.from(budget.updatedAt))
                s.executeUpdate()
            }
        }
    }

    fun findByOwner(ownerId: UserId): List<Budget> =
        connections.open().use { c ->
            c.prepareStatement(
                """
                SELECT id, user_id, category_id, amount_minor, currency, start_on, end_on,
                       description, status, created_at, updated_at
                FROM budgets
                WHERE user_id = ?
                ORDER BY start_on DESC, created_at DESC, id DESC
                """.trimIndent()
            ).use { s ->
                s.setObject(1, ownerId.value)
                s.executeQuery().use { r ->
                    buildList {
                        while (r.next()) add(r.toBudget())
                    }
                }
            }
        }

    fun findByOwnerAndId(ownerId: UserId, budgetId: BudgetId): Budget? =
        connections.open().use { c ->
            c.prepareStatement(
                """
                SELECT id, user_id, category_id, amount_minor, currency, start_on, end_on,
                       description, status, created_at, updated_at
                FROM budgets
                WHERE user_id = ? AND id = ?
                """.trimIndent()
            ).use { s ->
                s.setObject(1, ownerId.value)
                s.setObject(2, budgetId)
                s.executeQuery().use { r ->
                    if (r.next()) r.toBudget() else null
                }
            }
        }

    fun update(budget: Budget): Budget? =
        connections.open().use { c ->
            c.prepareStatement(
                """
                UPDATE budgets
                SET category_id = ?, amount_minor = ?, currency = ?, start_on = ?, end_on = ?,
                    description = ?, status = ?, updated_at = ?
                WHERE user_id = ? AND id = ?
                RETURNING id, user_id, category_id, amount_minor, currency, start_on, end_on,
                          description, status, created_at, updated_at
                """.trimIndent()
            ).use { s ->
                setUuidOrNull(s, 1, budget.categoryId?.value)
                s.setLong(2, budget.amount.amountInMinor)
                s.setString(3, budget.amount.currency.name)
                s.setObject(4, budget.startOn)
                s.setObject(5, budget.endOn)
                s.setString(6, budget.description)
                s.setString(7, budget.status.name)
                s.setTimestamp(8, Timestamp.from(budget.updatedAt))
                s.setObject(9, budget.userId.value)
                s.setObject(10, budget.id)
                s.executeQuery().use { r ->
                    if (r.next()) r.toBudget() else null
                }
            }
        }

    fun isCategoryAvailable(ownerId: UserId, categoryId: CategoryId): Boolean =
        connections.open().use { c ->
            c.prepareStatement(
                """
                SELECT 1
                FROM categories
                WHERE id = ? AND status = 'ACTIVE'
                  AND type IN ('EXPENSE', 'BOTH')
                  AND (owner_id IS NULL OR owner_id = ?)
                LIMIT 1
                """.trimIndent()
            ).use { s ->
                s.setObject(1, categoryId.value)
                s.setObject(2, ownerId.value)
                s.executeQuery().use { r -> r.next() }
            }
        }

    fun consumedMinor(ownerId: UserId, budget: Budget): Long =
        connections.open().use { c ->
            c.prepareStatement(
                """
                SELECT COALESCE(SUM(
                    CASE
                        WHEN t.type IN ('EXPENSE', 'PURCHASE') THEN t.amount_minor
                        WHEN t.type = 'REFUND' THEN -t.amount_minor
                        ELSE 0
                    END
                ), 0)
                FROM transactions t
                JOIN accounts a ON a.id = t.account_id
                WHERE a.owner_id = ?
                  AND t.status = 'POSTED'
                  AND t.occurred_on BETWEEN ? AND ?
                  AND t.type IN ('EXPENSE', 'PURCHASE', 'REFUND')
                  AND t.currency = ?
                  AND (? IS NULL OR t.category_id = ?)
                """.trimIndent()
            ).use { s ->
                s.setObject(1, ownerId.value)
                s.setObject(2, budget.startOn)
                s.setObject(3, budget.endOn)
                s.setString(4, budget.amount.currency.name)
                setUuidOrNull(s, 5, budget.categoryId?.value)
                setUuidOrNull(s, 6, budget.categoryId?.value)
                s.executeQuery().use { r ->
                    r.next()
                    r.getLong(1)
                }
            }
        }

    private fun setUuidOrNull(statement: java.sql.PreparedStatement, index: Int, value: UUID?) {
        if (value == null) statement.setNull(index, java.sql.Types.OTHER)
        else statement.setObject(index, value)
    }

    private fun java.sql.ResultSet.toBudget(): Budget =
        Budget.restore(
            id = getObject("id", UUID::class.java),
            userId = UserId(getObject("user_id", UUID::class.java)),
            categoryId = getObject("category_id", UUID::class.java)?.let(::CategoryId),
            amountMinor = getLong("amount_minor"),
            currency = CurrencyCode.valueOf(getString("currency")),
            startOn = getObject("start_on", LocalDate::class.java),
            endOn = getObject("end_on", LocalDate::class.java),
            description = getString("description"),
            status = BudgetStatus.valueOf(getString("status")),
            createdAt = getTimestamp("created_at").toInstant(),
            updatedAt = getTimestamp("updated_at").toInstant(),
        )
}
