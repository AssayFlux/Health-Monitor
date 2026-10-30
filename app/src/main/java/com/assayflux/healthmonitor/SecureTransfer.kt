package com.assayflux.healthmonitor

import android.content.Context
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Offline backup cryptography primitives.
 *
 * The export code is independent from the local login password. The caller must
 * keep it separate from the .hmbackup file. Never log it or place it in the archive.
 */
object SecureTransfer {
    private val random = SecureRandom()
    private val magic = byteArrayOf(0x48, 0x4d, 0x42, 0x4b) // HMBK
    private const val FORMAT_VERSION: Byte = 1
    private const val ITERATIONS = 310_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val MAX_ARCHIVE_BYTES = 100 * 1024 * 1024

    data class ExportCode(val value: String)

    /** Generates a 160-bit, human-readable code. Display once and share separately. */
    fun generateImportCode(): ExportCode {
        val bytes = ByteArray(20).also(random::nextBytes)
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val out = StringBuilder()
        var buffer = 0
        var bits = 0
        for (b in bytes) {
            buffer = (buffer shl 8) or (b.toInt() and 0xff)
            bits += 8
            while (bits >= 5) {
                bits -= 5
                out.append(alphabet[(buffer shr bits) and 31])
            }
        }
        if (bits > 0) out.append(alphabet[(buffer shl (5 - bits)) and 31])
        return ExportCode(out.toString().chunked(5).joinToString("-"))
    }

    /** Creates a ZIP containing only caller-selected entries, then encrypts the ZIP. */
    fun createEncryptedArchive(entries: Map<String, ByteArray>, importCode: String): ByteArray {
        require(entries.isNotEmpty()) { "Select at least one record category." }
        require(entries.keys.none { it.startsWith("/") || it.contains("..") || it.contains('\\') }) {
            "Invalid archive entry name."
        }
        val zipBytes = ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                val manifest = JSONObject()
                    .put("format", "HealthMonitorBackup")
                    .put("version", FORMAT_VERSION.toInt())
                    .put("entry_count", entries.size)
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                entries.toSortedMap().forEach { (name, bytes) ->
                    require(name.isNotBlank() && name != "manifest.json")
                    zip.putNextEntry(ZipEntry("records/$name"))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
            output.toByteArray()
        }
        require(zipBytes.size <= MAX_ARCHIVE_BYTES) { "Backup is too large." }
        return encrypt(zipBytes, importCode)
    }

    /** Authenticates and decrypts the archive; rejects invalid tags and unsafe ZIP entries. */
    fun openEncryptedArchive(encrypted: ByteArray, importCode: String): Map<String, ByteArray> {
        val plainZip = decrypt(encrypted, importCode)
        require(plainZip.size <= MAX_ARCHIVE_BYTES) { "Backup exceeds the supported size." }
        val result = linkedMapOf<String, ByteArray>()
        var total = 0
        ZipInputStream(ByteArrayInputStream(plainZip)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && !entry.name.startsWith("/") &&
                    !entry.name.contains("..") && !entry.name.contains('\\')) {
                    "Unsafe backup entry."
                }
                val bytes = ByteArrayOutputStream().use { out ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_ARCHIVE_BYTES) { "Backup expands beyond the size limit." }
                        out.write(buffer, 0, count)
                    }
                    out.toByteArray()
                }
                require(result.put(entry.name, bytes) == null) { "Duplicate backup entry." }
                zip.closeEntry()
            }
        }
        val manifestBytes = result.remove("manifest.json")
            ?: throw IllegalArgumentException("Not a Health Monitor backup.")
        val manifest = JSONObject(String(manifestBytes, Charsets.UTF_8))
        require(manifest.optString("format") == "HealthMonitorBackup" &&
            manifest.optInt("version") == FORMAT_VERSION.toInt()) {
            "Unsupported backup format."
        }
        require(manifest.optInt("entry_count") == result.size) { "Backup manifest does not match its contents." }
        require(result.keys.all { it.startsWith("records/") }) { "Unexpected backup content." }
        return result
    }

    private fun encrypt(plain: ByteArray, code: String): ByteArray {
        require(code.replace("-", "").length >= 20) { "Invalid import code." }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val key = deriveKey(code, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))
        cipher.updateAAD(magic + byteArrayOf(FORMAT_VERSION))
        val ciphertext = cipher.doFinal(plain)
        return magic + byteArrayOf(FORMAT_VERSION) + salt + nonce + ciphertext
    }

    private fun decrypt(data: ByteArray, code: String): ByteArray {
        val headerSize = magic.size + 1 + SALT_BYTES + NONCE_BYTES
        require(data.size > headerSize && data.copyOfRange(0, magic.size).contentEquals(magic)) {
            "This is not a Health Monitor encrypted backup."
        }
        require(data[magic.size] == FORMAT_VERSION) { "Unsupported encrypted backup version." }
        val saltStart = magic.size + 1
        val salt = data.copyOfRange(saltStart, saltStart + SALT_BYTES)
        val nonce = data.copyOfRange(saltStart + SALT_BYTES, headerSize)
        val ciphertext = data.copyOfRange(headerSize, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(code, salt), GCMParameterSpec(128, nonce))
        cipher.updateAAD(magic + byteArrayOf(FORMAT_VERSION))
        return try {
            cipher.doFinal(ciphertext)
        } catch (_: AEADBadTagException) {
            throw IllegalArgumentException("Incorrect import code or damaged backup.")
        }
    }

    private fun deriveKey(code: String, salt: ByteArray): SecretKeySpec {
        val normalized = code.replace("-", "").uppercase()
        val spec = PBEKeySpec(normalized.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}

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
