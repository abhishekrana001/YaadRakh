package com.example.yaadrakh.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class VoiceReminderHelper(private val context: Context) {

    private var tts: TextToSpeech? = null
    private val voiceSettings = VoiceSettings(context)

    fun speakReminder(
        title: String,
        message: String,
        repeatCount: Int = voiceSettings.getVoiceRepeatCount(),
        languageCode: String = voiceSettings.getVoiceLanguage(),
        onComplete: (() -> Unit)? = null
    ) {
        val userName = voiceSettings.getUserName()
        val isElderly = voiceSettings.isElderlyModeEnabled()

        val fullText = constructSpeechText(
            title = title,
            message = message,
            userName = userName,
            languageCode = languageCode,
            isElderly = isElderly,
            repeatCount = repeatCount
        )

        speakText(fullText, languageCode, onComplete)
    }

    fun speak(
        text: String,
        languageCode: String = voiceSettings.getVoiceLanguage(),
        onComplete: (() -> Unit)? = null
    ) {
        speakText(text, languageCode, onComplete)
    }

    private fun speakText(
        text: String,
        languageCode: String,
        onComplete: (() -> Unit)?
    ) {
        if (text.isBlank()) {
            onComplete?.invoke()
            return
        }

        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val targetLocale = if (languageCode.lowercase(Locale.ROOT) == VoiceSettings.LANGUAGE_HINDI) {
                    Locale.forLanguageTag("hi-IN")
                } else {
                    Locale.ENGLISH
                }

                var langResult = tts?.isLanguageAvailable(targetLocale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val fallbackLocale = Locale.ENGLISH
                    langResult = tts?.isLanguageAvailable(fallbackLocale)
                    if (langResult != TextToSpeech.LANG_MISSING_DATA && langResult != TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.language = fallbackLocale
                    }
                } else {
                    tts?.language = targetLocale
                }

                // Apply pitch & speed settings
                val pitch = voiceSettings.getVoicePitch()
                val speed = voiceSettings.getVoiceSpeed()
                tts?.setPitch(pitch)
                tts?.setSpeechRate(speed)

                val utteranceId = "yaadrakh_tts_${System.currentTimeMillis()}"

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}

                    override fun onDone(utteranceId: String?) {
                        shutdownTts()
                        onComplete?.invoke()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        shutdownTts()
                        onComplete?.invoke()
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        shutdownTts()
                        onComplete?.invoke()
                    }
                })

                val speakStatus = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

                if (speakStatus != TextToSpeech.SUCCESS) {
                    shutdownTts()
                    onComplete?.invoke()
                }
            } else {
                shutdownTts()
                onComplete?.invoke()
            }
        }
    }

    private fun constructSpeechText(
        title: String,
        message: String,
        userName: String,
        languageCode: String,
        isElderly: Boolean,
        repeatCount: Int
    ): String {
        val effectiveName = if (userName.isNotBlank()) userName else "Friend"
        val sb = java.lang.StringBuilder()

        for (i in 1..repeatCount.coerceAtLeast(1)) {
            if (i > 1) {
                sb.append(" . . . ") // Pause between repeats
            }

            if (languageCode.lowercase(Locale.ROOT) == VoiceSettings.LANGUAGE_HINDI) {
                if (i == 1) {
                    if (isElderly) {
                        sb.append("नमस्ते $effectiveName जी! कृपया ध्यान दें. आपका याद रख रिमाइंडर है: $title. $message")
                    } else {
                        sb.append("नमस्ते $effectiveName! यह आपका रिमाइंडर है: $title. $message")
                    }
                } else {
                    sb.append("रिमाइंडर दोहराया जा रहा है: $title. $message")
                }
            } else {
                if (i == 1) {
                    if (isElderly) {
                        sb.append("Hello $effectiveName. Please take note. Your YaadRakh reminder for $title is: $message")
                    } else {
                        sb.append("Hello $effectiveName! Here is your reminder for $title: $message")
                    }
                } else {
                    sb.append("Repeating reminder: $title. $message")
                }
            }
        }

        return sb.toString()
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shutdownTts() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
