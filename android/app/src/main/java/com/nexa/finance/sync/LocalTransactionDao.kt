package com.nexa.finance.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocalTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: LocalTransactionEntity)

    @Query("SELECT * FROM local_transactions WHERE id = :id")
    suspend fun get(id: String): LocalTransactionEntity?

    @Query("SELECT * FROM local_transactions WHERE accountId = :accountId ORDER BY occurredOn DESC, id DESC")
    suspend fun listByAccount(accountId: String): List<LocalTransactionEntity>
}
