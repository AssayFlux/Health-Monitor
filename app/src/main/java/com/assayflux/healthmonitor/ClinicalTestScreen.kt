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
fun ClinicalTestScreen(db: HealthDb, refreshToken: Int, onChanged: () -> Unit) {
    val members = remember(refreshToken) { db.members() }
    val tests = remember(refreshToken) { db.clinicalTests() }
    var editor by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = { editor = true }, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Forest)) { Text("Add clinical test result") }
        Text("Structured test observations, numeric values, units, reference intervals and qualitative findings.",
            color = Ink.copy(alpha = .65f), fontSize = 12.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tests) { row ->
                Card(colors = CardDefaults.cardColors(containerColor = Cream)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(row[3], color = Ink, style = MaterialTheme.typography.titleMedium)
                        Text("${row[1]} · ${row[2]}", color = Forest, fontSize = 12.sp)
                        Text(listOf(row[4], row[5], row[6]).filter { it.isNotBlank() }.joinToString(" · "),
                            color = Ink.copy(alpha = .72f), fontSize = 13.sp)
                        Text(row[7], color = Ink.copy(alpha = .5f), fontSize = 10.sp)
                    }
                }
            }
            if (tests.isEmpty()) item { Text("No clinical tests entered yet.", color = Ink.copy(alpha = .65f)) }
        }
    }
    if (editor) ClinicalTestEditor(members, onDismiss = { editor = false }, onSave = { v ->
        db.addClinicalTest(v.memberId, v.category, v.parameter, v.text, v.numeric, v.unit,
            v.low, v.high, v.status, v.notes)
        editor = false
        onChanged()
    })
}

private data class ClinicalTestValues(
    val memberId: Long, val category: String, val parameter: String, val text: String,
    val numeric: String, val unit: String, val low: String, val high: String,
    val status: String, val notes: String
)

@Composable
private fun ClinicalTestEditor(members: List<List<String>>, onDismiss: () -> Unit, onSave: (ClinicalTestValues) -> Unit) {
    var member by remember { mutableStateOf(members.firstOrNull()?.get(0) ?: "") }
    var category by remember { mutableStateOf("laboratory") }
    var parameter by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var numeric by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var low by remember { mutableStateOf("") }
    var high by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("not_determined") }
    var notes by remember { mutableStateOf("") }
    var qualitative by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Clinical test result") },
        text = {
            Column(Modifier.heightIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (members.isEmpty()) Text("Add a family member first.")
                else MemberPicker(members, member) { member = it }
                OutlinedTextField(category, { category = it }, label = { Text("Category / panel") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(parameter, { parameter = it }, label = { Text("Test / analyte name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(qualitative, { qualitative = it })
                    Text("Qualitative result")
                }
                if (qualitative) {
                    OutlinedTextField(text, { text = it }, label = { Text("Result / finding") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(status, { status = it }, label = { Text("Status") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                } else {
                    OutlinedTextField(numeric, { numeric = it }, label = { Text("Numeric value") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(unit, { unit = it }, label = { Text("Unit") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(low, { low = it }, label = { Text("Reference low (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(high, { high = it }, label = { Text("Reference high (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes / source") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = member.isNotBlank() && parameter.isNotBlank() &&
                (if (qualitative) text.isNotBlank() else numeric.toDoubleOrNull() != null),
                onClick = { onSave(ClinicalTestValues(member.toLong(), category.trim(), parameter.trim(),
                    text.trim(), if (qualitative) "" else numeric, unit.trim(), low, high,
                    if (qualitative) status.trim() else "numeric", notes.trim())) }) { Text("Save result") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
