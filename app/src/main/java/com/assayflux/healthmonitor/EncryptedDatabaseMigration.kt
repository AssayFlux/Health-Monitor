package com.assayflux.healthmonitor

import android.content.Context
import android.database.sqlite.SQLiteDatabase as FrameworkDb
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDb
import java.io.File

/** Recoverable one-time export from legacy plaintext SQLite to SQLCipher. */
object EncryptedDatabaseMigration {
    private const val NAME = "health_monitor.db"
    private const val TEMP = "health_monitor.encrypted.tmp"
    private const val BACKUP = "health_monitor.plaintext.migration-backup"
    // Keep aligned with every HealthDb table; these counts verify workflow records too.
    private val tables = listOf("members", "vitals", "labs", "medicines", "clinical_tests", "anthropometry", "appointments", "reminders")

    fun migrateIfNeeded(context: Context, passphrase: ByteArray) {
        System.loadLibrary("sqlcipher")
        val source = context.getDatabasePath(NAME)
        val parent = requireNotNull(source.parentFile) { "Database path has no parent directory." }
        check(parent.exists() || parent.mkdirs()) { "Could not create database directory: ${parent.absolutePath}" }
        val temp = File(parent, TEMP)
        val backup = File(parent, BACKUP)

        if (!source.exists() && backup.exists()) check(backup.renameTo(source))
        if (!source.exists()) {
            deleteDatabaseFiles(temp)
            return
        }
        if (readableEncrypted(source, passphrase)) {
            deleteDatabaseFiles(temp)
            deleteDatabaseFiles(backup)
            return
        }

        val counts = linkedMapOf<String, Long>()
        FrameworkDb.openDatabase(source.absolutePath, null, FrameworkDb.OPEN_READWRITE).use { legacy ->
            legacy.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { }
            tables.forEach { table ->
                val exists = legacy.rawQuery(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?",
                    arrayOf(table)
                ).use { it.moveToFirst() && it.getLong(0) == 1L }
                if (exists) {
                    counts[table] = legacy.rawQuery("SELECT COUNT(*) FROM $table", null).use {
                        check(it.moveToFirst()) { "Could not count rows in $table." }
                        it.getLong(0)
                    }
                }
            }
        }

        deleteDatabaseFiles(temp)
        try {
            // Explicitly create and key the destination before ATTACH. This avoids relying
            // on SQLite ATTACH to create a new file in the app's database directory.
            CipherDb.openOrCreateDatabase(temp, passphrase, null, null).use { destination ->
                destination.rawQuery("PRAGMA cipher_integrity_check", null).use { cursor ->
                    check(cursor.moveToFirst()) { "Could not initialize encrypted migration database." }
                }
            }
            check(temp.isFile && temp.length() > 0L) { "SQLCipher did not create the migration database." }

            val plain = CipherDb.openDatabase(
                source.absolutePath,
                byteArrayOf(),
                null,
                CipherDb.OPEN_READWRITE,
                null
            )
            try {
                val key = String(passphrase, Charsets.UTF_8)
                plain.execSQL(
                    "ATTACH DATABASE ? AS encrypted KEY ?",
                    arrayOf(temp.absolutePath, key)
                )
                plain.rawQuery("SELECT sqlcipher_export('encrypted')", null).use {
                    check(it.moveToFirst()) { "SQLCipher export did not return a result." }
                }
                plain.execSQL("DETACH DATABASE encrypted")
            } finally {
                plain.close()
            }

            validate(temp, passphrase, counts)
            if (backup.exists()) deleteDatabaseFiles(backup)
            check(source.renameTo(backup)) { "Could not preserve original database." }
            try {
                check(temp.renameTo(source)) { "Could not activate encrypted database." }
                validate(source, passphrase, counts)
                deleteDatabaseFiles(backup)
                listOf("-wal", "-shm", "-journal").forEach {
                    File(source.path + it).delete()
                    File(backup.path + it).delete()
                }
            } catch (e: Exception) {
                deleteDatabaseFiles(source)
                check(backup.renameTo(source)) { "Migration failed and original DB restore failed." }
                throw e
            }
        } catch (e: Exception) {
            deleteDatabaseFiles(temp)
            throw e
        } finally {
            deleteDatabaseFiles(temp)
        }
    }

    private fun deleteDatabaseFiles(file: File) {
        file.delete()
        listOf("-wal", "-shm", "-journal").forEach { File(file.path + it).delete() }
    }

    private fun readableEncrypted(file: File, key: ByteArray): Boolean =
        try {
            validate(file, key, emptyMap())
            true
        } catch (_: Exception) {
            false
        }

    private fun validate(file: File, key: ByteArray, expected: Map<String, Long>) {
        val db = CipherDb.openDatabase(
            file.absolutePath,
            key,
            null,
            CipherDb.OPEN_READWRITE,
            null
        )
        try {
            val ok = db.rawQuery("PRAGMA integrity_check", null).use {
                it.moveToFirst() && it.getString(0) == "ok"
            }
            check(ok) { "Encrypted database integrity check failed." }
            expected.forEach { (table, count) ->
                val actual = db.rawQuery("SELECT COUNT(*) FROM $table", null).use {
                    check(it.moveToFirst()) { "Could not verify rows in $table." }
                    it.getLong(0)
                }
                check(actual == count) { "Record verification failed for $table." }
            }
        } finally {
            db.close()
        }
    }
}
