package com.assayflux.healthmonitor

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

private fun stableDeviceId(context: Context): String {
    val prefs = context.getSharedPreferences("device_identity_v1", Context.MODE_PRIVATE)
    prefs.getString("device_id", null)?.let { return it }
    val generated = "HM-" + UUID.randomUUID().toString().uppercase()
    check(prefs.edit().putString("device_id", generated).commit()) {
        "Could not persist this device's identifier."
    }
    return generated
}

@Composable
fun ReceivedSharedScreen(db: HealthDb, refreshToken: Int, onChanged: () -> Unit) {
    val context = LocalContext.current
    val deviceId = remember { stableDeviceId(context) }
    val history = remember(refreshToken) { db.transferHistory() }
    var selected by remember { mutableStateOf("All") }
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Forest)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("THIS DEVICE", color = Sage, fontSize = 11.sp)
                Text(deviceId, color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.titleMedium)
                Text("App-generated identifier. It is not an IMEI or an authentication secret.",
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = .78f), fontSize = 11.sp)
            }
        }
        Text("RECEIVED & SHARED", color = Forest, fontSize = 11.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("All", "Received", "Shared").forEach { value ->
                FilterChip(selected == value, onClick = { selected = value }, label = { Text(value) })
            }
        }
        Text("Transfer history is stored locally and is retained until you delete an entry.",
            color = Ink.copy(alpha = .65f), fontSize = 12.sp)
        val visible = history.filter { selected == "All" || it[1].equals(selected, true) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(visible, key = { it[0] }) { row ->
                Card(colors = CardDefaults.cardColors(containerColor = Cream)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${row[1]} · ${row[3].ifBlank { "Unnamed device" }}",
                            color = Ink, style = MaterialTheme.typography.titleMedium)
                        Text("Device ID: ${row[2]}", color = Forest, fontSize = 11.sp)
                        Text("${row[4]} · ${row[5]} · ${row[6]} records",
                            color = Ink.copy(alpha = .7f), fontSize = 12.sp)
                        if (row[7].isNotBlank()) Text(row[7], color = Ink.copy(alpha = .65f), fontSize = 11.sp)
                        TextButton(onClick = { pendingDelete = row[0] }) { Text("Delete history entry") }
                    }
                }
            }
            if (visible.isEmpty()) item {
                Text("No device transfers recorded yet.", color = Ink.copy(alpha = .65f))
            }
        }
    }
    pendingDelete?.let { id ->
        AlertDialog(onDismissRequest = { pendingDelete = null },
            title = { Text("Delete transfer history?") },
            text = { Text("This removes only the history entry. It does not delete health records from either device.") },
            confirmButton = { TextButton(onClick = {
                db.deleteTransfer(id); pendingDelete = null; onChanged()
            }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } })
    }
}
