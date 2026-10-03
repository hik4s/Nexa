package com.nexa.finance.domain.card

import com.nexa.finance.domain.account.Account
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.account.AccountType
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.invoice.Invoice
import com.nexa.finance.domain.transaction.Transaction
import com.nexa.finance.domain.transaction.TransactionDirection
import com.nexa.finance.domain.transaction.TransactionSource
import com.nexa.finance.domain.transaction.TransactionStatus
import com.nexa.finance.domain.transaction.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

typealias CardPaymentId = UUID

@ConsistentCopyVisibility
data class CardPayment private constructor(
    val id: CardPaymentId,
    val userId: UserId,
    val cardId: CardId,
    val sourceAccountId: AccountId,
    val invoiceId: com.nexa.finance.domain.invoice.InvoiceId,
    val amount: Money,
    val occurredOn: LocalDate,
    val sourceTransaction: Transaction,
    val liabilityTransaction: Transaction,
    val createdAt: Instant,
) {
    init {
        if (amount.amountInMinor <= 0) throw DomainValidationException("Card payment amount must be greater than zero.")
        if (sourceTransaction.accountId != sourceAccountId) throw DomainValidationException("Payment source transaction must reference the source account.")
        if (liabilityTransaction.accountId == sourceAccountId) throw DomainValidationException("Payment source and liability accounts must be different.")
        if (sourceTransaction.amount != amount || liabilityTransaction.amount != amount) throw DomainValidationException("Both payment transactions must use the payment amount.")
        if (sourceTransaction.direction != TransactionDirection.DEBIT || liabilityTransaction.direction != TransactionDirection.DEBIT) throw DomainValidationException("Card payment must debit both the cash account and the liability account.")
        if (sourceTransaction.type != TransactionType.PAYMENT || liabilityTransaction.type != TransactionType.PAYMENT) throw DomainValidationException("Card payment transactions must have type PAYMENT.")
        if (sourceTransaction.relatedTransactionId != liabilityTransaction.id || liabilityTransaction.relatedTransactionId != sourceTransaction.id) throw DomainValidationException("Payment transactions must reference each other.")
        if (sourceTransaction.status != liabilityTransaction.status) throw DomainValidationException("Card payment transaction statuses must match.")
        if (sourceTransaction.status !in setOf(TransactionStatus.PENDING, TransactionStatus.POSTED)) throw DomainValidationException("Card payment must be pending or posted.")
    }

    fun post(now: Instant = Instant.now()): CardPayment =
        copy(
            sourceTransaction = sourceTransaction.post(now),
            liabilityTransaction = liabilityTransaction.post(now),
        )

    companion object {
        fun create(
            userId: UserId,
            sourceAccount: Account,
            card: Card,
            invoice: Invoice,
            amount: Money,
            occurredOn: LocalDate,
            source: TransactionSource = TransactionSource.MANUAL,
            clock: Clock = Clock.systemUTC(),
            id: CardPaymentId = UUID.randomUUID(),
        ): CardPayment {
            sourceAccount.ensureUsable()
            card.ensurePurchasesAllowed()
            if (sourceAccount.ownerId != userId || card.ownerId != userId) throw DomainValidationException("Payment accounts must belong to the same user.")
            if (sourceAccount.type == AccountType.CREDIT_CARD) throw DomainValidationException("A card payment source must be a non-card account.")
            if (invoice.cardId != card.id) throw DomainValidationException("Invoice does not belong to the selected card.")
            if (amount.currency != sourceAccount.currency || amount.currency != card.creditLimit.currency) throw DomainValidationException("Payment currency must match both accounts.")
            if (amount <= Money.zero(amount.currency)) throw DomainValidationException("Payment amount must be greater than zero.")
            if (amount > invoice.remainingAmount()) throw DomainValidationException("Payment cannot exceed the invoice remaining balance.")

            val now = Instant.now(clock)
            val liabilityId = UUID.randomUUID()
            val sourceId = UUID.randomUUID()
            val sourceTx = Transaction.create(
                accountId = sourceAccount.id,
                amount = amount,
                type = TransactionType.PAYMENT,
                direction = TransactionDirection.DEBIT,
                occurredOn = occurredOn,
                cardId = card.id,
                invoiceId = invoice.id,
                source = source,
                status = TransactionStatus.PENDING,
                relatedTransactionId = liabilityId,
                id = sourceId,
                createdAt = now,
                updatedAt = now,
            )
            val liabilityTx = Transaction.create(
                accountId = card.liabilityAccountId,
                amount = amount,
                type = TransactionType.PAYMENT,
                direction = TransactionDirection.DEBIT,
                occurredOn = occurredOn,
                cardId = card.id,
                invoiceId = invoice.id,
                source = source,
                status = TransactionStatus.PENDING,
                relatedTransactionId = sourceId,
                id = liabilityId,
                createdAt = now,
                updatedAt = now,
            )
            return CardPayment(id, userId, card.id, sourceAccount.id, invoice.id, amount, occurredOn, sourceTx, liabilityTx, now)
        }
    }
}

