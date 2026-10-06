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
    private val audit: AuditEventRecorder? = null,
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
        audit?.record(ownerId, AuditEventTypes.TRANSFER_CREATED, "TRANSFER", transfer.id)
        return transfer
    }

    fun list(ownerId: UserId): List<Transfer> = transfers.findByOwner(ownerId)

    fun get(ownerId: UserId, id: TransferId): Transfer =
        transfers.findByOwnerAndId(ownerId, id)
            ?: throw DomainValidationException("Transfer was not found.")

    fun post(ownerId: UserId, id: TransferId): Transfer {
        val current = get(ownerId, id)
        current.post()
        val updated = transfers.updateStatus(ownerId, id, TransferStatus.POSTED)
            ?: throw DomainValidationException("Transfer was not found.")
        audit?.record(ownerId, AuditEventTypes.TRANSFER_POSTED, "TRANSFER", updated.id)
        return updated
    }

    fun cancel(ownerId: UserId, id: TransferId): Transfer {
        val current = get(ownerId, id)
        current.cancel()
        val updated = transfers.updateStatus(ownerId, id, TransferStatus.CANCELLED)
            ?: throw DomainValidationException("Transfer was not found.")
        audit?.record(ownerId, AuditEventTypes.TRANSFER_CANCELLED, "TRANSFER", updated.id)
        return updated
    }
}