package com.nexa.finance.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_checkpoints")
data class SyncCheckpointEntity(
    @PrimaryKey val userId: String,
    val serverCursor: String? = null,
    val lastSyncedAtEpochMillis: Long? = null,
)
