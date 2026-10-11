package com.example.yaadrakh

import com.example.yaadrakh.alarm.AlarmScheduler
import com.example.yaadrakh.data.local.ReminderEntity
import com.example.yaadrakh.data.local.ReminderHistoryEntity
import com.example.yaadrakh.utils.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReminderLogicUnitTest {

    private fun calculatePercentage(completed: Int, total: Int): Int {
        return if (total > 0) (completed * 100) / total else 0
    }

    @Test
    fun testStartAndEndOfDay() {
        val now = System.currentTimeMillis()
        val startOfDay = DateTimeUtils.getStartOfDay(now)
        val endOfDay = DateTimeUtils.getEndOfDay(now)

        val calStart = Calendar.getInstance().apply { timeInMillis = startOfDay }
        assertEquals(0, calStart.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calStart.get(Calendar.MINUTE))
        assertEquals(0, calStart.get(Calendar.SECOND))
        assertEquals(0, calStart.get(Calendar.MILLISECOND))

        val calEnd = Calendar.getInstance().apply { timeInMillis = endOfDay }
        assertEquals(23, calEnd.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, calEnd.get(Calendar.MINUTE))
        assertEquals(59, calEnd.get(Calendar.SECOND))

        assertTrue(endOfDay > startOfDay)
    }

    @Test
    fun testCategoryEmoji() {
        assertEquals("💊", DateTimeUtils.getCategoryEmoji("Medicine"))
        assertEquals("📚", DateTimeUtils.getCategoryEmoji("Study"))
        assertEquals("💼", DateTimeUtils.getCategoryEmoji("Work"))
        assertEquals("🏋️", DateTimeUtils.getCategoryEmoji("Exercise"))
        assertEquals("🍴", DateTimeUtils.getCategoryEmoji("Food"))
        assertEquals("😴", DateTimeUtils.getCategoryEmoji("Sleep"))
        assertEquals("📞", DateTimeUtils.getCategoryEmoji("Call"))
        assertEquals("⏰", DateTimeUtils.getCategoryEmoji("Custom"))
    }

    @Test
    fun testFutureOneTimeTriggerTime() {
        val futureTime = System.currentTimeMillis() + 3600000L // 1 hour in future
        val result = AlarmScheduler.getNextTriggerTime(futureTime, "Doesn't repeat")
        assertEquals(futureTime, result)
    }

    @Test
    fun testPastDailyRecurringTriggerTime() {
        val now = System.currentTimeMillis()
        val pastTime = now - (2 * 24 * 3600 * 1000L) // 2 days ago
        val nextTrigger = AlarmScheduler.getNextTriggerTime(pastTime, "Every day")

        assertTrue(nextTrigger > now)
        val diffHours = (nextTrigger - now) / (3600 * 1000)
        assertTrue(diffHours in 0..24)
    }

    @Test
    fun testPastWeeklyRecurringTriggerTime() {
        val now = System.currentTimeMillis()
        val pastTime = now - (10 * 24 * 3600 * 1000L) // 10 days ago
        val nextTrigger = AlarmScheduler.getNextTriggerTime(pastTime, "Every week")

        assertTrue(nextTrigger > now)
        val diffDays = (nextTrigger - now) / (24 * 3600 * 1000)
        assertTrue(diffDays in 0..7)
    }

    @Test
    fun testProgressPercentageCalculation() {
        assertEquals(75, calculatePercentage(3, 4))
        assertEquals(50, calculatePercentage(1, 2))
        assertEquals(0, calculatePercentage(0, 0))
    }

    @Test
    fun testReminderFilteringByPersonAndStatus() {
        val now = System.currentTimeMillis()
        val r1 = ReminderEntity(
            id = 1,
            title = "Take Medicine",
            message = "Father medicine",
            category = "Medicine",
            date = "Today",
            time = "9:00 PM",
            scheduledTimeMillis = now + 10000,
            repeatType = "Doesn't repeat",
            forWhom = "Father"
        )
        val r2 = ReminderEntity(
            id = 2,
            title = "Exercise",
            message = "Workout",
            category = "Exercise",
            date = "Today",
            time = "10:00 PM",
            scheduledTimeMillis = now + 20000,
            repeatType = "Doesn't repeat",
            forWhom = "Mother"
        )

        val list = listOf(r1, r2)

        val fatherFiltered = list.filter { it.forWhom.equals("Father", ignoreCase = true) }
        assertEquals(1, fatherFiltered.size)
        assertEquals("Take Medicine", fatherFiltered[0].title)

        val motherFiltered = list.filter { it.forWhom.equals("Mother", ignoreCase = true) }
        assertEquals(1, motherFiltered.size)
        assertEquals("Exercise", motherFiltered[0].title)
    }

    @Test
    fun testHistoryFiltering() {
        val now = System.currentTimeMillis()
        val h1 = ReminderHistoryEntity(
            id = 1,
            reminderId = 10,
            title = "Take Medicine",
            message = "Morning dose",
            category = "Medicine",
            scheduledTimeMillis = now,
            status = "COMPLETED"
        )
        val h2 = ReminderHistoryEntity(
            id = 2,
            reminderId = 11,
            title = "Drink Water",
            message = "Stay hydrated",
            category = "Personal",
            scheduledTimeMillis = now - 3600000,
            status = "MISSED"
        )

        val historyList = listOf(h1, h2)

        val completedOnly = historyList.filter { it.status == "COMPLETED" }
        assertEquals(1, completedOnly.size)
        assertEquals("Take Medicine", completedOnly[0].title)

        val missedOnly = historyList.filter { it.status == "MISSED" }
        assertEquals(1, missedOnly.size)
        assertEquals("Drink Water", missedOnly[0].title)
    }
}
