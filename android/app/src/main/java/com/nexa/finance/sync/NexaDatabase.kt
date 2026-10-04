package com.nexa.finance.sync

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SyncOperationEntity::class,
        SyncCheckpointEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NexaDatabase : RoomDatabase() {
    abstract fun syncOperationDao(): SyncOperationDao
    abstract fun syncCheckpointDao(): SyncCheckpointDao
}
