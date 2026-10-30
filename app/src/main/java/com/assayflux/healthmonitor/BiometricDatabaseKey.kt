package com.assayflux.healthmonitor

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Optional biometric-only envelope for the already-random SQLCipher passphrase.
 *
 * Call enrollmentCipher(), pass the returned Cipher in BiometricPrompt.CryptoObject,
 * and call persistEnrollment() only from onAuthenticationSucceeded. For unlock,
 * pass unlockCipher() in CryptoObject and call decryptPassphrase() only with the
 * cipher returned by the successful prompt. The key is per-use authenticated;
 * it is not usable by a background process or by an ordinary password-only path.
 *
 * The password-based LocalKeyVault envelope remains the recovery path if the
 * device has no enrolled strong biometric or the biometric key is invalidated.
 */
class BiometricDatabaseKey(private val context: Context) {
    companion object {
        private const val STORE = "AndroidKeyStore"
        private const val ALIAS = "health_monitor_biometric_db_key_v1"
        private const val PREFS = "database_key_envelope_v1"
        private const val ENVELOPE = "biometric_wrapped_passphrase_v1"
        private const val AAD = "HealthMonitor/SQLCipher/biometric-envelope/v1"
    }

    fun isEnrolled(): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(ENVELOPE) &&
            loadKey() != null

    @Synchronized
    fun enrollmentCipher(): Cipher {
        require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { "Android Keystore authentication is unavailable." }
        val key = loadKey() ?: generateKey()
        return Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
    }

    @Synchronized
    fun unlockCipher(): Cipher {
        val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(ENVELOPE, null) ?: throw IllegalStateException("Biometric unlock has not been enabled.")
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        require(bytes.size > 1 + 12 + 16 && bytes[0].toInt() == 1) { "Biometric key envelope is invalid." }
        val key = loadKey() ?: throw IllegalStateException("Biometric key is unavailable. Unlock with the account password.")
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
            updateAAD(AAD.toByteArray(Charsets.UTF_8))
        }
    }

    /** Must be called only after the enrollment CryptoObject succeeds. */
    fun persistEnrollment(authenticatedCipher: Cipher, passphrase: ByteArray) {
        authenticatedCipher.updateAAD(AAD.toByteArray(Charsets.UTF_8))
        val ciphertext = authenticatedCipher.doFinal(passphrase)
        val envelope = byteArrayOf(1) + authenticatedCipher.iv + ciphertext
        check(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(ENVELOPE, Base64.encodeToString(envelope, Base64.NO_WRAP)).commit()) {
            "Could not save biometric database-key envelope."
        }
    }

    /** Must be called only with the Cipher returned by a successful BiometricPrompt. */
    fun decryptPassphrase(authenticatedCipher: Cipher): ByteArray {
        val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(ENVELOPE, null) ?: throw IllegalStateException("Biometric unlock is not enabled.")
        val envelope = Base64.decode(encoded, Base64.NO_WRAP)
        require(envelope.size > 29 && envelope[0].toInt() == 1) { "Biometric key envelope is invalid." }
        return authenticatedCipher.doFinal(envelope.copyOfRange(13, envelope.size))
    }

    @Synchronized
    fun disable() {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(ENVELOPE).commit()
        val store = KeyStore.getInstance(STORE).apply { load(null) }
        if (store.containsAlias(ALIAS)) store.deleteEntry(ALIAS)
    }

    private fun loadKey(): SecretKey? {
        val store = KeyStore.getInstance(STORE).apply { load(null) }
        return store.getKey(ALIAS, null) as? SecretKey
    }

    private fun generateKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, STORE)
        val builder = KeyGenParameterSpec.Builder(
            ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationValidityDurationSeconds(-1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0, KeyProperties.AUTH_BIOMETRIC_STRONG
            )
        }
        return generator.apply { init(builder.build()) }.generateKey()
    }
}

