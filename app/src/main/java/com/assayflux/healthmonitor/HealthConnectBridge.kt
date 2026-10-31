package com.assayflux.healthmonitor

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import net.zetetic.database.sqlcipher.SQLiteDatabase
import android.content.ContentValues
import java.time.Instant
import java.util.UUID

/** Health Connect adapter. Only reads user-authorized records; imported observations retain provenance. */
object HealthConnectBridge {
    val readPermissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class)
    )

    fun availability(context: Context): Int = HealthConnectClient.getSdkStatus(context)

    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun syncRecent(db: HealthDb, context: Context): Int {
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        require(granted.containsAll(readPermissions)) { "Grant Health Connect read access before syncing." }
        val since = Instant.now().minusSeconds(30L * 24L * 60L * 60L)
        var saved = 0
        val heart = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter = TimeRangeFilter.after(since)))
        heart.records.forEach { record ->
            record.samples.forEachIndexed { index, sample ->
                val source = "health-connect:heart-rate:${record.metadata.id}:$index"
                saved += insertObservation(db, source, record.startTime.toString(), "Heart rate",
                    sample.beatsPerMinute.toDouble(), "/min", "http://loinc.org", "8867-4")
            }
        }
        val weights = client.readRecords(ReadRecordsRequest(WeightRecord::class, timeRangeFilter = TimeRangeFilter.after(since)))
        weights.records.forEach { record ->
            saved += insertObservation(db, "health-connect:weight:${record.metadata.id}",
                record.time.toString(), "Body weight", record.weight.inKilograms, "kg",
                "http://loinc.org", "29463-7")
        }
        val steps = client.readRecords(ReadRecordsRequest(StepsRecord::class, timeRangeFilter = TimeRangeFilter.after(since)))
        steps.records.forEach { record ->
            saved += insertObservation(db, "health-connect:steps:${record.metadata.id}",
                record.startTime.toString(), "Step count", record.count.toDouble(), "{steps}",
                "http://loinc.org", "55423-8")
        }
        return saved
    }

    private fun insertObservation(db: HealthDb, source: String, time: String, display: String,
        value: Double, unit: String, system: String, code: String): Int {
        val memberId = db.members().firstOrNull()?.get(0)?.toLongOrNull()
            ?: throw IllegalStateException("Add a family member before syncing Health Connect data.")
        val values = ContentValues().apply {
            put("logical_id", UUID.nameUUIDFromBytes(source.toByteArray()).toString())
            put("member_id", memberId); put("resource_type", "Observation")
            put("code_system", system); put("code", code); put("code_display", display)
            put("status", "final"); put("effective_start", time); put("value_numeric", value)
            put("unit_code", unit); put("unit_system", "http://unitsofmeasure.org")
            put("source_system", "https://health.google/health-connect")
            put("source_reference", source); put("verification_status", "device_import_unreviewed")
        }
        return if (db.writableDatabase.insertWithOnConflict("clinical_records", null, values,
            SQLiteDatabase.CONFLICT_IGNORE) == -1L) 0 else 1
    }
}
