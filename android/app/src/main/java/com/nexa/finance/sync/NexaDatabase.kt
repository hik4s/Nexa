package com.nexa.finance.sync

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SyncOperationEntity::class,
        SyncCheckpointEntity::class,
        LocalTransactionEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class NexaDatabase : RoomDatabase() {
    abstract fun syncOperationDao(): SyncOperationDao
    abstract fun syncCheckpointDao(): SyncCheckpointDao
    abstract fun localTransactionDao(): LocalTransactionDao
}
