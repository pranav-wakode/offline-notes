package com.example.offlinenotes.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.offlinenotes.R
import com.example.offlinenotes.ui.activity.MainActivity

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Extract rich data from the alarm intent
        val title = intent.getStringExtra("title") ?: "Note Reminder"
        val content = intent.getStringExtra("content") ?: "You have a scheduled note."
        val noteId = intent.getIntExtra("noteId", 0)
        val reminderId = intent.getIntExtra("reminderId", -1)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Note Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Offline reminders for notes"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap action to open the specific note
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("noteId", noteId) // Pass the note ID to MainActivity
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle("⏰ $title")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content)) // Allow expanding text
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(reminderId, builder.build())
    }

    companion object {
        const val CHANNEL_ID = "offline_notes_reminders"
    }
}