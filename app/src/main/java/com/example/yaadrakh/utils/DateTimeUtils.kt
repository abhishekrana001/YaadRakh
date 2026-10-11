package com.example.yaadrakh.utils

import java.util.Calendar
import java.util.Locale

object DateTimeUtils {

    fun getStartOfDay(timeInMillis: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            this.timeInMillis = timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun getEndOfDay(timeInMillis: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            this.timeInMillis = timeInMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    fun getCategoryEmoji(category: String): String {
        return when (category.lowercase(Locale.ROOT)) {
            "medicine" -> "💊"
            "study" -> "📚"
            "work" -> "💼"
            "exercise" -> "🏋️"
            "food" -> "🍴"
            "sleep" -> "😴"
            "call" -> "📞"
            "personal" -> "👤"
            else -> "⏰"
        }
    }
}
