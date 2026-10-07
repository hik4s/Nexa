package com.nexa.finance.infrastructure.goal

import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.goal.Goal
import com.nexa.finance.domain.goal.GoalStatus
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Assumptions.assumeTrue

class JdbcGoalRepositoryIntegrationTest {
    @Test
    fun contributionsUpdateGoalAtomicallyAndStayIsolatedByUser() {
        assumeTrue(!System.getenv("POSTGRES_PASSWORD").isNullOrBlank())

        val connections = DatabaseConnectionProvider()
        val repository = JdbcGoalRepository(connections)
        val userId = createUser(connections, "goal-primary")
        val otherUserId = createUser(connections, "goal-other")

        try {
            val goal = Goal.create(
                userId = userId,
                name = "Reserva de emergência",
                targetAmount = Money(10_000, CurrencyCode.BRL),
                targetDate = LocalDate.of(2027, 10, 1),
            )
            val otherGoal = Goal.create(
                userId = otherUserId,
                name = "Outra meta",
                targetAmount = Money(50_000, CurrencyCode.BRL),
            )
            repository.create(goal)
            repository.create(otherGoal)

            val first = repository.contribute(
                ownerId = userId,
                goalId = goal.id,
                amount = Money(4_000, CurrencyCode.BRL),
                occurredOn = LocalDate.of(2026, 10, 4),
            )
            assertEquals(4_000, first.first.currentAmount.amountInMinor)
            assertEquals(GoalStatus.ACTIVE, first.first.status)

            val second = repository.contribute(
                ownerId = userId,
                goalId = goal.id,
                amount = Money(6_000, CurrencyCode.BRL),
                occurredOn = LocalDate.of(2026, 10, 5),
            )
            assertEquals(10_000, second.first.currentAmount.amountInMinor)
            assertEquals(GoalStatus.ACHIEVED, second.first.status)

            val contributions = repository.listContributions(userId, goal.id)
            assertEquals(2, contributions.size)
            assertEquals(6_000, contributions.first().amount.amountInMinor)

            assertFailsWith<DomainValidationException> {
                repository.contribute(
                    ownerId = userId,
                    goalId = goal.id,
                    amount = Money(1, CurrencyCode.BRL),
                    occurredOn = LocalDate.of(2026, 10, 6),
                )
            }

            assertEquals(null, repository.findByOwnerAndId(otherUserId, goal.id))
            assertEquals(otherGoal.id, repository.findByOwnerAndId(otherUserId, otherGoal.id)?.id)
        } finally {
            deleteUser(connections, userId)
            deleteUser(connections, otherUserId)
        }
    }

    private fun createUser(connections: DatabaseConnectionProvider, prefix: String): UserId {
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
                s.setString(2, "$prefix-$id@example.com")
                s.setString(3, "Goal Integration")
                s.setString(4, "UTC")
                s.executeUpdate()
            }
        }
        return UserId(id)
    }

    private fun deleteUser(connections: DatabaseConnectionProvider, id: UserId) {
        connections.open().use { c ->
            c.prepareStatement("DELETE FROM goals WHERE user_id = ?").use { s ->
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
