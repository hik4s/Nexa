package com.nexa.finance.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_transactions")
data class LocalTransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val amountMinor: Long,
    val currency: String,
    val type: String,
    val direction: String,
    val status: String,
    val occurredOn: String,
    val description: String?,
    val categoryId: String?,
    val source: String,
    val installmentId: String?,
    val installmentNumber: Int?,
)
