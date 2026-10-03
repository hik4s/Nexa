package com.nexa.finance.domain.account

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.transaction.Transaction
import com.nexa.finance.domain.transaction.TransactionDirection
import com.nexa.finance.domain.transaction.TransactionStatus

/**
 * Calculates the balance represented by posted transactions.
 *
 * Asset accounts: CREDIT increases balance, DEBIT decreases it.
 * Liability accounts: DEBIT reduces debt, CREDIT increases debt.
 */
object AccountBalance {
    fun calculate(account: Account, transactions: List<Transaction>): Money {
        val relevant = transactions.filter {
            it.accountId == account.id && it.status == TransactionStatus.POSTED
        }
        return relevant.fold(account.openingBalance) { balance, tx ->
            val signed = when (account.type) {
                AccountType.CREDIT_CARD -> when (tx.direction) {
                    TransactionDirection.CREDIT -> tx.amount
                    TransactionDirection.DEBIT -> -tx.amount
                }
                else -> when (tx.direction) {
                    TransactionDirection.CREDIT -> tx.amount
                    TransactionDirection.DEBIT -> -tx.amount
                }
            }
            balance + signed
        }
    }
}
