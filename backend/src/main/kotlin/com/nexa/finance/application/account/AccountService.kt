package com.nexa.finance.application.account

import com.nexa.finance.domain.account.Account
import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.account.AccountType
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.infrastructure.account.JdbcAccountRepository

class AccountService(
    private val repository: JdbcAccountRepository,
) {
    fun create(
        ownerId: UserId,
        name: String,
        type: AccountType,
        openingBalance: Money,
    ): Account {
        if (type == AccountType.CREDIT_CARD && openingBalance.amountInMinor > 0) {
            throw DomainValidationException("Credit card liability cannot start with a positive asset balance.")
        }
        val account = Account.create(ownerId, name, type, openingBalance)
        repository.create(account)
        return account
    }

    fun list(ownerId: UserId): List<Account> = repository.findByOwner(ownerId)

    fun get(ownerId: UserId, accountId: AccountId): Account =
        repository.findByOwnerAndId(ownerId, accountId)
            ?: throw DomainValidationException("Account was not found.")

    fun balance(ownerId: UserId, accountId: AccountId): Money {
        val account = get(ownerId, accountId)
        return repository.balance(account)
    }
}
