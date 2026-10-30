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
import java.util.concurrent.TimeUnit

@Composable
fun ReminderManagementScreen() {
    val context=LocalContext.current
    var reminderId by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("medicine") }
    var interval by remember { mutableStateOf("8") }
    var unit by remember { mutableStateOf("hours") }
    var repeat by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        message=if(granted) "Notifications enabled." else "Notifications are disabled. Enable them in Android Settings to receive reminders."
    }
    val kinds=listOf("medicine","appointment","test","vitals","follow_up","general")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("REMINDER MANAGEMENT",color=Forest,fontSize=11.sp)
        Text("Stay on schedule",color=Ink,fontSize=24.sp)
        Text("Reminders run locally using Android WorkManager. Notification wording stays generic on the lock screen.",color=Ink.copy(alpha=.7f),fontSize=13.sp)
        OutlinedTextField(reminderId,{reminderId=it},label={Text("Reminder ID (unique)")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        Text("Reminder type",color=Ink,fontSize=13.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            kinds.take(3).forEach { FilterChip(selected=kind==it,onClick={kind=it},label={Text(it.replace('_',' '))}) }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            kinds.drop(3).forEach { FilterChip(selected=kind==it,onClick={kind=it},label={Text(it.replace('_',' '))}) }
        }
        OutlinedTextField(interval,{interval=it.filter(Char::isDigit)},label={Text("Repeat interval")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("minutes","hours","days").forEach { FilterChip(selected=unit==it,onClick={unit=it},label={Text(it)}) }
        }
        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(repeat,{repeat=it})
            Text("Repeat automatically",color=Ink)
        }
        Button(onClick={
            try {
                val amount=interval.toLong()
                val timeUnit=when(unit){"minutes"->TimeUnit.MINUTES;"hours"->TimeUnit.HOURS;else->TimeUnit.DAYS}
                if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                if(repeat) ReminderScheduler.scheduleRecurring(context,reminderId,kind,amount,timeUnit)
                else ReminderScheduler.schedule(context,reminderId,kind,amount,timeUnit)
                message=if(repeat) "Repeating reminder scheduled." else "One-time reminder scheduled."
            } catch(e:Exception) { message=e.message ?: "Unable to schedule reminder." }
        },modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Forest)) { Text("Save reminder") }
        OutlinedButton(onClick={if(reminderId.isNotBlank()){ReminderScheduler.cancel(context,reminderId);message="Reminder cancelled."}},modifier=Modifier.fillMaxWidth()) { Text("Cancel reminder by ID") }
        if(message.isNotBlank()) Text(message,color=Forest,fontSize=12.sp)
        Text("Scheduling note: Android may defer background work to protect battery life. Periodic WorkManager intervals are approximate, not exact medication administration alarms. For critical medication timing, use clinician/pharmacist instructions and verify device notification/battery settings.",color=Ink.copy(alpha=.65f),fontSize=11.sp)
    }
}
