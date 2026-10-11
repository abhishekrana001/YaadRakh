package com.example.yaadrakh.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.yaadrakh.MainActivity
import com.example.yaadrakh.R
import com.example.yaadrakh.alarm.AlarmScheduler
import com.example.yaadrakh.receiver.ReminderReceiver

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "yaadrakh_reminders_channel"
        const val CHANNEL_NAME = "Reminder Notifications"
        const val CHANNEL_DESC = "Notifications for scheduled offline personal reminders and routines"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(
        reminderId: Long,
        title: String,
        message: String,
        category: String
    ) {
        val emoji = DateTimeUtils.getCategoryEmoji(category)
        val fullTitle = "$emoji $title"

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("REMINDER_ID", reminderId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_MARK_DONE
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + 100000).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze10Intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SNOOZE
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderReceiver.EXTRA_SNOOZE_MINUTES, 10)
        }
        val snooze10PendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + 200000).toInt(),
            snooze10Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze30Intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SNOOZE
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderReceiver.EXTRA_SNOOZE_MINUTES, 30)
        }
        val snooze30PendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + 300000).toInt(),
            snooze30Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(fullTitle)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, "✓ DONE", donePendingIntent)
            .addAction(0, "💤 10M SNOOZE", snooze10PendingIntent)
            .addAction(0, "⏱ 30M SNOOZE", snooze30PendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(reminderId.toInt(), builder.build())
    }

    fun showRoutineNotification(
        routineId: Long,
        title: String,
        message: String,
        category: String
    ) {
        val emoji = DateTimeUtils.getCategoryEmoji(category)
        val fullTitle = "🌅 Routine: $emoji $title"

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ROUTINE_ID", routineId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            (routineId + AlarmScheduler.ROUTINE_ALARM_OFFSET).toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_MARK_ROUTINE_DONE
            putExtra(ReminderReceiver.EXTRA_ROUTINE_ID, routineId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (routineId + AlarmScheduler.ROUTINE_ALARM_OFFSET + 50000).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(fullTitle)
            .setContentText(if (message.isNotBlank()) message else "Time for your $title routine")
            .setStyle(NotificationCompat.BigTextStyle().bigText(if (message.isNotBlank()) message else "Time for your $title routine"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, "✓ DONE", donePendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((routineId + AlarmScheduler.ROUTINE_ALARM_OFFSET).toInt(), builder.build())
    }
}
