package com.assayflux.healthmonitor

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
 var pendingHistoryId by remember{mutableStateOf("")}
 var confirm by remember{mutableStateOf(false)}
 var url by remember{mutableStateOf("")};var token by remember{mutableStateOf("")};var encryptionSecret by remember{mutableStateOf("")};var receiverId by remember{mutableStateOf("paired-receiver")};var receiverName by remember{mutableStateOf("Receiving device")}
 var status by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)}
 var deleteId by remember{mutableStateOf<String?>(null)}
 DisposableEffect(server){onDispose{try{server?.stop()}catch(_:Exception){}}}
 LaunchedEffect(db) {
  withContext(Dispatchers.IO) { db.pendingTransfer() }?.let { saved ->
   try {
    val restored=DeviceTransfer.validate(JSONObject(saved.second))
    pendingHistoryId=saved.first
    pending=restored
    status="A previously received transfer is ready for review."
   } catch(e:Exception) {
    db.discardPendingTransfer(saved.first)
    db.updateTransferStatus(saved.first,"Invalid","Saved transfer could not be validated")
    status="A saved pending transfer was invalid and has been discarded."
   }
  }
 }
 val scanner=rememberLauncherForActivityResult(ScanContract()){ result->
  if(!result.contents.isNullOrBlank()) try {
   val data=JSONObject(result.contents);url=data.getString("url");token=data.getString("token");encryptionSecret=data.getString("secret");receiverId=data.optString("receiver","paired-receiver");receiverName=data.optString("receiverName","Receiving device")
   status="Paired with receiver. Choose a member to send."
  } catch(_:Exception){status="Invalid Health Monitor QR code."}
 }
 val cameraPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ granted ->
  if(granted) scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt("Scan receiver QR").setBeepEnabled(false))
  else status="Camera permission is required to scan a transfer QR."
 }
 Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
  Card(colors=CardDefaults.cardColors(containerColor=Forest)){Column(Modifier.fillMaxWidth().padding(14.dp)){
   Text("THIS DEVICE",color=Sage,fontSize=11.sp);Text(deviceId,color=androidx.compose.ui.graphics.Color.White)
   Text("App-generated ID, not an IMEI or authentication secret.",color=androidx.compose.ui.graphics.Color.White,fontSize=11.sp)
  }}
  Text("OFFLINE DEVICE TRANSFER",color=Forest,fontSize=11.sp)
  Text("Connect both phones to the same Wi-Fi or hotspot. Data travels directly over the local network, without cloud storage.",color=Ink.copy(.7f),fontSize=12.sp)
  Text("Transfer payloads are protected with AES-256-GCM. The encryption key is exchanged in the scanned QR and is not sent in the network request. Use a trusted private Wi-Fi or hotspot.",color=Clay,fontSize=12.sp)
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   Button(onClick={
    if(pending!=null){status="Integrate or discard the current received transfer before receiving another."}
    else try{
     server?.stop();val key=DeviceTransfer.token();val secret=DeviceTransfer.encryptionSecret()
     val s=DeviceTransfer.Server(key,secret){item->
      val historyId=db.recordTransfer("Received",item.senderId,item.senderName,"Awaiting review",item.count,"Received; awaiting your confirmation")
      db.savePendingTransfer(historyId,item.json.toString())
      scope.launch(Dispatchers.Main){pending=item;pendingHistoryId=historyId;onChanged();status="Transfer received. Review it below."}
     }
     s.start();server=s
     val address="http://"+DeviceTransfer.ip()+":"+s.listeningPort+"/transfer"
     qr=DeviceTransfer.qrBitmap(JSONObject().put("url",address).put("token",key).put("secret",secret).put("receiver",deviceId).put("receiverName",android.os.Build.MODEL?:"Android device").toString())
     status="Receiver ready. Scan this one-time QR from the sending phone."
    }catch(e:Exception){status=e.message?:"Could not start receiver."}
   },modifier=Modifier.weight(1f)){Text("Receive via QR")}
   OutlinedButton(onClick={if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt("Scan receiver QR").setBeepEnabled(false)) else cameraPermission.launch(Manifest.permission.CAMERA)},modifier=Modifier.weight(1f)){Text("Scan to send")}
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
     DropdownMenu(
      expanded = menu,
      onDismissRequest = { menu = false }
     ) {
      members.forEach { memberRow ->
       DropdownMenuItem(
        text = { Text(memberRow[1]) },
        onClick = {
         selected = memberRow[0]
         menu = false
        }
       )
      }
     }
    }
    Button(enabled=selected.isNotBlank()&&!busy,onClick={
     busy=true;status="Sending over local Wi-Fi…"
     scope.launch{try{
      val data=withContext(Dispatchers.IO){DeviceTransfer.export(db,selected.toLong(),deviceId,android.os.Build.MODEL?:"Android")}
      withContext(Dispatchers.IO){DeviceTransfer.send(url,token,encryptionSecret,data)}
      db.recordTransfer("Shared",receiverId,receiverName,"Sent",data.optInt("count"),"Member: "+data.getJSONObject("member").optString("name"))
      status="Sent securely. The receiving phone must confirm integration.";url="";token="";encryptionSecret="";onChanged()
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
    TextButton(onClick={db.updateTransferStatus(pendingHistoryId,"Discarded","Transfer discarded before import");db.discardPendingTransfer(pendingHistoryId);pending=null;pendingHistoryId="";status="Transfer discarded; nothing imported.";onChanged()}){Text("Discard")}
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
  text={Text("This imports the received records into a new profile and does not overwrite or merge existing members. The transfer payload is encrypted with AES-256-GCM before it leaves this device.")},
  confirmButton={TextButton(onClick={scope.launch{try{val item=pending ?: error("No pending transfer to integrate.");val historyId=pendingHistoryId;withContext(Dispatchers.IO){DeviceTransfer.integrate(db,item,historyId)};pending=null;pendingHistoryId="";confirm=false;status="Imported as a separate member profile.";onChanged()}catch(e:Exception){confirm=false;status="Import failed: "+(e.message?:"database error")}}}){Text("Integrate")}},
  dismissButton={TextButton(onClick={confirm=false}){Text("Cancel")}})
 deleteId?.let{id->AlertDialog(onDismissRequest={deleteId=null},title={Text("Delete history entry?")},text={Text("Only history is deleted; imported health data remains.")},confirmButton={TextButton(onClick={db.deleteTransfer(id);db.discardPendingTransfer(id);if(pendingHistoryId==id){pending=null;pendingHistoryId=""};deleteId=null;onChanged()}){Text("Delete")}},dismissButton={TextButton(onClick={deleteId=null}){Text("Cancel")}})}
}
