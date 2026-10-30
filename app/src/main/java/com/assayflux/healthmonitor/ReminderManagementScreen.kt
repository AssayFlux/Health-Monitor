package com.assayflux.healthmonitor

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit

@Composable
fun ReminderManagementScreen(db: HealthDb, onSaved: () -> Unit) {
    val context = LocalContext.current
    val medicines = remember { db.medicinesForReminders() }
    val members = remember { db.members() }
    var selectedMedicine by remember { mutableStateOf(medicines.firstOrNull()?.get(0) ?: "") }
    var selectedAppointmentMember by remember { mutableStateOf(members.firstOrNull()?.get(0) ?: "") }
    var selectedAppointment by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(if (medicines.isNotEmpty()) "medicine" else "appointment") }
    var title by remember { mutableStateOf("") }
    var scheduledAt by remember { mutableStateOf(LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))) }
    var location by remember { mutableStateOf("") }
    var delayMinutes by remember { mutableStateOf("60") }
    var repeat by remember { mutableStateOf(false) }
    var repeatMinutes by remember { mutableStateOf("480") }
    var message by remember { mutableStateOf("") }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        message = if (granted) "Notification permission granted." else "Permission denied. Enable notifications in Android Settings."
    }
    val appointments = remember { db.appointments() }
    val savedReminders = remember(message) { db.reminderRows() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("REMINDER MANAGEMENT", color = Forest, fontSize = 11.sp)
        Text("Link reminders to real records", color = Ink, fontSize = 24.sp)
        Text("Reminders are stored in the encrypted local database and reference a medicine or appointment. Lock-screen text remains generic.", color = Ink.copy(alpha=.7f), fontSize=13.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterChip(selected=mode=="medicine",onClick={mode="medicine"},label={Text("Medicine")},enabled=medicines.isNotEmpty())
            FilterChip(selected=mode=="appointment",onClick={mode="appointment"},label={Text("Appointment")})
        }
        if (mode == "medicine") {
            if (medicines.isEmpty()) Text("Add a medicine in Reports before creating a medicine reminder.", color=Ink.copy(alpha=.7f))
            else {
                Text("Select medicine", color=Forest)
                medicines.forEach { row ->
                    FilterChip(selected=selectedMedicine==row[0],onClick={selectedMedicine=row[0]},label={Text("${row[2]} · ${row[1]}")})
                }
            }
        } else {
            Text("Create an appointment record", color=Forest)
            if (members.isEmpty()) Text("Add a family member first.", color=Ink.copy(alpha=.7f))
            else members.forEach { m -> FilterChip(selected=selectedAppointmentMember==m[0],onClick={selectedAppointmentMember=m[0]},label={Text(m[1])}) }
            OutlinedTextField(title,{title=it},label={Text("Appointment / follow-up title")},modifier=Modifier.fillMaxWidth(),singleLine=true)
            OutlinedTextField(scheduledAt,{scheduledAt=it},label={Text("Date and time (YYYY-MM-DD HH:MM)")},modifier=Modifier.fillMaxWidth(),singleLine=true)
            OutlinedTextField(location,{location=it},label={Text("Location (optional)")},modifier=Modifier.fillMaxWidth(),singleLine=true)
            Button(onClick={
                try {
                    LocalDateTime.parse(scheduledAt,DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    require(title.isNotBlank() && selectedAppointmentMember.isNotBlank()) { "Choose a member and enter an appointment title." }
                    val id=db.addAppointment(selectedAppointmentMember.toLong(),title.trim(),scheduledAt,location,"")
                    selectedAppointment=id.toString()
                    message="Appointment saved. Now schedule its reminder."
                    onSaved()
                } catch(e:Exception) { message=e.message ?: "Invalid appointment date or time." }
            }, modifier=Modifier.fillMaxWidth(), colors=ButtonDefaults.buttonColors(containerColor=Forest)) { Text("Save appointment") }
            if (appointments.isNotEmpty()) {
                Text("Existing appointments", color=Forest)
                appointments.forEach { a ->
                    FilterChip(selected=selectedAppointment==a[0],onClick={selectedAppointment=a[0];selectedAppointmentMember=a[5]},label={Text("${a[2]} · ${a[1]} · ${a[3]}")})
                }
            }
        }
        Divider()
        Text("Reminder timing", color=Forest)
        OutlinedTextField(delayMinutes,{delayMinutes=it.filter(Char::isDigit)},label={Text("First reminder in minutes")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(repeat,{repeat=it})
            Text("Repeat reminder (approximate WorkManager timing)",color=Ink)
        }
        if (repeat) OutlinedTextField(repeatMinutes,{repeatMinutes=it.filter(Char::isDigit)},label={Text("Repeat interval in minutes (minimum 15)")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        Button(onClick={
            try {
                val id=UUID.randomUUID().toString()
                val first=delayMinutes.toLong()
                require(first>=1) { "First reminder must be at least one minute from now." }
                val interval=if(repeat) repeatMinutes.toLong() else first
                require(!repeat || interval>=15) { "Periodic WorkManager reminders require at least 15 minutes." }
                val medicine=if(mode=="medicine") medicines.firstOrNull{it[0]==selectedMedicine} else null
                val appointment=if(mode=="appointment") appointments.firstOrNull{it[0]==selectedAppointment} else null
                require(medicine!=null || appointment!=null) { "Select a saved medicine or appointment record first." }
                val memberId=(medicine?.get(4) ?: appointment?.get(5))?.toLong()
                val medicineId=medicine?.get(0)?.toLong()
                val appointmentId=appointment?.get(0)?.toLong()
                val label=medicine?.get(2) ?: appointment?.get(2) ?: "Health reminder"
                val kind=if(medicine!=null) "medicine" else "appointment"
                val due=LocalDateTime.now().plusMinutes(first).toString()
                db.saveLinkedReminder(id,memberId,medicineId,appointmentId,label,kind,interval,repeat,due,ZoneId.systemDefault().id)
                if(repeat) ReminderScheduler.scheduleRecurring(context,id,kind,interval,TimeUnit.MINUTES)
                else ReminderScheduler.schedule(context,id,kind,first,TimeUnit.MINUTES)
                if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                message="Reminder saved and scheduled for the selected record."
                onSaved()
            } catch(e:Exception) { message=e.message ?: "Unable to save reminder." }
        },modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Forest)) { Text("Save linked reminder") }
        if(message.isNotBlank()) Text(message,color=Forest,fontSize=12.sp)
        Text("Android may defer background work. WorkManager periodic reminders are approximate and are not exact dose-time alarms. Verify notification permission, battery settings and the next due time on the device.",color=Ink.copy(alpha=.65f),fontSize=11.sp)
        Divider()
        Text("Saved reminders",color=Forest,fontSize=16.sp)
        if(savedReminders.isEmpty()) Text("No active reminders.",color=Ink.copy(alpha=.65f))
        savedReminders.forEach { r ->
            Card(colors=CardDefaults.cardColors(containerColor=Cream)) {
                Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(r[1],color=Ink)
                        Text("${r[2]} · every ${r[3]} min · due ${r[5]}",color=Ink.copy(alpha=.65f),fontSize=11.sp)
                    }
                    TextButton(onClick={ReminderScheduler.cancel(context,r[0]);message="Reminder cancelled.";onSaved()}) { Text("Cancel") }
                }
            }
        }
    }
}
