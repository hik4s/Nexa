package com.nexa.finance.application.card

import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.card.Card
import com.nexa.finance.domain.card.CardBrand
import com.nexa.finance.domain.card.CardId
import com.nexa.finance.domain.card.CardLedger
import com.nexa.finance.domain.card.CardStatus
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.transaction.Transaction
import com.nexa.finance.domain.transaction.TransactionSource
import com.nexa.finance.infrastructure.account.JdbcAccountRepository
import com.nexa.finance.infrastructure.card.JdbcCardRepository
import com.nexa.finance.infrastructure.transaction.JdbcTransactionRepository
import java.time.LocalDate

class CardService(
    private val cards: JdbcCardRepository,
    private val accounts: JdbcAccountRepository,
    private val transactions: JdbcTransactionRepository,
) {
    fun create(ownerId: UserId, liabilityAccountId: AccountId, name: String, brand: CardBrand, lastFourDigits: String, creditLimit: Money, closingDay: Int, dueDay: Int): Card {
        val account = accounts.findByOwnerAndId(ownerId, liabilityAccountId) ?: throw DomainValidationException("Liability account was not found.")
        val card = Card.create(ownerId, account, name, brand, lastFourDigits, creditLimit, closingDay, dueDay)
        cards.create(card)
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
        return cards.updateStatus(ownerId, id, next.status) ?: throw DomainValidationException("Card was not found.")
    }

    fun purchase(ownerId: UserId, cardId: CardId, amount: Money, occurredOn: LocalDate, invoiceId: com.nexa.finance.domain.invoice.InvoiceId?, description: String?, source: TransactionSource, post: Boolean): Transaction {
        val card = get(ownerId, cardId)
        val liability = accounts.findByOwnerAndId(ownerId, card.liabilityAccountId) ?: throw DomainValidationException("Card liability account was not found.")
        val available = CardLedger.availableLimit(card, accounts.balance(liability))
        if (amount > available) throw DomainValidationException("Card credit limit exceeded.")
        val transaction = CardLedger.purchase(card, amount, occurredOn, invoiceId, description, source)
        transactions.create(ownerId, transaction)
        return if (post) transactions.updateStatus(ownerId, transaction.id, com.nexa.finance.domain.transaction.TransactionStatus.POSTED) ?: throw DomainValidationException("Card purchase could not be posted.") else transaction
    }
}