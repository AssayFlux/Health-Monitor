package com.assayflux.healthmonitor

import android.content.Context
import java.security.MessageDigest

/**
 * Local-only authentication policy helpers. The existing LocalCredentialStore
 * remains the verifier store; this class adds persistent throttling and session policy.
 */
class LocalAuthPolicy(context: Context) {
    private val prefs = context.getSharedPreferences("local_auth_policy_v1", Context.MODE_PRIVATE)

    data class Attempt(val allowed: Boolean, val waitSeconds: Long)

    @Synchronized
    fun beforePasswordAttempt(nowMillis: Long = System.currentTimeMillis()): Attempt {
        val blockedUntil = prefs.getLong("blocked_until", 0L)
        if (nowMillis < blockedUntil) return Attempt(false, (blockedUntil - nowMillis + 999) / 1000)
        return Attempt(true, 0)
    }

    @Synchronized
    fun recordPasswordResult(success: Boolean, nowMillis: Long = System.currentTimeMillis()) {
        if (success) {
            prefs.edit().remove("failures").remove("blocked_until").apply()
            return
        }
        val failures = (prefs.getInt("failures", 0) + 1).coerceAtMost(20)
        // Progressive delay: 5th failure = 30s, 6th = 60s, capped at 15 minutes.
        val delay = if (failures < 5) 0L else
            (30_000L * (1L shl (failures - 5).coerceAtMost(5))).coerceAtMost(900_000L)
        prefs.edit().putInt("failures", failures)
            .putLong("blocked_until", if (delay == 0L) 0L else nowMillis + delay).apply()
    }

    fun isSessionExpired(lastAuthenticatedMillis: Long, nowMillis: Long = System.currentTimeMillis(),
                         timeoutMillis: Long = 2 * 60 * 1000L): Boolean =
        lastAuthenticatedMillis <= 0L || nowMillis - lastAuthenticatedMillis >= timeoutMillis

    /** Constant-time comparison for account identifiers after normalization. */
    fun sameUser(expected: String, supplied: String): Boolean =
        MessageDigest.isEqual(expected.trim().lowercase().toByteArray(),
            supplied.trim().lowercase().toByteArray())
}
