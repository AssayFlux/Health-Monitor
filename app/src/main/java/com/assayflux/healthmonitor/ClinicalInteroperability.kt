package com.assayflux.healthmonitor

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import net.zetetic.database.sqlcipher.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

/**
 * Stage 2 local interoperability layer.
 *
 * The normalized table is the canonical exchange projection. Existing app tables remain
 * the operational source of truth until a later migration deliberately consolidates them.
 * Imported content is staged as unverified and is never promoted without explicit review.
 */
object ClinicalInteroperability {
    const val MODEL_VERSION = 1
    private const val FHIR_VERSION = "4.0.1"

    data class TerminologyConcept(val system: String, val code: String, val display: String)
    data class ImportCandidate(
        val resourceType: String,
        val externalId: String,
        val display: String,
        val status: String,
        val codeSystem: String,
        val code: String,
        val effectiveStart: String,
        val effectiveEnd: String,
        val payload: String
    )
    data class DocumentMetadata(
        val id: String, val displayName: String, val mimeType: String,
        val sizeBytes: Long, val extractedText: String, val reviewState: String
    )

    fun ensureSchema(db: HealthDb) {
        val sql = db.writableDatabase
        sql.beginTransaction()
        try {
            sql.execSQL("CREATE TABLE IF NOT EXISTS clinical_model_meta(model_key TEXT PRIMARY KEY, model_version INTEGER NOT NULL)")
            sql.execSQL("INSERT OR REPLACE INTO clinical_model_meta(model_key,model_version) VALUES('normalized_clinical_model',$MODEL_VERSION)")
            sql.execSQL("""CREATE TABLE IF NOT EXISTS clinical_records(
                logical_id TEXT PRIMARY KEY, member_id INTEGER NOT NULL, resource_type TEXT NOT NULL,
                code_system TEXT, code TEXT, code_display TEXT, status TEXT, effective_start TEXT,
                effective_end TEXT, value_numeric REAL, value_text TEXT, unit_code TEXT,
                unit_system TEXT, source_system TEXT NOT NULL, source_reference TEXT,
                verification_status TEXT NOT NULL DEFAULT 'unconfirmed',
                recorded_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)""")
            sql.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_clinical_source ON clinical_records(source_system,source_reference) WHERE source_reference IS NOT NULL")
            sql.execSQL("""CREATE TABLE IF NOT EXISTS clinical_documents(
                document_id TEXT PRIMARY KEY, member_id INTEGER NOT NULL, display_name TEXT NOT NULL,
                mime_type TEXT, size_bytes INTEGER NOT NULL DEFAULT 0, content_uri TEXT,
                extracted_text TEXT, extraction_engine TEXT, review_state TEXT NOT NULL DEFAULT 'needs_review',
                source_hash TEXT, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)""")
            sql.execSQL("""CREATE TABLE IF NOT EXISTS interop_import_staging(
                staging_id TEXT PRIMARY KEY, bundle_id TEXT, resource_type TEXT NOT NULL,
                external_id TEXT, display TEXT NOT NULL, status TEXT, code_system TEXT, code TEXT,
                effective_start TEXT, effective_end TEXT, raw_json TEXT NOT NULL,
                review_state TEXT NOT NULL DEFAULT 'pending', target_member_id INTEGER,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)""")
            sql.execSQL("""CREATE TABLE IF NOT EXISTS terminology_mappings(
                source_system TEXT NOT NULL, source_code TEXT NOT NULL, target_system TEXT NOT NULL,
                target_code TEXT NOT NULL, target_display TEXT NOT NULL, jurisdiction TEXT,
                mapping_status TEXT NOT NULL DEFAULT 'candidate',
                PRIMARY KEY(source_system,source_code,target_system,jurisdiction))""")
            backfillLegacy(sql)
            sql.setTransactionSuccessful()
        } finally { sql.endTransaction() }
    }

    private fun backfillLegacy(db: SQLiteDatabase) {
        fun insert(type: String, member: String, display: String, date: String, source: String, status: String = "unknown") {
            val id = stableId("legacy:$source")
            val values = ContentValues().apply {
                put("logical_id", id); put("member_id", member.toLong()); put("resource_type", type)
                put("code_display", display); put("effective_start", date); put("status", status)
                put("source_system", "urn:assayflux:health-monitor:legacy")
                put("source_reference", source); put("verification_status", "legacy_unreviewed")
            }
            db.insertWithOnConflict("clinical_records", null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
        db.rawQuery("SELECT id,member_id,COALESCE(name,''),COALESCE(date(recorded_at),'') FROM labs", null).use { c ->
            while (c.moveToNext()) insert("DiagnosticReport", c.getString(1), c.getString(2), c.getString(3), "labs/${c.getString(0)}")
        }
        db.rawQuery("SELECT id,member_id,category,display,COALESCE(onset_date,''),COALESCE(status,'unknown') FROM clinical_history", null).use { c ->
            while (c.moveToNext()) {
                val type = when(c.getString(2)) {
                    "condition" -> "Condition"; "allergy" -> "AllergyIntolerance"; "procedure" -> "Procedure"
                    "immunisation" -> "Immunization"; "encounter" -> "Encounter"; else -> "Condition"
                }
                insert(type, c.getString(1), c.getString(3), c.getString(4), "clinical_history/${c.getString(0)}", c.getString(5))
            }
        }
        db.rawQuery("SELECT id,member_id,name,COALESCE(start_date,'') FROM medicines", null).use { c ->
            while (c.moveToNext()) insert("MedicationStatement", c.getString(1), c.getString(2), c.getString(3), "medicines/${c.getString(0)}")
        }
        db.rawQuery("SELECT id,member_id,COALESCE(measurement_date,''),COALESCE(notes,'') FROM vitals", null).use { c ->
            while (c.moveToNext()) insert("Observation", c.getString(1), "Vital signs " + c.getString(3), c.getString(2), "vitals/${c.getString(0)}")
        }
    }

    fun stableId(value: String): String = UUID.nameUUIDFromBytes(value.toByteArray(Charsets.UTF_8)).toString()

    /** Minimal, explicit terminology adapter. Unknown codes are retained, never guessed. */
    object Terminology {
        const val LOINC = "http://loinc.org"
        const val SNOMED = "http://snomed.info/sct"
        const val UCUM = "http://unitsofmeasure.org"
        const val ICD10 = "http://hl7.org/fhir/sid/icd-10"
        const val ICD10CM = "http://hl7.org/fhir/sid/icd-10-cm"

        fun knownSystem(value: String): Boolean = value in setOf(LOINC, SNOMED, UCUM, ICD10, ICD10CM)
        fun normalizeUnit(unit: String): TerminologyConcept? {
            val map = mapOf("kg" to "kg", "g" to "g", "cm" to "cm", "mm" to "mm",
                "mmHg" to "mm[Hg]", "bpm" to "/min", "%" to "%", "Cel" to "Cel",
                "°C" to "Cel", "mg/dL" to "mg/dL", "mmol/L" to "mmol/L", "mL" to "mL")
            val code = map[unit.trim()] ?: return null
            return TerminologyConcept(UCUM, code, unit.trim())
        }

        fun candidateMappings(code: String, sourceSystem: String, jurisdiction: String): List<TerminologyConcept> {
            // No cross-system equivalence is inferred. Add reviewed mappings to terminology_mappings.
            if (code.isBlank() || sourceSystem.isBlank() || jurisdiction.isBlank()) return emptyList()
            return emptyList()
        }
    }

    fun exportFhirR4(db: HealthDb, memberId: Long): String {
        val sql = db.readableDatabase
        val patient = sql.rawQuery("SELECT id,name,dob,sex FROM members WHERE id=?", arrayOf(memberId.toString())).use { c ->
            if (!c.moveToFirst()) throw IllegalArgumentException("Family member not found")
            JSONObject().put("resourceType", "Patient")
                .put("id", stableId("member:$memberId"))
                .put("identifier", JSONArray().put(JSONObject().put("system", "urn:assayflux:health-monitor:member").put("value", memberId.toString())))
                .put("name", JSONArray().put(JSONObject().put("text", c.getString(1))))
                .put("birthDate", c.getString(2).takeIf { !c.isNull(2) && it.isNotBlank() })
                .put("gender", fhirGender(c.getString(3)))
        }
        val entries = JSONArray().put(JSONObject().put("fullUrl", "urn:uuid:${patient.getString("id")}").put("resource", patient))
        sql.rawQuery("SELECT logical_id,resource_type,code_system,code,code_display,status,effective_start,effective_end,value_numeric,value_text,unit_code,unit_system,verification_status FROM clinical_records WHERE member_id=? ORDER BY recorded_at", arrayOf(memberId.toString())).use { c ->
            while (c.moveToNext()) {
                val resource = recordToFhir(c, patient.getString("id"))
                if (resource != null) entries.put(JSONObject().put("fullUrl", "urn:uuid:${resource.getString("id")}").put("resource", resource))
            }
        }
        return JSONObject().put("resourceType", "Bundle").put("id", UUID.randomUUID().toString())
            .put("meta", JSONObject().put("profile", JSONArray().put("http://hl7.org/fhir/StructureDefinition/Bundle")))
            .put("type", "collection").put("timestamp", java.time.Instant.now().toString())
            .put("entry", entries).put("fhirVersion", FHIR_VERSION).toString(2)
    }

    private fun fhirGender(value: String): String = when(value.lowercase()) {
        "male", "m" -> "male"; "female", "f" -> "female"; "other" -> "other"; else -> "unknown"
    }

    private fun recordToFhir(c: android.database.Cursor, patientId: String): JSONObject? {
        val id=c.getString(0); val type=c.getString(1)
        val system=c.getString(2); val code=c.getString(3); val display=c.getString(4)
        val status=c.getString(5); val start=c.getString(6); val end=c.getString(7)
        val num=if(c.isNull(8)) null else c.getDouble(8); val text=if(c.isNull(9)) null else c.getString(9)
        val unit=c.getString(10); val unitSystem=c.getString(11)
        val verification=c.getString(12)
        val r=JSONObject().put("resourceType",type).put("id",id)
        r.put("subject",JSONObject().put("reference","Patient/$patientId"))
        val coding=JSONArray()
        if (!system.isNullOrBlank() && !code.isNullOrBlank()) coding.put(JSONObject().put("system",system).put("code",code).put("display",display))
        val concept=JSONObject().put("text",display ?: "")
        if(coding.length()>0) concept.put("coding",coding)
        when(type) {
            "Condition" -> { r.put("clinicalStatus",codeable("http://terminology.hl7.org/CodeSystem/condition-clinical",status ?: "unknown")); r.put("code",concept); if(!start.isNullOrBlank())r.put("onsetDateTime",start);if(!end.isNullOrBlank())r.put("abatementDateTime",end) }
            "AllergyIntolerance" -> { r.put("clinicalStatus",codeable("http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical",status ?: "active"));r.put("code",concept);r.put("patient",JSONObject().put("reference","Patient/$patientId")) }
            "Procedure" -> { r.put("status",status ?: "unknown");r.put("code",concept);if(!start.isNullOrBlank())r.put("performedDateTime",start) }
            "Immunization" -> { r.put("status",status ?: "completed");r.put("vaccineCode",concept);r.put("patient",JSONObject().put("reference","Patient/$patientId"));if(!start.isNullOrBlank())r.put("occurrenceDateTime",start) }
            "Encounter" -> { r.put("status",status ?: "finished");r.put("class",JSONObject().put("system","http://terminology.hl7.org/CodeSystem/v3-ActCode").put("code","AMB"));r.put("type",JSONArray().put(concept));if(!start.isNullOrBlank())r.put("period",JSONObject().put("start",start).put("end",end)) }
            "MedicationStatement", "MedicationRequest" -> { r.put("status",status ?: "unknown");r.put("medicationCodeableConcept",concept);r.put("subject",JSONObject().put("reference","Patient/$patientId")) }
            "DiagnosticReport" -> { r.put("status",status ?: "unknown");r.put("code",concept);r.put("subject",JSONObject().put("reference","Patient/$patientId"));if(!start.isNullOrBlank())r.put("effectiveDateTime",start) }
            "Observation" -> { r.put("status",status ?: "final");r.put("code",concept);r.put("effectiveDateTime",start ?: "");if(num!=null)r.put("valueQuantity",JSONObject().put("value",num).put("unit",unit ?: "").put("system",unitSystem ?: Terminology.UCUM).put("code",unit ?: "")) else if(text!=null)r.put("valueString",text) }
            else -> return null
        }
        if(verification=="legacy_unreviewed") r.put("extension",JSONArray().put(JSONObject()
            .put("url","urn:assayflux:health-monitor:verification-status")
            .put("valueCode","legacy-unreviewed")))
        return r
    }

    private fun codeable(system: String, code: String) =
        JSONObject().put("coding",JSONArray().put(JSONObject().put("system",system).put("code",code)))

    /** Parses a FHIR R4 Bundle into a review queue. It does not write clinical_records. */
    fun stageFhirBundle(db: HealthDb, json: String): List<ImportCandidate> {
        val root=JSONObject(json)
        require(root.optString("resourceType")=="Bundle") { "Expected a FHIR R4 Bundle" }
        val entries=root.optJSONArray("entry") ?: JSONArray()
        require(entries.length() in 1..5000) { "Bundle must contain between 1 and 5000 entries" }
        val out=mutableListOf<ImportCandidate>()
        for(i in 0 until entries.length()) {
            val resource=entries.optJSONObject(i)?.optJSONObject("resource") ?: continue
            val type=resource.optString("resourceType")
            if(type=="Patient") continue
            val allowed=setOf("Observation","DiagnosticReport","MedicationStatement","MedicationRequest","Condition","AllergyIntolerance","Procedure","Immunization","Encounter")
            if(type !in allowed) continue
            val display=extractDisplay(resource,type)
            if(display.isBlank()) continue
            val codeable=resource.optJSONObject("code") ?: resource.optJSONObject("vaccineCode")
                ?: resource.optJSONObject("medicationCodeableConcept")
            val coding=codeable?.optJSONArray("coding")?.optJSONObject(0)
            val period=resource.optJSONObject("period")
            val effective=resource.optString("effectiveDateTime").ifBlank { resource.optString("onsetDateTime") }
                .ifBlank { resource.optString("occurrenceDateTime") }.ifBlank { period?.optString("start") ?: "" }
            val end=resource.optString("abatementDateTime").ifBlank { period?.optString("end") ?: "" }
            val item=ImportCandidate(type,resource.optString("id"),display,resource.optString("status"),
                coding?.optString("system") ?: "",coding?.optString("code") ?: "",effective,end,resource.toString())
            val sid=UUID.randomUUID().toString()
            db.writableDatabase.insert("interop_import_staging",null,ContentValues().apply {
                put("staging_id",sid);put("bundle_id",root.optString("id"));put("resource_type",item.resourceType)
                put("external_id",item.externalId);put("display",item.display);put("status",item.status)
                put("code_system",item.codeSystem);put("code",item.code);put("effective_start",item.effectiveStart)
                put("effective_end",item.effectiveEnd);put("raw_json",item.payload);put("review_state","pending")
            })
            out.add(item)
        }
        return out
    }

    private fun extractDisplay(r: JSONObject,type:String):String {
        val keys=when(type) {
            "AllergyIntolerance" -> listOf("code"); "Immunization" -> listOf("vaccineCode")
            "MedicationStatement","MedicationRequest" -> listOf("medicationCodeableConcept")
            else -> listOf("code")
        }
        for(k in keys) {
            val obj=r.optJSONObject(k) ?: continue
            if(obj.optString("text").isNotBlank()) return obj.optString("text")
            val cod=obj.optJSONArray("coding")?.optJSONObject(0)
            if(cod?.optString("display").orEmpty().isNotBlank()) return cod!!.optString("display")
        }
        return r.optString("description").ifBlank { r.optString("status") }
    }

    /** Explicit user confirmation is required to associate and promote a staged item. */
    fun confirmImportedRecord(db: HealthDb, stagingId: String, memberId: Long) {
        val sql=db.writableDatabase
        sql.beginTransaction()
        try {
            val row=sql.rawQuery("SELECT resource_type,display,status,code_system,code,effective_start,effective_end,raw_json FROM interop_import_staging WHERE staging_id=? AND review_state='pending'",arrayOf(stagingId)).use { c ->
                if(!c.moveToFirst()) throw IllegalArgumentException("Import candidate is no longer pending")
                (0 until c.columnCount).map { if(c.isNull(it)) "" else c.getString(it) }
            }
            require(sql.rawQuery("SELECT id FROM members WHERE id=?",arrayOf(memberId.toString())).use{it.moveToFirst()}) { "Choose a valid family member" }
            val sourceRef="fhir-import:$stagingId"
            sql.insert("clinical_records",null,ContentValues().apply {
                put("logical_id",UUID.randomUUID().toString());put("member_id",memberId);put("resource_type",row[0])
                put("code_display",row[1]);put("status",row[2]);put("code_system",row[3]);put("code",row[4])
                put("effective_start",row[5]);put("effective_end",row[6]);put("source_system","http://hl7.org/fhir")
                put("source_reference",sourceRef);put("verification_status","user_confirmed_import")
            })
            sql.execSQL("UPDATE interop_import_staging SET review_state='confirmed',target_member_id=? WHERE staging_id=?",arrayOf(memberId,stagingId))
            sql.setTransactionSuccessful()
        } finally { sql.endTransaction() }
    }

    fun saveDocumentMetadata(db: HealthDb, memberId: Long, uri: Uri, mime: String?, extractedText: String, displayName: String, size: Long): String {
        val id=UUID.randomUUID().toString()
        val digest=MessageDigest.getInstance("SHA-256").digest(extractedText.toByteArray()).joinToString(""){"%02x".format(it)}
        db.writableDatabase.insert("clinical_documents",null,ContentValues().apply {
            put("document_id",id);put("member_id",memberId);put("display_name",displayName)
            put("mime_type",mime);put("size_bytes",size);put("content_uri",uri.toString())
            put("extracted_text",extractedText);put("extraction_engine","ML Kit Text Recognition v2")
            put("review_state","needs_review");put("source_hash",digest)
        })
        return id
    }

    fun documentInfo(context: Context, uri: Uri): Triple<String,String,Long> {
        var name="Clinical document";var size=0L
        context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE),null,null,null)?.use { c ->
            if(c.moveToFirst()){name=c.getString(0) ?: name;if(!c.isNull(1))size=c.getLong(1)}
        }
        return Triple(name,context.contentResolver.getType(uri) ?: "application/octet-stream",size)
    }
}
