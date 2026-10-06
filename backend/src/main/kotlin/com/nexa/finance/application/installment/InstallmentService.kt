package com.nexa.finance.application.installment

import com.nexa.finance.application.audit.AuditEventRecorder
import com.nexa.finance.application.audit.AuditEventTypes
import com.nexa.finance.domain.category.CategoryId
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.installment.Installment
import com.nexa.finance.domain.installment.InstallmentId
import com.nexa.finance.infrastructure.installment.JdbcInstallmentRepository
import com.nexa.finance.infrastructure.transaction.JdbcTransactionRepository
import java.time.LocalDate

class InstallmentService(
    private val installments: JdbcInstallmentRepository,
    private val transactions: JdbcTransactionRepository,
    private val audit: AuditEventRecorder? = null,
) {
    fun create(ownerId: UserId, totalAmount: Money, count: Int, firstDueOn: LocalDate, description: String?, categoryId: CategoryId?): Installment {
        if (categoryId != null && !transactions.categoryBelongsToOwner(ownerId, categoryId)) {
            throw DomainValidationException("Category was not found.")
        }
        val plan = Installment.create(ownerId, totalAmount, count, firstDueOn, description, categoryId)
        installments.create(plan)
        audit?.record(ownerId, AuditEventTypes.INSTALLMENT_CREATED, "INSTALLMENT", plan.id)
        return plan
    }

    fun list(ownerId: UserId): List<Installment> = installments.list(ownerId)

    fun get(ownerId: UserId, id: InstallmentId): Installment =
        installments.findByOwnerAndId(ownerId, id)
            ?: throw DomainValidationException("Installment plan was not found.")

    fun complete(ownerId: UserId, id: InstallmentId, number: Int): Installment {
        val updated = get(ownerId, id).completeItem(number)
        installments.save(updated)
        audit?.record(ownerId, AuditEventTypes.INSTALLMENT_COMPLETED, "INSTALLMENT", updated.id)
        return updated
    }

    fun cancel(ownerId: UserId, id: InstallmentId): Installment {
        val updated = get(ownerId, id).cancel()
        installments.save(updated)
        audit?.record(ownerId, AuditEventTypes.INSTALLMENT_CANCELLED, "INSTALLMENT", updated.id)
        return updated
    }
}
