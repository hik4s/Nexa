package com.nexa.finance.sync

import android.content.Context
import android.util.Base64
import androidx.annotation.VisibleForTesting
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal object LocalDatabaseEncryption {
    private const val DATABASE_NAME = "nexa-local.db"
    private const val PREFS_NAME = "nexa_local_database"
    private const val WRAPPED_KEY = "wrapped_database_key"
    private const val KEY_ALIAS = "nexa_local_database_wrapper"
    private const val WRAPPING_ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_SIZE_BYTES = 32
    private const val GCM_TAG_BITS = 128
    private const val TEMP_SUFFIX = ".encrypted.tmp"
    private const val BACKUP_SUFFIX = ".plaintext.bak"
    private const val AAD = "NEXA_LOCAL_DATABASE_KEY_V1"

    fun prepare(context: Context): ByteArray {
        System.loadLibrary("sqlcipher")

        val databaseFile = context.getDatabasePath(DATABASE_NAME)
        val key = LocalDatabaseKeyStore(context).getOrCreate()

        recoverInterruptedMigration(databaseFile, key)

        if (!databaseFile.exists()) {
            return sqlCipherPasswordBytes(key)
        }

        if (isPlaintextSQLite(databaseFile)) {
            migratePlaintextDatabase(databaseFile, key)
        } else {
            validateEncryptedDatabase(databaseFile, key)
        }

        return sqlCipherPasswordBytes(key)
    }

    @VisibleForTesting
    fun encryptPlaintextDatabaseForTest(databaseFile: File, key: ByteArray) {
        require(key.size >= 16) { "Database key must contain at least 128 bits." }
        System.loadLibrary("sqlcipher")
        require(databaseFile.exists()) { "Source database does not exist." }
        if (isPlaintextSQLite(databaseFile)) {
            migratePlaintextDatabase(databaseFile, key)
        } else {
            validateEncryptedDatabase(databaseFile, key)
        }
    }

    private fun migratePlaintextDatabase(databaseFile: File, key: ByteArray) {
        val tempFile = File(databaseFile.parentFile, databaseFile.name + TEMP_SUFFIX)
        val backupFile = File(databaseFile.parentFile, databaseFile.name + BACKUP_SUFFIX)

        if (tempFile.exists()) tempFile.delete()
        if (backupFile.exists()) backupFile.delete()

        databaseFile.parentFile?.mkdirs()

        var source: SQLiteDatabase? = null
        try {
            source = SQLiteDatabase.openDatabase(
                databaseFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY,
            )

            checkpointWal(source)

            val userVersion = source.rawQuery("PRAGMA user_version", null).use { cursor ->
                check(cursor.moveToFirst()) { "Source database user_version returned no result." }
                cursor.getInt(0)
            }

            val passphrase = sqlCipherPassphrase(key)
            source.execSQL(
                "ATTACH DATABASE ? AS encrypted KEY ?",
                arrayOf(tempFile.absolutePath, passphrase),
            )
            try {
                source.rawQuery("SELECT sqlcipher_export('encrypted')", null).use { cursor ->
                    check(cursor.moveToFirst()) { "SQLCipher export returned no result." }
                }
                source.execSQL("PRAGMA encrypted.user_version = $userVersion")
            } finally {
                source.execSQL("DETACH DATABASE encrypted")
            }
        } finally {
            source?.close()
        }

        check(tempFile.exists()) { "SQLCipher export did not create the encrypted database." }
        validateEncryptedDatabase(tempFile, key)

        deleteIfExists(File(databaseFile.absolutePath + "-wal"))
        deleteIfExists(File(databaseFile.absolutePath + "-shm"))

        check(databaseFile.renameTo(backupFile)) {
            "Could not move plaintext database to backup."
        }

        try {
            check(tempFile.renameTo(databaseFile)) {
                "Could not replace plaintext database with encrypted database."
            }

            validateEncryptedDatabase(databaseFile, key)
            check(backupFile.delete()) {
                "Encrypted database is valid, but plaintext backup could not be removed."
            }
        } catch (error: Exception) {
            deleteIfExists(databaseFile)
            backupFile.renameTo(databaseFile)
            deleteIfExists(tempFile)
            throw error
        }
    }

    private fun recoverInterruptedMigration(databaseFile: File, key: ByteArray) {
        val tempFile = File(databaseFile.parentFile, databaseFile.name + TEMP_SUFFIX)
        val backupFile = File(databaseFile.parentFile, databaseFile.name + BACKUP_SUFFIX)

        if (!databaseFile.exists() && backupFile.exists()) {
            check(backupFile.renameTo(databaseFile)) {
                "Could not restore plaintext database after interrupted migration."
            }
        }

        if (databaseFile.exists()) {
            if (!isPlaintextSQLite(databaseFile)) {
                validateEncryptedDatabase(databaseFile, key)
                deleteIfExists(tempFile)
                deleteIfExists(backupFile)
            } else {
                deleteIfExists(tempFile)
            }
        } else if (tempFile.exists()) {
            try {
                validateEncryptedDatabase(tempFile, key)
                check(tempFile.renameTo(databaseFile)) {
                    "Could not recover encrypted database after interrupted migration."
                }
            } catch (error: Exception) {
                deleteIfExists(tempFile)
                throw error
            }
        }
    }

    private fun validateEncryptedDatabase(databaseFile: File, key: ByteArray) {
        val database = SQLiteDatabase.openDatabase(
            databaseFile.absolutePath,
            sqlCipherPasswordBytes(key),
            null,
            SQLiteDatabase.OPEN_READWRITE,
            null,
            null,
        )
        try {
            val integrity = database.rawQuery("PRAGMA integrity_check", null)
            try {
                check(integrity.moveToFirst()) { "SQLite integrity check returned no result." }
                check(integrity.getString(0).equals("ok", ignoreCase = true)) {
                    "SQLite integrity check failed."
                }
            } finally {
                integrity.close()
            }
        } finally {
            database.close()
        }
    }

    private fun checkpointWal(database: SQLiteDatabase) {
        val result = database.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null)
        result.close()
    }

    private fun isPlaintextSQLite(file: File): Boolean {
        if (file.length() < SQLITE_HEADER.size) return false

        return file.inputStream().use { input ->
            val header = ByteArray(SQLITE_HEADER.size)
            val read = input.read(header)
            read == SQLITE_HEADER.size && header.contentEquals(SQLITE_HEADER)
        }
    }

    private fun sqlCipherPassphrase(key: ByteArray): String =
        Base64.encodeToString(key, Base64.NO_WRAP)

    private fun sqlCipherPasswordBytes(key: ByteArray): ByteArray =
        sqlCipherPassphrase(key).toByteArray(Charsets.UTF_8)

    private fun deleteIfExists(file: File) {
        if (file.exists()) {
            check(file.delete()) { "Could not delete " + file.absolutePath }
        }
    }

    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
}

internal class LocalDatabaseKeyStore(private val context: Context) {
    fun getOrCreate(): ByteArray {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferences.getString(WRAPPED_KEY, null)?.let { return decrypt(it) }

        val databaseKey = ByteArray(KEY_SIZE_BYTES)
        SecureRandom().nextBytes(databaseKey)
        preferences.edit().putString(WRAPPED_KEY, encrypt(databaseKey)).commit()
        return databaseKey
    }

    private fun encrypt(value: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey())
        cipher.updateAAD(AAD.toByteArray(Charsets.UTF_8))
        val ciphertext = cipher.doFinal(value)
        val payload = ByteBuffer.allocate(4 + cipher.iv.size + ciphertext.size)
            .putInt(cipher.iv.size)
            .put(cipher.iv)
            .put(ciphertext)
            .array()
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): ByteArray {
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        val buffer = ByteBuffer.wrap(payload)
        require(buffer.remaining() >= Int.SIZE_BYTES) { "Invalid local database key payload." }

        val ivSize = buffer.int
        require(ivSize in 12..32) { "Invalid local database key IV." }
        require(buffer.remaining() > ivSize) { "Invalid local database key payload." }

        val iv = ByteArray(ivSize).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(AAD.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(ciphertext)
    }

    private fun wrappingKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(WRAPPING_ALGORITHM, ANDROID_KEYSTORE)
        generator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
            )
                .setKeySize(256)
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val PREFS_NAME = "nexa_local_database"
        private const val WRAPPED_KEY = "wrapped_database_key"
        private const val KEY_ALIAS = "nexa_local_database_wrapper"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val WRAPPING_ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BYTES = 32
        private const val GCM_TAG_BITS = 128
        private const val AAD = "NEXA_LOCAL_DATABASE_KEY_V1"
    }
}
