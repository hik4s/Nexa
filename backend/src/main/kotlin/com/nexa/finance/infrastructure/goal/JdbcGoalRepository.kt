package com.nexa.finance.infrastructure.goal

import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.goal.Goal
import com.nexa.finance.domain.goal.GoalContribution
import com.nexa.finance.domain.goal.GoalId
import com.nexa.finance.domain.goal.GoalStatus
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import java.sql.Timestamp
import java.time.LocalDate
import java.util.UUID

class JdbcGoalRepository(
    private val connections: DatabaseConnectionProvider = DatabaseConnectionProvider(),
) {
    fun create(goal: Goal) {
        connections.open().use { c ->
            c.prepareStatement(
                """
                INSERT INTO goals
                    (id, user_id, name, target_amount_minor, current_amount_minor, currency,
                     target_date, description, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { s ->
                bindGoal(s, goal)
                s.executeUpdate()
            }
        }
    }

    fun findByOwner(ownerId: UserId): List<Goal> =
        connections.open().use { c ->
            c.prepareStatement(
                """
                SELECT id, user_id, name, target_amount_minor, current_amount_minor, currency,
                       target_date, description, status, created_at, updated_at
                FROM goals
                WHERE user_id = ?
                ORDER BY status, target_date NULLS LAST, created_at DESC, id DESC
                """.trimIndent()
            ).use { s ->
                s.setObject(1, ownerId.value)
                s.executeQuery().use { r ->
                    buildList {
                        while (r.next()) add(r.toGoal())
                    }
                }
            }
        }

    fun findByOwnerAndId(ownerId: UserId, goalId: GoalId): Goal? =
        connections.open().use { c ->
            c.prepareStatement(
                """
                SELECT id, user_id, name, target_amount_minor, current_amount_minor, currency,
                       target_date, description, status, created_at, updated_at
                FROM goals
                WHERE user_id = ? AND id = ?
                """.trimIndent()
            ).use { s ->
                s.setObject(1, ownerId.value)
                s.setObject(2, goalId)
                s.executeQuery().use { r ->
                    if (r.next()) r.toGoal() else null
                }
            }
        }

    fun update(goal: Goal): Goal? =
        connections.open().use { c ->
            c.prepareStatement(
                """
                UPDATE goals
                SET name = ?, target_amount_minor = ?, current_amount_minor = ?, currency = ?,
                    target_date = ?, description = ?, status = ?, updated_at = ?
                WHERE user_id = ? AND id = ?
                RETURNING id, user_id, name, target_amount_minor, current_amount_minor, currency,
                          target_date, description, status, created_at, updated_at
                """.trimIndent()
            ).use { s ->
                bindGoalUpdate(s, goal)
                s.executeQuery().use { r ->
                    if (r.next()) r.toGoal() else null
                }
            }
        }

    fun contribute(
        ownerId: UserId,
        goalId: GoalId,
        amount: Money,
        occurredOn: LocalDate,
    ): Pair<Goal, GoalContribution> =
        connections.open().use { c ->
            c.autoCommit = false
            try {
                val current = c.prepareStatement(
                    """
                    SELECT id, user_id, name, target_amount_minor, current_amount_minor, currency,
                           target_date, description, status, created_at, updated_at
                    FROM goals
                    WHERE user_id = ? AND id = ?
                    FOR UPDATE
                    """.trimIndent()
                ).use { s ->
                    s.setObject(1, ownerId.value)
                    s.setObject(2, goalId)
                    s.executeQuery().use { r ->
                        if (r.next()) r.toGoal()
                        else throw DomainValidationException("Goal was not found.")
                    }
                }

                val updated = current.addContribution(amount)
                c.prepareStatement(
                    """
                    UPDATE goals
                    SET current_amount_minor = ?, status = ?, updated_at = ?
                    WHERE id = ? AND user_id = ?
                    """.trimIndent()
                ).use { s ->
                    s.setLong(1, updated.currentAmount.amountInMinor)
                    s.setString(2, updated.status.name)
                    s.setTimestamp(3, Timestamp.from(updated.updatedAt))
                    s.setObject(4, updated.id)
                    s.setObject(5, ownerId.value)
                    if (s.executeUpdate() != 1) {
                        throw DomainValidationException("Goal was not found.")
                    }
                }

                val contribution = GoalContribution.create(
                    goalId = updated.id,
                    userId = ownerId,
                    amount = amount,
                    occurredOn = occurredOn,
                )
                c.prepareStatement(
                    """
                    INSERT INTO goal_contributions
                        (id, goal_id, user_id, amount_minor, currency, occurred_on, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                ).use { s ->
                    s.setObject(1, contribution.id)
                    s.setObject(2, contribution.goalId)
                    s.setObject(3, contribution.userId.value)
                    s.setLong(4, contribution.amount.amountInMinor)
                    s.setString(5, contribution.amount.currency.name)
                    s.setObject(6, contribution.occurredOn)
                    s.setTimestamp(7, Timestamp.from(contribution.createdAt))
                    s.executeUpdate()
                }

                c.commit()
                updated to contribution
            } catch (e: Exception) {
                c.rollback()
                throw e
            } finally {
                c.autoCommit = true
            }
        }

    fun listContributions(ownerId: UserId, goalId: GoalId): List<GoalContribution> =
        connections.open().use { c ->
            c.prepareStatement(
                """
                SELECT gc.id, gc.goal_id, gc.user_id, gc.amount_minor, gc.currency, gc.occurred_on, gc.created_at
                FROM goal_contributions gc
                JOIN goals g ON g.id = gc.goal_id
                WHERE gc.user_id = ? AND gc.goal_id = ? AND g.user_id = ?
                ORDER BY gc.occurred_on DESC, gc.created_at DESC, gc.id DESC
                """.trimIndent()
            ).use { s ->
                s.setObject(1, ownerId.value)
                s.setObject(2, goalId)
                s.setObject(3, ownerId.value)
                s.executeQuery().use { r ->
                    buildList {
                        while (r.next()) add(r.toContribution())
                    }
                }
            }
        }

    private fun bindGoal(s: java.sql.PreparedStatement, goal: Goal) {
        s.setObject(1, goal.id)
        s.setObject(2, goal.userId.value)
        bindGoalFields(s, goal, 3)
    }

    private fun bindGoalFields(s: java.sql.PreparedStatement, goal: Goal, startIndex: Int) {
        s.setString(startIndex, goal.name)
        s.setLong(startIndex + 1, goal.targetAmount.amountInMinor)
        s.setLong(startIndex + 2, goal.currentAmount.amountInMinor)
        s.setString(startIndex + 3, goal.targetAmount.currency.name)
        if (goal.targetDate == null) s.setNull(startIndex + 4, java.sql.Types.DATE) else s.setObject(startIndex + 4, goal.targetDate)
        s.setString(startIndex + 5, goal.description)
        s.setString(startIndex + 6, goal.status.name)
        s.setTimestamp(startIndex + 7, Timestamp.from(goal.createdAt))
        s.setTimestamp(startIndex + 8, Timestamp.from(goal.updatedAt))
    }

    private fun bindGoalUpdate(s: java.sql.PreparedStatement, goal: Goal) {
        s.setString(1, goal.name)
        s.setLong(2, goal.targetAmount.amountInMinor)
        s.setLong(3, goal.currentAmount.amountInMinor)
        s.setString(4, goal.targetAmount.currency.name)
        if (goal.targetDate == null) s.setNull(5, java.sql.Types.DATE) else s.setObject(5, goal.targetDate)
        s.setString(6, goal.description)
        s.setString(7, goal.status.name)
        s.setTimestamp(8, Timestamp.from(goal.updatedAt))
        s.setObject(9, goal.userId.value)
        s.setObject(10, goal.id)
    }

    private fun java.sql.ResultSet.toGoal(): Goal = Goal.restore(
        id = getObject("id", UUID::class.java),
        userId = UserId(getObject("user_id", UUID::class.java)),
        name = getString("name"),
        targetAmountMinor = getLong("target_amount_minor"),
        currentAmountMinor = getLong("current_amount_minor"),
        currency = CurrencyCode.valueOf(getString("currency")),
        targetDate = getObject("target_date", LocalDate::class.java),
        description = getString("description"),
        status = GoalStatus.valueOf(getString("status")),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )

    private fun java.sql.ResultSet.toContribution(): GoalContribution = GoalContribution.restore(
        id = getObject("id", UUID::class.java),
        goalId = getObject("goal_id", UUID::class.java),
        userId = UserId(getObject("user_id", UUID::class.java)),
        amountMinor = getLong("amount_minor"),
        currency = CurrencyCode.valueOf(getString("currency")),
        occurredOn = getObject("occurred_on", LocalDate::class.java),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}
