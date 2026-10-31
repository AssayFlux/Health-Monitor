package com.assayflux.healthmonitor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HealthHistoryScreen(db: HealthDb, refreshToken: Int, onSaved: () -> Unit) {
    val members = remember(refreshToken) { db.members() }
    val records = remember(refreshToken) { db.healthHistory() }
    var showEditor by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Button(onClick = { showEditor = true }, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Forest)) {
            Text("Add medical history")
        }
        Spacer(Modifier.height(8.dp))
        Text("Conditions, allergies, procedures, immunisations, family history and encounters",
            color = Ink.copy(alpha = .65f), fontSize = 12.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(records) { row ->
                Card(colors = CardDefaults.cardColors(containerColor = Cream)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(row[3], color = Ink, style = MaterialTheme.typography.titleMedium)
                        Text("${row[1]} · ${row[2].replace('_', ' ').replaceFirstChar { it.uppercase() }}",
                            color = Forest, fontSize = 12.sp)
                        val details = listOf(row[4], row[5], row[6], row[7], row[8])
                            .filter { it.isNotBlank() }.joinToString(" · ")
                        if (details.isNotBlank()) Text(details, color = Ink.copy(alpha = .68f), fontSize = 12.sp)
                    }
                }
            }
            if (records.isEmpty()) item {
                Text("No medical history records yet. Add documented information for a family member.",
                    color = Ink.copy(alpha = .65f), modifier = Modifier.padding(12.dp))
            }
        }
    }
    if (showEditor) {
        HistoryEditor(members = members, onDismiss = { showEditor = false }, onSave = { values ->
            db.addHealthHistory(values.memberId, values.category, values.display, values.status,
                values.onset, values.resolved, values.severity, values.reaction, values.code, values.notes)
            showEditor = false
            onSaved()
        })
    }
}

private data class HistoryValues(
    val memberId: Long, val category: String, val display: String, val status: String,
    val onset: String, val resolved: String, val severity: String, val reaction: String,
    val code: String, val notes: String
)

@Composable
private fun HistoryEditor(
    members: List<List<String>>,
    onDismiss: () -> Unit,
    onSave: (HistoryValues) -> Unit
) {
    var memberId by remember { mutableStateOf(members.firstOrNull()?.get(0) ?: "") }
    var category by remember { mutableStateOf("condition") }
    var display by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("active") }
    var onset by remember { mutableStateOf("") }
    var resolved by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("") }
    var reaction by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val types = listOf("condition", "allergy", "procedure", "immunisation", "family_history", "encounter")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add medical history") },
        text = {
            Column(Modifier.heightIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (members.isEmpty()) Text("Add a family member first.")
                else {
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(members.firstOrNull { it[0] == memberId }?.get(1) ?: "Choose family member")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            members.forEach { row ->
                                DropdownMenuItem(text = { Text(row[1]) }, onClick = {
                                    memberId = row[0]; expanded = false
                                })
                            }
                        }
                    }
                }
                types.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        pair.forEach { type ->
                            FilterChip(selected = category == type, onClick = { category = type },
                                label = { Text(type.replace('_', ' ').replaceFirstChar { it.uppercase() }) })
                        }
                    }
                }
                OutlinedTextField(display, { display = it }, label = {
                    Text(when (category) {
                        "allergy" -> "Allergen"
                        "procedure" -> "Procedure"
                        "immunisation" -> "Vaccine"
                        "family_history" -> "Family condition"
                        "encounter" -> "Encounter title"
                        else -> "Condition / diagnosis"
                    })
                }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(status, { status = it }, label = { Text("Status") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(onset, { onset = it }, label = { Text("Start / occurrence date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(resolved, { resolved = it }, label = { Text("End / resolution date (optional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (category == "allergy") {
                    OutlinedTextField(reaction, { reaction = it }, label = { Text("Reaction") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(severity, { severity = it }, label = { Text("Severity") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                OutlinedTextField(code, { code = it }, label = { Text("Clinical code (optional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes / source details") },
                    modifier = Modifier.fillMaxWidth())
                Text("Record only information supported by the person's records or clinician. This app does not verify diagnoses.",
                    color = Ink.copy(alpha = .65f), fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(enabled = memberId.isNotBlank() && display.isNotBlank(), onClick = {
                onSave(HistoryValues(memberId.toLong(), category, display.trim(), status.trim(),
                    onset.trim(), resolved.trim(), severity.trim(), reaction.trim(), code.trim(), notes.trim()))
            }) { Text("Save record") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
