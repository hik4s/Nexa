package com.nexa.finance.infrastructure.category

import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import java.util.UUID

data class CategoryRecord(
    val id: UUID,
    val name: String,
    val type: String,
    val iconKey: String?,
    val sortOrder: Int,
)

class JdbcCategoryRepository(
    private val connections: DatabaseConnectionProvider = DatabaseConnectionProvider(),
) {
    fun listAvailable(userId: UUID, type: String? = null): List<CategoryRecord> =
        connections.open().use { c ->
            val sql = """
                SELECT id, name, type, icon_key, sort_order
                FROM categories
                WHERE status = 'ACTIVE'
                  AND (owner_id IS NULL OR owner_id = ?)
                  AND (? IS NULL OR type = 'BOTH' OR type = ?)
                ORDER BY sort_order, name
            """.trimIndent()
            c.prepareStatement(sql).use { s ->
                s.setObject(1, userId)
                s.setString(2, type)
                s.setString(3, type)
                s.executeQuery().use { r ->
                    buildList {
                        while (r.next()) add(
                            CategoryRecord(
                                r.getObject("id", UUID::class.java),
                                r.getString("name"),
                                r.getString("type"),
                                r.getString("icon_key"),
                                r.getInt("sort_order"),
                            )
                        )
                    }
                }
            }
        }
}
