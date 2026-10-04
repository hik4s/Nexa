package com.nexa.finance.infrastructure.ai

import com.nexa.finance.domain.common.UserId
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate

class JdbcAiUsageRepository(
    private val connections: DatabaseConnectionProvider = DatabaseConnectionProvider(),
) {
    fun tryConsume(userId: UserId, date: LocalDate, limit: Int, now: Instant = Instant.now()): Boolean =
        connections.open().use { c ->
            c.prepareStatement(
                """
                INSERT INTO ai_usage_daily (user_id, usage_date, request_count, updated_at)
                VALUES (?, ?, 1, ?)
                ON CONFLICT (user_id, usage_date)
                DO UPDATE SET request_count = ai_usage_daily.request_count + 1,
                              updated_at = EXCLUDED.updated_at
                WHERE ai_usage_daily.request_count < ?
                RETURNING request_count
                """.trimIndent()
            ).use { s ->
                s.setObject(1, userId.value)
                s.setObject(2, date)
                s.setTimestamp(3, Timestamp.from(now))
                s.setInt(4, limit)
                s.executeQuery().use { r -> r.next() }
            }
        }

    fun count(userId: UserId, date: LocalDate): Int =
        connections.open().use { c ->
            c.prepareStatement(
                "SELECT request_count FROM ai_usage_daily WHERE user_id = ? AND usage_date = ?"
            ).use { s ->
                s.setObject(1, userId.value)
                s.setObject(2, date)
                s.executeQuery().use { r -> if (r.next()) r.getInt(1) else 0 }
            }
        }
}
