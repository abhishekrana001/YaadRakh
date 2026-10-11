package com.example.yaadrakh.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.yaadrakh.data.local.ReminderEntity
import com.example.yaadrakh.data.local.RoutineEntity
import com.example.yaadrakh.receiver.ReminderReceiver
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(reminder: ReminderEntity) {
        if (!reminder.isNotificationEnabled || reminder.isCompleted) {
            cancelReminder(reminder)
            return
        }

        val now = System.currentTimeMillis()
        val triggerTime = if (reminder.snoozedUntil > now) {
            reminder.snoozedUntil
        } else {
            getNextTriggerTime(reminder.scheduledTimeMillis, reminder.repeatType)
        }

        if (triggerTime <= now) {
            return
        }

        scheduleAlarmAtTime(reminder, triggerTime)
    }

    fun scheduleSnooze(reminder: ReminderEntity, snoozeTimeMillis: Long) {
        if (snoozeTimeMillis <= System.currentTimeMillis()) return
        cancelReminder(reminder)
        scheduleAlarmAtTime(reminder, snoozeTimeMillis)
    }

    private fun scheduleAlarmAtTime(reminder: ReminderEntity, triggerTime: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
            putExtra(ReminderReceiver.EXTRA_MESSAGE, reminder.message)
            putExtra(ReminderReceiver.EXTRA_CATEGORY, reminder.category)
            putExtra(ReminderReceiver.EXTRA_REPEAT_TYPE, reminder.repeatType)
            putExtra(ReminderReceiver.EXTRA_IS_VOICE_ENABLED, reminder.isVoiceEnabled)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarmExact(triggerTime, pendingIntent)
    }

    fun cancelReminder(reminder: ReminderEntity) {
        cancelReminderById(reminder.id)
    }

    fun cancelSnooze(reminder: ReminderEntity) {
        cancelReminderById(reminder.id)
    }

    fun cancelReminderById(reminderId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TRIGGER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    // ==================== ROUTINE ALARMS ====================

    fun scheduleRoutine(routine: RoutineEntity) {
        if (!routine.enabled || !routine.notificationEnabled) {
            cancelRoutine(routine)
            return
        }

        val triggerTime = getNextRoutineTriggerTime(routine.hour, routine.minute)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TRIGGER_ROUTINE
            putExtra(ReminderReceiver.EXTRA_ROUTINE_ID, routine.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, routine.title)
            putExtra(ReminderReceiver.EXTRA_MESSAGE, routine.description)
            putExtra(ReminderReceiver.EXTRA_CATEGORY, routine.category)
            putExtra(ReminderReceiver.EXTRA_IS_VOICE_ENABLED, routine.voiceEnabled)
        }

        val requestCode = (routine.id + ROUTINE_ALARM_OFFSET).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarmExact(triggerTime, pendingIntent)
    }

    fun cancelRoutine(routine: RoutineEntity) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TRIGGER_ROUTINE
        }
        val requestCode = (routine.id + ROUTINE_ALARM_OFFSET).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    // ==================== MORNING BRIEFING ALARM ====================

    fun scheduleMorningBriefing(hour: Int, minute: Int) {
        val triggerTime = getNextRoutineTriggerTime(hour, minute)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TRIGGER_MORNING_BRIEFING
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MORNING_BRIEFING_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarmExact(triggerTime, pendingIntent)
    }

    fun cancelMorningBriefing() {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TRIGGER_MORNING_BRIEFING
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MORNING_BRIEFING_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun setAlarmExact(triggerTime: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    companion object {
        const val ROUTINE_ALARM_OFFSET = 200000L
        const val MORNING_BRIEFING_REQUEST_CODE = 300000

        fun getNextTriggerTime(scheduledTimeMillis: Long, repeatType: String): Long {
            val now = System.currentTimeMillis()
            if (scheduledTimeMillis > now) {
                return scheduledTimeMillis
            }

            return when (repeatType) {
                "Every day", "DAILY" -> {
                    val calendar = Calendar.getInstance().apply { timeInMillis = scheduledTimeMillis }
                    while (calendar.timeInMillis <= now) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    calendar.timeInMillis
                }
                "Every week", "WEEKLY" -> {
                    val calendar = Calendar.getInstance().apply { timeInMillis = scheduledTimeMillis }
                    while (calendar.timeInMillis <= now) {
                        calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    }
                    calendar.timeInMillis
                }
                else -> scheduledTimeMillis
            }
        }

        fun getNextRoutineTriggerTime(hour: Int, minute: Int): Long {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        }
    }
}
