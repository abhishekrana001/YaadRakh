package com.example.yaadrakh.model

data class ReminderDraft(
    val title: String,
    val message: String,
    val category: String,
    val date: String,
    val time: String,
    val repeatType: String,
    val isNotificationEnabled: Boolean,
    val isVoiceEnabled: Boolean
)
