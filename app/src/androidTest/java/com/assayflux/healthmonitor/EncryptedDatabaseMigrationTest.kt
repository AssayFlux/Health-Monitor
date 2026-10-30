package com.assayflux.healthmonitor

import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase as FrameworkDb
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.SecureRandom

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseMigrationTest {
    @Test fun migratesLegacyRowsAndRemovesPlaintextCopy() {
        val base = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val isolated = object : ContextWrapper(base) {
            override fun getDatabasePath(name: String): File = File(cacheDir, "migration-test-$name")
        }
        val source = isolated.getDatabasePath("health_monitor.db")
        source.parentFile?.mkdirs()
        source.delete()
        FrameworkDb.openOrCreateDatabase(source, null).use { legacy ->
            legacy.execSQL("CREATE TABLE members(id INTEGER PRIMARY KEY,name TEXT NOT NULL)")
            legacy.execSQL("INSERT INTO members(id,name) VALUES(1,'Test Member')")
            legacy.execSQL("CREATE TABLE vitals(id INTEGER PRIMARY KEY,member_id INTEGER)")
            legacy.execSQL("INSERT INTO vitals(id,member_id) VALUES(1,1)")
        }
        val key = "test-migration-passphrase-32-chars".toByteArray(Charsets.UTF_8)
        EncryptedDatabaseMigration.migrateIfNeeded(isolated, key)
        val plaintextReadable = try {
            FrameworkDb.openDatabase(source.absolutePath, null, FrameworkDb.OPEN_READONLY).use { db ->
                db.rawQuery("SELECT name FROM members", null).use { it.moveToFirst() && it.getString(0) == "Test Member" }
            }
        } catch (_: Exception) { false }
        assertFalse("The encrypted database must not be readable as plaintext", plaintextReadable)
        CipherDb.openDatabase(source.absolutePath, key, null, CipherDb.OPEN_READONLY, null).use { encrypted ->
            val count = encrypted.rawQuery("SELECT COUNT(*) FROM members", null).use { it.moveToFirst(); it.getInt(0) }
            assertEquals(1, count)
            val integrity = encrypted.rawQuery("PRAGMA integrity_check", null).use { it.moveToFirst(); it.getString(0) }
            assertEquals("ok", integrity)
        }
        source.delete()
    }
}
