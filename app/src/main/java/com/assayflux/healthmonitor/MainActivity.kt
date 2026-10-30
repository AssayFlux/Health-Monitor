package com.assayflux.healthmonitor

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.app.DatePickerDialog
import java.util.Calendar
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

private val Ivory=Color(0xFFF3EFE5); private val Cream=Color(0xFFFAF8F2)
private val Forest=Color(0xFF34483E); private val Sage=Color(0xFFA9B19F)
private val Clay=Color(0xFFA16F5D); private val Ink=Color(0xFF302F29)

class HealthDb(c:Context):SQLiteOpenHelper(c,"health_monitor.db",null,2){
 override fun onCreate(d:SQLiteDatabase){
  d.execSQL("CREATE TABLE members(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,dob TEXT,sex TEXT,blood TEXT,height REAL,notes TEXT)")
  d.execSQL("CREATE TABLE vitals(id INTEGER PRIMARY KEY AUTOINCREMENT,member_id INTEGER NOT NULL,recorded_at TEXT DEFAULT CURRENT_TIMESTAMP,measurement_date TEXT,time_of_day TEXT,systolic REAL,diastolic REAL,left_systolic REAL,left_diastolic REAL,right_systolic REAL,right_diastolic REAL,pulse REAL,weight REAL,height REAL,spo2 REAL,temp REAL,notes TEXT)")
  d.execSQL("CREATE TABLE labs(id INTEGER PRIMARY KEY AUTOINCREMENT,member_id INTEGER NOT NULL,recorded_at TEXT DEFAULT CURRENT_TIMESTAMP,name TEXT,values_json TEXT NOT NULL)")
  d.execSQL("CREATE TABLE medicines(id INTEGER PRIMARY KEY AUTOINCREMENT,member_id INTEGER NOT NULL,name TEXT NOT NULL,dose TEXT,start_date TEXT,end_date TEXT,notes TEXT)")
  d.execSQL("CREATE TABLE clinical_tests(id INTEGER PRIMARY KEY AUTOINCREMENT,member_id INTEGER NOT NULL,recorded_at TEXT DEFAULT CURRENT_TIMESTAMP,category TEXT,parameter TEXT,value_text TEXT,value_numeric REAL,unit TEXT,ref_low REAL,ref_high REAL,status TEXT,notes TEXT)")
 }
 override fun onUpgrade(d:SQLiteDatabase,o:Int,n:Int){if(o<2){listOf("measurement_date TEXT","time_of_day TEXT","left_systolic REAL","left_diastolic REAL","right_systolic REAL","right_diastolic REAL").forEach{d.execSQL("ALTER TABLE vitals ADD COLUMN $it")};d.execSQL("UPDATE vitals SET measurement_date=date(recorded_at),time_of_day='unknown' WHERE measurement_date IS NULL")}}
 fun addMember(name:String,dob:String,sex:String,blood:String,height:String){val v=ContentValues().apply{put("name",name);put("dob",dob);put("sex",sex);put("blood",blood);put("height",height.toDoubleOrNull())};writableDatabase.insert("members",null,v)}
 fun addVital(mid:Long,j:JSONObject,notes:String,date:String,period:String){val v=ContentValues().apply{put("member_id",mid);put("measurement_date",date);put("time_of_day",period);put("left_systolic",j.optDouble("left_systolic").takeIf{j.has("left_systolic")});put("left_diastolic",j.optDouble("left_diastolic").takeIf{j.has("left_diastolic")});put("right_systolic",j.optDouble("right_systolic").takeIf{j.has("right_systolic")});put("right_diastolic",j.optDouble("right_diastolic").takeIf{j.has("right_diastolic")});put("pulse",j.optDouble("pulse").takeIf{j.has("pulse")});put("weight",j.optDouble("weight_kg").takeIf{j.has("weight_kg")});put("height",j.optDouble("height_cm").takeIf{j.has("height_cm")});put("spo2",j.optDouble("spo2").takeIf{j.has("spo2")});put("temp",j.optDouble("temp_c").takeIf{j.has("temp_c")});put("notes",notes)};writableDatabase.insert("vitals",null,v)}
 fun addLab(mid:Long,name:String,data:String){writableDatabase.insert("labs",null,ContentValues().apply{put("member_id",mid);put("name",name);put("values_json",data)})}
 fun addMedicine(mid:Long,name:String,dose:String,start:String,end:String,notes:String){writableDatabase.insert("medicines",null,ContentValues().apply{put("member_id",mid);put("name",name);put("dose",dose);put("start_date",start);put("end_date",end);put("notes",notes)})}
 fun rows(sql:String):List<List<String>>{val out=mutableListOf<List<String>>();readableDatabase.rawQuery(sql,null).use{c->while(c.moveToNext())out.add((0 until c.columnCount).map{if(c.isNull(it))"" else c.getString(it)})};return out}
 fun members()=rows("SELECT id,name,COALESCE(sex,''),COALESCE(blood,'') FROM members ORDER BY name")
 fun vitals()=rows("SELECT v.id,m.name,COALESCE(v.measurement_date,date(v.recorded_at)),COALESCE(v.left_systolic,'')||'/'||COALESCE(v.left_diastolic,''),COALESCE(v.right_systolic,'')||'/'||COALESCE(v.right_diastolic,''),COALESCE(v.pulse,''),COALESCE(v.weight,''),COALESCE(v.spo2,''),COALESCE(v.time_of_day,'') FROM vitals v JOIN members m ON m.id=v.member_id ORDER BY v.id DESC LIMIT 30")
 fun labs()=rows("SELECT l.id,m.name,datetime(l.recorded_at,'localtime'),COALESCE(l.name,'Lab report') FROM labs l JOIN members m ON m.id=l.member_id ORDER BY l.id DESC LIMIT 30")
 fun medicines()=rows("SELECT x.id,m.name,x.name,COALESCE(x.dose,''),COALESCE(x.start_date,'') FROM medicines x JOIN members m ON m.id=x.member_id ORDER BY x.id DESC")
 fun count(table:String)=rows("SELECT COUNT(*) FROM $table").firstOrNull()?.firstOrNull()?.toIntOrNull()?:0
}

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(!Python.isStarted())Python.start(AndroidPlatform(this));val db=HealthDb(this);setContent{MaterialTheme(colorScheme=lightColorScheme(primary=Forest,background=Ivory,surface=Cream,onSurface=Ink,secondary=Clay)){HealthApp(db)}}}
}
@Composable private fun HealthApp(db:HealthDb){
 var tab by remember{mutableStateOf("Home")};var refresh by remember{mutableIntStateOf(0)};var dialog by remember{mutableStateOf("")}
 val members=remember(refresh){db.members()};val vitals=remember(refresh){db.vitals()};val labs=remember(refresh){db.labs()};val meds=remember(refresh){db.medicines()}
 Scaffold(containerColor=Ivory,bottomBar={NavigationBar(containerColor=Cream){listOf("Home","People","Vitals","Reports","More").forEach{t->NavigationBarItem(selected=tab==t,onClick={tab=t},icon={Icon(when(t){"Home"->Icons.Default.Home;"People"->Icons.Default.Groups;"Vitals"->Icons.Default.MonitorHeart;"Reports"->Icons.Default.Description;else->Icons.Default.MoreHoriz},null)},label={Text(t)})}}}){p->
  Column(Modifier.fillMaxSize().padding(p)){
   Row(Modifier.fillMaxWidth().padding(horizontal=20.dp, vertical=14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("HEALTH MONITOR",fontSize=11.sp,letterSpacing=2.sp,color=Forest,fontWeight=FontWeight.Bold);Text(tab,fontSize=27.sp,color=Ink,fontWeight=FontWeight.SemiBold)};Surface(color=Forest,shape=RoundedCornerShape(50)){Icon(Icons.Default.Favorite, null,tint=Color.White,modifier=Modifier.padding(11.dp).size(20.dp))}}
   when(tab){
    "Home"->LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
     item{Card(colors=CardDefaults.cardColors(containerColor=Forest),shape=RoundedCornerShape(24.dp)){Column(Modifier.fillMaxWidth().padding(22.dp)){Text("YOUR FAMILY HEALTH",color=Sage,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.5.sp);Text("A clearer view of
everyday wellbeing.",fontSize=25.sp,color=Color.White,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(8.dp));Text("Private records stored on this device.",color=Color.White.copy(.8f))}}}
     item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){MetricCard("People",members.size.toString(),Modifier.weight(1f));MetricCard("Vitals",db.count("vitals").toString(),Modifier.weight(1f));MetricCard("Reports",db.count("labs").toString(),Modifier.weight(1f))}}
     item{Text("QUICK ACTIONS",color=Forest,fontWeight=FontWeight.Bold,fontSize=12.sp,letterSpacing=1.3.sp)}
     item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){ActionTile("Add person",Icons.Default.PersonAdd,Modifier.weight(1f)){dialog="person"};ActionTile("Record vitals",Icons.Default.MonitorHeart,Modifier.weight(1f)){dialog="vitals"}}}
     item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){ActionTile("Lab report",Icons.Default.Science,Modifier.weight(1f)){dialog="lab"};ActionTile("Medicine",Icons.Default.Medication,Modifier.weight(1f)){dialog="medicine"}}}
     item{SectionTitle("RECENT VITALS")}
     items(vitals.take(4)){r->RecordCard("${r[1]} · ${r[2]}","BP ${r[3]}/${r[4]} mmHg   Pulse ${r[5]} bpm   SpO₂ ${r[7]}%")}
    }
    "People"->LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{ButtonRow("Add family member"){dialog="person"}};items(members){m->RecordCard(m[1],listOf(m[2],m[3]).filter{it.isNotBlank()}.joinToString(" · ").ifBlank{"Family profile"} )}}
    "Vitals"->LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{ButtonRow("Record new vitals"){dialog="vitals"}};items(vitals){r->RecordCard("${r[1]} · ${r[2]}","BP ${r[3]}/${r[4]} · Pulse ${r[5]} · Weight ${r[6]} kg · SpO₂ ${r[7]}%")}}
    "Reports"->LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{ButtonRow("Add laboratory report"){dialog="lab"}};items(labs){r->RecordCard("${r[1]} · ${r[2]}",r[3])};item{SectionTitle("MEDICINES")};item{ButtonRow("Add medicine"){dialog="medicine"}};items(meds){r->RecordCard("${r[2]} · ${r[1]}","${r[3]} · Started ${r[4]}")}}
    else->Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("Settings & privacy",fontSize=23.sp,fontWeight=FontWeight.SemiBold);RecordCard("On-device database","Health records are stored locally in the app database.");RecordCard("Calculation engine","Python 3.11 embedded with Chaquopy.");RecordCard("Clinical note","Reference ranges are informational and do not provide a diagnosis. Follow the reporting laboratory's interval and clinician guidance.")}
   }
  }
 }}
 when(dialog){ "person"->PersonDialog({dialog=""},{db.addMember(it[0],it[1],it[2],it[3],it[4]);refresh++})
 "vitals"->VitalDialog(members,{dialog=""},{mid,data,notes->db.addVital(mid,data,notes);refresh++})
 "lab"->LabDialog(members,{dialog=""},{mid,name,json->db.addLab(mid,name,json);refresh++})
 "medicine"->MedicineDialog(members,{dialog=""},{mid,n,d,s,e,no->db.addMedicine(mid,n,d,s,e,no);refresh++})}
}
@Composable private fun MetricCard(label:String,value:String,modifier:Modifier=Modifier){Card(modifier,colors=CardDefaults.cardColors(containerColor=Cream),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(15.dp)){Text(value,fontSize=24.sp,color=Forest,fontWeight=FontWeight.SemiBold);Text(label,color=Ink.copy(.65f),fontSize=12.sp)}}}
@Composable private fun ActionTile(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier=Modifier,onClick:()->Unit){Card(modifier.clickable{onClick()},colors=CardDefaults.cardColors(containerColor=Cream),shape=RoundedCornerShape(18.dp)){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Icon(icon,null,tint=Forest);Text(label,color=Ink,fontWeight=FontWeight.SemiBold)}}}
@Composable private fun SectionTitle(s:String){Text(s,color=Forest,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.4.sp)}
@Composable private fun RecordCard(title:String,subtitle:String){Card(colors=CardDefaults.cardColors(containerColor=Cream),shape=RoundedCornerShape(16.dp)){Column(Modifier.fillMaxWidth().padding(16.dp)){Text(title,color=Ink,fontWeight=FontWeight.SemiBold);Text(subtitle,color=Ink.copy(.62f),fontSize=13.sp)}}}
@Composable private fun ButtonRow(label:String,onClick:()->Unit){Button(onClick=onClick,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Forest)){Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text(label)}}
@Composable private fun FormField(label:String,value:String,onChange:(String)->Unit,numeric:Boolean=false){OutlinedTextField(value,onChange,label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=if(numeric)KeyboardType.Decimal else KeyboardType.Text))}
@Composable private fun PersonDialog(close:()->Unit,save:(List<String>)->Unit){var n by remember{mutableStateOf("")};var dob by remember{mutableStateOf("")};var sex by remember{mutableStateOf("M")};var blood by remember{mutableStateOf("")};var h by remember{mutableStateOf("")};AlertDialog(onDismissRequest=close,title={Text("Family member")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){FormField("Full name",n){n=it};FormField("Date of birth (YYYY-MM-DD)",dob){dob=it};Row{listOf("M","F","Other").forEach{FilterChip(sex==it,{sex=it},label={Text(it)})}};FormField("Blood group",blood){blood=it};FormField("Height (cm)",h,{h=it},true)}},confirmButton={TextButton(onClick={if(n.isNotBlank()){save(listOf(n,dob,sex,blood,h));close()}}){Text("Save")}},dismissButton={TextButton(onClick=close){Text("Cancel")}})}
@Composable private fun MemberPicker(members:List<List<String>>,selected:String,onPick:(String)->Unit){var expanded by remember{mutableStateOf(false)};Box{OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()){Text(members.firstOrNull{it[0]==selected}?.get(1)?: "Choose family member")};DropdownMenu(expanded,{expanded=false}){members.forEach{DropdownMenuItem(text={Text(it[1])},onClick={onPick(it[0]);expanded=false})}}}}
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
 FormField("Pulse · bpm",pulse,{pulse=it},true);FormField("Weight · kg",weight,{weight=it},true);FormField("Height · cm",height,{height=it},true);FormField("SpO₂ · %",spo,{spo=it},true);FormField("Temperature · °C",temp,{temp=it},true);FormField("Notes",note){note=it}
 if(result.isNotBlank())Text(result,color=Forest,fontSize=12.sp)
 }},confirmButton={TextButton(onClick={if(member.isNotBlank()){try{
 val o=JSONObject();listOf("left_systolic" to leftSys,"left_diastolic" to leftDia,"right_systolic" to rightSys,"right_diastolic" to rightDia,"pulse" to pulse,"weight_kg" to weight,"height_cm" to height,"spo2" to spo,"temp_c" to temp).forEach{(k,v)->v.toDoubleOrNull()?.let{o.put(k,it)}}
 val bp=Python.getInstance().getModule("observation_engine").callAttr("blood_pressure_comparison_json",hashMapOf("left_systolic" to (leftSys.toDoubleOrNull()?:0.0),"right_systolic" to (rightSys.toDoubleOrNull()?:0.0))).toString()
 val bpJson=JSONObject(bp);val diff=if(bpJson.optBoolean("available"))" · Inter-arm systolic difference ${bpJson.optString("systolic_difference")} mmHg" else ""
 save(member.toLong(),o,note,date,period);result="Saved $date$diff";close()
 }catch(e:Exception){result="Unable to save: ${e.message}"}}}){Text("Save vitals")}},dismissButton={TextButton(onClick=close){Text("Cancel")}})
}
@Composable private fun LabDialog(members:List<List<String>>,close:()->Unit,save:(Long,String,String)->Unit){var member by remember{mutableStateOf(members.firstOrNull()?.get(0)?:"")};var name by remember{mutableStateOf("Laboratory report")};var fields by remember{mutableStateOf(listOf("fasting_glucose","hba1c","total_cholesterol","hdl","triglycerides","creatinine","hemoglobin"))};var vals by remember{mutableStateOf(List(fields.size){""})}
 AlertDialog(onDismissRequest=close,title={Text("Lab results")},text={Column(Modifier.heightIn(max=520.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){if(members.isNotEmpty())MemberPicker(members,member){member=it}else Text("Add a family member first.");FormField("Report name",name){name=it};fields.forEachIndexed{i,f->FormField(f.replace('_',' '),vals[i],{v->vals=vals.toMutableList().also{it[i]=v}},true)}}},confirmButton={TextButton(onClick={if(member.isNotBlank()){try{val data=hashMapOf<String,Any>();fields.forEachIndexed{i,f->vals[i].toDoubleOrNull()?.let{data[f]=it}};val py=Python.getInstance().getModule("health_engine");val calculated=py.callAttr("calculate_labs_json",data).toString();save(member.toLong(),name,calculated);close()}catch(_:Exception){}}}){Text("Calculate & save")}},dismissButton={TextButton(onClick=close){Text("Cancel")}})}
@Composable private fun MedicineDialog(members:List<List<String>>,close:()->Unit,save:(Long,String,String,String,String,String)->Unit){var member by remember{mutableStateOf(members.firstOrNull()?.get(0)?:"")};var name by remember{mutableStateOf("")};var dose by remember{mutableStateOf("")};var start by remember{mutableStateOf("")};var end by remember{mutableStateOf("")};var notes by remember{mutableStateOf("")};AlertDialog(onDismissRequest=close,title={Text("Medicine")},text={Column(verticalArrangement=Arrangement.spacedBy(7.dp)){if(members.isNotEmpty())MemberPicker(members,member){member=it}else Text("Add a family member first.");FormField("Medicine name",name){name=it};FormField("Dose & schedule",dose){dose=it};FormField("Start date",start){start=it};FormField("End date (optional)",end){end=it};FormField("Notes",notes){notes=it}}},confirmButton={TextButton(onClick={if(member.isNotBlank()&&name.isNotBlank()){save(member.toLong(),name,dose,start,end,notes);close()}}){Text("Save")}},dismissButton={TextButton(onClick=close){Text("Cancel")}})}
