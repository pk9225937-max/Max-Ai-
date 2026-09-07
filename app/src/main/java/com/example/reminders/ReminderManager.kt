package com.example.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.ReminderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("REMINDER_ID", -1L)
        val title = intent.getStringExtra("REMINDER_TITLE") ?: "MAX Reminder"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "max_reminders_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "MAX Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for MAX Assistant scheduled reminders"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("MAX Reminder")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(reminderId.toInt(), notification)

        // Mark as completed in Room if not repeating
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val db = AppDatabase.getDatabase(context)
            val existing = db.reminderDao().getReminderById(reminderId)
            if (existing != null) {
                if (existing.repeatRule == "NONE") {
                    db.reminderDao().updateReminder(existing.copy(isCompleted = true))
                } else {
                    // Reschedule for next occurrence
                    val nextTime = when (existing.repeatRule) {
                        "DAILY" -> existing.timestamp + 24 * 60 * 60 * 1000L
                        "WEEKLY" -> existing.timestamp + 7 * 24 * 60 * 60 * 1000L
                        else -> existing.timestamp + 24 * 60 * 60 * 1000L
                    }
                    val updated = existing.copy(timestamp = nextTime)
                    db.reminderDao().updateReminder(updated)
                    ReminderManager(context).scheduleAlarm(updated)
                }
            }
        }
    }
}

class ReminderManager(private val context: Context) {

    private val alarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    }
    private val reminderDao by lazy {
        AppDatabase.getDatabase(context).reminderDao()
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

    suspend fun createReminder(
        title: String,
        triggerTimeMs: Long,
        repeatRule: String = "NONE"
    ): Long {
        val entity = ReminderEntity(
            title = title,
            timestamp = triggerTimeMs,
            repeatRule = repeatRule,
            isCompleted = false
        )
        val id = reminderDao.insertReminder(entity)
        scheduleAlarm(entity.copy(id = id))
        return id
    }

    fun scheduleAlarm(reminder: ReminderEntity) {
        val am = alarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("REMINDER_ID", reminder.id)
            putExtra("REMINDER_TITLE", reminder.title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.timestamp,
                    pendingIntent
                )
            } else {
                am.set(
                    AlarmManager.RTC_WAKEUP,
                    reminder.timestamp,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            am.set(
                AlarmManager.RTC_WAKEUP,
                reminder.timestamp,
                pendingIntent
            )
        } catch (e: Exception) {
            // Fallback gracefully
        }
    }

    suspend fun cancelReminder(id: Long) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
        }
        reminderDao.deleteReminderById(id)
    }

    fun getActiveReminders() = reminderDao.getActiveReminders()
}
