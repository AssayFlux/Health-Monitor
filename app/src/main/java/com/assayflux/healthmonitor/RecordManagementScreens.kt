package com.assayflux.healthmonitor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MemberManagementScreen(db: HealthDb, refreshToken: Int, onChanged: () -> Unit, onAdd: () -> Unit) {
    val members = remember(refreshToken) { db.members() }
    var editing by remember { mutableStateOf<List<String>?>(null) }
    var deleting by remember { mutableStateOf<List<String>?>(null) }
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Add family member") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(members, key = { it[0] }) { row ->
                Card(colors = CardDefaults.cardColors(containerColor = Cream)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(row[1], style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text(listOf(row[2], row[3], row[4]).filter { it.isNotBlank() }.joinToString(" · "),
                            color = Ink.copy(alpha = .65f))
                        Row {
                            TextButton(onClick = { editing = row }) { Text("Edit") }
                            TextButton(onClick = { deleting = row }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
    editing?.let { row ->
        var name by remember(row) { mutableStateOf(row[1]) }
        var sex by remember(row) { mutableStateOf(row[2]) }
        var blood by remember(row) { mutableStateOf(row[3]) }
        var dob by remember(row) { mutableStateOf(row[4]) }\n        var height by remember(row) { mutableStateOf(row.getOrElse(5) { "" }) }
        AlertDialog(onDismissRequest = { editing = null }, title = { Text("Edit family member") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") })
                OutlinedTextField(dob, { dob = it }, label = { Text("Date of birth") })
                OutlinedTextField(sex, { sex = it }, label = { Text("Sex") })
                OutlinedTextField(blood, { blood = it }, label = { Text("Blood group") })\n                OutlinedTextField(height, { height = it }, label = { Text("Height (cm)") })
            } },
            confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = {
                db.updateMember(row[0].toLong(), name.trim(), dob.trim(), sex.trim(), blood.trim(), height.trim())
                editing = null; onChanged()
            }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } })
    }
    deleting?.let { row ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete ${row[1]}?") },
            text = { Text("This permanently deletes this member and all health records, medicines, appointments and reminders linked to them.") },
            confirmButton = { TextButton(onClick = {
                db.deleteMember(row[0].toLong()); deleting = null; onChanged()
            }) { Text("Delete all") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } })
    }
}

@Composable
fun MedicineManagementScreen(db: HealthDb, refreshToken: Int, onChanged: () -> Unit, onAdd: () -> Unit) {
    val medicines = remember(refreshToken) { db.medicines() }
    var editing by remember { mutableStateOf<List<String>?>(null) }
    var deleting by remember { mutableStateOf<List<String>?>(null) }
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Add medicine") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(medicines, key = { it[0] }) { row ->
                Card(colors = CardDefaults.cardColors(containerColor = Cream)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(row[2], style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text("${row[1]} · ${row[3]} · Started ${row[4]}", color = Ink.copy(alpha = .65f))
                        Row {
                            TextButton(onClick = { editing = row }) { Text("Edit") }
                            TextButton(onClick = { deleting = row }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
    editing?.let { row ->
        var name by remember(row) { mutableStateOf(row[2]) }
        var dose by remember(row) { mutableStateOf(row[3]) }
        var start by remember(row) { mutableStateOf(row[4]) }
        var end by remember(row) { mutableStateOf("") }
        var notes by remember(row) { mutableStateOf("") }
        AlertDialog(onDismissRequest = { editing = null }, title = { Text("Edit medicine") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Medicine") })
                OutlinedTextField(dose, { dose = it }, label = { Text("Dose and schedule") })
                OutlinedTextField(start, { start = it }, label = { Text("Start date") })
                OutlinedTextField(end, { end = it }, label = { Text("End date") })
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") })
            } },
            confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = {
                db.updateMedicine(row[0].toLong(), name.trim(), dose.trim(), start.trim(), end.trim(), notes.trim())
                editing = null; onChanged()
            }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } })
    }
    deleting?.let { row ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete medicine?") },
            text = { Text("This removes the medicine record and any reminders linked to it.") },
            confirmButton = { TextButton(onClick = {
                db.deleteMedicine(row[0].toLong()); deleting = null; onChanged()
            }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } })
    }
}
