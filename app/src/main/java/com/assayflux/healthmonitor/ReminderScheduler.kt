package com.assayflux.healthmonitor

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Offline reminder scheduler. Reminder content is intentionally generic on the
 * lock screen; sensitive medicine names and diagnoses are never placed in notifications.
 */
object ReminderScheduler {
    const val CHANNEL_ID = "health_monitor_reminders"
    private const val INPUT_ID = "reminder_id"
    private const val INPUT_KIND = "reminder_kind"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_ID, "Health reminders", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Private reminders from Health Monitor" })
        }
    }

    /** Schedules a one-off reminder; repeat by scheduling the next occurrence on completion. */
    fun schedule(context: Context, id: String, kind: String, delay: Long, unit: TimeUnit) {
        require(id.matches(Regex("[A-Za-z0-9_-]{1,64}")))
        require(kind in setOf("medicine", "appointment", "test", "vitals", "follow_up", "general"))
        require(delay >= 0)
        val input = Data.Builder().putString(INPUT_ID, id).putString(INPUT_KIND, kind).build()
        val request = OneTimeWorkRequestBuilder<HealthReminderWorker>()
            .setInputData(input)
            .setInitialDelay(delay, unit)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "health-reminder-$id", ExistingWorkPolicy.REPLACE, request
        )
    }

    fun cancel(context: Context, id: String) {
        WorkManager.getInstance(context).cancelUniqueWork("health-reminder-$id")
    }

    internal fun notify(context: Context, id: String, kind: String) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return
        val label = when (kind) {
            "medicine" -> "A scheduled health reminder is due"
            "appointment" -> "You have an upcoming appointment"
            "test" -> "A health test reminder is due"
            "vitals" -> "It's time for a health measurement"
            "follow_up" -> "A follow-up reminder is due"
            else -> "You have a Health Monitor reminder"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Health Monitor")
            .setContentText(label)
            .setStyle(NotificationCompat.BigTextStyle().bigText(label))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id.hashCode(), notification)
    }
}

class HealthReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val id = inputData.getString("reminder_id") ?: return Result.failure()
        val kind = inputData.getString("reminder_kind") ?: "general"
        return try {
            ReminderScheduler.notify(applicationContext, id, kind)
            Result.success()
        } catch (_: SecurityException) {
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
