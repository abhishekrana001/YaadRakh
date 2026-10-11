package com.example.yaadrakh.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routine_occurrences",
    indices = [Index(value = ["routineId", "occurrenceDate"], unique = true)]
)
data class RoutineOccurrenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routineId: Long,
    val occurrenceDate: String, // "YYYY-MM-DD"
    val status: String, // "COMPLETED", "PENDING"
    val completedAt: Long = System.currentTimeMillis()
)
