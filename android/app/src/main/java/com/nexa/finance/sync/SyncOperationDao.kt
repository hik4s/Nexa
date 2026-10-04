package com.nexa.finance.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncOperationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(operation: SyncOperationEntity)

    @Query(
        """
        SELECT * FROM sync_operations
        WHERE userId = :userId
          AND status IN ('PENDING', 'FAILED')
          AND nextAttemptAtEpochMillis <= :nowEpochMillis
        ORDER BY createdAtEpochMillis ASC
        LIMIT :limit
        """,
    )
    suspend fun pending(userId: String, nowEpochMillis: Long, limit: Int): List<SyncOperationEntity>

    @Query("UPDATE sync_operations SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE sync_operations SET lastError = :lastError WHERE id = :id")
    suspend fun updateLastError(id: String, lastError: String)

    @Query(
        """
        UPDATE sync_operations
        SET status = 'FAILED',
            nextAttemptAtEpochMillis = :nowEpochMillis,
            lastError = 'Recovered after interrupted sync.'
        WHERE status = 'PROCESSING'
          AND createdAtEpochMillis < :cutoffEpochMillis
        """,
    )
    suspend fun recoverStaleProcessing(nowEpochMillis: Long, cutoffEpochMillis: Long)

    @Query(
        """
        UPDATE sync_operations
        SET status = 'FAILED',
            attemptCount = :attemptCount,
            nextAttemptAtEpochMillis = :nextAttemptAtEpochMillis,
            lastError = :lastError
        WHERE id = :id
        """,
    )
    suspend fun markFailed(
        id: String,
        attemptCount: Int,
        nextAttemptAtEpochMillis: Long,
        lastError: String,
    )

    @Query("DELETE FROM sync_operations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM sync_operations WHERE userId = :userId")
    suspend fun count(userId: String): Int
}
