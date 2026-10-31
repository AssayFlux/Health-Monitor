package com.assayflux.healthmonitor

import android.content.ContentValues
import fi.iki.elonen.NanoHTTPD
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import org.json.JSONArray
import org.json.JSONObject
import java.net.NetworkInterface
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import android.util.Base64

object DeviceTransfer {
    private val tables = listOf("vitals","labs","medicines","clinical_tests","anthropometry","appointments","clinical_history","clinical_records","clinical_documents","reminders")
    data class Pending(val json: JSONObject, val senderId: String, val senderName: String, val count: Int)

    fun token(): String = ByteArray(24).also { SecureRandom().nextBytes(it) }
        .let { Base64.encodeToString(it, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING) }

    fun ip(): String {
        val candidates=mutableListOf<Pair<Int,String>>()
        val nets=NetworkInterface.getNetworkInterfaces()
        while(nets.hasMoreElements()) {
            val net=nets.nextElement()
            if(!net.isUp || net.isLoopback) continue
            val name=net.name.lowercase()
            val priority=if(name.contains("wlan") || name.contains("wifi") || name.contains("ap") || name.contains("p2p")) 0 else 1
            val addrs=net.inetAddresses
            while(addrs.hasMoreElements()) {
                val address=addrs.nextElement()
                if(address is java.net.Inet4Address && address.isSiteLocalAddress && !address.isLoopbackAddress) {
                    address.hostAddress?.let { candidates.add(priority to it) }
                }
            }
        }
        return candidates.sortedBy { it.first }.firstOrNull()?.second
            ?: error("Connect both phones to the same Wi-Fi or hotspot first.")
    }

    fun export(db: HealthDb, memberId: Long, deviceId: String, deviceName: String): JSONObject {
        val member = db.readableDatabase.rawQuery("SELECT name,dob,sex,blood,height,notes FROM members WHERE id=?",arrayOf(memberId.toString())).use { c ->
            require(c.moveToFirst()) { "Selected member not found." }
            JSONObject().put("name",c.getString(0)).put("dob",c.getString(1)).put("sex",c.getString(2))
                .put("blood",c.getString(3)).put("height",if(c.isNull(4)) JSONObject.NULL else c.getString(4))
                .put("notes",c.getString(5))
        }
        val records=JSONObject(); var count=0
        tables.forEach { table ->
            val arr=JSONArray()
            db.readableDatabase.rawQuery("SELECT * FROM $table WHERE member_id=?",arrayOf(memberId.toString())).use { c ->
                while(c.moveToNext()) {
                    val row=JSONObject()
                    for(i in 0 until c.columnCount) if(c.getColumnName(i)!="member_id" && !(table=="clinical_documents" && c.getColumnName(i)=="content_uri")) {
                        if(c.isNull(i)) row.put(c.getColumnName(i),JSONObject.NULL) else row.put(c.getColumnName(i),c.getString(i))
                    }
                    arr.put(row); count++
                }
            }
            records.put(table,arr)
        }
        return JSONObject().put("format","health-monitor-local-transfer").put("version",1)
            .put("sender",JSONObject().put("id",deviceId).put("name",deviceName))
            .put("member",member).put("records",records).put("count",count)
    }

    fun validate(json: JSONObject): Pending {
        require(json.optString("format")=="health-monitor-local-transfer" && json.optInt("version")==1) { "Unsupported transfer format." }
        val m=json.optJSONObject("member") ?: error("Member details missing.")
        require(m.optString("name").isNotBlank()) { "Member name is missing." }
        val r=json.optJSONObject("records") ?: error("Health records missing.")
        var total=0
        tables.forEach { table ->
            val n=r.optJSONArray(table)?.length() ?: 0
            require(n<=100000) { "Too many records in $table." }
            total+=n
        }
        val s=json.optJSONObject("sender") ?: JSONObject()
        return Pending(json,s.optString("id","unknown"),s.optString("name","Other device"),total)
    }

    /** Atomic import into a brand-new member; existing profiles are never modified. */
    fun integrate(db: HealthDb, p: Pending, historyId: String): Long {
        val sql=db.writableDatabase
        sql.beginTransaction()
        try {
            val m=p.json.getJSONObject("member")
            val mid=sql.insertOrThrow("members",null,ContentValues().apply {
                put("name",m.optString("name")); put("dob",m.optString("dob")); put("sex",m.optString("sex"))
                put("blood",m.optString("blood")); put("height",m.optString("height").toDoubleOrNull())
                put("notes",(m.optString("notes")+"\nImported from "+p.senderName+" ("+p.senderId+")").trim())
            })
            val data=p.json.getJSONObject("records")
            val maps=mapOf("medicines" to mutableMapOf<String,String>(),"appointments" to mutableMapOf<String,String>())
            (tables.filter { it!="reminders" }).forEach { table ->
                val arr=data.optJSONArray(table) ?: JSONArray()
                for(i in 0 until arr.length()) {
                    val old=arr.getJSONObject(i); val v=ContentValues(); val oldId=old.optString("id")
                    val keys=old.keys()
                    while(keys.hasNext()) {
                        val k=keys.next(); if(k=="id") continue
                        if(table=="clinical_documents" && k=="content_uri") continue
                        val value=old.opt(k)
                        when {
                            value==null || value==JSONObject.NULL -> v.putNull(k)
                            k=="logical_id" || k=="document_id" -> v.put(k,UUID.randomUUID().toString())
                            value is Number -> v.put(k,value.toString())
                            else -> v.put(k,value.toString())
                        }
                    }
                    v.put("member_id",mid)
                    if(table=="clinical_documents") v.putNull("content_uri")
                    if(table=="clinical_records") v.put("source_reference","device-transfer:"+UUID.randomUUID())
                    val newId=sql.insertOrThrow(table,null,v)
                    if(maps.containsKey(table) && oldId.isNotBlank()) maps[table]!![oldId]=newId.toString()
                }
            }
            val reminders=data.optJSONArray("reminders") ?: JSONArray()
            for(i in 0 until reminders.length()) {
                val old=reminders.getJSONObject(i); val v=ContentValues(); val keys=old.keys()
                while(keys.hasNext()) {
                    val k=keys.next(); if(k=="id") continue
                    val value=old.opt(k)
                    when {
                        value==null || value==JSONObject.NULL -> v.putNull(k)
                        k=="medicine_id" -> v.put(k,maps["medicines"]?.get(value.toString())?.toLongOrNull())
                        k=="appointment_id" -> v.put(k,maps["appointments"]?.get(value.toString())?.toLongOrNull())
                        else -> v.put(k,value.toString())
                    }
                }
                v.put("id",UUID.randomUUID().toString()); v.put("member_id",mid)
                sql.insertOrThrow("reminders",null,v)
            }
            db.completeTransferImport(historyId,"Imported as a separate member profile")
            sql.setTransactionSuccessful()
            return mid
        } finally { sql.endTransaction() }
    }


    fun qrBitmap(text: String): android.graphics.Bitmap {
        val matrix=com.google.zxing.MultiFormatWriter().encode(text,com.google.zxing.BarcodeFormat.QR_CODE,640,640)
        return android.graphics.Bitmap.createBitmap(640,640,android.graphics.Bitmap.Config.ARGB_8888).also { b ->
            for(x in 0 until 640) for(y in 0 until 640)
                b.setPixel(x,y,if(matrix[x,y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }

    class Server(token: String, private val onReceive:(Pending)->Unit): NanoHTTPD("0.0.0.0",0) {
        private val key=token
        private val expiresAt=System.currentTimeMillis()+5*60*1000
        private val used=AtomicBoolean(false)
        override fun serve(session:IHTTPSession): Response {
            if(session.method!=Method.POST || session.uri!="/transfer") return newFixedLengthResponse(Response.Status.NOT_FOUND,"text/plain","Not found")
            if(System.currentTimeMillis()>expiresAt) return newFixedLengthResponse(Response.Status.GONE,"text/plain","Pairing expired")
            if(used.get()) return newFixedLengthResponse(Response.Status.GONE,"text/plain","Pairing expired")
            if(session.headers["authorization"]!="Bearer "+key) return newFixedLengthResponse(Response.Status.UNAUTHORIZED,"text/plain","Invalid pairing token")
            return try {
                val declaredLength=session.headers["content-length"]?.toLongOrNull() ?: 0L
                require(declaredLength in 1..MAX_TRANSFER_BYTES) { "Missing or oversized transfer body." }
                val files=HashMap<String,String>(); session.parseBody(files)
                val body=files["postData"] ?: error("Empty transfer")
                require(body.toByteArray(Charsets.UTF_8).size<=MAX_TRANSFER_BYTES) { "Transfer exceeds size limit." }
                val p=validate(decryptPayload(JSONObject(body), encryptionSecret))
                if(!used.compareAndSet(false,true)) return newFixedLengthResponse(Response.Status.GONE,"text/plain","Pairing already used")
                onReceive(p)
                newFixedLengthResponse(Response.Status.OK,"text/plain","Transfer received. Review it on the receiving phone.")
            } catch(e:Exception) { newFixedLengthResponse(Response.Status.BAD_REQUEST,"text/plain",e.message ?: "Invalid transfer") }
        }
    }

    fun send(url:String, token:String, encryptionSecret:String, payload:JSONObject) {
        val endpoint=java.net.URL(url)
        require(endpoint.protocol=="http" && endpoint.path=="/transfer" && endpoint.userInfo==null && endpoint.query==null && endpoint.ref==null && endpoint.port in 1024..65535) { "QR destination is not a valid local transfer endpoint." }
        require(endpoint.host.matches(Regex("\\d{1,3}(\\.\\d{1,3}){3}"))) { "Transfer QR must contain a local IPv4 address." }
        val destination=java.net.InetAddress.getByName(endpoint.host)
        require(destination is java.net.Inet4Address && destination.isSiteLocalAddress && !destination.isLoopbackAddress) { "Transfer destination must be on a private local network." }
        val c=endpoint.openConnection() as java.net.HttpURLConnection
        try {
            c.requestMethod="POST"; c.connectTimeout=12000; c.readTimeout=30000; c.doOutput=true
            c.setRequestProperty("Content-Type","application/json"); c.setRequestProperty("Authorization","Bearer "+token)
            val bytes=payload.toString().toByteArray(Charsets.UTF_8)
            require(bytes.size<=14000000) { "Transfer too large for one local session." }
            c.setFixedLengthStreamingMode(bytes.size); c.outputStream.use { it.write(bytes) }
            check(c.responseCode in 200..299) { "Receiver rejected the transfer: "+(c.errorStream?.bufferedReader()?.use{it.readText()} ?: c.responseCode.toString()) }
        } finally { c.disconnect() }
    }
}