package com.nexa.finance.application.transfer

import com.nexa.finance.domain.account.AccountId
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.Money
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.transaction.TransactionSource
import com.nexa.finance.domain.transfer.Transfer
import com.nexa.finance.domain.transfer.TransferId
import com.nexa.finance.domain.transfer.TransferStatus
import com.nexa.finance.infrastructure.account.JdbcAccountRepository
import com.nexa.finance.infrastructure.transfer.JdbcTransferRepository
import java.time.LocalDate

class TransferService(
    private val transfers: JdbcTransferRepository,
    private val accounts: JdbcAccountRepository,
) {
    fun create(
        ownerId: UserId,
        sourceAccountId: AccountId,
        destinationAccountId: AccountId,
        amount: Money,
        occurredOn: LocalDate,
        source: TransactionSource,
    ): Transfer {
        val sourceAccount = accounts.findByOwnerAndId(ownerId, sourceAccountId)
            ?: throw DomainValidationException("Source account was not found.")
        val destinationAccount = accounts.findByOwnerAndId(ownerId, destinationAccountId)
            ?: throw DomainValidationException("Destination account was not found.")
        val transfer = Transfer.create(ownerId, sourceAccount, destinationAccount, amount, occurredOn, source)
        transfers.create(ownerId, transfer)
        return transfer
    }

    fun list(ownerId: UserId): List<Transfer> = transfers.findByOwner(ownerId)

    fun get(ownerId: UserId, id: TransferId): Transfer =
        transfers.findByOwnerAndId(ownerId, id)
            ?: throw DomainValidationException("Transfer was not found.")

    fun post(ownerId: UserId, id: TransferId): Transfer {
        val current = get(ownerId, id)
        current.post()
        return transfers.updateStatus(ownerId, id, TransferStatus.POSTED)
            ?: throw DomainValidationException("Transfer was not found.")
    }

    fun cancel(ownerId: UserId, id: TransferId): Transfer {
        val current = get(ownerId, id)
        current.cancel()
        return transfers.updateStatus(ownerId, id, TransferStatus.CANCELLED)
            ?: throw DomainValidationException("Transfer was not found.")
    }
}