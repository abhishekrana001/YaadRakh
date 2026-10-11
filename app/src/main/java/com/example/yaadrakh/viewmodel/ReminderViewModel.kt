package com.example.yaadrakh.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.yaadrakh.alarm.AlarmScheduler
import com.example.yaadrakh.data.local.AppDatabase
import com.example.yaadrakh.data.local.FamilyProfileEntity
import com.example.yaadrakh.data.local.ReminderEntity
import com.example.yaadrakh.data.local.ReminderHistoryEntity
import com.example.yaadrakh.data.local.RoutineEntity
import com.example.yaadrakh.data.local.RoutineOccurrenceEntity
import com.example.yaadrakh.data.repository.ReminderRepository
import com.example.yaadrakh.utils.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ReminderRepository
    private val alarmScheduler: AlarmScheduler = AlarmScheduler(application)

    val allReminders: LiveData<List<ReminderEntity>>
    val todayReminders: LiveData<List<ReminderEntity>>
    val pendingReminders: LiveData<List<ReminderEntity>>
    val missedReminders: LiveData<List<ReminderEntity>>

    val allHistory: LiveData<List<ReminderHistoryEntity>>
    val todayHistory: LiveData<List<ReminderHistoryEntity>>

    val allRoutines: LiveData<List<RoutineEntity>>
    val enabledRoutines: LiveData<List<RoutineEntity>>
    val todayRoutineOccurrences: LiveData<List<RoutineOccurrenceEntity>>

    val allFamilyProfiles: LiveData<List<FamilyProfileEntity>>

    init {
        val reminderDao = AppDatabase.getDatabase(application).reminderDao()
        repository = ReminderRepository(reminderDao)

        allReminders = repository.allReminders.asLiveData()
        todayReminders = repository.getTodayReminders().asLiveData()
        pendingReminders = repository.pendingReminders.asLiveData()
        missedReminders = repository.getMissedReminders().asLiveData()

        allHistory = repository.allHistory.asLiveData()
        todayHistory = repository.getTodayHistory().asLiveData()

        allRoutines = repository.allRoutines.asLiveData()
        enabledRoutines = repository.enabledRoutines.asLiveData()
        todayRoutineOccurrences = repository.getTodayRoutineOccurrences().asLiveData()

        allFamilyProfiles = repository.allFamilyProfiles.asLiveData()
    }

    fun addReminder(reminder: ReminderEntity, onComplete: (() -> Unit)? = null) =
        viewModelScope.launch(Dispatchers.IO) {
            val newId = repository.insertReminder(reminder)
            val inserted = reminder.copy(id = newId)
            if (inserted.isNotificationEnabled) {
                alarmScheduler.scheduleReminder(inserted)
            }
            launch(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }

    fun updateReminder(reminder: ReminderEntity, onComplete: (() -> Unit)? = null) =
        viewModelScope.launch(Dispatchers.IO) {
            alarmScheduler.cancelReminder(reminder)
            repository.updateReminder(reminder)
            if (reminder.isNotificationEnabled && !reminder.isCompleted) {
                alarmScheduler.scheduleReminder(reminder)
            }
            launch(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }

    fun deleteReminder(reminder: ReminderEntity, onComplete: (() -> Unit)? = null) =
        viewModelScope.launch(Dispatchers.IO) {
            alarmScheduler.cancelReminder(reminder)
            repository.deleteReminder(reminder)
            launch(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }

    fun markReminderCompleted(id: Long, isCompleted: Boolean, onComplete: (() -> Unit)? = null) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCompletionStatus(id, isCompleted)
            val reminder = repository.getReminderById(id)
            if (reminder != null) {
                if (isCompleted) {
                    alarmScheduler.cancelReminder(reminder)
                    repository.recordCompletion(reminder)
                } else if (reminder.isNotificationEnabled) {
                    alarmScheduler.scheduleReminder(reminder)
                }
            }
            launch(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }

    fun toggleReminderEnable(reminder: ReminderEntity, isEnabled: Boolean) =
        viewModelScope.launch(Dispatchers.IO) {
            val updated = reminder.copy(isNotificationEnabled = isEnabled)
            repository.updateReminder(updated)
            if (isEnabled && !updated.isCompleted) {
                alarmScheduler.scheduleReminder(updated)
            } else {
                alarmScheduler.cancelReminder(updated)
            }
        }

    fun snoozeReminder(reminder: ReminderEntity, minutes: Int, onComplete: (() -> Unit)? = null) =
        viewModelScope.launch(Dispatchers.IO) {
            val snoozeTimeMillis = System.currentTimeMillis() + (minutes * 60 * 1000L)
            repository.snoozeReminder(reminder.id, snoozeTimeMillis)
            repository.recordSnooze(reminder, minutes)
            alarmScheduler.scheduleSnooze(reminder, snoozeTimeMillis)
            launch(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }

    fun recordMissed(reminder: ReminderEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.recordMissed(reminder)
    }

    fun getReminderById(id: Long, onResult: (ReminderEntity?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val reminder = repository.getReminderById(id)
            launch(Dispatchers.Main) {
                onResult(reminder)
            }
        }
    }

    // ==================== ROUTINE OPERATIONS ====================

    fun addRoutine(routine: RoutineEntity, onComplete: (() -> Unit)? = null) =
        viewModelScope.launch(Dispatchers.IO) {
            val newId = repository.insertRoutine(routine)
            val inserted = routine.copy(id = newId)
            if (inserted.enabled) {
                alarmScheduler.scheduleRoutine(inserted)
            }
            launch(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }

    fun updateRoutine(routine: RoutineEntity, onComplete: (() -> Unit)? = null) =
        viewModelScope.launch(Dispatchers.IO) {
            alarmScheduler.cancelRoutine(routine)
            repository.updateRoutine(routine)
            if (routine.enabled) {
                alarmScheduler.scheduleRoutine(routine)
            }
            launch(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }

    fun deleteRoutine(routine: RoutineEntity, onComplete: (() -> Unit)? = null) =
        viewModelScope.launch(Dispatchers.IO) {
            alarmScheduler.cancelRoutine(routine)
            repository.deleteRoutine(routine)
            launch(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }

    fun toggleRoutineEnabled(routine: RoutineEntity, isEnabled: Boolean) =
        viewModelScope.launch(Dispatchers.IO) {
            val updated = routine.copy(enabled = isEnabled)
            repository.updateRoutine(updated)
            if (isEnabled) {
                alarmScheduler.scheduleRoutine(updated)
            } else {
                alarmScheduler.cancelRoutine(updated)
            }
        }

    fun markRoutineOccurrence(routineId: Long, isCompleted: Boolean) =
        viewModelScope.launch(Dispatchers.IO) {
            val todayDate = ReminderRepository.getTodayDateString()
            repository.markRoutineOccurrence(routineId, todayDate, isCompleted)
        }

    // ==================== FAMILY PROFILE OPERATIONS ====================

    fun addFamilyProfile(profile: FamilyProfileEntity, onComplete: (() -> Unit)? = null) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertFamilyProfile(profile)
            launch(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }

    fun updateFamilyProfile(profile: FamilyProfileEntity, onComplete: (() -> Unit)? = null) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFamilyProfile(profile)
            launch(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }

    fun deleteFamilyProfile(profile: FamilyProfileEntity, onComplete: (() -> Unit)? = null) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFamilyProfile(profile)
            launch(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }

    fun filterAndSortReminders(
        list: List<ReminderEntity>,
        query: String,
        filter: FilterType,
        personFilter: String = "ALL"
    ): List<ReminderEntity> {
        val trimmedQuery = query.trim().lowercase()
        val now = System.currentTimeMillis()

        val filtered = list.filter { reminder ->
            val matchesSearch = trimmedQuery.isEmpty() ||
                    reminder.title.lowercase().contains(trimmedQuery) ||
                    reminder.message.lowercase().contains(trimmedQuery) ||
                    reminder.category.lowercase().contains(trimmedQuery) ||
                    reminder.forWhom.lowercase().contains(trimmedQuery)

            val startOfDay = DateTimeUtils.getStartOfDay()
            val endOfDay = DateTimeUtils.getEndOfDay()

            val matchesFilter = when (filter) {
                FilterType.ALL -> true
                FilterType.TODAY -> reminder.scheduledTimeMillis in startOfDay..endOfDay
                FilterType.UPCOMING -> reminder.scheduledTimeMillis > endOfDay && !reminder.isCompleted
                FilterType.COMPLETED -> reminder.isCompleted
                FilterType.MISSED -> !reminder.isCompleted && reminder.isNotificationEnabled && reminder.scheduledTimeMillis < now && reminder.snoozedUntil <= now
            }

            val matchesPerson = when {
                personFilter.equals("ALL", ignoreCase = true) -> true
                personFilter.equals("Self / None", ignoreCase = true) -> reminder.forWhom.isBlank()
                else -> reminder.forWhom.equals(personFilter, ignoreCase = true)
            }

            matchesSearch && matchesFilter && matchesPerson
        }

        val startOfDay = DateTimeUtils.getStartOfDay()
        val endOfDay = DateTimeUtils.getEndOfDay()

        return filtered.sortedWith(
            compareBy<ReminderEntity> { reminder ->
                when {
                    reminder.isCompleted -> 3
                    reminder.scheduledTimeMillis in startOfDay..endOfDay -> 1
                    reminder.scheduledTimeMillis > endOfDay -> 2
                    else -> 4
                }
            }.thenBy { it.scheduledTimeMillis }
        )
    }

    fun filterAndSearchHistory(
        list: List<ReminderHistoryEntity>,
        query: String,
        statusFilter: HistoryStatusFilter,
        dateFilter: HistoryDateFilter
    ): List<ReminderHistoryEntity> {
        val trimmedQuery = query.trim().lowercase()
        val startOfToday = DateTimeUtils.getStartOfDay()
        val endOfToday = DateTimeUtils.getEndOfDay()
        val startOfYesterday = startOfToday - (24 * 60 * 60 * 1000L)

        return list.filter { history ->
            val matchesSearch = trimmedQuery.isEmpty() ||
                    history.title.lowercase().contains(trimmedQuery) ||
                    history.message.lowercase().contains(trimmedQuery) ||
                    history.category.lowercase().contains(trimmedQuery)

            val matchesStatus = when (statusFilter) {
                HistoryStatusFilter.ALL -> true
                HistoryStatusFilter.COMPLETED -> history.status == "COMPLETED"
                HistoryStatusFilter.MISSED -> history.status == "MISSED"
                HistoryStatusFilter.SNOOZED -> history.status == "SNOOZED"
            }

            val matchesDate = when (dateFilter) {
                HistoryDateFilter.ALL -> true
                HistoryDateFilter.TODAY -> history.scheduledTimeMillis in startOfToday..endOfToday
                HistoryDateFilter.YESTERDAY -> history.scheduledTimeMillis in startOfYesterday until startOfToday
                HistoryDateFilter.OLDER -> history.scheduledTimeMillis < startOfYesterday
            }

            matchesSearch && matchesStatus && matchesDate
        }.sortedByDescending { it.scheduledTimeMillis }
    }

    enum class FilterType {
        ALL, TODAY, UPCOMING, COMPLETED, MISSED
    }

    enum class HistoryStatusFilter {
        ALL, COMPLETED, MISSED, SNOOZED
    }

    enum class HistoryDateFilter {
        ALL, TODAY, YESTERDAY, OLDER
    }
}
