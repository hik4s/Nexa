package com.nexa.finance.sync

import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.security.SecureRandom
import net.zetetic.database.sqlcipher.SQLiteDatabase as SQLCipherDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDatabaseEncryptionInstrumentedTest {

    @Test
    fun migratesV3PlaintextDatabaseAndPreservesData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseFile = context.getDatabasePath("nexa-local.db")
        cleanup(databaseFile)

        val key = ByteArray(32).also(SecureRandom()::nextBytes)
        createV3PlaintextDatabase(databaseFile)

        assertTrue(readHeader(databaseFile).contentEquals(SQLITE_HEADER))

        LocalDatabaseEncryption.encryptPlaintextDatabaseForTest(databaseFile, key)

        assertFalse(readHeader(databaseFile).contentEquals(SQLITE_HEADER))
        assertFalse(File(databaseFile.absolutePath + ".encrypted.tmp").exists())
        assertFalse(File(databaseFile.absolutePath + ".plaintext.bak").exists())

        assertEquals("18.90", readDescription(databaseFile, key))
        assertEquals(3, readUserVersion(databaseFile, key))

        LocalDatabaseEncryption.encryptPlaintextDatabaseForTest(databaseFile, key)

        assertEquals("18.90", readDescription(databaseFile, key))

        val wrongKey = ByteArray(32).also(SecureRandom()::nextBytes)
        var wrongKeyRejected = false
        try {
            readUserVersion(databaseFile, wrongKey)
        } catch (_: Exception) {
            wrongKeyRejected = true
        }
        assertTrue(wrongKeyRejected)

        cleanup(databaseFile)
    }

    @Test
    fun localDatabaseProviderMigratesV3DatabaseWithKeystoreKey() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseFile = context.getDatabasePath("nexa-local.db")

        LocalDatabaseProvider.resetForTesting()
        cleanup(databaseFile)
        createV3PlaintextDatabase(databaseFile)

        assertTrue(readHeader(databaseFile).contentEquals(SQLITE_HEADER))

        val database = LocalDatabaseProvider.get(context)
        try {
            assertFalse(readHeader(databaseFile).contentEquals(SQLITE_HEADER))
        } finally {
            database.close()
            LocalDatabaseProvider.resetForTesting()
            cleanup(databaseFile)
        }
    }

    @Test
    fun databaseKeyStoreReturnsStableWrappedKey() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val keyStore = LocalDatabaseKeyStore(context)

        val first = keyStore.getOrCreate()
        val second = keyStore.getOrCreate()

        assertTrue(first.contentEquals(second))
        assertEquals(32, first.size)
    }

    private fun createV3PlaintextDatabase(file: File) {
        file.parentFile?.mkdirs()
        val database = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            database.execSQL(
                "CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)"
            )
            database.execSQL(
                "INSERT INTO room_master_table(id, identity_hash) VALUES(42, '551109071a0779eb34bac679d350b519')"
            )
            database.execSQL(
                """
                CREATE TABLE sync_operations (
                    id TEXT NOT NULL PRIMARY KEY,
                    userId TEXT NOT NULL,
                    entityType TEXT NOT NULL,
                    entityId TEXT NOT NULL,
                    operation TEXT NOT NULL,
                    payloadJson TEXT NOT NULL,
                    status TEXT NOT NULL,
                    attemptCount INTEGER NOT NULL,
                    nextAttemptAtEpochMillis INTEGER NOT NULL,
                    createdAtEpochMillis INTEGER NOT NULL,
                    lastError TEXT
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                CREATE TABLE sync_checkpoints (
                    userId TEXT NOT NULL PRIMARY KEY,
                    serverCursor TEXT,
                    lastSyncedAtEpochMillis INTEGER
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                CREATE TABLE local_transactions (
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
                    installmentNumber INTEGER,
                    version INTEGER NOT NULL
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO local_transactions(
                    id, accountId, amountMinor, currency, type, direction, status,
                    occurredOn, description, categoryId, source, installmentId,
                    installmentNumber, version
                ) VALUES(
                    'tx-1', 'account-1', 1890, 'BRL', 'EXPENSE', 'DEBIT', 'POSTED',
                    '2026-10-05', '18.90', NULL, 'MANUAL', NULL, NULL, 1
                )
                """.trimIndent()
            )
            database.execSQL("PRAGMA user_version = 3")
        } finally {
            database.close()
        }
    }

    private fun readDescription(file: File, key: ByteArray): String {
        val database = SQLCipherDatabase.openDatabase(
            file.absolutePath,
            password(key),
            null,
            SQLCipherDatabase.OPEN_READONLY,
            null,
            null,
        )
        return try {
            database.rawQuery(
                "SELECT description FROM local_transactions WHERE id = 'tx-1'",
                null,
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                cursor.getString(0)
            }
        } finally {
            database.close()
        }
    }

    private fun password(key: ByteArray): ByteArray =
        Base64.encodeToString(key, Base64.NO_WRAP).toByteArray(Charsets.UTF_8)

    private fun readUserVersion(file: File, key: ByteArray): Int {
        val database = SQLCipherDatabase.openDatabase(
            file.absolutePath,
            password(key),
            null,
            SQLCipherDatabase.OPEN_READONLY,
            null,
            null,
        )
        return try {
            database.rawQuery("PRAGMA user_version", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                cursor.getInt(0)
            }
        } finally {
            database.close()
        }
    }

    private fun readHeader(file: File): ByteArray {
        file.inputStream().use { input ->
            val header = ByteArray(SQLITE_HEADER.size)
            input.read(header)
            return header
        }
    }

    private fun cleanup(file: File) {
        file.delete()
        File(file.absolutePath + "-wal").delete()
        File(file.absolutePath + "-shm").delete()
        File(file.absolutePath + ".encrypted.tmp").delete()
        File(file.absolutePath + ".plaintext.bak").delete()
    }

    companion object {
        private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    }
}
