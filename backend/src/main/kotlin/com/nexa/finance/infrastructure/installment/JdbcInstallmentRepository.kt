package com.nexa.finance.infrastructure.installment

import com.nexa.finance.domain.card.Card
import com.nexa.finance.domain.category.CategoryId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.installment.*
import com.nexa.finance.domain.transaction.Transaction
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import java.sql.Timestamp
import java.time.LocalDate
import java.util.UUID

class JdbcInstallmentRepository(private val connections: DatabaseConnectionProvider = DatabaseConnectionProvider()) {
    fun create(plan: Installment) {
        connections.open().use { c ->
            c.autoCommit = false
            try {
                c.prepareStatement("INSERT INTO installments (id,user_id,total_amount_minor,currency,installment_count,first_due_on,description,category_id,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)").use { s ->
                    s.setObject(1,plan.id); s.setObject(2,plan.userId.value); s.setLong(3,plan.totalAmount.amountInMinor); s.setString(4,plan.totalAmount.currency.name)
                    s.setInt(5,plan.installmentCount); s.setObject(6,plan.firstDueOn); s.setString(7,plan.description)
                    if(plan.categoryId==null)s.setNull(8,java.sql.Types.OTHER) else s.setObject(8,plan.categoryId.value)
                    s.setString(9,plan.status.name); s.setTimestamp(10,Timestamp.from(plan.createdAt)); s.setTimestamp(11,Timestamp.from(plan.updatedAt)); s.executeUpdate()
                }
                c.prepareStatement("INSERT INTO installment_items (installment_id,number,amount_minor,currency,due_on,status) VALUES (?,?,?,?,?,?)").use { s ->
                    plan.items.forEach { i -> s.setObject(1,plan.id); s.setInt(2,i.number); s.setLong(3,i.amount.amountInMinor); s.setString(4,i.amount.currency.name); s.setObject(5,i.dueOn); s.setString(6,i.status.name); s.addBatch() }; s.executeBatch()
                }
                c.commit()
            } catch(e:Exception){c.rollback();throw e} finally{c.autoCommit=true}
        }
    }
    fun persistCardPurchase(ownerId: UserId, card: Card, plan: Installment, transactions: List<Transaction>) {
        require(transactions.size == plan.installmentCount)
        connections.open().use { c ->
            c.autoCommit = false
            try {
                c.prepareStatement("SELECT a.opening_balance_minor, a.currency, a.status FROM accounts a WHERE a.id = ? AND a.owner_id = ? AND a.type = 'CREDIT_CARD' FOR UPDATE").use { s ->
                    s.setObject(1, card.liabilityAccountId.value)
                    s.setObject(2, ownerId.value)
                    s.executeQuery().use { r ->
                        if (!r.next()) throw DomainValidationException("Card liability account was not found.")
                        if (r.getString("status") != "ACTIVE") throw DomainValidationException("Card liability account is not active.")
                        val opening = r.getLong("opening_balance_minor")
                        val currency = CurrencyCode.valueOf(r.getString("currency"))
                        if (currency != card.creditLimit.currency || currency != plan.totalAmount.currency) throw DomainValidationException("Card installment currency does not match the liability account.")
                        val postedBalance = c.prepareStatement("SELECT COALESCE(SUM(CASE WHEN direction = 'CREDIT' THEN amount_minor ELSE -amount_minor END), 0) FROM transactions WHERE account_id = ? AND status = 'POSTED'").use { b ->
                            b.setObject(1, card.liabilityAccountId.value)
                            b.executeQuery().use { br -> br.next(); br.getLong(1) }
                        }
                        val available = card.creditLimit.amountInMinor - (opening + postedBalance)
                        if (plan.totalAmount.amountInMinor > available) throw DomainValidationException("Card credit limit exceeded.")
                    }
                }
                c.prepareStatement("INSERT INTO installments (id,user_id,total_amount_minor,currency,installment_count,first_due_on,description,category_id,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)").use { s ->
                    s.setObject(1, plan.id); s.setObject(2, plan.userId.value); s.setLong(3, plan.totalAmount.amountInMinor); s.setString(4, plan.totalAmount.currency.name)
                    s.setInt(5, plan.installmentCount); s.setObject(6, plan.firstDueOn); s.setString(7, plan.description)
                    if (plan.categoryId == null) s.setNull(8, java.sql.Types.OTHER) else s.setObject(8, plan.categoryId.value)
                    s.setString(9, plan.status.name); s.setTimestamp(10, Timestamp.from(plan.createdAt)); s.setTimestamp(11, Timestamp.from(plan.updatedAt)); s.executeUpdate()
                }
                c.prepareStatement("INSERT INTO installment_items (installment_id,number,amount_minor,currency,due_on,status) VALUES (?,?,?,?,?,?)").use { s ->
                    plan.items.forEach { item ->
                        s.setObject(1, plan.id); s.setInt(2, item.number); s.setLong(3, item.amount.amountInMinor); s.setString(4, item.amount.currency.name); s.setObject(5, item.dueOn); s.setString(6, item.status.name); s.addBatch()
                    }
                    s.executeBatch()
                }
                c.prepareStatement("INSERT INTO transactions (id,account_id,amount_minor,currency,type,direction,status,occurred_on,description,category_id,card_id,invoice_id,installment_id,installment_number,recurrence_id,recurrence_occurrence_number,source,related_transaction_id,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)").use { s ->
                    transactions.forEach { t ->
                        s.setObject(1, t.id); s.setObject(2, t.accountId.value); s.setLong(3, t.amount.amountInMinor); s.setString(4, t.amount.currency.name)
                        s.setString(5, t.type.name); s.setString(6, t.direction.name); s.setString(7, t.status.name); s.setObject(8, t.occurredOn); s.setString(9, t.description)
                        if (t.categoryId == null) s.setNull(10, java.sql.Types.OTHER) else s.setObject(10, t.categoryId.value)
                        s.setObject(11, t.cardId?.value); s.setObject(12, t.invoiceId?.value); s.setObject(13, t.installmentId); s.setObject(14, t.installmentNumber)
                        if (t.recurrenceId == null) s.setNull(15, java.sql.Types.OTHER) else s.setObject(15, t.recurrenceId)
                        if (t.recurrenceOccurrenceNumber == null) s.setNull(16, java.sql.Types.INTEGER) else s.setInt(16, t.recurrenceOccurrenceNumber)
                        s.setString(17, t.source.name); if (t.relatedTransactionId == null) s.setNull(18, java.sql.Types.OTHER) else s.setObject(18, t.relatedTransactionId)
                        s.setTimestamp(19, Timestamp.from(t.createdAt)); s.setTimestamp(20, Timestamp.from(t.updatedAt)); s.addBatch()
                    }
                    if (s.executeBatch().size != transactions.size) throw DomainValidationException("Card installment transactions could not be persisted.")
                }
                c.commit()
            } catch (e: Exception) {
                c.rollback()
                throw e
            } finally {
                c.autoCommit = true
            }
        }
    }

    fun list(ownerId:UserId)=connections.open().use{c->c.prepareStatement("SELECT * FROM installments WHERE user_id=? ORDER BY first_due_on,created_at,id").use{s->s.setObject(1,ownerId.value);s.executeQuery().use{r->buildList{while(r.next())add(r.toPlan(c))}}}}
    fun findByOwnerAndId(ownerId:UserId,id:InstallmentId)=connections.open().use{c->c.prepareStatement("SELECT * FROM installments WHERE user_id=? AND id=?").use{s->s.setObject(1,ownerId.value);s.setObject(2,id);s.executeQuery().use{r->if(r.next())r.toPlan(c)else null}}}
    fun save(plan:Installment)=connections.open().use{c->c.autoCommit=false;try{
        c.prepareStatement("UPDATE installments SET status=?,updated_at=? WHERE user_id=? AND id=?").use{s->s.setString(1,plan.status.name);s.setTimestamp(2,Timestamp.from(plan.updatedAt));s.setObject(3,plan.userId.value);s.setObject(4,plan.id);s.executeUpdate()}
        c.prepareStatement("UPDATE installment_items SET status=? WHERE installment_id=? AND number=?").use{s->plan.items.forEach{i->s.setString(1,i.status.name);s.setObject(2,plan.id);s.setInt(3,i.number);s.addBatch()};s.executeBatch()};c.commit()
    }catch(e:Exception){c.rollback();throw e}finally{c.autoCommit=true}}
    private fun java.sql.ResultSet.toPlan(c:java.sql.Connection):Installment{
        val id=getObject("id",UUID::class.java)
        val items=c.prepareStatement("SELECT * FROM installment_items WHERE installment_id=? ORDER BY number").use{s->s.setObject(1,id);s.executeQuery().use{r->buildList{while(r.next())add(InstallmentItem(r.getInt("number"),Money(r.getLong("amount_minor"),CurrencyCode.valueOf(r.getString("currency"))),r.getObject("due_on",LocalDate::class.java),InstallmentItemStatus.valueOf(r.getString("status"))))}}}
        return Installment.restore(id,UserId(getObject("user_id",UUID::class.java)),Money(getLong("total_amount_minor"),CurrencyCode.valueOf(getString("currency"))),getInt("installment_count"),getObject("first_due_on",LocalDate::class.java),getString("description"),getObject("category_id",UUID::class.java)?.let(::CategoryId),items,InstallmentPlanStatus.valueOf(getString("status")),getTimestamp("created_at").toInstant(),getTimestamp("updated_at").toInstant())
    }
}
