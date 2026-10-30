package com.assayflux.healthmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

private val Ivory = Color(0xFFF3EFE5)
private val Cream = Color(0xFFFAF8F2)
private val Forest = Color(0xFF34483E)
private val Sage = Color(0xFFA9B19F)
private val Terracotta = Color(0xFFA16F5D)
private val Ink = Color(0xFF302F29)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Python.isStarted()) Python.start(AndroidPlatform(this))
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary=Forest, background=Ivory, surface=Cream, onSurface=Ink)) { HealthMonitorApp() } }
    }
}

@Composable
private fun HealthMonitorApp() {
    var selected by remember { mutableStateOf("Home") }
    var entry by remember { mutableStateOf("") }
    Scaffold(containerColor=Ivory, bottomBar={
        NavigationBar(containerColor=Cream) {
            listOf("Home","Records","Trends","Medicines","More").forEach { item ->
                NavigationBarItem(selected==item, onClick={selected=item;entry=""}, icon={
                    Icon(when(item){"Home"->Icons.Default.Home;"Records"->Icons.Default.FolderOpen;"Trends"->Icons.Default.ShowChart;"Medicines"->Icons.Default.Medication;else->Icons.Default.MoreHoriz}, item)
                }, label={Text(item)})
            }
        }
    }) { padding ->
        if (entry.isNotBlank()) EntryScreen(entry, {entry=""}) else when(selected) {
            "Home" -> HomeScreen(padding, {entry=it})
            "Records" -> RecordsScreen(padding, {entry=it})
            else -> SimpleScreen(selected, padding)
        }
    }
}

@Composable
private fun HomeScreen(padding: PaddingValues, open:(String)->Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal=18.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Spacer(Modifier.height(8.dp)); Text("HEALTH MONITOR", color=Forest, fontSize=12.sp, fontWeight=FontWeight.Bold, letterSpacing=2.sp); Text("Good health starts\nhere.", fontSize=30.sp, fontWeight=FontWeight.SemiBold, color=Ink); Text("Your family's health, thoughtfully organized.", color=Ink.copy(alpha=.65f)) }
        item { Card(colors=CardDefaults.cardColors(containerColor=Forest), shape=RoundedCornerShape(22.dp)) { Column(Modifier.padding(20.dp)) { Text("FAMILY OVERVIEW", color=Sage, fontSize=11.sp, fontWeight=FontWeight.Bold); Text("Your health space", color=Color.White, fontSize=22.sp, fontWeight=FontWeight.SemiBold); Text("Add a family member to begin tracking records.", color=Color.White.copy(alpha=.8f)); Spacer(Modifier.height(14.dp)); OutlinedButton(onClick={open("Medical record")}, colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.White)) { Text("Add medical record") } } } }
        item { Text("QUICK ENTRY", color=Forest, fontSize=12.sp, fontWeight=FontWeight.Bold, letterSpacing=1.5.sp) }
        item { Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { QuickTile("Vitals", Icons.Default.MonitorHeart, Modifier.weight(1f)){open("Vitals")}; QuickTile("Lab results", Icons.Default.Science, Modifier.weight(1f)){open("Lab results")} } }
        item { Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { QuickTile("Medical", Icons.Default.MedicalServices, Modifier.weight(1f)){open("Medical record")}; QuickTile("Medicine", Icons.Default.Medication, Modifier.weight(1f)){open("Medicine")} } }
        item { Text("RECENT ACTIVITY", color=Forest, fontSize=12.sp, fontWeight=FontWeight.Bold, letterSpacing=1.5.sp) }
        item { Card(colors=CardDefaults.cardColors(containerColor=Cream), shape=RoundedCornerShape(18.dp)) { Column(Modifier.padding(18.dp)) { Text("No records yet", fontWeight=FontWeight.SemiBold, color=Ink); Text("Your saved vitals, visits and laboratory reports will appear here.", color=Ink.copy(alpha=.6f), fontSize=13.sp) } } }
        item { Text("Python calculation engine connected · Local-first Android interface", color=Forest.copy(alpha=.7f), fontSize=11.sp); Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun QuickTile(title:String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier:Modifier=Modifier, onClick:()->Unit) {
    Card(modifier.clickable(onClick=onClick), colors=CardDefaults.cardColors(containerColor=Cream), shape=RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) { Icon(icon, null, tint=Forest); Text(title, color=Ink, fontWeight=FontWeight.SemiBold) }
    }
}

@Composable
private fun RecordsScreen(padding:PaddingValues, open:(String)->Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(18.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Health records", fontSize=28.sp, fontWeight=FontWeight.SemiBold, color=Ink)
        listOf("Vitals","Medical record","Lab results","Diagnostic tests","Medicines").forEach { label ->
            Card(Modifier.fillMaxWidth().clickable{open(label)}, colors=CardDefaults.cardColors(containerColor=Cream), shape=RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment=Alignment.CenterVertically) { Text(label, Modifier.weight(1f), color=Ink, fontWeight=FontWeight.Medium); Icon(Icons.Default.ChevronRight, null, tint=Forest) }
            }
        }
    }
}

@Composable
private fun EntryScreen(type:String, close:()->Unit) {
    var systolic by remember { mutableStateOf("") }; var diastolic by remember { mutableStateOf("") }
    var pulse by remember { mutableStateOf("") }; var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }; var age by remember { mutableStateOf("30") }
    var output by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment=Alignment.CenterVertically) { IconButton(onClick=close){Icon(Icons.Default.ArrowBack,null)}; Text(type, fontSize=25.sp, fontWeight=FontWeight.SemiBold, color=Ink) } }
        if (type=="Vitals") {
            item { Field("Systolic (mmHg)",systolic){systolic=it} }
            item { Field("Diastolic (mmHg)",diastolic){diastolic=it} }
            item { Field("Pulse (bpm)",pulse){pulse=it} }
            item { Field("Weight (kg)",weight){weight=it} }
            item { Field("Height (cm)",height){height=it} }
            item { Field("Age (years)",age){age=it} }
            item { Button(onClick={
                try {
                    val py=Python.getInstance().getModule("health_engine")
                    val data=hashMapOf<String,Any>("systolic" to systolic,"diastolic" to diastolic,"pulse" to pulse,"weight_kg" to weight,"height_cm" to height,"age" to age)
                    output=py.callAttr("calculate_vitals",data).toString()
                } catch(e:Exception) { output="Calculation error: ${e.message}" }
            }, colors=ButtonDefaults.buttonColors(containerColor=Forest), modifier=Modifier.fillMaxWidth()){Text("Calculate with Python")} }
            if(output.isNotBlank()) item { Text(output, color=Forest) }
        } else {
            item { Text("This record form is being built as a native Kotlin screen. Existing Python calculation modules remain separate from the UI.", color=Ink.copy(alpha=.7f)) }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun Field(label:String, value:String, change:(String)->Unit) {
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), label={Text(label)}, singleLine=true)
}

@Composable
private fun SimpleScreen(title:String, padding:PaddingValues) {
    Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment=Alignment.Center) { Text(title, color=Forest, fontSize=26.sp, fontWeight=FontWeight.SemiBold) }
}
