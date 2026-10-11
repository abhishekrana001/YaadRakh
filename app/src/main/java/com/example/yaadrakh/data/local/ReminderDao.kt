package com.example.yaadrakh.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, scheduledTimeMillis ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE scheduledTimeMillis >= :startOfDay AND scheduledTimeMillis <= :endOfDay ORDER BY isCompleted ASC, scheduledTimeMillis ASC")
    fun getTodayReminders(startOfDay: Long, endOfDay: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY scheduledTimeMillis ASC")
    fun getPendingReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND isNotificationEnabled = 1 AND scheduledTimeMillis < :now AND snoozedUntil <= :now ORDER BY scheduledTimeMillis DESC")
    fun getMissedReminders(now: Long = System.currentTimeMillis()): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET isCompleted = :isCompleted, snoozedUntil = 0 WHERE id = :id")
    suspend fun updateCompletionStatus(id: Long, isCompleted: Boolean)

    @Query("UPDATE reminders SET snoozedUntil = :snoozedUntil WHERE id = :id")
    suspend fun snoozeReminder(id: Long, snoozedUntil: Long)

    // ==================== HISTORY QUERIES ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ReminderHistoryEntity): Long

    @Query("SELECT * FROM reminder_history WHERE reminderId = :reminderId AND scheduledTimeMillis = :scheduledTimeMillis LIMIT 1")
    suspend fun getHistoryByReminderAndScheduledTime(reminderId: Long, scheduledTimeMillis: Long): ReminderHistoryEntity?

    @Query("SELECT * FROM reminder_history ORDER BY scheduledTimeMillis DESC, actionTimeMillis DESC")
    fun getAllHistory(): Flow<List<ReminderHistoryEntity>>

    @Query("SELECT * FROM reminder_history WHERE scheduledTimeMillis >= :startOfDay AND scheduledTimeMillis <= :endOfDay ORDER BY scheduledTimeMillis DESC")
    fun getTodayHistory(startOfDay: Long, endOfDay: Long): Flow<List<ReminderHistoryEntity>>

    @Query("SELECT * FROM reminder_history WHERE status = :status ORDER BY scheduledTimeMillis DESC")
    fun getHistoryByStatus(status: String): Flow<List<ReminderHistoryEntity>>

    @Query("SELECT * FROM reminder_history WHERE title LIKE '%' || :query || '%' OR message LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY scheduledTimeMillis DESC")
    fun searchHistory(query: String): Flow<List<ReminderHistoryEntity>>

    @Query("UPDATE reminder_history SET status = :status, actionTimeMillis = :actionTimeMillis, note = :note WHERE id = :id")
    suspend fun updateHistoryStatus(id: Long, status: String, actionTimeMillis: Long, note: String)

    // ==================== ROUTINE QUERIES ====================

    @Query("SELECT * FROM routines ORDER BY hour ASC, minute ASC")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE enabled = 1 ORDER BY hour ASC, minute ASC")
    fun getEnabledRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE enabled = 1 ORDER BY hour ASC, minute ASC")
    suspend fun getEnabledRoutinesSync(): List<RoutineEntity>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutineById(id: Long): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Delete
    suspend fun deleteRoutine(routine: RoutineEntity)

    @Query("UPDATE routines SET enabled = :enabled WHERE id = :id")
    suspend fun updateRoutineEnabled(id: Long, enabled: Boolean)

    // ==================== ROUTINE OCCURRENCES QUERIES ====================

    @Query("SELECT * FROM routine_occurrences WHERE occurrenceDate = :date")
    fun getOccurrencesForDate(date: String): Flow<List<RoutineOccurrenceEntity>>

    @Query("SELECT * FROM routine_occurrences WHERE occurrenceDate = :date")
    suspend fun getOccurrencesForDateSync(date: String): List<RoutineOccurrenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateOccurrence(occurrence: RoutineOccurrenceEntity): Long

    @Query("DELETE FROM routine_occurrences WHERE routineId = :routineId AND occurrenceDate = :date")
    suspend fun deleteOccurrence(routineId: Long, date: String)

    @Query("DELETE FROM routine_occurrences WHERE routineId = :routineId")
    suspend fun deleteOccurrencesForRoutine(routineId: Long)

    // ==================== FAMILY PROFILES QUERIES ====================

    @Query("SELECT * FROM family_profiles ORDER BY name ASC")
    fun getAllFamilyProfiles(): Flow<List<FamilyProfileEntity>>

    @Query("SELECT * FROM family_profiles WHERE id = :id")
    suspend fun getFamilyProfileById(id: Long): FamilyProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyProfile(profile: FamilyProfileEntity): Long

    @Update
    suspend fun updateFamilyProfile(profile: FamilyProfileEntity)

    @Delete
    suspend fun deleteFamilyProfile(profile: FamilyProfileEntity)
}
