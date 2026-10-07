package com.nexa.finance.infrastructure.ai

import com.nexa.finance.domain.common.UserId
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

class JdbcAiUsageRepositoryIntegrationTest {
    @Test
    fun dailyLimitIsAtomicAndScopedToUser() {
        assumeTrue(!System.getenv("POSTGRES_PASSWORD").isNullOrBlank())

        val connections = DatabaseConnectionProvider()
        val repository = JdbcAiUsageRepository(connections)
        val userId = createUser(connections, "ai-usage")
        val otherUserId = createUser(connections, "ai-usage-other")
        val day = LocalDate.of(2026, 10, 4)

        try {
            assertTrue(repository.tryConsume(userId, day, limit = 2))
            assertTrue(repository.tryConsume(userId, day, limit = 2))
            assertFalse(repository.tryConsume(userId, day, limit = 2))
            assertEquals(2, repository.count(userId, day))

            assertTrue(repository.tryConsume(otherUserId, day, limit = 2))
            assertEquals(1, repository.count(otherUserId, day))
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
                s.setString(2, prefix + "-" + id + "@example.com")
                s.setString(3, "AI Usage Test")
                s.setString(4, "UTC")
                s.executeUpdate()
            }
        }
        return UserId(id)
    }

    private fun deleteUser(connections: DatabaseConnectionProvider, id: UserId) {
        connections.open().use { c ->
            c.prepareStatement("DELETE FROM users WHERE id = ?").use { s ->
                s.setObject(1, id.value)
                s.executeUpdate()
            }
        }
    }
}
