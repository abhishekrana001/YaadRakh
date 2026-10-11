package com.example.yaadrakh.data.repository

import com.example.yaadrakh.data.local.FamilyProfileEntity
import com.example.yaadrakh.data.local.ReminderDao
import com.example.yaadrakh.data.local.ReminderEntity
import com.example.yaadrakh.data.local.ReminderHistoryEntity
import com.example.yaadrakh.data.local.RoutineEntity
import com.example.yaadrakh.data.local.RoutineOccurrenceEntity
import com.example.yaadrakh.utils.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderRepository(private val reminderDao: ReminderDao) {

    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()

    fun getTodayReminders(): Flow<List<ReminderEntity>> {
        val startOfDay = DateTimeUtils.getStartOfDay()
        val endOfDay = DateTimeUtils.getEndOfDay()
        return reminderDao.getTodayReminders(startOfDay, endOfDay)
    }

    val pendingReminders: Flow<List<ReminderEntity>> = reminderDao.getPendingReminders()

    fun getMissedReminders(now: Long = System.currentTimeMillis()): Flow<List<ReminderEntity>> {
        return reminderDao.getMissedReminders(now)
    }

    suspend fun getReminderById(id: Long): ReminderEntity? {
        return reminderDao.getReminderById(id)
    }

    suspend fun insertReminder(reminder: ReminderEntity): Long {
        return reminderDao.insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: ReminderEntity) {
        reminderDao.updateReminder(reminder)
    }

    suspend fun deleteReminder(reminder: ReminderEntity) {
        reminderDao.deleteReminder(reminder)
    }

    suspend fun updateCompletionStatus(id: Long, isCompleted: Boolean) {
        reminderDao.updateCompletionStatus(id, isCompleted)
    }

    suspend fun snoozeReminder(id: Long, snoozedUntil: Long) {
        reminderDao.snoozeReminder(id, snoozedUntil)
    }

    // ==================== HISTORY OPERATIONS ====================

    val allHistory: Flow<List<ReminderHistoryEntity>> = reminderDao.getAllHistory()

    fun getTodayHistory(): Flow<List<ReminderHistoryEntity>> {
        val startOfDay = DateTimeUtils.getStartOfDay()
        val endOfDay = DateTimeUtils.getEndOfDay()
        return reminderDao.getTodayHistory(startOfDay, endOfDay)
    }

    suspend fun recordCompletion(reminder: ReminderEntity) {
        val existing = reminderDao.getHistoryByReminderAndScheduledTime(reminder.id, reminder.scheduledTimeMillis)
        val now = System.currentTimeMillis()

        if (existing != null) {
            reminderDao.updateHistoryStatus(existing.id, "COMPLETED", now, "Completed")
        } else {
            val history = ReminderHistoryEntity(
                reminderId = reminder.id,
                title = reminder.title,
                message = reminder.message,
                category = reminder.category,
                scheduledTimeMillis = reminder.scheduledTimeMillis,
                actionTimeMillis = now,
                status = "COMPLETED",
                note = "Completed"
            )
            reminderDao.insertHistory(history)
        }
    }

    suspend fun recordCompletionById(id: Long) {
        val reminder = reminderDao.getReminderById(id) ?: return
        recordCompletion(reminder)
    }

    suspend fun recordMissed(reminder: ReminderEntity) {
        val existing = reminderDao.getHistoryByReminderAndScheduledTime(reminder.id, reminder.scheduledTimeMillis)
        if (existing == null) {
            val history = ReminderHistoryEntity(
                reminderId = reminder.id,
                title = reminder.title,
                message = reminder.message,
                category = reminder.category,
                scheduledTimeMillis = reminder.scheduledTimeMillis,
                actionTimeMillis = System.currentTimeMillis(),
                status = "MISSED",
                note = "Missed"
            )
            reminderDao.insertHistory(history)
        }
    }

    suspend fun recordSnooze(reminder: ReminderEntity, snoozeMinutes: Int) {
        val existing = reminderDao.getHistoryByReminderAndScheduledTime(reminder.id, reminder.scheduledTimeMillis)
        val now = System.currentTimeMillis()
        val snoozeNote = "Snoozed for $snoozeMinutes min"

        if (existing != null) {
            reminderDao.updateHistoryStatus(existing.id, "SNOOZED", now, snoozeNote)
        } else {
            val history = ReminderHistoryEntity(
                reminderId = reminder.id,
                title = reminder.title,
                message = reminder.message,
                category = reminder.category,
                scheduledTimeMillis = reminder.scheduledTimeMillis,
                actionTimeMillis = now,
                status = "SNOOZED",
                note = snoozeNote
            )
            reminderDao.insertHistory(history)
        }
    }

    // ==================== ROUTINE OPERATIONS ====================

    val allRoutines: Flow<List<RoutineEntity>> = reminderDao.getAllRoutines()
    val enabledRoutines: Flow<List<RoutineEntity>> = reminderDao.getEnabledRoutines()

    fun getTodayRoutineOccurrences(): Flow<List<RoutineOccurrenceEntity>> {
        val todayDate = getTodayDateString()
        return reminderDao.getOccurrencesForDate(todayDate)
    }

    suspend fun getRoutineById(id: Long): RoutineEntity? {
        return reminderDao.getRoutineById(id)
    }

    suspend fun insertRoutine(routine: RoutineEntity): Long {
        return reminderDao.insertRoutine(routine)
    }

    suspend fun updateRoutine(routine: RoutineEntity) {
        reminderDao.updateRoutine(routine)
    }

    suspend fun deleteRoutine(routine: RoutineEntity) {
        reminderDao.deleteOccurrencesForRoutine(routine.id)
        reminderDao.deleteRoutine(routine)
    }

    suspend fun updateRoutineEnabled(id: Long, enabled: Boolean) {
        reminderDao.updateRoutineEnabled(id, enabled)
    }

    suspend fun markRoutineOccurrence(routineId: Long, date: String, isCompleted: Boolean) {
        if (isCompleted) {
            val occurrence = RoutineOccurrenceEntity(
                routineId = routineId,
                occurrenceDate = date,
                status = "COMPLETED",
                completedAt = System.currentTimeMillis()
            )
            reminderDao.insertOrUpdateOccurrence(occurrence)
        } else {
            reminderDao.deleteOccurrence(routineId, date)
        }
    }

    suspend fun getEnabledRoutinesSync(): List<RoutineEntity> {
        return reminderDao.getEnabledRoutinesSync()
    }

    // ==================== FAMILY PROFILE OPERATIONS ====================

    val allFamilyProfiles: Flow<List<FamilyProfileEntity>> = reminderDao.getAllFamilyProfiles()

    suspend fun getFamilyProfileById(id: Long): FamilyProfileEntity? {
        return reminderDao.getFamilyProfileById(id)
    }

    suspend fun insertFamilyProfile(profile: FamilyProfileEntity): Long {
        return reminderDao.insertFamilyProfile(profile)
    }

    suspend fun updateFamilyProfile(profile: FamilyProfileEntity) {
        reminderDao.updateFamilyProfile(profile)
    }

    suspend fun deleteFamilyProfile(profile: FamilyProfileEntity) {
        reminderDao.deleteFamilyProfile(profile)
    }

    companion object {
        fun getTodayDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}
