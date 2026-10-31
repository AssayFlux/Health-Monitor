package com.assayflux.healthmonitor

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.zetetic.database.sqlcipher.SQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SqlCipherRuntimeTest {

    private val passphrase = "sqlcipher-runtime-test-key".toByteArray(Charsets.UTF_8)

    @Test
    fun nativeLibraryLoadsAndEncryptedDatabaseCanBeCreatedAndReopened() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as HealthMonitorApplication
        assertNull("SQLCipher native initialization failed: ${application.sqlCipherInitializationError}",
            application.sqlCipherInitializationError)

        val name = testDatabaseName()
        val databaseFile = context.getDatabasePath(name)
        try {
            val helper = HealthDb(context, passphrase.copyOf(), name)
            try {
                helper.writableDatabase
                ClinicalInteroperability.ensureSchema(helper)
                helper.addMember("SQLCipher test member", "", "", "", "")
            } finally {
                helper.close()
            }

            assertTrue("Encrypted database file was not created", databaseFile.isFile)
            val header = ByteArray(16)
            FileInputStream(databaseFile).use { input ->
                assertEquals("Database file is shorter than an SQLite page header", 16, input.read(header))
            }
            assertFalse(
                "Database appears to be plaintext SQLite instead of SQLCipher",
                header.contentEquals("SQLite format 3\u0000".toByteArray(Charsets.US_ASCII))
            )

            val reopened = HealthDb(context, passphrase.copyOf(), name)
            try {
                assertEquals(7, reopened.readableDatabase.version)
                assertEquals(1, reopened.members().size)
                assertEquals(1, reopened.count("clinical_model_meta"))
            } finally {
                reopened.close()
            }
        } finally {
            passphrase.fill(0)
            context.deleteDatabase(name)
        }
    }

    @Test
    fun databaseRejectsAnIncorrectPassphrase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = testDatabaseName()
        try {
            val created = HealthDb(context, passphrase.copyOf(), name)
            try {
                created.writableDatabase
                created.addMember("Protected test row", "", "", "", "")
            } finally {
                created.close()
            }

            val incorrect = HealthDb(
                context,
                "definitely-not-the-database-key".toByteArray(Charsets.UTF_8),
                name
            )
            try {
                incorrect.writableDatabase
                throw AssertionError("SQLCipher unexpectedly opened the database with an incorrect passphrase")
            } catch (expected: RuntimeException) {
                // An authentication/decryption failure is the expected result.
            } finally {
                incorrect.close()
            }
        } finally {
            passphrase.fill(0)
            context.deleteDatabase(name)
        }
    }

    @Test
    fun versionSixDatabaseMigratesToVersionSeven() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = testDatabaseName()
        val databaseFile = context.getDatabasePath(name)
        try {
            val versionSix = SQLiteDatabase.openOrCreateDatabase(
                databaseFile,
                passphrase.copyOf(),
                null,
                null,
                null
            )
            try {
                versionSix.version = 6
            } finally {
                versionSix.close()
            }

            val upgraded = HealthDb(context, passphrase.copyOf(), name)
            try {
                val database = upgraded.writableDatabase
                assertEquals(7, database.version)
                database.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='transfer_history'",
                    null
                ).use { cursor ->
                    assertTrue("Version 7 transfer_history migration did not run", cursor.moveToFirst())
                }
            } finally {
                upgraded.close()
            }
        } finally {
            passphrase.fill(0)
            context.deleteDatabase(name)
        }
    }

    private fun testDatabaseName(): String =
        "sqlcipher_test_${UUID.randomUUID().toString().replace("-", "")}.db"
}
