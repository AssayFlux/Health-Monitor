package com.assayflux.healthmonitor

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

private fun stableDeviceId(context:Context):String {
 val p=context.getSharedPreferences("device_identity_v1",Context.MODE_PRIVATE)
 p.getString("device_id",null)?.let{return it}
 val id="HM-"+UUID.randomUUID().toString().uppercase()
 check(p.edit().putString("device_id",id).commit());return id
}

@Composable
fun ReceivedSharedScreen(db:HealthDb,refreshToken:Int,onChanged:()->Unit) {
 val context=LocalContext.current;val scope= rememberCoroutineScope()
 val deviceId= remember{stableDeviceId(context)}
 val history=remember(refreshToken){db.transferHistory()}
 val members=remember(refreshToken){db.members()}
 var filter by remember{mutableStateOf("All")}
 var selected by remember{mutableStateOf(members.firstOrNull()?.get(0).orEmpty())}
 var menu by remember{mutableStateOf(false)}
 var server by remember{mutableStateOf<DeviceTransfer.Server?>(null)}
 var qr by remember{mutableStateOf<android.graphics.Bitmap?>(null)}
 var pending by remember{mutableStateOf<DeviceTransfer.Pending?>(null)}
 var confirm by remember{mutableStateOf(false)}
 var url by remember{mutableStateOf("")};var token by remember{mutableStateOf("")};var receiverId by remember{mutableStateOf("paired-receiver")};var receiverName by remember{mutableStateOf("Receiving device")}
 var status by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)}
 var deleteId by remember{mutableStateOf<String?>(null)}
 DisposableEffect(server){onDispose{try{server?.stop()}catch(_:Exception){}}}
 val scanner=rememberLauncherForActivityResult(ScanContract()){ result->
  if(!result.contents.isNullOrBlank()) try {
   val data=JSONObject(result.contents);url=data.getString("url");token=data.getString("token");receiverId=data.optString("receiver","paired-receiver");receiverName=data.optString("receiverName","Receiving device")
   status="Paired with receiver. Choose a member to send."
  } catch(_:Exception){status="Invalid Health Monitor QR code."}
 }
 Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
  Card(colors=CardDefaults.cardColors(containerColor=Forest)){Column(Modifier.fillMaxWidth().padding(14.dp)){
   Text("THIS DEVICE",color=Sage,fontSize=11.sp);Text(deviceId,color=androidx.compose.ui.graphics.Color.White)
   Text("App-generated ID, not an IMEI or authentication secret.",color=androidx.compose.ui.graphics.Color.White,fontSize=11.sp)
  }}
  Text("OFFLINE DEVICE TRANSFER",color=Forest,fontSize=11.sp)
  Text("Connect both phones to the same Wi-Fi or hotspot. Data travels directly over the local network, without cloud storage.",color=Ink.copy(.7f),fontSize=12.sp)
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   Button(onClick={
    try{
     server?.stop();val key=DeviceTransfer.token()
     val s=DeviceTransfer.Server(key){item->scope.launch(Dispatchers.Main){pending=item;status="Transfer received. Review it below."}}
     s.start();server=s
     val address="http://"+DeviceTransfer.ip()+":"+s.listeningPort+"/transfer"
     qr=DeviceTransfer.qrBitmap(JSONObject().put("url",address).put("token",key).put("receiver",deviceId).put("receiverName",android.os.Build.MODEL?:"Android device").toString())
     status="Receiver ready. Scan this one-time QR from the sending phone."
    }catch(e:Exception){status=e.message?:"Could not start receiver."}
   },modifier=Modifier.weight(1f)){Text("Receive via QR")}
   OutlinedButton(onClick={scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt("Scan receiver QR").setBeepEnabled(false))},modifier=Modifier.weight(1f)){Text("Scan to send")}
  }
  qr?.let{bitmap->Card(colors=CardDefaults.cardColors(containerColor=androidx.compose.ui.graphics.Color.White)){
   Column(Modifier.fillMaxWidth().padding(10.dp),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally){
    Image(bitmap.asImageBitmap(),null,Modifier.size(210.dp))
    Text("Scan from the sending phone",color=Ink)
    TextButton(onClick={server?.stop();server=null;qr=null}){Text("Stop receiving")}
   }
  }}
  if(url.isNotBlank()) Card(colors=CardDefaults.cardColors(containerColor=Cream)){
   Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
    Text("SEND MEMBER DATA",color=Forest)
    Box{OutlinedButton(onClick={menu=true},modifier=Modifier.fillMaxWidth()){Text(members.firstOrNull{it[0]==selected}?.get(1)?:"Choose member")}
     DropdownMenu(expanded=menu,onDismissRequest={menu=false}){members.forEach{m->DropdownMenuItem(text={Text(m[1])},onClick={selected=m[0];menu=false)}}}
    }
    Button(enabled=selected.isNotBlank()&&!busy,onClick={
     busy=true;status="Sending over local Wi-Fi…"
     scope.launch{try{
      val data=withContext(Dispatchers.IO){DeviceTransfer.export(db,selected.toLong(),deviceId,android.os.Build.MODEL?:"Android")}
      withContext(Dispatchers.IO){DeviceTransfer.send(url,token,data)}
      db.recordTransfer("Shared",receiverId,receiverName,"Sent",data.optInt("count"),"Member: "+data.getJSONObject("member").optString("name"))
      status="Sent. The receiving phone must confirm integration.";url="";token="";onChanged()
     }catch(e:Exception){status=e.message?:"Transfer failed."}finally{busy=false}}
    },modifier=Modifier.fillMaxWidth()){Text(if(busy)"Sending…" else "Send selected member")}
   }
  }
  pending?.let{p->Card(colors=CardDefaults.cardColors(containerColor=Cream)){
   Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
    Text("REVIEW RECEIVED DATA",color=Forest)
    Text("Member: "+p.json.getJSONObject("member").optString("name"),color=Ink)
    Text("Sender: "+p.senderName+" · "+p.senderId,color=Ink,fontSize=12.sp)
    Text("Records: "+p.count,color=Ink,fontSize=12.sp)
    Text("Integrate creates a new member profile; it does not alter existing profiles.",color=Ink,fontSize=12.sp)
    Button(onClick={confirm=true},modifier=Modifier.fillMaxWidth()){Text("Integrate as new member")}
    TextButton(onClick={pending=null;status="Transfer discarded; nothing imported."}){Text("Discard")}
   }
  }}
  if(status.isNotBlank())Text(status,color=Forest,fontSize=12.sp)
  Divider();Text("RECEIVED & SHARED HISTORY",color=Forest,fontSize=11.sp)
  Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("All","Received","Shared").forEach{v->FilterChip(filter==v,{filter=v},label={Text(v)})}}
  LazyColumn(verticalArrangement=Arrangement.spacedBy(7.dp)){
   items(history.filter{filter=="All"||it[1].equals(filter,true)},key={it[0]}){r->Card(colors=CardDefaults.cardColors(containerColor=Cream)){
    Column(Modifier.padding(11.dp)){Text(r[1]+" · "+r[3],color=Ink);Text(r[2]+" · "+r[4]+" · "+r[5]+" · "+r[6]+" records",color=Ink.copy(.7f),fontSize=11.sp);TextButton(onClick={deleteId=r[0]}){Text("Delete history entry")}}
   }}
   if(history.isEmpty())item{Text("No transfers yet.",color=Ink.copy(.7f))}
  }
 }
 if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Integrate as a separate member?")},
  text={Text("This imports the received records into a new profile and does not overwrite or merge existing members. Transfer payload is plain local-network data, as requested.")},
  confirmButton={TextButton(onClick={try{pending?.let{DeviceTransfer.integrate(db,it)};pending=null;confirm=false;status="Imported as a separate member profile.";onChanged()}catch(e:Exception){confirm=false;status="Import failed: "+(e.message?:"database error")}}){Text("Integrate")}},
  dismissButton={TextButton(onClick={confirm=false}){Text("Cancel")}})
 deleteId?.let{id->AlertDialog(onDismissRequest={deleteId=null},title={Text("Delete history entry?")},text={Text("Only history is deleted; imported health data remains.")},confirmButton={TextButton(onClick={db.deleteTransfer(id);deleteId=null;onChanged()}){Text("Delete")}},dismissButton={TextButton(onClick={deleteId=null}){Text("Cancel")}})}
}
