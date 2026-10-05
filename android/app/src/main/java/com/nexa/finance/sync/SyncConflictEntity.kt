package com.nexa.finance.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Retains the local operation payload and the server-authoritative snapshot after a sync conflict.
 *
 * The record remains until an explicit conflict-resolution flow consumes it, preventing
 * an offline edit from being silently discarded.
 */
@Entity(
    tableName = "sync_conflicts",
)
data class SyncConflictEntity(
    @PrimaryKey val operationId: String,
    val userId: String,
    val entityType: String,
    val entityId: String,
    val localPayloadJson: String,
    val serverTransactionJson: String?,
    val serverVersion: Long?,
    val createdAtEpochMillis: Long,
)