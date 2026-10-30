package com.assayflux.healthmonitor

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Wraps a random SQLCipher passphrase with a non-exportable Android Keystore AES key.
 * The returned passphrase is held only in memory by the caller and must be zeroed
 * after the database has opened. This class deliberately does not silently recreate
 * a missing wrapping key: doing so would make existing records unrecoverable.
 */
class LocalKeyVault {
    companion object {
        private const val STORE = "AndroidKeyStore"
        private const val ALIAS = "health_monitor_database_key_wrap_v1"
        private const val PREFS = "database_key_envelope_v1"
        private const val KEY = "wrapped_passphrase"
    }

    fun hasEnvelope(context: android.content.Context): Boolean =
        context.getSharedPreferences(PREFS, 0).contains(KEY)

    @Synchronized
    fun getOrCreateDatabasePassphrase(context: android.content.Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS, 0)
        val wrapped = prefs.getString(KEY, null)
        if (wrapped != null) return unwrap(wrapped)
        check(!prefs.contains("key_initialization_started")) {
            "Database key setup is incomplete. Restore from an authenticated backup."
        }
        check(prefs.edit().putBoolean("key_initialization_started", true).commit()) {
            "Could not persist database key setup state."
        }
        val passphrase = ByteArray(32).also(SecureRandom()::nextBytes)
        try {
            val envelope = wrap(passphrase)
            check(prefs.edit().putString(KEY, envelope).commit()) {
                "Could not save the protected database key."
            }
            return passphrase
        } catch (failure: Exception) {
            passphrase.fill(0)
            throw failure
        }
    }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance(STORE).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, STORE)
        generator.init(KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(false)
            .build())
        return generator.generateKey()
    }

    private fun wrap(clear: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(clear)
        val envelope = byteArrayOf(1) + cipher.iv + encrypted
        return Base64.encodeToString(envelope, Base64.NO_WRAP)
    }

    private fun unwrap(encoded: String): ByteArray {
        val envelope = Base64.decode(encoded, Base64.NO_WRAP)
        require(envelope.size > 1 + 12 + 16 && envelope[0].toInt() == 1) {
            "Unsupported database key envelope."
        }
        val iv = envelope.copyOfRange(1, 13)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(envelope.copyOfRange(13, envelope.size))
    }
}
