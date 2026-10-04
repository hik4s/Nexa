package com.nexa.finance.infrastructure.transaction

import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.category.CategoryId
import com.nexa.finance.domain.card.CardId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.invoice.InvoiceId
import com.nexa.finance.domain.transaction.Transaction
import com.nexa.finance.domain.transaction.TransactionDirection
import com.nexa.finance.domain.transaction.TransactionId
import com.nexa.finance.domain.transaction.TransactionSource
import com.nexa.finance.domain.transaction.TransactionStatus
import com.nexa.finance.domain.transaction.TransactionType
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import java.sql.Timestamp
import java.time.LocalDate
import java.util.UUID

class JdbcTransactionRepository(
    private val connections: DatabaseConnectionProvider = DatabaseConnectionProvider(),
) {
    fun create(ownerId: UserId, transaction: Transaction) {
        connections.open().use { c ->
            c.prepareStatement(
                """
                INSERT INTO transactions
                    (id, account_id, amount_minor, currency, type, direction, status,
                     occurred_on, description, category_id, card_id, invoice_id, source, related_transaction_id,
                     created_at, updated_at)
                SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                WHERE EXISTS (
                    SELECT 1 FROM accounts
                    WHERE id = ? AND owner_id = ? AND status = 'ACTIVE'
                )
                  AND (
                    CAST(? AS UUID) IS NULL OR EXISTS (
                        SELECT 1 FROM categories c
                        WHERE c.id = CAST(? AS UUID)
                          AND c.status = 'ACTIVE'
                          AND (c.owner_id IS NULL OR c.owner_id = ?)
                    )
                  )
                """.trimIndent()
            ).use { s ->
                bindTransaction(s, transaction)
                s.setObject(17, transaction.accountId.value)
                s.setObject(18, ownerId.value)
                setUuidOrNull(s, 19, transaction.categoryId?.value)
                setUuidOrNull(s, 20, transaction.categoryId?.value)
                s.setObject(21, ownerId.value)
                if (s.executeUpdate() != 1) {
                    throw DomainValidationException("Account was not found for this user.")
                }
            }
        }
    }

    fun findByOwner(ownerId: UserId, accountId: AccountId? = null): List<Transaction> =
        connections.open().use { c ->
            c.prepareStatement(
                """
                SELECT t.*
                FROM transactions t
                JOIN accounts a ON a.id = t.account_id
                WHERE a.owner_id = ?
                  AND (? IS NULL OR t.account_id = ?)
                ORDER BY t.occurred_on DESC, t.created_at DESC, t.id DESC
                """.trimIndent()
            ).use { s ->
                s.setObject(1, ownerId.value)
                s.setObject(2, accountId?.value)
                s.setObject(3, accountId?.value)
                s.executeQuery().use { r ->
                    buildList {
                        while (r.next()) add(r.toTransaction())
                    }
                }
            }
        }

    fun findByOwnerAndId(ownerId: UserId, id: TransactionId): Transaction? =
        connections.open().use { c ->
            c.prepareStatement(
                """
                SELECT t.*
                FROM transactions t
                JOIN accounts a ON a.id = t.account_id
                WHERE t.id = ? AND a.owner_id = ?
                """.trimIndent()
            ).use { s ->
                s.setObject(1, id)
                s.setObject(2, ownerId.value)
                s.executeQuery().use { r ->
                    if (r.next()) r.toTransaction() else null
                }
            }
        }

    fun updateStatus(ownerId: UserId, id: TransactionId, status: TransactionStatus): Transaction? =
        connections.open().use { c ->
            c.prepareStatement(
                """
                UPDATE transactions t
                SET status = ?, updated_at = CURRENT_TIMESTAMP
                FROM accounts a
                WHERE t.account_id = a.id
                  AND t.id = ?
                  AND a.owner_id = ?
                RETURNING t.*
                """.trimIndent()
            ).use { s ->
                s.setString(1, status.name)
                s.setObject(2, id)
                s.setObject(3, ownerId.value)
                s.executeQuery().use { r ->
                    if (r.next()) r.toTransaction() else null
                }
            }
        }

    private fun bindTransaction(s: java.sql.PreparedStatement, t: Transaction) {
        s.setObject(1, t.id)
        s.setObject(2, t.accountId.value)
        s.setLong(3, t.amount.amountInMinor)
        s.setString(4, t.amount.currency.name)
        s.setString(5, t.type.name)
        s.setString(6, t.direction.name)
        s.setString(7, t.status.name)
        s.setObject(8, t.occurredOn)
        s.setString(9, t.description)
        setUuidOrNull(s, 10, t.categoryId?.value)
        setUuidOrNull(s, 11, t.cardId?.value)
        setUuidOrNull(s, 12, t.invoiceId?.value)
        s.setString(13, t.source.name)
        setUuidOrNull(s, 14, t.relatedTransactionId)
        s.setTimestamp(15, Timestamp.from(t.createdAt))
        s.setTimestamp(16, Timestamp.from(t.updatedAt))
    }

    private fun setUuidOrNull(statement: java.sql.PreparedStatement, index: Int, value: UUID?) {
        if (value == null) statement.setNull(index, java.sql.Types.OTHER)
        else statement.setObject(index, value)
    }

    private fun java.sql.ResultSet.toTransaction(): Transaction =
        Transaction.create(
            id = getObject("id", UUID::class.java),
            accountId = AccountId(getObject("account_id", UUID::class.java)),
            amount = Money(getLong("amount_minor"), CurrencyCode.valueOf(getString("currency"))),
            type = TransactionType.valueOf(getString("type")),
            direction = TransactionDirection.valueOf(getString("direction")),
            status = TransactionStatus.valueOf(getString("status")),
            occurredOn = getObject("occurred_on", LocalDate::class.java),
            description = getString("description"),
            categoryId = getObject("category_id", UUID::class.java)?.let(::CategoryId),
            source = TransactionSource.valueOf(getString("source")),
            cardId = getObject("card_id", UUID::class.java)?.let(::CardId),
            invoiceId = getObject("invoice_id", UUID::class.java)?.let(::InvoiceId),
            relatedTransactionId = getObject("related_transaction_id", UUID::class.java),
            createdAt = getTimestamp("created_at").toInstant(),
            updatedAt = getTimestamp("updated_at").toInstant(),
        )
}
