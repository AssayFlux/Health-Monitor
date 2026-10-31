package com.assayflux.healthmonitor

import android.content.ContentValues
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant

object PrivacyAudit {
    const val POLICY_VERSION = "2026-10-31-v1"

    fun record(db: HealthDb, event: String, subjectType: String? = null,
        subjectId: String? = null, outcome: String = "success", details: String? = null) {
        db.writableDatabase.insert("access_audit", null, ContentValues().apply {
            put("event_type", event); put("subject_type", subjectType); put("subject_id", subjectId)
            put("outcome", outcome); put("details", details?.take(240))
        })
    }

    fun consent(db: HealthDb, key: String, granted: Boolean) {
        db.writableDatabase.insertWithOnConflict("privacy_consents", null, ContentValues().apply {
            put("consent_key", key); put("granted", if (granted) 1 else 0)
            put("policy_version", POLICY_VERSION); put("decided_at", Instant.now().toString())
            put("source", "in_app")
        }, net.zetetic.database.sqlcipher.SQLiteDatabase.CONFLICT_REPLACE)
        record(db, "consent_changed", "consent", key, if (granted) "granted" else "withdrawn")
    }

    fun granted(db: HealthDb, key: String): Boolean =
        db.rows("SELECT granted FROM privacy_consents WHERE consent_key='$key'").firstOrNull()?.firstOrNull() == "1"

    fun events(db: HealthDb): List<List<String>> =
        db.rows("SELECT occurred_at,event_type,COALESCE(subject_type,''),COALESCE(subject_id,''),outcome,COALESCE(details,'') FROM access_audit ORDER BY audit_id DESC LIMIT 100")
}

@Composable
fun PrivacyControlsScreen(db: HealthDb, refreshToken: Int, onChanged: () -> Unit) {
    val context = LocalContext.current
    val biometric = remember { BiometricDatabaseKey(context) }
    var healthConnect by remember(refreshToken) { mutableStateOf(PrivacyAudit.granted(db, "health_connect_read")) }
    var documentOcr by remember(refreshToken) { mutableStateOf(PrivacyAudit.granted(db, "document_ocr_local")) }
    var fhirExchange by remember(refreshToken) { mutableStateOf(PrivacyAudit.granted(db, "fhir_exchange")) }
    var analytics by remember(refreshToken) { mutableStateOf(PrivacyAudit.granted(db, "local_analytics")) }
    var showDisable by remember { mutableStateOf(false) }
    val events = remember(refreshToken) { PrivacyAudit.events(db) }

    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("PRIVACY & SECURITY", color = Forest, fontSize = 11.sp)
            Text("Your data, your choices", color = Ink, style = MaterialTheme.typography.headlineSmall)
            Text("These controls record your choices locally in the encrypted database. Turning off a consent stops future use by the corresponding workflow; it does not erase records already saved.", color = Ink.copy(alpha = .7f), fontSize = 12.sp)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Cream)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Database protection", color = Forest, style = MaterialTheme.typography.titleMedium)
                    Text("SQLCipher encrypted local database · Android Keystore key wrapping · password authentication before database open", color = Ink, fontSize = 12.sp)
                    Text(if (biometric.isEnrolled()) "Strong biometric key is enrolled; password remains the recovery path." else "Biometric unlock is not enrolled. Enable it from the next unlock flow after password authentication.", color = Ink.copy(alpha = .7f), fontSize = 11.sp)
                    if (biometric.isEnrolled()) TextButton(onClick = { showDisable = true }) { Text("Disable biometric unlock") }
                    if (!biometric.isEnrolled()) BiometricEnrollmentButton(db, onChanged)
                }
            }
        }
        item { ConsentRow("Health Connect read access", "Allow importing supported measurements from Health Connect after Android permission is granted.", healthConnect) {
            healthConnect = it; PrivacyAudit.consent(db, "health_connect_read", it); onChanged()
        } }
        item { ConsentRow("Document OCR", "Allow on-device text recognition for documents you select. OCR output remains unverified until reviewed.", documentOcr) {
            documentOcr = it; PrivacyAudit.consent(db, "document_ocr_local", it); onChanged()
        } }
        item { ConsentRow("FHIR data exchange", "Allow manual FHIR import/export actions. Files are handled only when you explicitly select a source or destination.", fhirExchange) {
            fhirExchange = it; PrivacyAudit.consent(db, "fhir_exchange", it); onChanged()
        } }
        item { ConsentRow("Local analytics", "Allow the app to calculate summaries from records stored on this device. No analytics are sent to a server.", analytics) {
            analytics = it; PrivacyAudit.consent(db, "local_analytics", it); onChanged()
        } }
        item {
            Text("Privacy safeguards", color = Forest, style = MaterialTheme.typography.titleMedium)
            Text("• Screenshots and task-switcher previews are blocked by FLAG_SECURE\n• Android backup and device-transfer extraction are disabled\n• Notification content is generic and avoids diagnoses and medicine names\n• No cloud account or server sync is configured\n• Audit history records selected local security and consent events", color = Ink.copy(alpha = .75f), fontSize = 12.sp)
            Text("Encrypted backup and cloud sharing are unavailable. QR device transfer encrypts payloads with AES-256-GCM before sending them over the local network. The one-time encryption secret is exchanged in the scanned QR, not in the HTTP request. Use a trusted private Wi-Fi or hotspot; network-level metadata and traffic timing are still visible. Do not uninstall or clear app data as a recovery method.", color = Ink, fontSize = 12.sp)
        }
        item {
            Text("Recent access and consent events", color = Forest, style = MaterialTheme.typography.titleMedium)
            Text("Latest 100 events. This is an application-level audit trail, not a tamper-proof forensic log.", color = Ink.copy(alpha = .65f), fontSize = 11.sp)
        }
        items(events) { row ->
            Card(colors = CardDefaults.cardColors(containerColor = Cream)) {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    Text(row[1].replace('_', ' ').uppercase(), color = Forest, fontSize = 11.sp)
                    Text("${row[0]} · ${row[4]}", color = Ink, fontSize = 12.sp)
                    if (row[2].isNotBlank() || row[3].isNotBlank()) Text("${row[2]} · ${row[3]}", color = Ink.copy(alpha = .65f), fontSize = 11.sp)
                }
            }
        }
    }
    if (showDisable) AlertDialog(
        onDismissRequest = { showDisable = false },
        title = { Text("Disable biometric unlock?") },
        text = { Text("The biometric key envelope will be removed. Your local account password and database key envelope remain available for the next unlock.") },
        confirmButton = {
            TextButton(onClick = {
                try {
                    biometric.disable()
                    PrivacyAudit.record(db, "biometric_unlock_disabled")
                    showDisable = false
                    onChanged()
                } catch (e: Exception) { showDisable = false }
            }) { Text("Disable") }
        },
        dismissButton = { TextButton(onClick = { showDisable = false }) { Text("Cancel") } }
    )
}

@Composable
private fun ConsentRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Cream)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Ink, style = MaterialTheme.typography.titleSmall)
                Text(description, color = Ink.copy(alpha = .68f), fontSize = 11.sp)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}
