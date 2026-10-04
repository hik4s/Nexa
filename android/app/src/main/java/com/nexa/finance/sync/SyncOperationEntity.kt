package com.nexa.finance.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_operations")
data class SyncOperationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payloadJson: String,
    val status: String = STATUS_PENDING,
    val attemptCount: Int = 0,
    val nextAttemptAtEpochMillis: Long = 0L,
    val createdAtEpochMillis: Long,
    val lastError: String? = null,
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_PROCESSING = "PROCESSING"
        const val STATUS_FAILED = "FAILED"
    }
}
