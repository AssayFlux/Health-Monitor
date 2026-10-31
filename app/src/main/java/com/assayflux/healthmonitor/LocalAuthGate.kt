package com.assayflux.healthmonitor

import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import android.widget.Toast
import javax.crypto.Cipher

/**
 * The database key is released only after local password or strong-biometric
 * authentication. The caller owns the returned byte array and must zero it.
 */
@Composable
fun LocalAuthGate(content: @Composable (ByteArray) -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val credentials = remember { LocalCredentialStore(context) }
    val policy = remember { LocalAuthPolicy(context) }
    val vault = remember { LocalKeyVault() }
    val biometric = remember { BiometricDatabaseKey(context) }
    var unlockedKey by remember { mutableStateOf<ByteArray?>(null) }
    var createMode by remember { mutableStateOf(!credentials.hasAccount()) }
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var biometricBusy by remember { mutableStateOf(false) }

    fun acceptKey(key: ByteArray) {
        unlockedKey?.fill(0)
        unlockedKey = key
        error = ""
        password = ""
        confirm = ""
    }

    fun passwordUnlock() {
        try {
            if (createMode) {
                require(password == confirm) { "Passwords do not match." }
                credentials.create(userId.trim(), password.toCharArray())
                policy.recordPasswordResult(true)
                acceptKey(vault.getOrCreateDatabasePassphrase(context))
            } else {
                val attempt = policy.beforePasswordAttempt()
                require(attempt.allowed) { "Too many attempts. Try again in ${attempt.waitSeconds} seconds." }
                val ok = credentials.verify(userId.trim(), password.toCharArray())
                policy.recordPasswordResult(ok)
                require(ok) { "Incorrect user ID or password." }
                acceptKey(vault.getOrCreateDatabasePassphrase(context))
            }
        } catch (e: Exception) {
            error = e.message ?: "Unable to authenticate."
        } finally {
            password = ""
            confirm = ""
        }
    }

    fun showBiometricPrompt() {
        if (activity == null) { error = "Biometric unlock requires an Android activity."; return }
        try {
            val cipher = biometric.unlockCipher()
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    biometricBusy = false
                    try {
                        val authenticatedCipher = result.cryptoObject?.cipher
                            ?: throw IllegalStateException("Biometric authentication did not return a secure cipher.")
                        acceptKey(biometric.decryptPassphrase(authenticatedCipher))
                    } catch (e: Exception) {
                        error = "Biometric key could not be used. Unlock with your account password. ${e.message}"
                    }
                }
                override fun onAuthenticationError(code: Int, message: CharSequence) {
                    biometricBusy = false
                    error = "Biometric unlock cancelled or unavailable. Use your account password."
                }
                override fun onAuthenticationFailed() {
                    error = "Biometric not recognised. Try again or use your password."
                }
            })
            biometricBusy = true
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Health Monitor")
                    .setSubtitle("Authenticate to release the encrypted health database key")
                    .setNegativeButtonText("Use password")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build(),
                BiometricPrompt.CryptoObject(cipher)
            )
        } catch (e: Exception) {
            biometricBusy = false
            error = "Biometric unlock is unavailable. Use your account password. ${e.message}"
        }
    }

    fun enrollBiometrics() {
        if (activity == null) { error = "Biometric enrollment requires an Android activity."; return }
        val key = unlockedKey ?: return
        try {
            val cipher = biometric.enrollmentCipher()
            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        try {
                            val authenticated = result.cryptoObject?.cipher
                                ?: throw IllegalStateException("Secure biometric cipher unavailable.")
                            biometric.persistEnrollment(authenticated, key)
                            Toast.makeText(context, "Biometric database unlock enabled.", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) { error = "Could not enable biometric unlock: ${e.message}" }
                    }
                    override fun onAuthenticationError(code: Int, message: CharSequence) {
                        error = "Biometric enrollment cancelled: $message"
                    }
                })
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Enable biometric unlock")
                    .setSubtitle("Wrap the SQLCipher key with an authentication-required Keystore key")
                    .setNegativeButtonText("Cancel")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build(),
                BiometricPrompt.CryptoObject(cipher)
            )
        } catch (e: Exception) { error = "Biometric enrollment unavailable: ${e.message}" }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                unlockedKey?.fill(0)
                unlockedKey = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            unlockedKey?.fill(0)
        }
    }

    val key = unlockedKey
    if (key != null) {
        content(key)
    } else {
        Surface(Modifier.fillMaxSize(), color = Ivory) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text("HEALTH MONITOR", color = Forest, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(12.dp))
                Text(if (createMode) "Create your local account" else "Welcome back", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("Your family health workspace stays on this device. Authentication is required before the encrypted database is opened.", color = Ink.copy(alpha = .7f), fontSize = 13.sp)
                Spacer(Modifier.height(22.dp))
                OutlinedTextField(userId, { userId = it }, label = { Text("User ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(password, { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
                if (createMode) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(confirm, { confirm = it }, label = { Text("Confirm password") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
                    Spacer(Modifier.height(8.dp))
                    Text("Use 4–32 characters for the ID and at least 12 characters for the password. Keep the password safe; there is no cloud password reset.", color = Ink.copy(alpha = .65f), fontSize = 12.sp)
                }
                if (error.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = Color(0xFF9B342E), fontSize = 12.sp)
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { passwordUnlock() }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Forest)) {
                    Text(if (createMode) "Create account and unlock" else "Unlock with password")
                }
                if (!createMode && biometric.isEnrolled()) {
                    OutlinedButton(onClick = { showBiometricPrompt() }, enabled = !biometricBusy, modifier = Modifier.fillMaxWidth()) {
                        Text(if (biometricBusy) "Waiting for biometric…" else "Unlock with biometrics")
                    }
                }
                if (credentials.hasAccount()) {
                    TextButton(onClick = { createMode = false; error = "" }, modifier = Modifier.fillMaxWidth()) {
                        Text("Back to password login")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("The password is the recovery path. If biometric enrollment changes or the biometric key is invalidated, use the local account password. Never clear app data to troubleshoot a key error.", color = Ink.copy(alpha = .55f), fontSize = 11.sp)
            }
        }
    }
}
