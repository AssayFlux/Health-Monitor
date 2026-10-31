package com.assayflux.healthmonitor

import android.content.ContentValues
import android.graphics.Bitmap
import net.zetetic.database.sqlcipher.SQLiteDatabase
import fi.iki.elonen.NanoHTTPD
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import org.json.JSONArray
import org.json.JSONObject
import java.net.NetworkInterface
import java.security.SecureRandom
import java.util.UUID
import android.util.Base64

object DeviceTransfer {
    private val tables = listOf("vitals","labs","medicines","clinical_tests","anthropometry","appointments","clinical_history","clinical_records","clinical_documents","reminders")
    data class Pending(val json: JSONObject, val senderId: String, val senderName: String, val count: Int)

    fun token(): String = ByteArray(24).also { SecureRandom().nextBytes(it) }
        .let { Base64.encodeToString(it, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING) }

    fun ip(): String {
        val nets = NetworkInterface.getNetworkInterfaces()
        while (nets.hasMoreElements()) {
            val net = nets.nextElement()
            if (!net.isUp || net.isLoopback) continue
            val addrs = net.inetAddresses
            while (addrs.hasMoreElements()) {
                val a = addrs.nextElement()
                if (a is java.net.Inet4Address && !a.isLoopbackAddress) return a.hostAddress ?: continue
            }
        }
        error("Connect both phones to the same Wi-Fi or hotspot first.")
    }

    fun qr(text: String): Bitmap {
        val matrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 640, 640)
        return Bitmap.createBitmap(640,640,Bitmap.Config.ARGB_8888).also { b ->
            for (x in 0 until 640) for (y in 0 until 640)
                b.setPixel(x,y,if(matrix[x,y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
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
                    for(i in 0 until c.columnCount) if(c.getColumnName(i)!="member_id") {
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
        tables.forEach { require((r.optJSONArray(it)?.length() ?: 0) <= 100000) { "Too many records in $it." } }
        val s=json.optJSONObject("sender") ?: JSONObject()
        return Pending(json,s.optString("id","unknown"),s.optString("name","Other device"),json.optInt("count"))
    }

    /** Atomic import into a brand-new member; existing profiles are never modified. */
    fun integrate(db: HealthDb, p: Pending): Long {
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
                            k=="logical_id" -> v.put(k,UUID.randomUUID().toString())
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
            db.recordTransfer("Received",p.senderId,p.senderName,"Integrated",p.count,"Imported as a separate member profile")
            sql.setTransactionSuccessful()
            return mid
        } finally { sql.endTransaction() }
    }

    class Server(token: String, private val onReceive:(Pending)->Unit): NanoHTTPD("0.0.0.0",0) {
        private val key=token
        @Volatile private var used=false
        override fun serve(session:IHTTPSession): Response {
            if(session.method!=Method.POST || session.uri!="/transfer") return newFixedLengthResponse(Response.Status.NOT_FOUND,"text/plain","Not found")
            if(used) return newFixedLengthResponse(Response.Status.GONE,"text/plain","Pairing expired")
            if(session.headers["authorization"]!="Bearer "+key) return newFixedLengthResponse(Response.Status.UNAUTHORIZED,"text/plain","Invalid pairing token")
            return try {
                val files=HashMap<String,String>(); session.parseBody(files)
                val body=files["postData"] ?: error("Empty transfer")
                require(body.length<=14000000) { "Transfer exceeds size limit." }
                val p=validate(JSONObject(body)); used=true; onReceive(p)
                newFixedLengthResponse(Response.Status.OK,"text/plain","Transfer received. Review it on the receiving phone.")
            } catch(e:Exception) { newFixedLengthResponse(Response.Status.BAD_REQUEST,"text/plain",e.message ?: "Invalid transfer") }
        }
    }

    fun send(url:String, token:String, payload:JSONObject) {
        val c=java.net.URL(url).openConnection() as java.net.HttpURLConnection
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