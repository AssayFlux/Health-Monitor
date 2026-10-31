package com.assayflux.healthmonitor

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
fun BiometricEnrollmentButton(db: HealthDb, onChanged: () -> Unit) {
    val context = LocalContext.current
    val biometric = BiometricDatabaseKey(context)
    TextButton(onClick = {
        val activity = context as? FragmentActivity
        if (activity == null) {
            PrivacyAudit.record(db, "biometric_enrollment_failed", outcome = "no_activity")
        } else try {
            val cipher = biometric.enrollmentCipher()
            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        var passphrase: ByteArray? = null
                        try {
                            val authenticated = result.cryptoObject?.cipher ?: error("Authenticated cipher missing")
                            val keyMaterial = LocalKeyVault().getOrCreateDatabasePassphrase(context)
                            passphrase = keyMaterial
                            biometric.persistEnrollment(authenticated, keyMaterial)
                            PrivacyAudit.record(db, "biometric_enrollment_enabled")
                            onChanged()
                        } catch (e: Exception) {
                            PrivacyAudit.record(db, "biometric_enrollment_failed", outcome = "error", details = e.message)
                        } finally { passphrase?.fill(0) }
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        PrivacyAudit.record(db, "biometric_enrollment_failed", outcome = "cancelled")
                    }
                })
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder().setTitle("Enable biometric unlock")
                    .setSubtitle("Protect the SQLCipher database key with Android Keystore")
                    .setNegativeButtonText("Cancel")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG).build(),
                BiometricPrompt.CryptoObject(cipher))
        } catch (e: Exception) {
            PrivacyAudit.record(db, "biometric_enrollment_failed", outcome = "error", details = e.message)
        }
    }) { Text("Enable biometric unlock") }
}