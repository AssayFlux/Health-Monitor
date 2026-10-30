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
        val temp = File(source.parentFile, TEMP)
        val backup = File(source.parentFile, BACKUP)
        source.parentFile?.mkdirs()
        if (!source.exists() && backup.exists()) check(backup.renameTo(source))
        if (!source.exists()) { temp.delete(); return }
        if (readableEncrypted(source, passphrase)) { temp.delete(); backup.delete(); return }
        val counts = linkedMapOf<String, Long>()
        FrameworkDb.openDatabase(source.absolutePath, null, FrameworkDb.OPEN_READWRITE).use { legacy ->
            legacy.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { }
            tables.forEach { table ->
                val exists = legacy.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { it.moveToFirst() && it.getLong(0) == 1L }
                if (exists) counts[table] = legacy.rawQuery("SELECT COUNT(*) FROM $table", null).use { it.moveToFirst(); it.getLong(0) }
            }
        }
        temp.delete()
        val plain = CipherDb.openDatabase(source.absolutePath, byteArrayOf(), null, CipherDb.OPEN_READWRITE, null)
        try {
            val key = String(passphrase, Charsets.UTF_8)
            plain.execSQL("ATTACH DATABASE ? AS encrypted KEY ?", arrayOf(temp.absolutePath, key))
            plain.rawQuery("SELECT sqlcipher_export('encrypted')", null).use { it.moveToFirst() }
            plain.execSQL("DETACH DATABASE encrypted")
        } finally { plain.close() }
        validate(temp, passphrase, counts)
        if (backup.exists()) check(backup.delete())
        check(source.renameTo(backup)) { "Could not preserve original database." }
        try {
            check(temp.renameTo(source)) { "Could not activate encrypted database." }
            validate(source, passphrase, counts)
            check(backup.delete()) { "Encrypted DB verified; plaintext cleanup requires retry." }
            listOf("-wal", "-shm", "-journal").forEach { File(source.path + it).delete(); File(backup.path + it).delete() }
        } catch (e: Exception) {
            source.delete()
            check(backup.renameTo(source)) { "Migration failed and original DB restore failed." }
            throw e
        } finally { temp.delete() }
    }
    private fun readableEncrypted(file: File, key: ByteArray): Boolean = try { validate(file, key, emptyMap()); true } catch (_: Exception) { false }
    private fun validate(file: File, key: ByteArray, expected: Map<String, Long>) {
        val db = CipherDb.openDatabase(file.absolutePath, key, null, CipherDb.OPEN_READWRITE, null)
        try {
            val ok = db.rawQuery("PRAGMA integrity_check", null).use { it.moveToFirst() && it.getString(0) == "ok" }
            check(ok) { "Encrypted database integrity check failed." }
            expected.forEach { (table, count) ->
                val actual = db.rawQuery("SELECT COUNT(*) FROM $table", null).use { it.moveToFirst(); it.getLong(0) }
                check(actual == count) { "Record verification failed for $table." }
            }
        } finally { db.close() }
    }
}