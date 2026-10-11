package com.example.yaadrakh.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.yaadrakh.alarm.AlarmScheduler
import com.example.yaadrakh.data.local.AppDatabase
import com.example.yaadrakh.data.repository.ReminderRepository
import com.example.yaadrakh.utils.NotificationHelper
import com.example.yaadrakh.utils.VoiceReminderHelper
import com.example.yaadrakh.utils.VoiceSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.example.yaadrakh.ACTION_TRIGGER_REMINDER"
        const val ACTION_MARK_DONE = "com.example.yaadrakh.ACTION_MARK_DONE"
        const val ACTION_SNOOZE = "com.example.yaadrakh.ACTION_SNOOZE"

        const val ACTION_TRIGGER_ROUTINE = "com.example.yaadrakh.ACTION_TRIGGER_ROUTINE"
        const val ACTION_MARK_ROUTINE_DONE = "com.example.yaadrakh.ACTION_MARK_ROUTINE_DONE"
        const val ACTION_TRIGGER_MORNING_BRIEFING = "com.example.yaadrakh.ACTION_TRIGGER_MORNING_BRIEFING"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_ROUTINE_ID = "extra_routine_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_REPEAT_TYPE = "extra_repeat_type"
        const val EXTRA_IS_VOICE_ENABLED = "extra_is_voice_enabled"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)

        when (intent.action) {
            ACTION_TRIGGER_REMINDER -> {
                if (reminderId == -1L) return

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val dao = db.reminderDao()
                        val reminder = dao.getReminderById(reminderId)

                        val title = reminder?.title ?: intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
                        val message = reminder?.message ?: intent.getStringExtra(EXTRA_MESSAGE) ?: ""
                        val category = reminder?.category ?: intent.getStringExtra(EXTRA_CATEGORY) ?: "Personal"
                        val repeatType = reminder?.repeatType ?: intent.getStringExtra(EXTRA_REPEAT_TYPE) ?: "Doesn't repeat"
                        val isVoiceEnabled = reminder?.isVoiceEnabled ?: intent.getBooleanExtra(EXTRA_IS_VOICE_ENABLED, false)

                        // If it was snoozed, reset snoozedUntil in DB now that it triggered
                        if (reminder != null && reminder.snoozedUntil > 0) {
                            dao.snoozeReminder(reminderId, 0L)
                        }

                        // 1. Show Notification
                        val notificationHelper = NotificationHelper(context)
                        notificationHelper.showReminderNotification(reminderId, title, message, category)

                        // 2. Handle repeating reminders
                        if (reminder != null && reminder.isNotificationEnabled && !reminder.isCompleted) {
                            if (repeatType == "Every day" || repeatType == "Every week" || repeatType == "DAILY" || repeatType == "WEEKLY") {
                                val nextTime = AlarmScheduler.getNextTriggerTime(reminder.scheduledTimeMillis, repeatType)
                                val updatedReminder = reminder.copy(scheduledTimeMillis = nextTime, snoozedUntil = 0L)
                                dao.updateReminder(updatedReminder)
                                AlarmScheduler(context).scheduleReminder(updatedReminder)
                            }
                        }

                        // 3. Handle Voice Reminder if enabled
                        if (isVoiceEnabled && message.isNotBlank()) {
                            val voiceSettings = VoiceSettings(context)
                            val languageCode = voiceSettings.getVoiceLanguage()
                            val repeatCount = reminder?.voiceRepeatCount ?: voiceSettings.getVoiceRepeatCount()

                            withContext(Dispatchers.Main) {
                                VoiceReminderHelper(context).speakReminder(
                                    title = title,
                                    message = message,
                                    repeatCount = repeatCount,
                                    languageCode = languageCode
                                ) {
                                    pendingResult.finish()
                                }
                            }
                        } else {
                            pendingResult.finish()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        pendingResult.finish()
                    }
                }
            }

            ACTION_MARK_DONE -> {
                if (reminderId == -1L) return

                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(reminderId.toInt())

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val dao = db.reminderDao()
                        val repository = ReminderRepository(dao)

                        val reminder = dao.getReminderById(reminderId)
                        if (reminder != null) {
                            dao.updateCompletionStatus(reminderId, true)
                            repository.recordCompletion(reminder)

                            if (reminder.repeatType == "Doesn't repeat" || reminder.repeatType == "NONE") {
                                AlarmScheduler(context).cancelReminderById(reminderId)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_SNOOZE -> {
                if (reminderId == -1L) return

                val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(reminderId.toInt())

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val dao = db.reminderDao()
                        val repository = ReminderRepository(dao)
                        val reminder = dao.getReminderById(reminderId)

                        if (reminder != null) {
                            val snoozeTimeMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
                            dao.snoozeReminder(reminderId, snoozeTimeMillis)
                            repository.recordSnooze(reminder, snoozeMinutes)
                            val alarmScheduler = AlarmScheduler(context)
                            alarmScheduler.scheduleSnooze(reminder, snoozeTimeMillis)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_TRIGGER_ROUTINE -> {
                if (routineId == -1L) return

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val dao = db.reminderDao()
                        val routine = dao.getRoutineById(routineId)

                        val title = routine?.title ?: intent.getStringExtra(EXTRA_TITLE) ?: "Daily Routine"
                        val message = routine?.description ?: intent.getStringExtra(EXTRA_MESSAGE) ?: ""
                        val category = routine?.category ?: intent.getStringExtra(EXTRA_CATEGORY) ?: "Personal"
                        val isVoiceEnabled = routine?.voiceEnabled ?: intent.getBooleanExtra(EXTRA_IS_VOICE_ENABLED, false)

                        val notificationHelper = NotificationHelper(context)
                        notificationHelper.showRoutineNotification(routineId, title, message, category)

                        if (routine != null && routine.enabled) {
                            AlarmScheduler(context).scheduleRoutine(routine)
                        }

                        if (isVoiceEnabled && message.isNotBlank()) {
                            val voiceSettings = VoiceSettings(context)
                            val languageCode = voiceSettings.getVoiceLanguage()
                            val repeatCount = voiceSettings.getVoiceRepeatCount()

                            withContext(Dispatchers.Main) {
                                VoiceReminderHelper(context).speakReminder(
                                    title = title,
                                    message = message,
                                    repeatCount = repeatCount,
                                    languageCode = languageCode
                                ) {
                                    pendingResult.finish()
                                }
                            }
                        } else {
                            pendingResult.finish()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        pendingResult.finish()
                    }
                }
            }

            ACTION_MARK_ROUTINE_DONE -> {
                if (routineId == -1L) return

                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel((routineId + AlarmScheduler.ROUTINE_ALARM_OFFSET).toInt())

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val dao = db.reminderDao()
                        val repository = ReminderRepository(dao)
                        val todayDate = ReminderRepository.getTodayDateString()

                        repository.markRoutineOccurrence(routineId, todayDate, true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_TRIGGER_MORNING_BRIEFING -> {
                val voiceSettings = VoiceSettings(context)
                if (!voiceSettings.isMorningBriefingEnabled()) {
                    return
                }

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val dao = db.reminderDao()

                        val routines = dao.getEnabledRoutinesSync()
                        val todayDate = ReminderRepository.getTodayDateString()
                        val occurrences = dao.getOccurrencesForDateSync(todayDate)

                        val totalRoutines = routines.size
                        val completedRoutines = occurrences.count { it.status == "COMPLETED" }
                        val remainingRoutines = maxOf(0, totalRoutines - completedRoutines)

                        val languageCode = voiceSettings.getVoiceLanguage()
                        val textToSpeak = if (languageCode == VoiceSettings.LANGUAGE_HINDI) {
                            "गुड मॉर्निंग! आज आपकी $totalRoutines रूटीन निर्धारित हैं, $completedRoutines पूरी हो चुकी हैं, $remainingRoutines बाकी हैं।"
                        } else {
                            "Good Morning! Today you have $totalRoutines routines scheduled, $completedRoutines completed, $remainingRoutines remaining."
                        }

                        // Reschedule briefing for tomorrow
                        val alarmScheduler = AlarmScheduler(context)
                        alarmScheduler.scheduleMorningBriefing(
                            voiceSettings.getBriefingHour(),
                            voiceSettings.getBriefingMinute()
                        )

                        withContext(Dispatchers.Main) {
                            VoiceReminderHelper(context).speak(
                                text = textToSpeak,
                                languageCode = languageCode
                            ) {
                                pendingResult.finish()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
