package com.nexa.finance.infrastructure.invoice

import com.nexa.finance.domain.account.Account
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.card.Card
import com.nexa.finance.domain.card.CardId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.invoice.Invoice
import com.nexa.finance.domain.invoice.InvoiceId
import com.nexa.finance.domain.invoice.InvoiceStatus
import com.nexa.finance.domain.transaction.Transaction
import com.nexa.finance.domain.transaction.TransactionDirection
import com.nexa.finance.domain.transaction.TransactionId
import com.nexa.finance.domain.transaction.TransactionStatus
import com.nexa.finance.domain.transaction.TransactionType
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import java.sql.Timestamp
import java.time.LocalDate
import java.util.UUID

class JdbcInvoiceRepository(private val connections: DatabaseConnectionProvider = DatabaseConnectionProvider()) {
    fun findByOwner(ownerId: UserId): List<Invoice> = connections.open().use { c ->
        c.prepareStatement("SELECT i.* FROM invoices i JOIN cards c ON c.id = i.card_id WHERE c.owner_id = ? ORDER BY i.period_end DESC, i.id DESC").use { s ->
            s.setObject(1, ownerId.value); s.executeQuery().use { r -> buildList { while (r.next()) add(r.toInvoice(c)) } }
        }
    }

    fun findByOwnerAndId(ownerId: UserId, id: InvoiceId): Invoice? = connections.open().use { c ->
        c.prepareStatement("SELECT i.* FROM invoices i JOIN cards c ON c.id = i.card_id WHERE c.owner_id = ? AND i.id = ?").use { s ->
            s.setObject(1, ownerId.value); s.setObject(2, id.value); s.executeQuery().use { r -> if (r.next()) r.toInvoice(c) else null }
        }
    }

    fun createForPeriod(ownerId: UserId, card: Card, periodStart: LocalDate, periodEnd: LocalDate, dueOn: LocalDate): Invoice {
        connections.open().use { c ->
            c.autoCommit = false
            try {
                c.prepareStatement("SELECT 1 FROM cards WHERE id = ? AND owner_id = ? FOR UPDATE").use { s ->
                    s.setObject(1, card.id.value); s.setObject(2, ownerId.value)
                    s.executeQuery().use { r -> if (!r.next()) throw DomainValidationException("Card was not found.") }
                }
                c.prepareStatement("SELECT 1 FROM invoices WHERE card_id = ? AND period_start = ? AND period_end = ? AND status <> 'CANCELLED' LIMIT 1").use { s ->
                    s.setObject(1, card.id.value); s.setObject(2, periodStart); s.setObject(3, periodEnd)
                    s.executeQuery().use { r -> if (r.next()) throw DomainValidationException("An invoice already exists for this card and period.") }
                }
                val txs = mutableListOf<TransactionRow>()
                c.prepareStatement("SELECT id, amount_minor, currency, type FROM transactions WHERE card_id = ? AND invoice_id IS NULL AND status = 'POSTED' AND occurred_on BETWEEN ? AND ? AND type IN ('PURCHASE','REFUND') ORDER BY occurred_on, created_at, id").use { s ->
                    s.setObject(1, card.id.value); s.setObject(2, periodStart); s.setObject(3, periodEnd)
                    s.executeQuery().use { r -> while (r.next()) txs += TransactionRow(r.getObject("id", UUID::class.java), r.getLong("amount_minor"), CurrencyCode.valueOf(r.getString("currency")), TransactionType.valueOf(r.getString("type"))) }
                }
                if (txs.isEmpty()) throw DomainValidationException("No unbilled posted card transactions exist in the selected period.")
                val totalMinor = txs.fold(0L) { acc, tx -> acc + if (tx.type == TransactionType.PURCHASE) tx.amountMinor else -tx.amountMinor }
                if (totalMinor <= 0) throw DomainValidationException("Invoice total must be greater than zero.")
                val currency = txs.first().currency
                if (txs.any { it.currency != currency }) throw DomainValidationException("Invoice transactions must use the same currency.")
                val invoice = Invoice.create(card.id, periodStart, periodEnd, dueOn, Money(totalMinor, currency), txs.map { it.id })
                c.prepareStatement("INSERT INTO invoices (id, card_id, period_start, period_end, due_on, total_amount_minor, currency, paid_amount_minor, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?)").use { s ->
                    s.setObject(1, invoice.id.value); s.setObject(2, invoice.cardId.value); s.setObject(3, invoice.periodStart); s.setObject(4, invoice.periodEnd); s.setObject(5, invoice.dueOn); s.setLong(6, invoice.totalAmount.amountInMinor); s.setString(7, invoice.totalAmount.currency.name); s.setString(8, invoice.status.name); s.setTimestamp(9, Timestamp.from(invoice.createdAt)); s.setTimestamp(10, Timestamp.from(invoice.updatedAt)); s.executeUpdate()
                }
                c.prepareStatement("UPDATE transactions SET invoice_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND card_id = ? AND invoice_id IS NULL").use { s ->
                    txs.forEach { tx -> s.setObject(1, invoice.id.value); s.setObject(2, tx.id); s.setObject(3, card.id.value); s.addBatch() }
                    if (s.executeBatch().sum() != txs.size) throw DomainValidationException("Failed to attach all transactions to the invoice.")
                }
                c.commit(); return invoice
            } catch (e: Exception) { c.rollback(); throw e } finally { c.autoCommit = true }
        }
    }

    fun updateStatus(ownerId: UserId, id: InvoiceId, status: InvoiceStatus): Invoice? = connections.open().use { c ->
        c.prepareStatement("UPDATE invoices i SET status = ?, updated_at = CURRENT_TIMESTAMP FROM cards c WHERE i.card_id = c.id AND i.id = ? AND c.owner_id = ? RETURNING i.*").use { s ->
            s.setString(1, status.name); s.setObject(2, id.value); s.setObject(3, ownerId.value); s.executeQuery().use { r -> if (r.next()) r.toInvoice(c) else null }
        }
    }

    fun persistPayment(ownerId: UserId, payment: com.nexa.finance.domain.card.CardPayment): Invoice {
        connections.open().use { c ->
            c.autoCommit = false
            try {
                c.prepareStatement("SELECT i.* FROM invoices i JOIN cards card ON card.id = i.card_id WHERE i.id = ? AND card.owner_id = ? AND i.status IN ('CLOSED','OVERDUE') FOR UPDATE").use { s ->
                    s.setObject(1, payment.invoiceId.value); s.setObject(2, ownerId.value)
                    s.executeQuery().use { r -> if (!r.next()) throw DomainValidationException("Invoice is not payable by this user.") }
                }
                val current = findInvoiceOnConnection(c, payment.invoiceId) ?: throw DomainValidationException("Invoice was not found.")
                val updated = current.registerPayment(payment.amount, payment.liabilityTransaction.id)
                insertTransaction(c, payment.sourceTransaction, null)
                insertTransaction(c, payment.liabilityTransaction, null)
                c.prepareStatement("UPDATE transactions SET related_transaction_id = CASE id WHEN ? THEN ? WHEN ? THEN ? END WHERE id IN (?, ?)").use { s ->
                    s.setObject(1, payment.sourceTransaction.id)
                    s.setObject(2, payment.liabilityTransaction.id)
                    s.setObject(3, payment.liabilityTransaction.id)
                    s.setObject(4, payment.sourceTransaction.id)
                    s.setObject(5, payment.sourceTransaction.id)
                    s.setObject(6, payment.liabilityTransaction.id)
                    s.executeUpdate()
                }
                c.prepareStatement("UPDATE invoices SET paid_amount_minor = ?, status = ?, updated_at = ? WHERE id = ?").use { s ->
                    s.setLong(1, updated.paidAmount.amountInMinor); s.setString(2, updated.status.name); s.setTimestamp(3, Timestamp.from(updated.updatedAt)); s.setObject(4, updated.id.value)
                    if (s.executeUpdate() != 1) throw DomainValidationException("Invoice payment could not be applied.")
                }
                c.prepareStatement("INSERT INTO invoice_payment_transactions (invoice_id, transaction_id) VALUES (?, ?)").use { s -> s.setObject(1, payment.invoiceId.value); s.setObject(2, payment.liabilityTransaction.id); s.executeUpdate() }
                val result = findInvoiceOnConnection(c, payment.invoiceId) ?: throw DomainValidationException("Invoice payment could not be loaded.")
                c.commit(); return result
            } catch (e: Exception) { c.rollback(); throw e } finally { c.autoCommit = true }
        }
    }

    private fun findInvoiceOnConnection(c: java.sql.Connection, id: InvoiceId): Invoice? =
        c.prepareStatement("SELECT * FROM invoices WHERE id = ?").use { s ->
            s.setObject(1, id.value); s.executeQuery().use { r -> if (r.next()) r.toInvoice(c) else null }
        }
    private fun insertTransaction(c: java.sql.Connection, t: Transaction, relatedTransactionId: UUID? = t.relatedTransactionId) {
        c.prepareStatement("INSERT INTO transactions (id, account_id, amount_minor, currency, type, direction, status, occurred_on, description, category_id, card_id, invoice_id, source, related_transaction_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { s ->
            s.setObject(1, t.id); s.setObject(2, t.accountId.value); s.setLong(3, t.amount.amountInMinor); s.setString(4, t.amount.currency.name); s.setString(5, t.type.name); s.setString(6, t.direction.name); s.setString(7, t.status.name); s.setObject(8, t.occurredOn); s.setString(9, t.description); if (t.categoryId == null) s.setNull(10, java.sql.Types.OTHER) else s.setObject(10, t.categoryId.value); s.setObject(11, t.cardId?.value); s.setObject(12, t.invoiceId?.value); s.setString(13, t.source.name); if (relatedTransactionId == null) s.setNull(14, java.sql.Types.OTHER) else s.setObject(14, relatedTransactionId); s.setTimestamp(15, Timestamp.from(t.createdAt)); s.setTimestamp(16, Timestamp.from(t.updatedAt)); s.executeUpdate()
        }
    }

    private data class TransactionRow(val id: UUID, val amountMinor: Long, val currency: CurrencyCode, val type: TransactionType)

    private fun java.sql.ResultSet.toInvoice(c: java.sql.Connection): Invoice {
        val id = InvoiceId(getObject("id", UUID::class.java))
        val txIds = c.prepareStatement("SELECT id FROM transactions WHERE invoice_id = ? ORDER BY occurred_on, created_at, id").use { s -> s.setObject(1, id.value); s.executeQuery().use { r -> buildList { while (r.next()) add(r.getObject("id", UUID::class.java)) } } }
        val paymentIds = c.prepareStatement("SELECT transaction_id FROM invoice_payment_transactions WHERE invoice_id = ? ORDER BY transaction_id").use { s -> s.setObject(1, id.value); s.executeQuery().use { r -> buildList { while (r.next()) add(r.getObject("transaction_id", UUID::class.java)) } } }
        return Invoice.restore(id, CardId(getObject("card_id", UUID::class.java)), getObject("period_start", LocalDate::class.java), getObject("period_end", LocalDate::class.java), getObject("due_on", LocalDate::class.java), Money(getLong("total_amount_minor"), CurrencyCode.valueOf(getString("currency"))), txIds, Money(getLong("paid_amount_minor"), CurrencyCode.valueOf(getString("currency"))), InvoiceStatus.valueOf(getString("status")), paymentIds, getTimestamp("created_at").toInstant(), getTimestamp("updated_at").toInstant())
    }
}