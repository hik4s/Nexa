package com.nexa.finance.infrastructure.notification

import com.nexa.finance.domain.common.UserId
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

data class NotificationRecord(
    val id: UUID,
    val userId: UserId,
    val type: String,
    val severity: String,
    val title: String,
    val message: String,
    val readAt: Instant?,
    val createdAt: Instant,
)

class JdbcNotificationRepository(
    private val connections: DatabaseConnectionProvider = DatabaseConnectionProvider(),
) {
    fun createIfAbsent(
        userId: UserId,
        type: String,
        severity: String,
        title: String,
        message: String,
        dedupeKey: String,
        now: Instant = Instant.now(),
    ): Boolean =
        connections.open().use { c ->
            c.prepareStatement(
                """
                INSERT INTO notifications
                    (id, user_id, type, severity, title, message, dedupe_key, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, dedupe_key) DO NOTHING
                """.trimIndent()
            ).use { s ->
                s.setObject(1, UUID.randomUUID())
                s.setObject(2, userId.value)
                s.setString(3, type)
                s.setString(4, severity)
                s.setString(5, title)
                s.setString(6, message)
                s.setString(7, dedupeKey)
                s.setTimestamp(8, Timestamp.from(now))
                s.executeUpdate() == 1
            }
        }

    fun list(userId: UserId, unreadOnly: Boolean = false, limit: Int = 50): List<NotificationRecord> =
        connections.open().use { c ->
            c.prepareStatement(
                """
                SELECT id, user_id, type, severity, title, message, read_at, created_at
                FROM notifications
                WHERE user_id = ? AND (? = FALSE OR read_at IS NULL)
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                """.trimIndent()
            ).use { s ->
                s.setObject(1, userId.value)
                s.setBoolean(2, unreadOnly)
                s.setInt(3, limit.coerceIn(1, 100))
                s.executeQuery().use { r ->
                    buildList {
                        while (r.next()) add(
                            NotificationRecord(
                                id = r.getObject("id", UUID::class.java),
                                userId = UserId(r.getObject("user_id", UUID::class.java)),
                                type = r.getString("type"),
                                severity = r.getString("severity"),
                                title = r.getString("title"),
                                message = r.getString("message"),
                                readAt = r.getTimestamp("read_at")?.toInstant(),
                                createdAt = r.getTimestamp("created_at").toInstant(),
                            )
                        )
                    }
                }
            }
        }

    fun markRead(userId: UserId, id: UUID): Boolean =
        connections.open().use { c ->
            c.prepareStatement(
                """
                UPDATE notifications
                SET read_at = COALESCE(read_at, CURRENT_TIMESTAMP)
                WHERE user_id = ? AND id = ?
                """.trimIndent()
            ).use { s ->
                s.setObject(1, userId.value)
                s.setObject(2, id)
                s.executeUpdate() == 1
            }
        }
}
