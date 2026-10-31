package com.assayflux.healthmonitor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun InteroperabilityScreen(db: HealthDb, refreshToken: Int, onChanged: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allowFhir = remember(refreshToken) { PrivacyAudit.granted(db, "fhir_exchange") }
    val allowOcr = remember(refreshToken) { PrivacyAudit.granted(db, "document_ocr_local") }
    val allowHealthConnect = remember(refreshToken) { PrivacyAudit.granted(db, "health_connect_read") }
    val members = remember(refreshToken) { db.members() }
    var selectedMember by remember { mutableStateOf(members.firstOrNull()?.get(0) ?: "") }
    var memberMenu by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("FHIR R4 · Normalized model v${ClinicalInteroperability.MODEL_VERSION}") }
    var pending by remember(refreshToken) { mutableStateOf(db.rows("SELECT staging_id,resource_type,display,COALESCE(status,''),COALESCE(code_system,''),COALESCE(code,''),COALESCE(effective_start,'') FROM interop_import_staging WHERE review_state='pending' ORDER BY created_at DESC")) }
    var ocrText by remember { mutableStateOf("") }
    var ocrUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var ocrName by remember { mutableStateOf("Clinical document") }
    var ocrMime by remember { mutableStateOf("image/*") }
    var ocrSize by remember { mutableLongStateOf(0L) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/fhir+json")) { uri ->
        if (uri != null) scope.launch {
            try {
                val member = selectedMember.toLong()
                val bundle = withContext(Dispatchers.IO) { ClinicalInteroperability.exportFhirR4(db, member) }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(bundle) }
                        ?: error("Unable to write the selected destination.")
                }
                message = "FHIR R4 Bundle exported. Review the receiving system's implementation guide before clinical use."
            } catch (e: Exception) { message = "FHIR export failed: ${e.message}" }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                val raw = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Unable to read the selected FHIR file.")
                }
                val staged = withContext(Dispatchers.IO) { ClinicalInteroperability.stageFhirBundle(db, raw) }
                pending = db.rows("SELECT staging_id,resource_type,display,COALESCE(status,''),COALESCE(code_system,''),COALESCE(code,''),COALESCE(effective_start,'') FROM interop_import_staging WHERE review_state='pending' ORDER BY created_at DESC")
                message = "${staged.size} supported resource(s) staged for review. Nothing was added to the health record yet."
            } catch (e: Exception) { message = "FHIR import rejected: ${e.message}" }
        }
    }
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            try {
                val info = ClinicalInteroperability.documentInfo(context, uri)
                ocrUri = uri; ocrName = info.first; ocrMime = info.second; ocrSize = info.third
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                ocrText = recognizer.process(image).await().text
                recognizer.close()
                message = "OCR complete. Review every extracted field against the original document before creating any clinical record."
            } catch (e: Exception) { message = "OCR could not read this image: ${e.message}" }
        }
    }
    val healthPermissionsLauncher = rememberLauncherForActivityResult(HealthConnectBridge.permissionContract()) { granted ->
        if (granted.containsAll(HealthConnectBridge.readPermissions)) {
            scope.launch {
                try {
                    val count = withContext(Dispatchers.IO) { HealthConnectBridge.syncRecent(db, context, selectedMember.toLong()) }
                    message = "Health Connect sync complete: ${count} new observation(s) from the last 30 days. Source provenance retained."
                    onChanged()
                } catch (e: Exception) { message = "Health Connect sync failed: ${e.message}" }
            }
        } else message = "Health Connect read permission was not fully granted. No records were synced."
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Clinical data exchange", color = Forest, style = MaterialTheme.typography.titleLarge)
            Text("FHIR R4 JSON · local normalized model · traceable source provenance", color = Ink.copy(.65f), fontSize = 12.sp)
            Box {
                OutlinedButton(onClick = { memberMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(members.firstOrNull { it[0] == selectedMember }?.get(1) ?: "Choose family member")
                }
                DropdownMenu(expanded = memberMenu, onDismissRequest = { memberMenu = false }) {
                    members.forEach { m -> DropdownMenuItem(text = { Text(m[1]) }, onClick = { selectedMember = m[0]; memberMenu = false }) }
                }
            }
            Button(onClick = { if (selectedMember.isNotBlank()) exportLauncher.launch("health-monitor-${selectedMember}-fhir-r4.json") },
                enabled = selectedMember.isNotBlank() && allowFhir, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Forest)) { Text("Export FHIR R4 Bundle") }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/fhir+json", "application/json")) }, enabled = allowFhir,
                modifier = Modifier.fillMaxWidth()) { Text("Import FHIR R4 Bundle for review") }
        }
        item {
            HorizontalDivider()
            Text("Health Connect", color = Forest, style = MaterialTheme.typography.titleMedium)
            Text("Read-only import of heart rate, steps and body weight for the last 30 days. Data is assigned to the selected family profile and labelled as device-imported.",
                color = Ink.copy(.7f), fontSize = 12.sp)
            OutlinedButton(onClick = {
                if (selectedMember.isBlank()) message = "Add a family member first."
                else healthPermissionsLauncher.launch(HealthConnectBridge.readPermissions)
            }, enabled = selectedMember.isNotBlank() && allowHealthConnect, modifier = Modifier.fillMaxWidth()) { Text("Connect / sync Health Connect") }
        }
        item {
            HorizontalDivider()
            Text("Document OCR", color = Forest, style = MaterialTheme.typography.titleMedium)
            Text("Choose a clear image of a report. OCR text is only a draft; it is not automatically converted into a diagnosis, result or medicine.",
                color = Ink.copy(.7f), fontSize = 12.sp)
            OutlinedButton(onClick = { imageLauncher.launch("image/*") }, enabled = allowOcr, modifier = Modifier.fillMaxWidth()) {
                Text("Select report image and extract text")
            }
            if (ocrText.isNotBlank()) {
                OutlinedTextField(ocrText, { ocrText = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Review and correct extracted text") }, minLines = 5)
                Button(onClick = {
                    val uri = ocrUri
                    if (uri != null && selectedMember.isNotBlank()) {
                        try {
                            ClinicalInteroperability.saveDocumentMetadata(db, selectedMember.toLong(), uri,
                                ocrMime, ocrText, ocrName, ocrSize)
                            message = "Document metadata saved as needs-review. No clinical values were auto-entered."
                            ocrText = ""; ocrUri = null; onChanged()
                        } catch (e: Exception) { message = "Could not save document metadata: ${e.message}" }
                    }
                }, enabled = selectedMember.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text("Save document for manual review")
                }
            }
        }
        item {
            HorizontalDivider()
            Text("Import review queue (${pending.size})", color = Forest, style = MaterialTheme.typography.titleMedium)
            Text("Imported resources remain pending until you explicitly associate them with a family member.",
                color = Ink.copy(.7f), fontSize = 12.sp)
        }
        items(pending, key = { it[0] }) { row ->
            Card(colors = CardDefaults.cardColors(containerColor = Cream)) {
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(row[2], color = Ink, style = MaterialTheme.typography.titleSmall)
                    Text("${row[1]} · ${row[3]} · ${row[6]}", color = Forest, fontSize = 11.sp)
                    if (row[4].isNotBlank() || row[5].isNotBlank()) Text("${row[4]} · ${row[5]}", color = Ink.copy(.6f), fontSize = 10.sp)
                    Button(onClick = {
                        if (selectedMember.isBlank()) message = "Choose a family member first."
                        else try {
                            ClinicalInteroperability.confirmImportedRecord(db, row[0], selectedMember.toLong())
                            pending = db.rows("SELECT staging_id,resource_type,display,COALESCE(status,''),COALESCE(code_system,''),COALESCE(code,''),COALESCE(effective_start,'') FROM interop_import_staging WHERE review_state='pending' ORDER BY created_at DESC")
                            message = "Resource confirmed and added to the selected family member."
                            onChanged()
                        } catch (e: Exception) { message = "Could not confirm resource: ${e.message}" }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Forest)) { Text("Confirm for selected member") }
                }
            }
        }
        item {
            Text(message, color = Forest, fontSize = 12.sp)
            Text("Enable FHIR exchange, Health Connect and OCR separately in the Privacy tab before using these features. Terminology note: LOINC, SNOMED CT, UCUM and ICD identifiers are preserved when supplied. The app does not guess cross-system equivalence or bundle licensed terminology content.",
                color = Ink.copy(.65f), fontSize = 11.sp)
        }
    }
}
