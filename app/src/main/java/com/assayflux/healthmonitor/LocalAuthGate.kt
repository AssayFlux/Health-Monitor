package com.assayflux.healthmonitor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

/**
 * Local account gate. A single app-owner credential protects the family workspace;
 * family members remain profiles inside that workspace. No email, phone, OTP or server.
 */
@Composable
fun LocalAuthGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val credentials = remember { LocalCredentialStore(context) }
    val policy = remember { LocalAuthPolicy(context) }
    var authenticated by remember { mutableStateOf(false) }
    var createMode by remember { mutableStateOf(!credentials.hasAccount()) }
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    if (authenticated) {
        content()
    } else {
        Surface(Modifier.fillMaxSize(), color=Ivory) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement=Arrangement.Center
            ) {
                Text("HEALTH MONITOR", color=Forest, fontSize=11.sp, fontWeight=FontWeight.Bold, letterSpacing=1.5.sp)
                Spacer(Modifier.height(12.dp))
                Text(if(createMode) "Create your local account" else "Welcome back", color=Ink, fontSize=26.sp, fontWeight=FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("Your family health workspace stays on this device. No online account is used.", color=Ink.copy(alpha=.7f), fontSize=13.sp)
                Spacer(Modifier.height(22.dp))
                OutlinedTextField(userId,{userId=it},label={Text("User ID")},modifier=Modifier.fillMaxWidth(),singleLine=true)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(password,{password=it},label={Text("Password")},modifier=Modifier.fillMaxWidth(),singleLine=true,visualTransformation=PasswordVisualTransformation())
                if(createMode) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(confirm,{confirm=it},label={Text("Confirm password")},modifier=Modifier.fillMaxWidth(),singleLine=true,visualTransformation=PasswordVisualTransformation())
                    Spacer(Modifier.height(8.dp))
                    Text("Use 4–32 characters for the ID and at least 12 characters for the password. Keep the password safe; there is no cloud password reset.", color=Ink.copy(alpha=.65f), fontSize=12.sp)
                }
                if(error.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(error,color=Color(0xFF9B342E),fontSize=12.sp)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        try {
                            if(createMode) {
                                require(password == confirm) { "Passwords do not match." }
                                credentials.create(userId.trim(),password.toCharArray())
                                policy.recordPasswordResult(true)
                                authenticated=true
                            } else {
                                val attempt=policy.beforePasswordAttempt()
                                require(attempt.allowed) { "Too many attempts. Try again in ${attempt.waitSeconds} seconds." }
                                val ok=credentials.verify(userId.trim(),password.toCharArray())
                                policy.recordPasswordResult(ok)
                                require(ok) { "Incorrect user ID or password." }
                                authenticated=true
                            }
                            error=""
                        } catch(e:Exception) {
                            error=e.message ?: "Unable to authenticate."
                        } finally {
                            password=""
                            confirm=""
                        }
                    },
                    modifier=Modifier.fillMaxWidth(),
                    colors=ButtonDefaults.buttonColors(containerColor=Forest)
                ) { Text(if(createMode) "Create account" else "Unlock Health Monitor") }
                if(credentials.hasAccount()) {
                    TextButton(onClick={createMode=!createMode;error=""},modifier=Modifier.fillMaxWidth()) {
                        Text(if(createMode) "Back to login" else "Create a different local account is unavailable on this installation")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Security note: local password verification is active. Biometric unlock and encrypted-database key gating are not enabled until the database migration is completed.", color=Ink.copy(alpha=.55f), fontSize=11.sp)
            }
        }
    }
}
