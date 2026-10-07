package com.nexa.finance.infrastructure.transfer

import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.card.CardId
import com.nexa.finance.domain.category.CategoryId
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
import com.nexa.finance.domain.transfer.Transfer
import com.nexa.finance.domain.transfer.TransferId
import com.nexa.finance.domain.transfer.TransferStatus
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class JdbcTransferRepository(private val connections: DatabaseConnectionProvider = DatabaseConnectionProvider()) {
    fun create(ownerId: UserId, transfer: Transfer) {
        connections.open().use { c ->
            c.autoCommit = false
            try {
                validateAccounts(c, ownerId, transfer.sourceAccountId, transfer.destinationAccountId)
                insertTransaction(c, ownerId, transfer.sourceTransaction)
                insertTransaction(c, ownerId, transfer.destinationTransaction)
                linkTransaction(c, transfer.sourceTransaction.id, transfer.destinationTransaction.id, transfer.sourceTransaction.updatedAt)
                linkTransaction(c, transfer.destinationTransaction.id, transfer.sourceTransaction.id, transfer.destinationTransaction.updatedAt)
                insertTransfer(c, ownerId, transfer)
                c.commit()
            } catch (e: Exception) {
                runCatching { c.rollback() }
                throw e
            } finally {
                c.autoCommit = true
            }
        }
    }

    fun findByOwner(ownerId: UserId): List<Transfer> =
        connections.open().use { c ->
            c.prepareStatement("SELECT id FROM transfers WHERE user_id = ? ORDER BY occurred_on DESC, created_at DESC, id DESC").use { s ->
                s.setObject(1, ownerId.value)
                s.executeQuery().use { r ->
                    buildList {
                        while (r.next()) findById(c, ownerId, r.getObject("id", UUID::class.java))?.let(::add)
                    }
                }
            }
        }

    fun findByOwnerAndId(ownerId: UserId, id: TransferId): Transfer? =
        connections.open().use { c -> findById(c, ownerId, id) }

    fun updateStatus(ownerId: UserId, id: TransferId, status: TransferStatus): Transfer? =
        connections.open().use { c ->
            c.autoCommit = false
            try {
                val current = findById(c, ownerId, id) ?: return@use null
                if (current.status != TransferStatus.PENDING) throw DomainValidationException("Only pending transfers can change status.")
                val transactionStatus = when (status) {
                    TransferStatus.POSTED -> TransactionStatus.POSTED
                    TransferStatus.CANCELLED -> TransactionStatus.CANCELLED
                    TransferStatus.PENDING -> throw DomainValidationException("Transfer is already pending.")
                }
                val now = Instant.now()
                c.prepareStatement("UPDATE transactions SET status = ?, updated_at = ? WHERE id IN (?, ?)").use { s ->
                    s.setString(1, transactionStatus.name)
                    s.setTimestamp(2, Timestamp.from(now))
                    s.setObject(3, current.sourceTransaction.id)
                    s.setObject(4, current.destinationTransaction.id)
                    if (s.executeUpdate() != 2) throw DomainValidationException("Transfer transactions could not be updated.")
                }
                c.prepareStatement("UPDATE transfers SET status = ?, updated_at = ? WHERE id = ? AND user_id = ? AND status = 'PENDING'").use { s ->
                    s.setString(1, status.name)
                    s.setTimestamp(2, Timestamp.from(now))
                    s.setObject(3, id)
                    s.setObject(4, ownerId.value)
                    if (s.executeUpdate() != 1) throw DomainValidationException("Transfer could not be updated.")
                }
                c.commit()
                findById(c, ownerId, id)
            } catch (e: Exception) {
                runCatching { c.rollback() }
                throw e
            } finally {
                c.autoCommit = true
            }
        }

    private fun validateAccounts(c: Connection, ownerId: UserId, sourceId: AccountId, destinationId: AccountId) {
        c.prepareStatement("SELECT COUNT(*) FROM accounts WHERE owner_id = ? AND id IN (?, ?) AND status = 'ACTIVE'").use { s ->
            s.setObject(1, ownerId.value)
            s.setObject(2, sourceId.value)
            s.setObject(3, destinationId.value)
            s.executeQuery().use { r ->
                r.next()
                if (r.getInt(1) != 2) throw DomainValidationException("Both transfer accounts must belong to the authenticated user and be active.")
            }
        }
    }

    private fun insertTransaction(c: Connection, ownerId: UserId, t: Transaction) {
        c.prepareStatement("""INSERT INTO transactions
            (id, account_id, amount_minor, currency, type, direction, status, occurred_on,
             description, category_id, source, related_transaction_id, created_at, updated_at)
            SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?
            WHERE EXISTS (SELECT 1 FROM accounts WHERE id = ? AND owner_id = ? AND status = 'ACTIVE')""".trimIndent()).use { s ->
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
            s.setString(11, t.source.name)
            s.setTimestamp(12, Timestamp.from(t.createdAt))
            s.setTimestamp(13, Timestamp.from(t.updatedAt))
            s.setObject(14, t.accountId.value)
            s.setObject(15, ownerId.value)
            if (s.executeUpdate() != 1) throw DomainValidationException("Transfer account was not found for this user.")
        }
    }

    private fun linkTransaction(c: Connection, id: TransactionId, relatedId: TransactionId, updatedAt: Instant) {
        c.prepareStatement("UPDATE transactions SET related_transaction_id = ?, updated_at = ? WHERE id = ?").use { s ->
            s.setObject(1, relatedId)
            s.setTimestamp(2, Timestamp.from(updatedAt))
            s.setObject(3, id)
            if (s.executeUpdate() != 1) throw DomainValidationException("Failed to link transfer transactions.")
        }
    }

    private fun insertTransfer(c: Connection, ownerId: UserId, t: Transfer) {
        c.prepareStatement("""INSERT INTO transfers
            (id, user_id, source_account_id, destination_account_id, amount_minor, currency, occurred_on,
             source_transaction_id, destination_transaction_id, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""".trimIndent()).use { s ->
            s.setObject(1, t.id)
            s.setObject(2, ownerId.value)
            s.setObject(3, t.sourceAccountId.value)
            s.setObject(4, t.destinationAccountId.value)
            s.setLong(5, t.amount.amountInMinor)
            s.setString(6, t.amount.currency.name)
            s.setObject(7, t.occurredOn)
            s.setObject(8, t.sourceTransaction.id)
            s.setObject(9, t.destinationTransaction.id)
            s.setString(10, t.status.name)
            s.setTimestamp(11, Timestamp.from(t.createdAt))
            s.setTimestamp(12, Timestamp.from(t.updatedAt))
            s.executeUpdate()
        }
    }

    private fun findById(c: Connection, ownerId: UserId, id: TransferId): Transfer? {
        val row = c.prepareStatement("SELECT * FROM transfers WHERE id = ? AND user_id = ?").use { s ->
            s.setObject(1, id)
            s.setObject(2, ownerId.value)
            s.executeQuery().use { r -> if (r.next()) readTransferRow(r) else null }
        } ?: return null
        val source = findTransaction(c, row.sourceTransactionId) ?: throw DomainValidationException("Transfer source transaction is missing.")
        val destination = findTransaction(c, row.destinationTransactionId) ?: throw DomainValidationException("Transfer destination transaction is missing.")
        return Transfer.restore(row.id, row.userId, row.sourceAccountId, row.destinationAccountId, Money(row.amountMinor, row.currency), row.occurredOn, source, destination, row.status, row.createdAt, row.updatedAt)
    }

    private fun findTransaction(c: Connection, id: TransactionId): Transaction? =
        c.prepareStatement("SELECT * FROM transactions WHERE id = ?").use { s ->
            s.setObject(1, id)
            s.executeQuery().use { r -> if (r.next()) r.toTransaction() else null }
        }

    private data class TransferRow(
        val id: TransferId, val userId: UserId, val sourceAccountId: AccountId, val destinationAccountId: AccountId,
        val amountMinor: Long, val currency: CurrencyCode, val occurredOn: LocalDate,
        val sourceTransactionId: TransactionId, val destinationTransactionId: TransactionId, val status: TransferStatus,
        val createdAt: Instant, val updatedAt: Instant,
    )

    private fun readTransferRow(r: java.sql.ResultSet) = TransferRow(
        r.getObject("id", UUID::class.java),
        UserId(r.getObject("user_id", UUID::class.java)),
        AccountId(r.getObject("source_account_id", UUID::class.java)),
        AccountId(r.getObject("destination_account_id", UUID::class.java)),
        r.getLong("amount_minor"),
        CurrencyCode.valueOf(r.getString("currency")),
        r.getObject("occurred_on", LocalDate::class.java),
        r.getObject("source_transaction_id", UUID::class.java),
        r.getObject("destination_transaction_id", UUID::class.java),
        TransferStatus.valueOf(r.getString("status")),
        r.getTimestamp("created_at").toInstant(),
        r.getTimestamp("updated_at").toInstant(),
    )

    private fun java.sql.ResultSet.toTransaction(): Transaction = Transaction.create(
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

    private fun setUuidOrNull(statement: java.sql.PreparedStatement, index: Int, value: UUID?) {
        if (value == null) statement.setNull(index, java.sql.Types.OTHER) else statement.setObject(index, value)
    }
}