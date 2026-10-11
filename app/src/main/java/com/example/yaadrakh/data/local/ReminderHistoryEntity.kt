package com.example.yaadrakh.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_history")
data class ReminderHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reminderId: Long,
    val title: String,
    val message: String,
    val category: String,
    val scheduledTimeMillis: Long,
    val actionTimeMillis: Long = System.currentTimeMillis(),
    val status: String, // "COMPLETED", "MISSED", "SNOOZED"
    val note: String = ""
)
