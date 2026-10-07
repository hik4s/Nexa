package com.nexa.finance.application.installment

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
) {
    fun create(ownerId: UserId, totalAmount: Money, count: Int, firstDueOn: LocalDate, description: String?, categoryId: CategoryId?): Installment {
        if (categoryId != null && !transactions.categoryBelongsToOwner(ownerId, categoryId)) {
            throw DomainValidationException("Category was not found.")
        }
        val plan = Installment.create(ownerId, totalAmount, count, firstDueOn, description, categoryId)
        installments.create(plan)
        return plan
    }

    fun list(ownerId: UserId): List<Installment> = installments.list(ownerId)

    fun get(ownerId: UserId, id: InstallmentId): Installment =
        installments.findByOwnerAndId(ownerId, id)
            ?: throw DomainValidationException("Installment plan was not found.")

    fun complete(ownerId: UserId, id: InstallmentId, number: Int): Installment {
        val updated = get(ownerId, id).completeItem(number)
        installments.save(updated)
        return updated
    }

    fun cancel(ownerId: UserId, id: InstallmentId): Installment {
        val updated = get(ownerId, id).cancel()
        installments.save(updated)
        return updated
    }
}
