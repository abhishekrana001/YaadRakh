package com.example.yaadrakh.model

data class ReminderUiModel(
    val id: Long,
    val title: String,
    val message: String,
    val time: String,
    val category: String,
    val categoryEmoji: String,
    val isCompleted: Boolean = false
)
