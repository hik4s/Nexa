package com.nexa.finance.infrastructure.installment

import com.nexa.finance.domain.category.CategoryId
import com.nexa.finance.domain.common.CurrencyCode
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.installment.*
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
