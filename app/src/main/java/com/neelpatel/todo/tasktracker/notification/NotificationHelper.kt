package com.neelpatel.todo.tasktracker.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.neelpatel.todo.tasktracker.common.PreferenceManager
import com.neelpatel.todo.tasktracker.model.TaskRequest
import java.text.SimpleDateFormat
import java.util.*

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "task_notifications"
        const val CHANNEL_NAME = "Task Notifications"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"

        const val TYPE_START_REMINDER = "start_reminder" // 15 min before start
        const val TYPE_START_NOW = "start_now"           // at start time
        const val TYPE_DUE_REMINDER = "due_reminder"     // 30 min before due
        const val TYPE_MISSED = "missed"                 // after due time
    }

    init {
        Log.d("NotificationHelper", "NotificationHelper initialized")
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifications for task starts and deadlines"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleTaskNotifications(task: TaskRequest) {
        if (task.id == null) {
            Log.e("NotificationHelper", "Cannot schedule notifications: Task ID is null")
            return
        }
        
        if (task.status == "Completed") {
            Log.d("NotificationHelper", "Task ${task.title} is completed, canceling existing alarms and skipping scheduling")
            cancelTaskNotifications(task.id)
            return
        }

        val preferenceManager = PreferenceManager(context)
        if (!preferenceManager.isNotificationsEnabled()) {
            Log.d("NotificationHelper", "Notifications are disabled in settings. Canceling existing and skipping scheduling.")
            cancelTaskNotifications(task.id)
            return
        }
        
        Log.d("NotificationHelper", "Scheduling notifications for task: ${task.title}")

        val startTimeMillis = parseDateTime(task.startDate, task.startTime)
        val dueTimeMillis = parseDateTime(task.dueDate, task.dueTime)
        val currentTime = System.currentTimeMillis()

        Log.d("NotificationHelper", "Current Time: ${Date(currentTime)}")
        Log.d("NotificationHelper", "Parsed Start Time: ${startTimeMillis?.let { Date(it) } ?: "NULL"}")
        Log.d("NotificationHelper", "Parsed Due Time: ${dueTimeMillis?.let { Date(it) } ?: "NULL"}")

        // 1. Start Reminder (15 min before)
        if (startTimeMillis != null) {
            val startReminderTime = startTimeMillis - (15 * 60 * 1000)
            Log.d("NotificationHelper", "Start Reminder target: ${Date(startReminderTime)}")
            if (startReminderTime > currentTime) {
                scheduleAlarm(task.id, task.title, TYPE_START_REMINDER, startReminderTime)
            } else {
                Log.w("NotificationHelper", "Start reminder skipped: time is in the past")
            }

            // 2. Start Now
            if (startTimeMillis > currentTime) {
                scheduleAlarm(task.id, task.title, TYPE_START_NOW, startTimeMillis)
            } else {
                Log.w("NotificationHelper", "Start now skipped: time is in the past")
            }
        }

        // 3. Due Reminder (30 min before)
        if (dueTimeMillis != null) {
            val dueReminderTime = dueTimeMillis - (30 * 60 * 1000)
            if (dueReminderTime > currentTime) {
                scheduleAlarm(task.id, task.title, TYPE_DUE_REMINDER, dueReminderTime)
            }

            // 4. Missed Task (After due time)
            if (dueTimeMillis > currentTime) {
                scheduleAlarm(task.id, task.title, TYPE_MISSED, dueTimeMillis)
            }
        }
    }

    private fun scheduleAlarm(taskId: String, taskTitle: String, type: String, timeInMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        Log.d("NotificationHelper", "Scheduling alarm for task: $taskTitle ($type) at ${Date(timeInMillis)}")
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(EXTRA_TASK_TITLE, taskTitle)
            putExtra(EXTRA_NOTIFICATION_TYPE, type)
            putExtra("taskId", taskId)
        }

        // Unique ID for each notification type for each task
        val requestCode = (taskId.hashCode() + type.hashCode())
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.w("NotificationHelper", "Cannot schedule exact alarm, falling back to non-exact")
                alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                }
            }
            Log.d("NotificationHelper", "Successfully scheduled alarm for $taskTitle ($type)")
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "SecurityException while scheduling exact alarm", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        }
    }

    fun cancelTaskNotifications(taskId: String) {
        val types = listOf(TYPE_START_REMINDER, TYPE_START_NOW, TYPE_DUE_REMINDER, TYPE_MISSED)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        types.forEach { type ->
            val intent = Intent(context, NotificationReceiver::class.java)
            val requestCode = (taskId.hashCode() + type.hashCode()).hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }
    }

    private fun parseDateTime(date: String, time: String): Long? {
        if (date.isEmpty() || time.isEmpty()) {
            Log.e("NotificationHelper", "Empty date ($date) or time ($time)")
            return null
        }
        return try {
            val format = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            val dateObj = format.parse("$date $time")
            Log.d("NotificationHelper", "Successfully parsed $date $time to ${dateObj?.time}")
            dateObj?.time
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to parse date/time: $date $time", e)
            null
        }
    }
}
