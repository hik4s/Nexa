package com.nexa.finance.application.card

import com.nexa.finance.application.audit.AuditEventRecorder
import com.nexa.finance.application.audit.AuditEventTypes
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.card.Card
import com.nexa.finance.domain.card.CardBrand
import com.nexa.finance.domain.card.CardId
import com.nexa.finance.domain.card.CardLedger
import com.nexa.finance.domain.card.CardStatus
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.installment.Installment
import com.nexa.finance.domain.transaction.Transaction
import com.nexa.finance.domain.transaction.TransactionSource
import com.nexa.finance.infrastructure.account.JdbcAccountRepository
import com.nexa.finance.infrastructure.card.JdbcCardRepository
import com.nexa.finance.infrastructure.installment.JdbcInstallmentRepository
import com.nexa.finance.infrastructure.transaction.JdbcTransactionRepository
import java.time.LocalDate
import java.time.YearMonth

class CardService(
    private val cards: JdbcCardRepository,
    private val accounts: JdbcAccountRepository,
    private val transactions: JdbcTransactionRepository,
    private val installments: JdbcInstallmentRepository,
    private val audit: AuditEventRecorder? = null,
) {
    fun create(ownerId: UserId, liabilityAccountId: AccountId, name: String, brand: CardBrand, lastFourDigits: String, creditLimit: Money, closingDay: Int, dueDay: Int): Card {
        val account = accounts.findByOwnerAndId(ownerId, liabilityAccountId) ?: throw DomainValidationException("Liability account was not found.")
        val card = Card.create(ownerId, account, name, brand, lastFourDigits, creditLimit, closingDay, dueDay)
        cards.create(card)
        audit?.record(ownerId, AuditEventTypes.CARD_CREATED, "CARD", card.id.value)
        return card
    }

    fun list(ownerId: UserId): List<Card> = cards.findByOwner(ownerId)

    fun availableLimit(ownerId: UserId, id: CardId): Money {
        val card = get(ownerId, id)
        val account = accounts.findByOwnerAndId(ownerId, card.liabilityAccountId) ?: throw DomainValidationException("Card liability account was not found.")
        return CardLedger.availableLimit(card, accounts.balance(account))
    }
    fun get(ownerId: UserId, id: CardId): Card = cards.findByOwnerAndId(ownerId, id) ?: throw DomainValidationException("Card was not found.")

    fun updateStatus(ownerId: UserId, id: CardId, status: CardStatus): Card {
        val current = get(ownerId, id)
        val next = when (status) {
            CardStatus.ACTIVE -> {
                if (current.status != CardStatus.BLOCKED) throw DomainValidationException("Only blocked cards can be unblocked.")
                current.unblock()
            }
            CardStatus.BLOCKED -> current.block()
            CardStatus.CANCELLED -> current.cancel()
        }
        val updated = cards.updateStatus(ownerId, id, next.status) ?: throw DomainValidationException("Card was not found.")
        audit?.record(ownerId, AuditEventTypes.CARD_STATUS_CHANGED, "CARD", updated.id.value)
        return updated
    }

    fun purchase(ownerId: UserId, cardId: CardId, amount: Money, occurredOn: LocalDate, invoiceId: com.nexa.finance.domain.invoice.InvoiceId?, description: String?, source: TransactionSource, post: Boolean, installmentCount: Int = 1): Transaction {
        val card = get(ownerId, cardId)
        val liability = accounts.findByOwnerAndId(ownerId, card.liabilityAccountId) ?: throw DomainValidationException("Card liability account was not found.")
        val available = CardLedger.availableLimit(card, accounts.balance(liability))
        if (amount > available) throw DomainValidationException("Card credit limit exceeded.")
        if (installmentCount == 1) {
            val transaction = CardLedger.purchase(card, amount, occurredOn, invoiceId = invoiceId, description = description, source = source)
            transactions.create(ownerId, transaction)
            audit?.record(ownerId, AuditEventTypes.CARD_PURCHASE_CREATED, "TRANSACTION", transaction.id)
            return if (post) transactions.updateStatus(
                ownerId,
                transaction.id,
                com.nexa.finance.domain.transaction.TransactionStatus.POSTED,
                transaction.version,
            ) ?: throw DomainValidationException("Card purchase could not be posted.") else transaction
        }
        if (installmentCount !in Installment.MIN_INSTALLMENTS..Installment.MAX_INSTALLMENTS) {
            throw DomainValidationException("Installment count must be between ${Installment.MIN_INSTALLMENTS} and ${Installment.MAX_INSTALLMENTS}.")
        }

        val firstDueOn = firstInstallmentDueOn(card, occurredOn)
        val plan = Installment.create(ownerId, amount, installmentCount, firstDueOn, description)
        val cycleEnd = firstBillingCycleEnd(card, occurredOn)
        val installmentTransactions = plan.items.mapIndexed { index, item ->
            val billingOccurrence = if (index == 0) occurredOn else monthlyDate(cycleEnd, index)
            val installmentDescription = "${(description ?: "Compra parcelada").take(145)} • parcela ${item.number}/$installmentCount"
            val pending = CardLedger.purchase(
                card = card,
                amount = item.amount,
                occurredOn = billingOccurrence,
                invoiceId = null,
                installmentId = plan.id,
                installmentNumber = item.number,
                description = installmentDescription,
                source = source,
            )
            if (post) pending.post() else pending
        }
        installments.persistCardPurchase(ownerId, card, plan, installmentTransactions)
        audit?.record(ownerId, AuditEventTypes.CARD_PURCHASE_CREATED, "TRANSACTION", installmentTransactions.first().id)
        return installmentTransactions.first()
    }

    private fun firstBillingCycleEnd(card: Card, occurredOn: LocalDate): LocalDate {
        val month = YearMonth.from(occurredOn)
        val target = if (occurredOn.dayOfMonth <= card.closingDay) month else month.plusMonths(1)
        return target.atDay(minOf(card.closingDay, target.lengthOfMonth()))
    }

    private fun firstInstallmentDueOn(card: Card, occurredOn: LocalDate): LocalDate {
        val cycleEnd = firstBillingCycleEnd(card, occurredOn)
        val dueMonth = if (card.dueDay > card.closingDay) YearMonth.from(cycleEnd) else YearMonth.from(cycleEnd).plusMonths(1)
        return dueMonth.atDay(minOf(card.dueDay, dueMonth.lengthOfMonth()))
    }

    private fun monthlyDate(base: LocalDate, offset: Int): LocalDate {
        val month = YearMonth.from(base).plusMonths(offset.toLong())
        return month.atDay(minOf(base.dayOfMonth, month.lengthOfMonth()))
    }
}