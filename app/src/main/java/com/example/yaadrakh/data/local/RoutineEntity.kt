package com.example.yaadrakh.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String,
    val time: String, // "07:00 AM"
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val notificationEnabled: Boolean = true,
    val voiceEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
