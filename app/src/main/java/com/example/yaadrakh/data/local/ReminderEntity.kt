package com.example.yaadrakh.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val category: String,
    val date: String,
    val time: String,
    val scheduledTimeMillis: Long,
    val repeatType: String,
    val isNotificationEnabled: Boolean = true,
    val isVoiceEnabled: Boolean = false,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val snoozedUntil: Long = 0L,
    val forWhom: String = "",
    val voiceRepeatCount: Int = 2
)
