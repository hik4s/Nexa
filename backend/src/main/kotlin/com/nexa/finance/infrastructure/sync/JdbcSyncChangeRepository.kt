package com.nexa.finance.infrastructure.sync

import com.nexa.finance.domain.common.UserId
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import java.util.UUID

class JdbcSyncChangeRepository(private val connections: DatabaseConnectionProvider) {
    data class Change(val cursor: Long, val entityType: String, val entityId: UUID, val operation: String)

    fun changesAfter(ownerId: UserId, cursor: Long, limit: Int): List<Change> = connections.open().use { c ->
        c.prepareStatement("""
            SELECT cursor, entity_type, entity_id, operation
            FROM sync_changes
            WHERE user_id = ? AND cursor > ?
            ORDER BY cursor ASC
            LIMIT ?
        """.trimIndent()).use { s ->
            s.setObject(1, ownerId.value); s.setLong(2, cursor); s.setInt(3, limit)
            s.executeQuery().use { r ->
                buildList { while (r.next()) add(Change(r.getLong(1), r.getString(2), r.getObject(3, UUID::class.java), r.getString(4))) }
            }
        }
    }

    fun latestCursor(ownerId: UserId): Long = connections.open().use { c ->
        c.prepareStatement("SELECT COALESCE(MAX(cursor), 0) FROM sync_changes WHERE user_id = ?").use { s ->
            s.setObject(1, ownerId.value); s.executeQuery().use { r -> r.next(); r.getLong(1) }
        }
    }
}
