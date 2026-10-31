package com.assayflux.healthmonitor

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Local credential primitives. Password verifiers are salted PBKDF2 hashes;
 * they are not encryption keys and cannot be used as a substitute for database
 * encryption. UI, lockout policy and Android Keystore-backed biometric unlock
 * must be integrated by the application layer.
 */
class LocalCredentialStore(context: Context) {
    private val prefs = context.getSharedPreferences("local_auth_v1", Context.MODE_PRIVATE)
    private val random = SecureRandom()
    private val iterations = 310_000

    @Synchronized
    fun create(userId: String, password: CharArray) {
        require(userId.matches(Regex("[A-Za-z0-9._-]{4,32}"))) { "User ID must be 4–32 characters." }
        require(password.size >= 12) { "Use a password with at least 12 characters." }
        require(prefs.getString("user_id", null) == null) { "A local account already exists on this device." }
        val salt = ByteArray(16).also(random::nextBytes)
        val hash = hash(password, salt)
        val ok = prefs.edit()
            .putString("user_id", userId)
            .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("verifier", Base64.encodeToString(hash, Base64.NO_WRAP))
            .commit()
        require(ok) { "Could not save local account." }
    }

    fun verify(userId: String, password: CharArray): Boolean {
        if (prefs.getString("user_id", null) != userId) return false
        val salt = prefs.getString("salt", null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val expected = prefs.getString("verifier", null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        return MessageDigest.isEqual(expected, hash(password, salt))
    }

    fun hasAccount(): Boolean = prefs.getString("user_id", null) != null

    private fun hash(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
