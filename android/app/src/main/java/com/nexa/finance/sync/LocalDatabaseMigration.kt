package com.nexa.finance.sync

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_transactions (
                id TEXT NOT NULL PRIMARY KEY,
                accountId TEXT NOT NULL,
                amountMinor INTEGER NOT NULL,
                currency TEXT NOT NULL,
                type TEXT NOT NULL,
                direction TEXT NOT NULL,
                status TEXT NOT NULL,
                occurredOn TEXT NOT NULL,
                description TEXT,
                categoryId TEXT,
                source TEXT NOT NULL,
                installmentId TEXT,
                installmentNumber INTEGER
            )
        """.trimIndent())
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE local_transactions ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
    }
}
