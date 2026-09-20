package com.neelpatel.todo.tasktracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.neelpatel.todo.tasktracker.MainActivity
import com.neelpatel.todo.tasktracker.R
import com.neelpatel.todo.tasktracker.model.TaskRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.neelpatel.todo.tasktracker.common.PreferenceManager
import android.app.PendingIntent
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val preferenceManager = PreferenceManager(context)
        if (!preferenceManager.isNotificationsEnabled()) {
            Log.d("NotificationReceiver", "Notifications are disabled in settings. Skipping.")
            return
        }

        val taskTitle = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "Task"
        val type = intent.getStringExtra(NotificationHelper.EXTRA_NOTIFICATION_TYPE) ?: ""
        val taskId = intent.getStringExtra("taskId") ?: ""
        
        Log.d("NotificationReceiver", "Received alarm for task: $taskTitle, type: $type")

        if (taskId.isNotEmpty()) {
            checkTaskStatusAndNotify(context, taskId, taskTitle, type)
        }
    }

    private fun checkTaskStatusAndNotify(context: Context, taskId: String, taskTitle: String, type: String) {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().reference

        database.child("users").child(userId).child("tasks").child(taskId).get()
            .addOnSuccessListener { snapshot ->
                val task = snapshot.getValue(TaskRequest::class.java)
                if (task != null) {
                    val isCompleted = task.status == "Completed"
                    
                    val (title, message) = when (type) {
                        NotificationHelper.TYPE_START_REMINDER -> {
                            if (!isCompleted) "Upcoming Task" to "Your task '$taskTitle' starts in 15 minutes." else null
                        }
                        NotificationHelper.TYPE_START_NOW -> {
                            if (!isCompleted) {
                                // Update status to In Progress
                                database.child("users").child(userId).child("tasks").child(taskId).child("status").setValue("In Progress")
                                "Task Started" to "It's time to start your task: '$taskTitle'. Status updated to In Progress."
                            } else null
                        }
                        NotificationHelper.TYPE_DUE_REMINDER -> {
                            if (!isCompleted) "Deadline Approaching" to "Your task '$taskTitle' is still not completed and is due in 30 minutes." else null
                        }
                        NotificationHelper.TYPE_MISSED -> {
                            if (!isCompleted) {
                                // Update status to Pending
                                val pendingUpdates = mapOf(
                                    "status" to "Pending",
                                    "pendingAt" to System.currentTimeMillis()
                                )
                                database.child("users").child(userId).child("tasks").child(taskId).updateChildren(pendingUpdates)
                                "Task Missed" to "You missed your current task: '$taskTitle'. Status updated to Pending."
                            } else null
                        }
                        else -> null
                    } ?: return@addOnSuccessListener

                    Log.d("NotificationReceiver", "Showing notification: $title - $message")
                    showNotification(context, title, message, taskId.hashCode() + type.hashCode())
                }
            }
            .addOnFailureListener {
                Log.e("NotificationReceiver", "Failed to fetch task for notification", it)
            }
    }

    private fun showNotification(context: Context, title: String, message: String, notificationId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            notificationId, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.play_store_logo) // Ensure this exists or use a generic one
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e("NotificationReceiver", "Notification permission not granted", e)
        }
    }
}
