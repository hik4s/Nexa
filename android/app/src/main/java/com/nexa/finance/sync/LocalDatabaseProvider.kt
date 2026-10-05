package com.nexa.finance.sync

import android.content.Context
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object LocalDatabaseProvider {
    @Volatile
    private var instance: NexaDatabase? = null

    fun get(context: Context): NexaDatabase =
        instance ?: synchronized(this) {
            instance ?: run {
                val applicationContext = context.applicationContext
                val databaseKey = LocalDatabaseEncryption.prepare(applicationContext)
                Room.databaseBuilder(
                    applicationContext,
                    NexaDatabase::class.java,
                    DATABASE_NAME,
                ).openHelperFactory(SupportOpenHelperFactory(databaseKey.copyOf()))
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { instance = it }
            }
        }

    internal fun resetForTesting() {
        synchronized(this) {
            instance?.close()
            instance = null
        }
    }

    private const val DATABASE_NAME = "nexa-local.db"
}
