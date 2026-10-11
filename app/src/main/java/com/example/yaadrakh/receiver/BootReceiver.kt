package com.example.yaadrakh.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.yaadrakh.alarm.AlarmScheduler
import com.example.yaadrakh.data.local.AppDatabase
import com.example.yaadrakh.utils.VoiceSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val dao = db.reminderDao()
                    val scheduler = AlarmScheduler(context)

                    // 1. Reschedule reminders
                    val pendingReminders = dao.getPendingReminders().first()
                    for (reminder in pendingReminders) {
                        if (reminder.isNotificationEnabled && !reminder.isCompleted) {
                            scheduler.scheduleReminder(reminder)
                        }
                    }

                    // 2. Reschedule routines
                    val enabledRoutines = dao.getEnabledRoutinesSync()
                    for (routine in enabledRoutines) {
                        scheduler.scheduleRoutine(routine)
                    }

                    // 3. Reschedule morning briefing if enabled
                    val voiceSettings = VoiceSettings(context)
                    if (voiceSettings.isMorningBriefingEnabled()) {
                        scheduler.scheduleMorningBriefing(
                            voiceSettings.getBriefingHour(),
                            voiceSettings.getBriefingMinute()
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
