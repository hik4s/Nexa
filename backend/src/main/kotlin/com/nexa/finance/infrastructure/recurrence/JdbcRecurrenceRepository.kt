package com.nexa.finance.infrastructure.recurrence

import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.category.CategoryId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.recurrence.Recurrence
import com.nexa.finance.domain.recurrence.RecurrenceFrequency
import com.nexa.finance.domain.recurrence.RecurrenceId
import com.nexa.finance.domain.recurrence.RecurrenceStatus
import com.nexa.finance.domain.transaction.TransactionId
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import java.sql.Timestamp
import java.time.LocalDate
import java.util.UUID

class JdbcRecurrenceRepository(
    private val connections: DatabaseConnectionProvider = DatabaseConnectionProvider(),
) {
    fun create(recurrence: Recurrence) {
        connections.open().use { c ->
            c.prepareStatement(
                """INSERT INTO recurrences
                (id,user_id,account_id,amount_minor,currency,transaction_type,frequency,interval_value,
                 start_on,end_on,max_occurrences,description,category_id,status,created_at,updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""".trimIndent()
            ).use { s ->
                s.setObject(1, recurrence.id)
                s.setObject(2, recurrence.userId.value)
                s.setObject(3, recurrence.accountId.value)
                s.setLong(4, recurrence.amount.amountInMinor)
                s.setString(5, recurrence.amount.currency.name)
                s.setString(6, recurrence.transactionType.name)
                s.setString(7, recurrence.frequency.name)
                s.setInt(8, recurrence.interval)
                s.setObject(9, recurrence.startOn)
                if (recurrence.endOn == null) s.setNull(10, java.sql.Types.DATE) else s.setObject(10, recurrence.endOn)
                if (recurrence.maxOccurrences == null) s.setNull(11, java.sql.Types.INTEGER) else s.setInt(11, recurrence.maxOccurrences)
                s.setString(12, recurrence.description)
                if (recurrence.categoryId == null) s.setNull(13, java.sql.Types.OTHER) else s.setObject(13, recurrence.categoryId.value)
                s.setString(14, recurrence.status.name)
                s.setTimestamp(15, Timestamp.from(recurrence.createdAt))
                s.setTimestamp(16, Timestamp.from(recurrence.updatedAt))
                s.executeUpdate()
            }
        }
    }

    fun list(ownerId: UserId): List<Recurrence> =
        connections.open().use { c ->
            c.prepareStatement("SELECT * FROM recurrences WHERE user_id = ? ORDER BY start_on, created_at, id").use { s ->
                s.setObject(1, ownerId.value)
                s.executeQuery().use { r -> buildList { while (r.next()) add(r.toRecurrence()) } }
            }
        }

    fun findByOwnerAndId(ownerId: UserId, id: RecurrenceId): Recurrence? =
        connections.open().use { c ->
            c.prepareStatement("SELECT * FROM recurrences WHERE user_id = ? AND id = ?").use { s ->
                s.setObject(1, ownerId.value)
                s.setObject(2, id)
                s.executeQuery().use { r -> if (r.next()) r.toRecurrence() else null }
            }
        }

    fun update(recurrence: Recurrence): Recurrence? =
        connections.open().use { c ->
            c.prepareStatement(
                """
                UPDATE recurrences
                SET account_id = ?, amount_minor = ?, currency = ?, transaction_type = ?,
                    frequency = ?, interval_value = ?, start_on = ?, end_on = ?,
                    max_occurrences = ?, description = ?, category_id = ?, status = ?, updated_at = ?
                WHERE user_id = ? AND id = ?
                RETURNING *
                """.trimIndent()
            ).use { s ->
                s.setObject(1, recurrence.accountId.value)
                s.setLong(2, recurrence.amount.amountInMinor)
                s.setString(3, recurrence.amount.currency.name)
                s.setString(4, recurrence.transactionType.name)
                s.setString(5, recurrence.frequency.name)
                s.setInt(6, recurrence.interval)
                s.setObject(7, recurrence.startOn)
                if (recurrence.endOn == null) s.setNull(8, java.sql.Types.DATE) else s.setObject(8, recurrence.endOn)
                if (recurrence.maxOccurrences == null) s.setNull(9, java.sql.Types.INTEGER) else s.setInt(9, recurrence.maxOccurrences)
                s.setString(10, recurrence.description)
                if (recurrence.categoryId == null) s.setNull(11, java.sql.Types.OTHER) else s.setObject(11, recurrence.categoryId.value)
                s.setString(12, recurrence.status.name)
                s.setTimestamp(13, Timestamp.from(recurrence.updatedAt))
                s.setObject(14, recurrence.userId.value)
                s.setObject(15, recurrence.id)
                s.executeQuery().use { r -> if (r.next()) r.toRecurrence() else null }
            }
        }

    fun updateStatus(ownerId: UserId, id: RecurrenceId, status: RecurrenceStatus): Recurrence? =
        connections.open().use { c ->
            c.prepareStatement("UPDATE recurrences SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ? AND id = ? RETURNING *").use { s ->
                s.setString(1, status.name)
                s.setObject(2, ownerId.value)
                s.setObject(3, id)
                s.executeQuery().use { r -> if (r.next()) r.toRecurrence() else null }
            }
        }

    fun findExecution(ownerId: UserId, recurrenceId: RecurrenceId, occurrenceNumber: Int): TransactionId? =
        connections.open().use { c ->
            c.prepareStatement("""SELECT t.id FROM transactions t
                JOIN accounts a ON a.id = t.account_id
                WHERE a.owner_id = ? AND t.recurrence_id = ? AND t.recurrence_occurrence_number = ?""".trimIndent()).use { s ->
                s.setObject(1, ownerId.value)
                s.setObject(2, recurrenceId)
                s.setInt(3, occurrenceNumber)
                s.executeQuery().use { r -> if (r.next()) r.getObject(1, UUID::class.java) else null }
            }
        }

    private fun java.sql.ResultSet.toRecurrence(): Recurrence =
        Recurrence.restore(
            id = getObject("id", UUID::class.java),
            userId = UserId(getObject("user_id", UUID::class.java)),
            accountId = AccountId(getObject("account_id", UUID::class.java)),
            amount = Money(getLong("amount_minor"), CurrencyCode.valueOf(getString("currency"))),
            transactionType = com.nexa.finance.domain.transaction.TransactionType.valueOf(getString("transaction_type")),
            frequency = RecurrenceFrequency.valueOf(getString("frequency")),
            interval = getInt("interval_value"),
            startOn = getObject("start_on", LocalDate::class.java),
            endOn = getObject("end_on", LocalDate::class.java),
            maxOccurrences = getInt("max_occurrences").let { if (wasNull()) null else it },
            description = getString("description"),
            categoryId = getObject("category_id", UUID::class.java)?.let(::CategoryId),
            status = RecurrenceStatus.valueOf(getString("status")),
            createdAt = getTimestamp("created_at").toInstant(),
            updatedAt = getTimestamp("updated_at").toInstant(),
        )
}
