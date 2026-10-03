package com.nexa.finance.domain.card

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.invoice.InvoiceId
import com.nexa.finance.domain.transaction.Transaction
import com.nexa.finance.domain.transaction.TransactionDirection
import com.nexa.finance.domain.transaction.TransactionSource
import com.nexa.finance.domain.transaction.TransactionType`r`nimport com.nexa.finance.domain.transaction.TransactionStatus
import java.time.Clock
import java.time.LocalDate

object CardLedger {
    fun purchase(
        card: Card,
        amount: Money,
        occurredOn: LocalDate,
        invoiceId: InvoiceId? = null,
        description: String? = null,
        source: TransactionSource = TransactionSource.MANUAL,
        clock: Clock = Clock.systemUTC(),
    ): Transaction {
        card.ensurePurchasesAllowed()
        requireCurrency(card, amount)
        return Transaction.create(
            accountId = card.liabilityAccountId,
            amount = amount,
            type = TransactionType.PURCHASE,
            direction = TransactionDirection.CREDIT,
            occurredOn = occurredOn,
            description = description,
            cardId = card.id,
            invoiceId = invoiceId,
            source = source,
            createdAt = java.time.Instant.now(clock),
        )
    }

    fun refund(
        card: Card,
        amount: Money,
        occurredOn: LocalDate,
        invoiceId: InvoiceId? = null,
        description: String? = null,
        source: TransactionSource = TransactionSource.MANUAL,
        clock: Clock = Clock.systemUTC(),
    ): Transaction {
        card.ensurePurchasesAllowed()
        requireCurrency(card, amount)
        return Transaction.create(
            accountId = card.liabilityAccountId,
            amount = amount,
            type = TransactionType.REFUND,
            direction = TransactionDirection.DEBIT,
            occurredOn = occurredOn,
            description = description,
            cardId = card.id,
            invoiceId = invoiceId,
            source = source,
            createdAt = java.time.Instant.now(clock),
        )
    }

    fun payment(
        card: Card,
        amount: Money,
        occurredOn: LocalDate,
        invoiceId: InvoiceId,
        description: String? = null,
        source: TransactionSource = TransactionSource.MANUAL,
        clock: Clock = Clock.systemUTC(),
    ): Transaction {
        requireCurrency(card, amount)
        return Transaction.create(
            accountId = card.liabilityAccountId,
            amount = amount,
            type = TransactionType.PAYMENT,
            direction = TransactionDirection.DEBIT,
            occurredOn = occurredOn,
            description = description,
            cardId = card.id,
            invoiceId = invoiceId,
            source = source,
            createdAt = java.time.Instant.now(clock),
        )
    }

    fun availableLimit(card: Card, liabilityBalance: Money): Money {
        requireCurrency(card, liabilityBalance)
        val available = card.creditLimit - liabilityBalance
        return if (available.isNegative()) Money.zero(card.creditLimit.currency) else available
    }

    private fun requireCurrency(card: Card, amount: Money) {
        if (amount.currency != card.creditLimit.currency) {
            throw DomainValidationException("Card transaction currency must match the card credit limit currency.")
        }
        if (amount.amountInMinor <= 0) {
            throw DomainValidationException("Card transaction amount must be greater than zero.")
        }
    }
}


