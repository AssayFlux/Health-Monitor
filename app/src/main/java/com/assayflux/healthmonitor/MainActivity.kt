package com.assayflux.healthmonitor

import android.content.ContentValues
import android.content.Context
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteOpenHelper
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.DatePickerDialog
import java.util.Calendar
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONObject

private data class VintagePalette(val id:String,val name:String,val ivory:Color,val cream:Color,val forest:Color,val sage:Color,val clay:Color,val ink:Color)
private val vintagePalettes=listOf(
 VintagePalette("botanical","Antique Botanical",Color(0xFFF5F0E4),Color(0xFFE7DDC8),Color(0xFF35483A),Color(0xFF66745B),Color(0xFFB18A55),Color(0xFF302F29)),
 VintagePalette("apothecary","French Apothecary",Color(0xFFF4EBDD),Color(0xFFDED0B9),Color(0xFF783F45),Color(0xFF778795),Color(0xFFA58B68),Color(0xFF302F29)),
 VintagePalette("terracotta","Heritage Olive & Terracotta",Color(0xFFF3EBDD),Color(0xFFDCCBB1),Color(0xFF493D32),Color(0xFF73734D),Color(0xFFA65F48),Color(0xFF302F29)),
 VintagePalette("rose","Victorian Rose & Parchment",Color(0xFFF6EFE6),Color(0xFFE5D7CA),Color(0xFF714B50),Color(0xFFB98783),Color(0xFF9A805C),Color(0xFF302F29))
)
private var activeVintagePalette by mutableStateOf(vintagePalettes.first())
val Ivory get()=activeVintagePalette.ivory
val Cream get()=activeVintagePalette.cream
val Forest get()=activeVintagePalette.forest
val Sage get()=activeVintagePalette.sage
val Clay get()=activeVintagePalette.clay
val Ink get()=activeVintagePalette.ink

class HealthDb(c:Context, passphrase:ByteArray):SQLiteOpenHelper(c,"health_monitor.db",passphrase,null,7,0,null,null,false){
 override fun onCreate(d:SQLiteDatabase){
  d.execSQL("CREATE TABLE members(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,dob TEXT,sex TEXT,blood TEXT,height REAL,notes TEXT)")
  d.execSQL("CREATE TABLE vitals(id INTEGER PRIMARY KEY AUTOINCREMENT,member_id INTEGER NOT NULL,recorded_at TEXT DEFAULT CURRENT_TIMESTAMP,measurement_date TEXT,time_of_day TEXT,systolic REAL,diastolic REAL,left_systolic REAL,left_diastolic REAL,right_systolic REAL,right_diastolic REAL,pulse REAL,weight REAL,height REAL,spo2 REAL,temp REAL,notes TEXT)")
  d.execSQL("CREATE TABLE labs(id INTEGER PRIMARY KEY AUTOINCREMENT,member_id INTEGER NOT NULL,recorded_at TEXT DEFAULT CURRENT_TIMESTAMP,name TEXT,values_json TEXT NOT NULL)")
  d.execSQL("CREATE TABLE medicines(id INTEGER PRIMARY KEY AUTOINCREMENT,member_id INTEGER NOT NULL,name TEXT NOT NULL,dose TEXT,start_date TEXT,end_date TEXT,notes TEXT)")
  d.execSQL("CREATE TABLE clinical_tests(id INTEGER PRIMARY KEY AUTOINCREMENT,member_id INTEGER NOT NULL,recorded_at TEXT DEFAULT CURRENT_TIMESTAMP,category TEXT,parameter TEXT,value_text TEXT,value_numeric REAL,unit TEXT,ref_low REAL,ref_high REAL,status TEXT,notes TEXT)")
  d.execSQL("CREATE TABLE anthropometry(id INTEGER PRIMARY KEY AUTOINCREMENT,member_id INTEGER NOT NULL,measurement_date TEXT NOT NULL,recorded_at TEXT DEFAULT CURRENT_TIMESTAMP,values_json TEXT NOT NULL)")
  d.execSQL("CREATE TABLE appointments(id INTEGER PRIMARY KEY AUTOINCREMENT,member_id INTEGER NOT NULL,title TEXT NOT NULL,scheduled_at TEXT NOT NULL,location TEXT,notes TEXT,created_at TEXT DEFAULT CURRENT_TIMESTAMP)")
   d.execSQL("CREATE TABLE reminders(id TEXT PRIMARY KEY,member_id INTEGER,medicine_id INTEGER,appointment_id INTEGER,label TEXT NOT NULL,kind TEXT NOT NULL,interval_minutes INTEGER NOT NULL,is_repeating INTEGER NOT NULL,active INTEGER NOT NULL DEFAULT 1,due_at TEXT,timezone_id TEXT,created_at TEXT DEFAULT CURRENT_TIMESTAMP)")
  d.execSQL("CREATE TABLE clinical_history(id INTEGER PRIMARY KEY AUTOINCREMENT,member_id INTEGER NOT NULL,category TEXT NOT NULL,display TEXT NOT NULL,clinical_code TEXT,status TEXT,onset_date TEXT,resolved_date TEXT,severity TEXT,reaction TEXT,notes TEXT,recorded_at TEXT DEFAULT CURRENT_TIMESTAMP)")
 }
 override fun onUpgrade(d:SQLiteDatabase,o:Int,n:Int){if(o<2){listOf("measurement_date TEXT","time_of_day TEXT","left_systolic REAL","left_diastolic REAL","right_systolic REAL","right_diastolic REAL").forEach{d.execSQL("ALTER TABLE vitals ADD COLUMN $it")};d.execSQL("UPDATE vitals SET measurement_date=date(recorded_at),time_of_day='unknown' WHERE measurement_date IS NULL")};if(o<3){d.execSQL("CREATE TABLE anthropometry(id INTEGER PRIMARY KEY AUTOINCREMENT,member_id INTEGER NOT NULL,measurement_date TEXT NOT NULL,recorded_at TEXT DEFAULT CURRENT_TIMESTAMP,values_json TEXT NOT NULL)")};if(o<4){d.execSQL("CREATE TABLE reminders(id TEXT PRIMARY KEY,member_id INTEGER,label TEXT NOT NULL,kind TEXT NOT NULL,interval_minutes INTEGER NOT NULL,is_repeating INTEGER NOT NULL,active INTEGER NOT NULL DEFAULT 1,created_at TEXT DEFAULT CURRENT_TIMESTAMP)")};if(o<5){d.execSQL("CREATE TABLE appointments(id INTEGER PRIMARY KEY AUTOINCREMENT,member_id INTEGER NOT NULL,title TEXT NOT NULL,scheduled_at TEXT NOT NULL,location TEXT,notes TEXT,created_at TEXT DEFAULT CURRENT_TIMESTAMP)");d.execSQL("ALTER TABLE reminders ADD COLUMN medicine_id INTEGER");d.execSQL("ALTER TABLE reminders ADD COLUMN appointment_id INTEGER");d.execSQL("ALTER TABLE reminders ADD COLUMN due_at TEXT");d.execSQL("ALTER TABLE reminders ADD COLUMN timezone_id TEXT")};if(o<6){d.execSQL("CREATE TABLE clinical_history(id INTEGER PRIMARY KEY AUTOINCREMENT,member_id INTEGER NOT NULL,category TEXT NOT NULL,display TEXT NOT NULL,clinical_code TEXT,status TEXT,onset_date TEXT,resolved_date TEXT,severity TEXT,reaction TEXT,notes TEXT,recorded_at TEXT DEFAULT CURRENT_TIMESTAMP)")};if(o<7){d.execSQL("CREATE TABLE IF NOT EXISTS transfer_history(id TEXT PRIMARY KEY,direction TEXT NOT NULL,device_id TEXT NOT NULL,device_name TEXT,occurred_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,status TEXT NOT NULL,record_count INTEGER NOT NULL DEFAULT 0,details TEXT)")}}

 fun updateMember(id:Long,name:String,dob:String,sex:String,blood:String,height:String){writableDatabase.update("members",ContentValues().apply{put("name",name);put("dob",dob);put("sex",sex);put("blood",blood);put("height",height.toDoubleOrNull())},"id=?",arrayOf(id.toString()))}
 fun deleteMember(id:Long){val d=writableDatabase;d.beginTransaction();try{listOf("vitals","labs","medicines","clinical_tests","anthropometry","appointments","reminders","clinical_history","clinical_records","clinical_documents","interop_import_staging").forEach{table->try{d.delete(table,"member_id=?",arrayOf(id.toString()))}catch(_:Exception){}};d.delete("members","id=?",arrayOf(id.toString()));d.setTransactionSuccessful()}finally{d.endTransaction()}}
 fun updateMedicine(id:Long,name:String,dose:String,start:String,end:String,notes:String){writableDatabase.update("medicines",ContentValues().apply{put("name",name);put("dose",dose);put("start_date",start);put("end_date",end);put("notes",notes)},"id=?",arrayOf(id.toString()))}
 fun deleteMedicine(id:Long){writableDatabase.delete("reminders","medicine_id=?",arrayOf(id.toString()));writableDatabase.delete("medicines","id=?",arrayOf(id.toString()))}
 fun addClinicalTest(mid:Long,category:String,parameter:String,valueText:String,valueNumeric:String,unit:String,low:String,high:String,status:String,notes:String){require(parameter.isNotBlank());writableDatabase.insert("clinical_tests",null,ContentValues().apply{put("member_id",mid);put("category",category);put("parameter",parameter);put("value_text",valueText);put("value_numeric",valueNumeric.toDoubleOrNull());put("unit",unit);put("ref_low",low.toDoubleOrNull());put("ref_high",high.toDoubleOrNull());put("status",status);put("notes",notes)})}
 fun clinicalTests()=rows("SELECT t.id,m.name,COALESCE(t.category,''),t.parameter,COALESCE(t.value_numeric,t.value_text,''),COALESCE(t.unit,''),COALESCE(t.status,''),COALESCE(t.recorded_at,'') FROM clinical_tests t JOIN members m ON m.id=t.member_id ORDER BY t.id DESC")
 private fun ensureTransferHistory(){
  writableDatabase.execSQL("CREATE TABLE IF NOT EXISTS transfer_history(id TEXT PRIMARY KEY,direction TEXT NOT NULL,device_id TEXT NOT NULL,device_name TEXT,occurred_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,status TEXT NOT NULL,record_count INTEGER NOT NULL DEFAULT 0,details TEXT)")
  writableDatabase.execSQL("CREATE TABLE IF NOT EXISTS transfer_pending(transfer_id TEXT PRIMARY KEY,payload_json TEXT NOT NULL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)")
 }
 fun savePendingTransfer(id:String,payload:String){ensureTransferHistory();writableDatabase.insertWithOnConflict("transfer_pending",null,ContentValues().apply{put("transfer_id",id);put("payload_json",payload)},SQLiteDatabase.CONFLICT_REPLACE)}
 fun pendingTransfer():Pair<String,String>?{ensureTransferHistory();return readableDatabase.rawQuery("SELECT transfer_id,payload_json FROM transfer_pending ORDER BY created_at LIMIT 1",null).use{c->if(c.moveToFirst())c.getString(0) to c.getString(1) else null}}
 fun discardPendingTransfer(id:String){ensureTransferHistory();writableDatabase.delete("transfer_pending","transfer_id=?",arrayOf(id))}
 fun completeTransferImport(id:String,details:String){writableDatabase.update("transfer_history",ContentValues().apply{put("status","Integrated");put("details",details)},"id=?",arrayOf(id));writableDatabase.delete("transfer_pending","transfer_id=?",arrayOf(id))}
 fun recordTransfer(direction:String,deviceId:String,deviceName:String,status:String,count:Int,details:String):String{ensureTransferHistory();val id=java.util.UUID.randomUUID().toString();writableDatabase.insert("transfer_history",null,ContentValues().apply{put("id",id);put("direction",direction);put("device_id",deviceId);put("device_name",deviceName);put("status",status);put("record_count",count);put("details",details)});return id}
 fun updateTransferStatus(id:String,status:String,details:String){ensureTransferHistory();writableDatabase.update("transfer_history",ContentValues().apply{put("status",status);put("details",details)},"id=?",arrayOf(id))}
 fun transferHistory():List<List<String>>{ensureTransferHistory();return rows("SELECT id,direction,device_id,COALESCE(device_name,''),occurred_at,status,record_count,COALESCE(details,'') FROM transfer_history ORDER BY occurred_at DESC")}
 fun deleteTransfer(id:String){ensureTransferHistory();writableDatabase.delete("transfer_history","id=?",arrayOf(id))}

 fun addAnthropometry(mid:Long,date:String,json:String){writableDatabase.insert("anthropometry",null,ContentValues().apply{put("member_id",mid);put("measurement_date",date);put("values_json",json)})}
 fun addMember(name:String,dob:String,sex:String,blood:String,height:String){val v=ContentValues().apply{put("name",name);put("dob",dob);put("sex",sex);put("blood",blood);put("height",height.toDoubleOrNull())};writableDatabase.insert("members",null,v)}
 fun addVital(mid:Long,j:JSONObject,notes:String,date:String,period:String){val v=ContentValues().apply{put("member_id",mid);put("measurement_date",date);put("time_of_day",period);put("left_systolic",j.optDouble("left_systolic").takeIf{j.has("left_systolic")});put("left_diastolic",j.optDouble("left_diastolic").takeIf{j.has("left_diastolic")});put("right_systolic",j.optDouble("right_systolic").takeIf{j.has("right_systolic")});put("right_diastolic",j.optDouble("right_diastolic").takeIf{j.has("right_diastolic")});put("pulse",j.optDouble("pulse").takeIf{j.has("pulse")});put("weight",j.optDouble("weight_kg").takeIf{j.has("weight_kg")});put("height",j.optDouble("height_cm").takeIf{j.has("height_cm")});put("spo2",j.optDouble("spo2").takeIf{j.has("spo2")});put("temp",j.optDouble("temp_c").takeIf{j.has("temp_c")});put("notes",notes)};writableDatabase.insert("vitals",null,v)}
 fun addLab(mid:Long,name:String,data:String){writableDatabase.insert("labs",null,ContentValues().apply{put("member_id",mid);put("name",name);put("values_json",data)})}
 fun addAppointment(mid:Long,title:String,scheduledAt:String,location:String,notes:String):Long=writableDatabase.insert("appointments",null,ContentValues().apply{put("member_id",mid);put("title",title);put("scheduled_at",scheduledAt);put("location",location);put("notes",notes)})
 fun appointments()=rows("SELECT a.id,m.name,a.title,a.scheduled_at,COALESCE(a.location,''),a.member_id FROM appointments a JOIN members m ON m.id=a.member_id ORDER BY a.scheduled_at")
 fun medicinesForReminders()=rows("SELECT x.id,m.name,x.name,COALESCE(x.dose,''),x.member_id FROM medicines x JOIN members m ON m.id=x.member_id ORDER BY x.name")
 fun saveLinkedReminder(id:String,memberId:Long?,medicineId:Long?,appointmentId:Long?,label:String,kind:String,minutes:Long,repeating:Boolean,dueAt:String,zone:String){val v=ContentValues().apply{put("id",id);if(memberId!=null)put("member_id",memberId);if(medicineId!=null)put("medicine_id",medicineId);if(appointmentId!=null)put("appointment_id",appointmentId);put("label",label);put("kind",kind);put("interval_minutes",minutes);put("is_repeating",if(repeating)1 else 0);put("due_at",dueAt);put("timezone_id",zone);put("active",1)};writableDatabase.insertWithOnConflict("reminders",null,v,SQLiteDatabase.CONFLICT_REPLACE)}
 fun reminderRows()=rows("SELECT id,label,kind,interval_minutes,is_repeating,COALESCE(due_at,''),COALESCE(medicine_id,''),COALESCE(appointment_id,'') FROM reminders WHERE active=1 ORDER BY due_at")
 fun addMedicine(mid:Long,name:String,dose:String,start:String,end:String,notes:String){writableDatabase.insert("medicines",null,ContentValues().apply{put("member_id",mid);put("name",name);put("dose",dose);put("start_date",start);put("end_date",end);put("notes",notes)})}
 fun addHealthHistory(mid:Long,category:String,display:String,status:String,onset:String,resolved:String,severity:String,reaction:String,code:String,notes:String){require(display.isNotBlank());writableDatabase.insert("clinical_history",null,ContentValues().apply{put("member_id",mid);put("category",category);put("display",display.trim());put("status",status);put("onset_date",onset);put("resolved_date",resolved);put("severity",severity);put("reaction",reaction);put("clinical_code",code);put("notes",notes)})}
 fun healthHistory()=rows("SELECT h.id,m.name,h.category,h.display,COALESCE(h.status,'active'),COALESCE(h.onset_date,''),COALESCE(h.severity,''),COALESCE(h.reaction,''),COALESCE(h.notes,'') FROM clinical_history h JOIN members m ON m.id=h.member_id ORDER BY h.id DESC")
 fun rows(sql:String):List<List<String>>{val out=mutableListOf<List<String>>();readableDatabase.rawQuery(sql,null).use{c->while(c.moveToNext())out.add((0 until c.columnCount).map{if(c.isNull(it))"" else c.getString(it)})};return out}
 fun members()=rows("SELECT id,name,COALESCE(sex,''),COALESCE(blood,''),COALESCE(dob,''),COALESCE(height,'') FROM members ORDER BY name")
 fun vitals()=rows("SELECT v.id,m.name,COALESCE(v.measurement_date,date(v.recorded_at)),COALESCE(v.left_systolic,'')||'/'||COALESCE(v.left_diastolic,''),COALESCE(v.right_systolic,'')||'/'||COALESCE(v.right_diastolic,''),COALESCE(v.pulse,''),COALESCE(v.weight,''),COALESCE(v.spo2,''),COALESCE(v.time_of_day,'') FROM vitals v JOIN members m ON m.id=v.member_id ORDER BY v.id DESC LIMIT 30")
 fun labs()=rows("SELECT l.id,m.name,datetime(l.recorded_at,'localtime'),COALESCE(l.name,'Lab report') FROM labs l JOIN members m ON m.id=l.member_id ORDER BY l.id DESC LIMIT 30")
 fun medicines()=rows("SELECT x.id,m.name,x.name,COALESCE(x.dose,''),COALESCE(x.start_date,''),COALESCE(x.end_date,''),COALESCE(x.notes,'') FROM medicines x JOIN members m ON m.id=x.member_id ORDER BY x.id DESC")
 fun count(table:String)=rows("SELECT COUNT(*) FROM $table").firstOrNull()?.firstOrNull()?.toIntOrNull()?:0
}

class MainActivity:FragmentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  activeVintagePalette = vintagePalettes.firstOrNull { it.id == getSharedPreferences("appearance_v1", MODE_PRIVATE).getString("palette", "botanical") } ?: vintagePalettes.first()
  if(!Python.isStarted()) Python.start(AndroidPlatform(this))
  setContent {
   MaterialTheme(colorScheme=lightColorScheme(primary=Forest,background=Ivory,surface=Cream,onSurface=Ink,secondary=Clay)) {
    LocalAuthGate { passphrase -> AuthenticatedDatabaseHost(passphrase) }
   }
  }
 }
}

@Composable
private fun AuthenticatedDatabaseHost(passphrase: ByteArray) {
 val context=androidx.compose.ui.platform.LocalContext.current
 var db by remember { mutableStateOf<HealthDb?>(null) }
 var error by remember { mutableStateOf("") }
 DisposableEffect(db) { onDispose { db?.close() } }
 LaunchedEffect(Unit) {
  try {
   val opened=withContext(Dispatchers.IO) {
    try {
     HealthDb(context,passphrase).also { ClinicalInteroperability.ensureSchema(it); PrivacyAudit.record(it, "database_unlocked", outcome = "authenticated") }
    } finally { passphrase.fill(0) }
   }
   db=opened
  } catch(e:Exception) { error=e.message ?: "Database unlock failed." }
 }
 if(db!=null) {
  HealthApp(db!!)
 } else if(error.isNotBlank()) {
  Surface(Modifier.fillMaxSize(),color=Ivory) {
   Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center) {
    Text("Health database could not be safely opened",fontSize=22.sp,color=Ink)
    Spacer(Modifier.height(12.dp))
    Text(error,color=Ink)
    Text("Do not uninstall the app or clear its data.",color=Ink)
   }
  }
 } else {
  Surface(Modifier.fillMaxSize(),color=Ivory) {
   Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { CircularProgressIndicator(color=Forest) }
  }
 }
}

@Composable private fun HealthApp(db:HealthDb){
 var tab by remember{mutableStateOf("Home")};var refresh by remember{mutableIntStateOf(0)};var dialog by remember{mutableStateOf("")}
 val members=remember(refresh){db.members()};val vitals=remember(refresh){db.vitals()};val labs=remember(refresh){db.labs()};val meds=remember(refresh){db.medicines()}
 val context=androidx.compose.ui.platform.LocalContext.current
 val pdfLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")){uri->
  if(uri!=null){try{
   val sections=listOf(
    "FAMILY MEMBERS" to db.rows("SELECT name,COALESCE(dob,''),COALESCE(sex,''),COALESCE(blood,'') FROM members ORDER BY name").map{"Name: ${it[0]} · DOB: ${it[1]} · Sex: ${it[2]} · Blood group: ${it[3]}"},
    "VITALS" to db.rows("SELECT m.name,COALESCE(v.measurement_date,''),COALESCE(v.left_systolic,'')||'/'||COALESCE(v.left_diastolic,''),COALESCE(v.right_systolic,'')||'/'||COALESCE(v.right_diastolic,''),COALESCE(v.pulse,''),COALESCE(v.weight,''),COALESCE(v.spo2,''),COALESCE(v.temp,'') FROM vitals v JOIN members m ON m.id=v.member_id ORDER BY v.id").map{"${it[0]} · ${it[1]} · Left BP ${it[2]} · Right BP ${it[3]} · Pulse ${it[4]} · Weight ${it[5]} kg · SpO₂ ${it[6]}% · Temp ${it[7]} °C"},
    "LAB REPORTS" to db.rows("SELECT m.name,COALESCE(l.name,'Report'),l.values_json FROM labs l JOIN members m ON m.id=l.member_id ORDER BY l.id").map{"${it[0]} · ${it[1]} · ${it[2]}"},
    "MEDICINES" to db.rows("SELECT m.name,x.name,COALESCE(x.dose,''),COALESCE(x.start_date,''),COALESCE(x.end_date,'') FROM medicines x JOIN members m ON m.id=x.member_id ORDER BY x.id").map{"${it[0]} · ${it[1]} · ${it[2]} · ${it[3]} to ${it[4]}"},
    "ANTHROPOMETRY" to db.rows("SELECT m.name,a.measurement_date,a.values_json FROM anthropometry a JOIN members m ON m.id=a.member_id ORDER BY a.id").map{"${it[0]} · ${it[1]} · ${it[2]}"}
   )
   NativePdfExport.write(context,uri,"Health Monitor - Family Health Report",sections)
   Toast.makeText(context,"PDF exported",Toast.LENGTH_LONG).show()
  }catch(e:Exception){Toast.makeText(context,"PDF export failed: ${e.message}",Toast.LENGTH_LONG).show()}}
 }
 Scaffold(containerColor=Ivory,floatingActionButton={ExtendedFloatingActionButton(onClick={pdfLauncher.launch("Health-Monitor-Report.pdf")},text={Text("Export PDF")},icon={Icon(Icons.Default.PictureAsPdf,null)})},bottomBar={NavigationBar(containerColor=Cream){listOf("Home","Records","Assistant","Analytics","More").forEach{t->val selected=when(t){"Records"->tab in setOf("Records","People","Vitals","Reports","Medicines","History","Tests");"More"->tab in setOf("More","Reminders","Interop","Received & Shared","Privacy","Settings");else->tab==t};NavigationBarItem(selected=selected,onClick={tab=t},icon={Icon(when(t){"Home"->Icons.Default.Home;"Records"->Icons.Default.FolderOpen;"Assistant"->Icons.Default.SmartToy;"Analytics"->Icons.Default.Analytics;else->Icons.Default.MoreHoriz},null)},label={Text(t)})}}}){p->
  Column(Modifier.fillMaxSize().padding(p)){
   Row(Modifier.fillMaxWidth().padding(horizontal=20.dp, vertical=14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("HEALTH MONITOR",fontSize=11.sp,letterSpacing=2.sp,color=Forest,fontWeight=FontWeight.Bold);Text(tab,fontSize=27.sp,color=Ink,fontWeight=FontWeight.SemiBold)};Surface(color=Forest,shape=RoundedCornerShape(50)){Icon(Icons.Default.Favorite, null,tint=Color.White,modifier=Modifier.padding(11.dp).size(20.dp))}}
   when(tab){
    "Home"->LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
     item{Card(colors=CardDefaults.cardColors(containerColor=Forest),shape=RoundedCornerShape(24.dp)){Column(Modifier.fillMaxWidth().padding(22.dp)){Text("YOUR FAMILY HEALTH",color=Sage,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.5.sp);Text("A clearer view of everyday wellbeing.",fontSize=25.sp,color=Color.White,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(8.dp));Text("Private records stored on this device.",color=Color.White.copy(.8f))}}}
     item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){MetricCard("People",members.size.toString(),Modifier.weight(1f));MetricCard("Vitals",db.count("vitals").toString(),Modifier.weight(1f));MetricCard("Reports",db.count("labs").toString(),Modifier.weight(1f))}}
     item{Text("QUICK ACTIONS",color=Forest,fontWeight=FontWeight.Bold,fontSize=12.sp,letterSpacing=1.3.sp)}
     item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){ActionTile("Add person",Icons.Default.PersonAdd,Modifier.weight(1f)){dialog="person"};ActionTile("Record vitals",Icons.Default.MonitorHeart,Modifier.weight(1f)){dialog="vitals"}}}
     item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){ActionTile("Lab report",Icons.Default.Science,Modifier.weight(1f)){dialog="lab"};ActionTile("Medicine",Icons.Default.Medication,Modifier.weight(1f)){dialog="medicine"}}}
     item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){ActionTile("Anthropometry",Icons.Default.Accessibility,Modifier.weight(1f)){dialog="anthropometry"};ActionTile("Clinical tests",Icons.Default.Biotech,Modifier.weight(1f)){tab="Tests"}}}
     item{SectionTitle("RECENT VITALS")}
     items(vitals.take(4)){r->RecordCard("${r[1]} · ${r[2]}","BP ${r[3]}/${r[4]} mmHg   Pulse ${r[5]} bpm   SpO₂ ${r[7]}%")}
    }
    "Assistant"->OfflineAssistantScreen(db,refresh)
    "People"->MemberManagementScreen(db,refresh,{refresh++}){dialog="person"}
    "Vitals"->LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{ButtonRow("Record new vitals"){dialog="vitals"}};items(vitals){r->RecordCard("${r[1]} · ${r[2]}","BP ${r[3]}/${r[4]} · Pulse ${r[5]} · Weight ${r[6]} kg · SpO₂ ${r[7]}%")}}
    "Reports"->LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{ButtonRow("Add laboratory report"){dialog="lab"}};items(labs){r->RecordCard("${r[1]} · ${r[2]}",r[3])}}
    "Medicines"->MedicineManagementScreen(db,refresh,{refresh++}){dialog="medicine"}
    "Analytics"->if(PrivacyAudit.granted(db, "local_analytics")) AnalyticsDashboard(db,refresh) else Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center){Text("Local analytics is disabled",color=Ink,fontSize=22.sp);Text("Enable local analytics in the Privacy tab to view summaries. No data leaves this device.",color=Ink.copy(.7f))}
    "Reminders"->ReminderManagementScreen(db,refresh){refresh++}
    "History"->HealthHistoryScreen(db,refresh){refresh++}
    "Interop"->InteroperabilityScreen(db,refresh){refresh++}
    "Received & Shared"->ReceivedSharedScreen(db,refresh){refresh++}
    "Privacy"->PrivacyControlsScreen(db,refresh){refresh++}
    else->Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
     Text("Settings & privacy",fontSize=23.sp,fontWeight=FontWeight.SemiBold)
     SectionTitle("VINTAGE COLOUR PALETTE")
     Text("Choose a heritage colour story. Your selection updates the app immediately.",color=Ink.copy(.7f),fontSize=13.sp)
     vintagePalettes.forEach{palette->
      Card(colors=CardDefaults.cardColors(containerColor=if(activeVintagePalette.id==palette.id)palette.cream else Cream),shape=RoundedCornerShape(18.dp)){
       Row(Modifier.fillMaxWidth().clickable{activeVintagePalette=palette;context.getSharedPreferences("appearance_v1",Context.MODE_PRIVATE).edit().putString("palette",palette.id).apply()}.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
        Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf(palette.ivory,palette.cream,palette.forest,palette.sage,palette.clay).forEach{swatch->Surface(color=swatch,shape=RoundedCornerShape(6.dp),modifier=Modifier.size(22.dp)){} }}
        Column(Modifier.weight(1f)){Text(palette.name,color=palette.ink,fontWeight=FontWeight.SemiBold);Text("Warm paper · heritage accents",color=palette.ink.copy(.7f),fontSize=11.sp)}
        RadioButton(selected=activeVintagePalette.id==palette.id,onClick={activeVintagePalette=palette})
       }
      }
     }
     RecordCard("Surface finish","Subtle paper-like warmth with clean, softly rounded cards and quiet borders.")
     RecordCard("On-device database","Health records are stored locally in the app database.")
     RecordCard("Calculation engine","Python 3.11 embedded with Chaquopy.")
     RecordCard("Clinical note","Reference ranges are informational and do not provide a diagnosis. Follow the reporting laboratory's interval and clinician guidance.")
    }
   }
  }
 }
 when(dialog){ "person"->PersonDialog({dialog=""},{db.addMember(it[0],it[1],it[2],it[3],it[4]);refresh++})
 "vitals"->VitalDialog(members,{dialog=""},{mid,data,notes,date,period->db.addVital(mid,data,notes,date,period);refresh++})
 "lab"->LabDialog(members,{dialog=""},{mid,name,json->db.addLab(mid,name,json);refresh++})
 "medicine"->MedicineDialog(members,{dialog=""},{mid,n,d,s,e,no->db.addMedicine(mid,n,d,s,e,no);refresh++})
 "anthropometry"->AnthropometryDialog(members,{dialog=""},{mid,date,json->db.addAnthropometry(mid,date,json);refresh++})}
}

@Composable
private fun NavigationHubScreen(
    title: String,
    description: String,
    entries: List<Pair<String, androidx.compose.ui.graphics.vector.ImageVector>>,
    onNavigate: (String) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(title, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = Ink.copy(alpha = .68f), fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 4.dp))
        }
        items(entries) { (label, icon) ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigate(label) },
                colors = CardDefaults.cardColors(containerColor = Cream),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 17.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = Forest)
                    Text(label, color = Ink, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = Ink.copy(alpha = .5f))
                }
            }
        }
    }
}

@Composable private fun MetricCard(label:String,value:String,modifier:Modifier=Modifier){Card(modifier,colors=CardDefaults.cardColors(containerColor=Cream),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(15.dp)){Text(value,fontSize=24.sp,color=Forest,fontWeight=FontWeight.SemiBold);Text(label,color=Ink.copy(.65f),fontSize=12.sp)}}}
@Composable private fun ActionTile(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier=Modifier,onClick:()->Unit){Card(modifier.clickable{onClick()},colors=CardDefaults.cardColors(containerColor=Cream),shape=RoundedCornerShape(18.dp)){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Icon(icon,null,tint=Forest);Text(label,color=Ink,fontWeight=FontWeight.SemiBold)}}}
@Composable private fun SectionTitle(s:String){Text(s,color=Forest,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.4.sp)}
@Composable private fun RecordCard(title:String,subtitle:String){Card(colors=CardDefaults.cardColors(containerColor=Cream),shape=RoundedCornerShape(16.dp)){Column(Modifier.fillMaxWidth().padding(16.dp)){Text(title,color=Ink,fontWeight=FontWeight.SemiBold);Text(subtitle,color=Ink.copy(.62f),fontSize=13.sp)}}}
@Composable private fun ButtonRow(label:String,onClick:()->Unit){Button(onClick=onClick,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Forest)){Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text(label)}}
@Composable private fun FormField(label:String,value:String,onChange:(String)->Unit,numeric:Boolean=false){OutlinedTextField(value,onChange,label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=if(numeric)KeyboardType.Decimal else KeyboardType.Text))}
@Composable private fun PersonDialog(close:()->Unit,save:(List<String>)->Unit){var n by remember{mutableStateOf("")};var dob by remember{mutableStateOf("")};var sex by remember{mutableStateOf("M")};var blood by remember{mutableStateOf("")};var h by remember{mutableStateOf("")};AlertDialog(onDismissRequest=close,title={Text("Family member")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){FormField("Full name",n,{n=it});FormField("Date of birth (YYYY-MM-DD)",dob,{dob=it});Row{listOf("M","F","Other").forEach{FilterChip(selected=sex==it,onClick={sex=it},label={Text(it)})}};FormField("Blood group",blood,{blood=it});FormField("Height (cm)",h,{h=it},true)}},confirmButton={TextButton(onClick={if(n.isNotBlank()){save(listOf(n,dob,sex,blood,h));close()}}){Text("Save")}},dismissButton={TextButton(onClick=close){Text("Cancel")}})}
@Composable private fun MemberPicker(members:List<List<String>>,selected:String,onPick:(String)->Unit){
 var expanded by remember{mutableStateOf(false)}
 Box {
  OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()){Text(members.firstOrNull{it[0]==selected}?.get(1)?: "Choose family member")}
  DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
   members.forEach{member->DropdownMenuItem(text={Text(member[1])},onClick={onPick(member[0]);expanded=false})}
  }
 }
}
@Composable private fun VitalDialog(members:List<List<String>>,close:()->Unit,save:(Long,JSONObject,String,String,String)->Unit){
 var member by remember{mutableStateOf(members.firstOrNull()?.get(0)?:"")}
 var leftSys by remember{mutableStateOf("")};var leftDia by remember{mutableStateOf("")};var rightSys by remember{mutableStateOf("")};var rightDia by remember{mutableStateOf("")}
 var pulse by remember{mutableStateOf("")};var weight by remember{mutableStateOf("")};var height by remember{mutableStateOf("")};var spo by remember{mutableStateOf("")};var temp by remember{mutableStateOf("")};var note by remember{mutableStateOf("")}
 var date by remember{mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(java.util.Date()))}
 var period by remember{mutableStateOf("morning")};var periodOpen by remember{mutableStateOf(false)};var result by remember{mutableStateOf("")}
 val context=androidx.compose.ui.platform.LocalContext.current
 AlertDialog(onDismissRequest=close,title={Text("Record vitals")},text={Column(Modifier.heightIn(max=570.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
 if(members.isEmpty())Text("Add a family member first.")else MemberPicker(members,member){member=it}
 OutlinedButton(onClick={val c=Calendar.getInstance();DatePickerDialog(context,{_,y,m,d->date=String.format(java.util.Locale.US,"%04d-%02d-%02d",y,m+1,d)},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show()},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.CalendarMonth,null);Spacer(Modifier.width(8.dp));Text("Measurement date · $date")}
 Box{OutlinedButton(onClick={periodOpen=true},modifier=Modifier.fillMaxWidth()){Text("Time of day · "+period.replaceFirstChar{it.uppercase()})};DropdownMenu(expanded=periodOpen,onDismissRequest={periodOpen=false}){listOf("morning","noon","afternoon","evening","night","overnight","unknown").forEach{p->DropdownMenuItem(text={Text(p.replaceFirstChar{it.uppercase()})},onClick={period=p;periodOpen=false})}}}
 Text("BLOOD PRESSURE · BOTH ARMS",color=Forest,fontSize=11.sp,fontWeight=FontWeight.Bold)
 FormField("Left arm systolic · mmHg",leftSys,{leftSys=it},true);FormField("Left arm diastolic · mmHg",leftDia,{leftDia=it},true);FormField("Right arm systolic · mmHg",rightSys,{rightSys=it},true);FormField("Right arm diastolic · mmHg",rightDia,{rightDia=it},true)
 Text("OTHER VITALS",color=Forest,fontSize=11.sp,fontWeight=FontWeight.Bold)
 FormField("Pulse · bpm",pulse,{pulse=it},true);FormField("Weight · kg",weight,{weight=it},true);FormField("Height · cm",height,{height=it},true);FormField("SpO₂ · %",spo,{spo=it},true);FormField("Temperature · °C",temp,{temp=it},true);FormField("Notes",note,{note=it})
 if(result.isNotBlank())Text(result,color=Forest,fontSize=12.sp)
 }},confirmButton={TextButton(onClick={if(member.isNotBlank()){try{
 val o=JSONObject();listOf("left_systolic" to leftSys,"left_diastolic" to leftDia,"right_systolic" to rightSys,"right_diastolic" to rightDia,"pulse" to pulse,"weight_kg" to weight,"height_cm" to height,"spo2" to spo,"temp_c" to temp).forEach{(k,v)->v.toDoubleOrNull()?.let{o.put(k,it)}}
 val diff=if(leftSys.toDoubleOrNull()!=null && rightSys.toDoubleOrNull()!=null){val bp=Python.getInstance().getModule("observation_engine").callAttr("blood_pressure_comparison_json",hashMapOf("left_systolic" to leftSys.toDouble(),"right_systolic" to rightSys.toDouble())).toString();val bpJson=JSONObject(bp);" · Inter-arm systolic difference ${bpJson.optString("systolic_difference")} mmHg"}else ""
 save(member.toLong(),o,note,date,period);result="Saved $date$diff";close()
 }catch(e:Exception){result="Unable to save: ${e.message}"}}}){Text("Save vitals")}},dismissButton={TextButton(onClick=close){Text("Cancel")}})
}
@Composable private fun LabDialog(members:List<List<String>>,close:()->Unit,save:(Long,String,String)->Unit){
 var member by remember{mutableStateOf(members.firstOrNull()?.get(0)?:"")}
 var report by remember{mutableStateOf("Laboratory report")};var parameter by remember{mutableStateOf("")};var kind by remember{mutableStateOf("quantitative")}
 var value by remember{mutableStateOf("")};var unit by remember{mutableStateOf("")};var note by remember{mutableStateOf("")}
 var status by remember{mutableStateOf("not_determined")};var statusOpen by remember{mutableStateOf(false)};var kindOpen by remember{mutableStateOf(false)}
 var date by remember{mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(java.util.Date()))}
 val context=androidx.compose.ui.platform.LocalContext.current
 val statuses=listOf("positive","negative","reactive","non_reactive","detected","not_detected","present","absent","seen","not_seen","trace","normal","abnormal","borderline","indeterminate","inconclusive","not_determined","pending","not_done","insufficient_sample","contaminated","invalid","pass","refer","susceptible","intermediate","resistant","weakly_positive","strongly_positive","equivocal","other")
 AlertDialog(onDismissRequest=close,title={Text("Add test result")},text={Column(Modifier.heightIn(max=560.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
 if(members.isNotEmpty())MemberPicker(members,member){member=it}else Text("Add a family member first.")
 FormField("Report / panel name",report,{report=it});FormField("Test or observation name",parameter,{parameter=it})
 OutlinedButton(onClick={val c=Calendar.getInstance();DatePickerDialog(context,{_,y,m,d->date=String.format(java.util.Locale.US,"%04d-%02d-%02d",y,m+1,d)},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show()},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.CalendarMonth,null);Spacer(Modifier.width(8.dp));Text("Collection / observation date · $date")}
 Box{OutlinedButton(onClick={kindOpen=true},modifier=Modifier.fillMaxWidth()){Text("Result format · "+kind.replaceFirstChar{it.uppercase()})};DropdownMenu(expanded=kindOpen,onDismissRequest={kindOpen=false}){listOf("quantitative","qualitative","graded","report").forEach{k->DropdownMenuItem(text={Text(k.replaceFirstChar{it.uppercase()})},onClick={kind=k;kindOpen=false})}}}
 if(kind=="quantitative"){FormField("Numeric result",value,{value=it},true);FormField("Unit (as printed on report)",unit,{unit=it})}
 else {Box{OutlinedButton(onClick={statusOpen=true},modifier=Modifier.fillMaxWidth()){Text("Result status · "+status.replace('_',' ').replaceFirstChar{it.uppercase()})};DropdownMenu(expanded=statusOpen,onDismissRequest={statusOpen=false}){statuses.forEach{st->DropdownMenuItem(text={Text(st.replace('_',' ').replaceFirstChar{it.uppercase()})},onClick={status=st;statusOpen=false})}}};FormField("Report wording / finding",value,{value=it});if(kind=="graded")FormField("Grade / scale unit",unit,{unit=it})}
 FormField("Notes / report details",note,{note=it})
 }},confirmButton={TextButton(onClick={if(member.isNotBlank()&&parameter.isNotBlank()){try{
 val payload=hashMapOf<String,Any>("parameter" to parameter,"result_kind" to kind,"recorded_date" to date,"unit" to unit,"notes" to note)
 if(kind=="quantitative"){value.toDoubleOrNull()?.let{payload["value_numeric"]=it}}else{payload["qualitative_status"]=status;if(value.isNotBlank())payload["value_text"]=value}
 val normalized=Python.getInstance().getModule("observation_engine").callAttr("normalize_observation_json",payload).toString()
 save(member.toLong(),report,normalized);close()
 }catch(e:Exception){}}}){Text("Save result")}},dismissButton={TextButton(onClick=close){Text("Cancel")}})
}

@Composable private fun AnthropometryDialog(members:List<List<String>>,close:()->Unit,save:(Long,String,String)->Unit){
 var member by remember{mutableStateOf(members.firstOrNull()?.get(0)?:"")}
 var date by remember{mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(java.util.Date()))}
 var profile by remember{mutableStateOf("SOUTH_ASIAN")}
 var weight by remember{mutableStateOf("")};var height by remember{mutableStateOf("")};var length by remember{mutableStateOf("")}
 var waist by remember{mutableStateOf("")};var hip by remember{mutableStateOf("")};var neck by remember{mutableStateOf("")};var head by remember{mutableStateOf("")}
 var muac by remember{mutableStateOf("")};var arm by remember{mutableStateOf("")};var sitting by remember{mutableStateOf("")};var calf by remember{mutableStateOf("")}
 var triceps by remember{mutableStateOf("")};var subscapular by remember{mutableStateOf("")};var message by remember{mutableStateOf("")}
 var profileOpen by remember{mutableStateOf(false)}
 val context=androidx.compose.ui.platform.LocalContext.current
 val person=members.firstOrNull{it[0]==member}
 AlertDialog(onDismissRequest=close,title={Text("Anthropometric measurements")},text={Column(Modifier.heightIn(max=570.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),verticalArrangement=Arrangement.spacedBy(7.dp)){
 if(members.isNotEmpty())MemberPicker(members,member){member=it}else Text("Add a family member first.")
 OutlinedButton(onClick={val c=Calendar.getInstance();DatePickerDialog(context,{_,y,m,d->date=String.format(java.util.Locale.US,"%04d-%02d-%02d",y,m+1,d)},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show()},modifier=Modifier.fillMaxWidth()){Text("Measurement date · $date")}
 Box{OutlinedButton(onClick={profileOpen=true},modifier=Modifier.fillMaxWidth()){Text(if(profile=="SOUTH_ASIAN")"Population profile · South Asian" else "Population profile · WHO global")};DropdownMenu(expanded=profileOpen,onDismissRequest={profileOpen=false}){listOf("SOUTH_ASIAN","WHO_GLOBAL").forEach{v->DropdownMenuItem(text={Text(if(v=="SOUTH_ASIAN")"South Asian risk-screening" else "WHO global")},onClick={profile=v;profileOpen=false})}}}
 Text("CORE MEASUREMENTS",color=Forest,fontSize=11.sp,fontWeight=FontWeight.Bold)
 FormField("Weight · kg",weight,{weight=it},true);FormField("Standing height · cm",height,{height=it},true);FormField("Recumbent length · cm",length,{length=it},true)
 Text("CIRCUMFERENCES",color=Forest,fontSize=11.sp,fontWeight=FontWeight.Bold)
 FormField("Waist · cm",waist,{waist=it},true);FormField("Hip · cm",hip,{hip=it},true);FormField("Neck · cm",neck,{neck=it},true);FormField("Head · cm",head,{head=it},true);FormField("Mid-upper arm · cm",muac,{muac=it},true);FormField("Calf · cm",calf,{calf=it},true)
 Text("ADDITIONAL MEASUREMENTS",color=Forest,fontSize=11.sp,fontWeight=FontWeight.Bold)
 FormField("Arm span · cm",arm,{arm=it},true);FormField("Sitting height · cm",sitting,{sitting=it},true);FormField("Triceps skinfold · mm",triceps,{triceps=it},true);FormField("Subscapular skinfold · mm",subscapular,{subscapular=it},true)
 if(message.isNotBlank())Text(message,color=Forest,fontSize=12.sp)
 }},confirmButton={TextButton(onClick={if(member.isNotBlank()&&listOf(weight,height,length,waist,hip,neck,head,muac,arm,sitting,calf,triceps,subscapular).any{it.isNotBlank()}){try{
 val fields=listOf("weight_kg" to weight,"height_cm" to height,"length_cm" to length,"waist_cm" to waist,"hip_cm" to hip,"neck_cm" to neck,"head_cm" to head,"mid_upper_arm_cm" to muac,"arm_span_cm" to arm,"sitting_height_cm" to sitting,"calf_cm" to calf,"skin_fold_triceps_mm" to triceps,"skin_fold_subscapular_mm" to subscapular)
 val payload=hashMapOf<String,Any>("measurement_date" to date,"date_of_birth" to (person?.get(4)?: ""),"sex" to (person?.get(2)?: "unknown"),"population_profile" to profile)
 fields.forEach{(k,v)->v.toDoubleOrNull()?.let{payload[k]=it}}
 val raw=Python.getInstance().getModule("anthropometry").callAttr("calculate_json",payload).toString()
 val parsed=JSONObject(raw);val d=parsed.optJSONObject("derived")?:JSONObject()
 val summary=listOf("bmi","waist_hip_ratio","waist_height_ratio","body_surface_area").mapNotNull{k->d.optJSONObject(k)?.let{"$k: ${it.optString("value")} ${it.optString("unit")}"} }.joinToString(" · ")
 val stored=JSONObject().put("calculation",parsed).put("entered_measurements",JSONObject().apply{fields.forEach{(k,v)->v.toDoubleOrNull()?.let{put(k,it)}}}).toString()
 save(member.toLong(),date,stored);message=summary.ifBlank{"Measurements saved"};close()
 }catch(e:Exception){message="Unable to save: ${e.message}"}}}){Text("Save measurements")}},dismissButton={TextButton(onClick=close){Text("Cancel")}})
}

@Composable private fun MedicineDialog(members:List<List<String>>,close:()->Unit,save:(Long,String,String,String,String,String)->Unit){
 var member by remember{mutableStateOf(members.firstOrNull()?.get(0)?:"")};var name by remember{mutableStateOf("")};var dose by remember{mutableStateOf("")};var start by remember{mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(java.util.Date()))};var end by remember{mutableStateOf("")};var notes by remember{mutableStateOf("")}
 val context=androidx.compose.ui.platform.LocalContext.current
 AlertDialog(onDismissRequest=close,title={Text("Medicine")},text={Column(Modifier.heightIn(max=500.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
 if(members.isNotEmpty())MemberPicker(members,member){member=it}else Text("Add a family member first.")
 FormField("Medicine name",name,{name=it});FormField("Dose & schedule",dose,{dose=it})
 OutlinedButton(onClick={val c=Calendar.getInstance();DatePickerDialog(context,{_,y,m,d->start=String.format(java.util.Locale.US,"%04d-%02d-%02d",y,m+1,d)},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show()},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.CalendarMonth,null);Spacer(Modifier.width(8.dp));Text("Start date · $start")}
 OutlinedButton(onClick={val c=Calendar.getInstance();DatePickerDialog(context,{_,y,m,d->end=String.format(java.util.Locale.US,"%04d-%02d-%02d",y,m+1,d)},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show()},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.CalendarMonth,null);Spacer(Modifier.width(8.dp));Text(if(end.isBlank())"Set end date (optional)" else "End date · $end")}
 FormField("Notes",notes,{notes=it})
 }},confirmButton={TextButton(onClick={if(member.isNotBlank()&&name.isNotBlank()){save(member.toLong(),name,dose,start,end,notes);close()}}){Text("Save")}},dismissButton={TextButton(onClick=close){Text("Cancel")}})
}
