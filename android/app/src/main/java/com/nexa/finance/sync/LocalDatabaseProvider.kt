package com.nexa.finance.sync

import android.content.Context
import androidx.room.Room

object LocalDatabaseProvider {
    @Volatile
    private var instance: NexaDatabase? = null

    fun get(context: Context): NexaDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                NexaDatabase::class.java,
                "nexa-local.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { instance = it }
        }
}
