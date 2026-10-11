package com.example.yaadrakh.utils

import android.content.Context
import android.content.SharedPreferences

class VoiceSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "yaadrakh_settings"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_VOICE_LANGUAGE = "voice_language"
        private const val KEY_VOICE_REPEAT_COUNT = "voice_repeat_count"
        private const val KEY_VOICE_SPEED = "voice_speed"
        private const val KEY_VOICE_PITCH = "voice_pitch"
        private const val KEY_VOICE_TONE = "voice_tone"
        private const val KEY_MORNING_BRIEFING = "morning_briefing_enabled"
        private const val KEY_BRIEFING_HOUR = "briefing_hour"
        private const val KEY_BRIEFING_MINUTE = "briefing_minute"
        private const val KEY_ELDERLY_MODE = "elderly_mode_enabled"

        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_HINDI = "hi"

        const val TONE_WARM = "warm"
        const val TONE_HIGH_CLEAR = "high_clear"
        const val TONE_DEEP = "deep"
    }

    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "") ?: ""
    }

    fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name.trim()).apply()
    }

    fun isUserLoggedIn(): Boolean {
        return prefs.contains(KEY_USER_NAME) && getUserName().isNotBlank()
    }

    fun getVoiceLanguage(): String {
        return prefs.getString(KEY_VOICE_LANGUAGE, LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
    }

    fun setVoiceLanguage(languageCode: String) {
        prefs.edit().putString(KEY_VOICE_LANGUAGE, languageCode).apply()
    }

    fun getVoiceRepeatCount(): Int {
        return prefs.getInt(KEY_VOICE_REPEAT_COUNT, 2)
    }

    fun setVoiceRepeatCount(count: Int) {
        prefs.edit().putInt(KEY_VOICE_REPEAT_COUNT, count.coerceIn(1, 5)).apply()
    }

    fun getVoiceSpeed(): Float {
        return prefs.getFloat(KEY_VOICE_SPEED, 0.95f)
    }

    fun setVoiceSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_VOICE_SPEED, speed).apply()
    }

    fun getVoicePitch(): Float {
        return prefs.getFloat(KEY_VOICE_PITCH, 1.0f)
    }

    fun setVoicePitch(pitch: Float) {
        prefs.edit().putFloat(KEY_VOICE_PITCH, pitch).apply()
    }

    fun getVoiceTone(): String {
        return prefs.getString(KEY_VOICE_TONE, TONE_WARM) ?: TONE_WARM
    }

    fun setVoiceTone(tone: String) {
        when (tone) {
            TONE_HIGH_CLEAR -> {
                setVoicePitch(1.15f)
                setVoiceSpeed(0.95f)
            }
            TONE_DEEP -> {
                setVoicePitch(0.85f)
                setVoiceSpeed(0.90f)
            }
            else -> {
                setVoicePitch(1.0f)
                setVoiceSpeed(0.95f)
            }
        }
        prefs.edit().putString(KEY_VOICE_TONE, tone).apply()
    }

    fun isMorningBriefingEnabled(): Boolean {
        return prefs.getBoolean(KEY_MORNING_BRIEFING, false)
    }

    fun setMorningBriefingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MORNING_BRIEFING, enabled).apply()
    }

    fun getBriefingHour(): Int {
        return prefs.getInt(KEY_BRIEFING_HOUR, 8)
    }

    fun setBriefingHour(hour: Int) {
        prefs.edit().putInt(KEY_BRIEFING_HOUR, hour).apply()
    }

    fun getBriefingMinute(): Int {
        return prefs.getInt(KEY_BRIEFING_MINUTE, 0)
    }

    fun setBriefingMinute(minute: Int) {
        prefs.edit().putInt(KEY_BRIEFING_MINUTE, minute).apply()
    }

    fun isElderlyModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_ELDERLY_MODE, false)
    }

    fun setElderlyModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ELDERLY_MODE, enabled).apply()
    }
}
