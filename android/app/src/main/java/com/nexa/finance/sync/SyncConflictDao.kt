package com.nexa.finance.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncConflictDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conflict: SyncConflictEntity)

    @Query(
        """
        SELECT * FROM sync_conflicts
        WHERE userId = :userId
        ORDER BY createdAtEpochMillis ASC
        """,
    )
    suspend fun pending(userId: String): List<SyncConflictEntity>

    @Query("SELECT COUNT(*) FROM sync_conflicts WHERE userId = :userId")
    suspend fun count(userId: String): Int

    @Query("DELETE FROM sync_conflicts WHERE operationId = :operationId")
    suspend fun delete(operationId: String)

    @Query("DELETE FROM sync_conflicts WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}