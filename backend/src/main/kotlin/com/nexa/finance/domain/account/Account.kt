package com.nexa.finance.domain.account

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import java.time.Clock
import java.time.Instant
import java.util.UUID

@JvmInline
value class AccountId(val value: UUID) {
    companion object {
        fun new(): AccountId = AccountId(UUID.randomUUID())
    }
}

enum class AccountType {
    CHECKING,
    SAVINGS,
    CASH,
    WALLET,
    INVESTMENT,
    OTHER
}

enum class AccountStatus {
    ACTIVE,
    ARCHIVED
}

class Account private constructor(
    val id: AccountId,
    val ownerId: UserId,
    name: String,
    val type: AccountType,
    val openingBalance: Money,
    val createdAt: Instant,
    updatedAt: Instant,
    status: AccountStatus
) {
    val currency get() = openingBalance.currency

    var name: String = validateName(name)
        private set

    var updatedAt: Instant = updatedAt
        private set

    var status: AccountStatus = status
        private set

    fun rename(newName: String, now: Instant) {
        ensureNotArchived()
        name = validateName(newName)
        updatedAt = now
    }

    fun archive(now: Instant) {
        if (status == AccountStatus.ARCHIVED) return
        status = AccountStatus.ARCHIVED
        updatedAt = now
    }

    fun restore(now: Instant) {
        if (status == AccountStatus.ACTIVE) return
        status = AccountStatus.ACTIVE
        updatedAt = now
    }

    fun ensureUsable() {
        ensureNotArchived()
    }

    private fun ensureNotArchived() {
        if (status == AccountStatus.ARCHIVED) {
            throw DomainValidationException("Archived accounts cannot be modified or used for new transactions.")
        }
    }

    companion object {
        fun create(
            ownerId: UserId,
            name: String,
            type: AccountType,
            openingBalance: Money,
            clock: Clock = Clock.systemUTC(),
            id: AccountId = AccountId.new()
        ): Account {
            val now = Instant.now(clock)
            return Account(
                id = id,
                ownerId = ownerId,
                name = name,
                type = type,
                openingBalance = openingBalance,
                createdAt = now,
                updatedAt = now,
                status = AccountStatus.ACTIVE
            )
        }

        private fun validateName(value: String): String {
            val normalized = value.trim()
            if (normalized.length !in 1..80) {
                throw DomainValidationException("Account name must contain between 1 and 80 characters.")
            }
            return normalized
        }
    }
}
