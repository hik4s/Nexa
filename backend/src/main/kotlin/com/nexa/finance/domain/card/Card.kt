package com.nexa.finance.domain.card

import com.nexa.finance.domain.account.Account
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.account.AccountType
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import java.time.Clock
import java.time.Instant
import java.util.UUID

@JvmInline
value class CardId(val value: UUID) {
    companion object {
        fun new(): CardId = CardId(UUID.randomUUID())
    }
}

enum class CardBrand { VISA, MASTERCARD, ELO, AMEX, HIPERCARD, OTHER }
enum class CardStatus { ACTIVE, BLOCKED, CANCELLED }

@ConsistentCopyVisibility
data class Card private constructor(
    val id: CardId,
    val ownerId: UserId,
    val liabilityAccountId: AccountId,
    val name: String,
    val brand: CardBrand,
    val lastFourDigits: String,
    val creditLimit: Money,
    val closingDay: Int,
    val dueDay: Int,
    val status: CardStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        if (name.isBlank() || name.length > MAX_NAME_LENGTH) {
            throw DomainValidationException("Card name must contain between 1 and $MAX_NAME_LENGTH characters.")
        }
        if (!lastFourDigits.matches(Regex("\\d{4}"))) {
            throw DomainValidationException("Card last four digits must contain exactly four digits.")
        }
        if (creditLimit.amountInMinor <= 0) {
            throw DomainValidationException("Credit limit must be greater than zero.")
        }
        if (closingDay !in 1..31) throw DomainValidationException("Closing day must be between 1 and 31.")
        if (dueDay !in 1..31) throw DomainValidationException("Due day must be between 1 and 31.")
        if (status == CardStatus.CANCELLED && updatedAt < createdAt) {
            throw DomainValidationException("Card timestamps are invalid.")
        }
    }

    fun block(now: Instant = Instant.now()): Card {
        if (status == CardStatus.CANCELLED) throw DomainValidationException("Cancelled cards cannot be blocked.")
        if (status == CardStatus.BLOCKED) return this
        return copy(status = CardStatus.BLOCKED, updatedAt = now)
    }

    fun unblock(now: Instant = Instant.now()): Card {
        if (status != CardStatus.BLOCKED) return this
        return copy(status = CardStatus.ACTIVE, updatedAt = now)
    }

    fun cancel(now: Instant = Instant.now()): Card {
        if (status == CardStatus.CANCELLED) return this
        return copy(status = CardStatus.CANCELLED, updatedAt = now)
    }

    fun ensurePurchasesAllowed() {
        if (status != CardStatus.ACTIVE) {
            throw DomainValidationException("Only active cards can be used for purchases.")
        }
    }

    companion object {
        const val MAX_NAME_LENGTH = 80

        fun restore(
            id: CardId,
            ownerId: UserId,
            liabilityAccountId: AccountId,
            name: String,
            brand: CardBrand,
            lastFourDigits: String,
            creditLimit: Money,
            closingDay: Int,
            dueDay: Int,
            status: CardStatus,
            createdAt: Instant,
            updatedAt: Instant,
        ): Card = Card(id, ownerId, liabilityAccountId, name, brand, lastFourDigits, creditLimit, closingDay, dueDay, status, createdAt, updatedAt)

        fun create(
            ownerId: UserId,
            liabilityAccount: Account,
            name: String,
            brand: CardBrand,
            lastFourDigits: String,
            creditLimit: Money,
            closingDay: Int,
            dueDay: Int,
            clock: Clock = Clock.systemUTC(),
            id: CardId = CardId.new(),
        ): Card {
            if (liabilityAccount.ownerId != ownerId) throw DomainValidationException("Card and liability account must belong to the same user.")
            if (liabilityAccount.type != AccountType.CREDIT_CARD) throw DomainValidationException("Card must be backed by a CREDIT_CARD liability account.")
            if (liabilityAccount.currency != creditLimit.currency) throw DomainValidationException("Card credit limit currency must match the liability account currency.")
            liabilityAccount.ensureUsable()
            val now = Instant.now(clock)
            return Card(
                id = id,
                ownerId = ownerId,
                liabilityAccountId = liabilityAccount.id,
                name = name.trim(),
                brand = brand,
                lastFourDigits = lastFourDigits,
                creditLimit = creditLimit,
                closingDay = closingDay,
                dueDay = dueDay,
                status = CardStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
