package com.nexa.finance.application.notification

import com.nexa.finance.domain.common.UserId
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import com.nexa.finance.infrastructure.notification.JdbcNotificationRepository
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

class NotificationRepositoryIntegrationTest {
    @Test
    fun duplicateDedupeKeyDoesNotCreateSecondNotification() {
        assumeTrue(!System.getenv("POSTGRES_PASSWORD").isNullOrBlank())

        val connections = DatabaseConnectionProvider()
        val repository = JdbcNotificationRepository(connections)
        val userId = createUser(connections)

        try {
            assertTrue(repository.createIfAbsent(userId, "BUDGET_ALERT", "WARNING", "Orçamento", "Atenção", "budget:1:warning"))
            assertEquals(false, repository.createIfAbsent(userId, "BUDGET_ALERT", "WARNING", "Orçamento", "Atenção", "budget:1:warning"))
            assertEquals(1, repository.list(userId).size)
        } finally {
            connections.open().use { c ->
                c.prepareStatement("DELETE FROM users WHERE id = ?").use { s ->
                    s.setObject(1, userId.value)
                    s.executeUpdate()
                }
            }
        }
    }

    private fun createUser(connections: DatabaseConnectionProvider): UserId {
        val id = UUID.randomUUID()
        connections.open().use { c ->
            c.prepareStatement(
                """
                INSERT INTO users
                    (id, email, display_name, time_zone, status, created_at, updated_at)
                VALUES (?, ?, 'Notification Test', 'UTC', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """.trimIndent()
            ).use { s ->
                s.setObject(1, id)
                s.setString(2, "notification-" + id + "@example.com")
                s.executeUpdate()
            }
        }
        return UserId(id)
    }
}
