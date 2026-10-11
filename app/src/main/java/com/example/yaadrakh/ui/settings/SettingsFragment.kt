package com.example.yaadrakh.ui.settings

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.yaadrakh.R
import com.example.yaadrakh.alarm.AlarmScheduler
import com.example.yaadrakh.databinding.FragmentSettingsBinding
import com.example.yaadrakh.ui.auth.NameSetupDialogFragment
import com.example.yaadrakh.ui.family.FamilyProfilesDialogFragment
import com.example.yaadrakh.utils.VoiceReminderHelper
import com.example.yaadrakh.utils.VoiceSettings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var voiceSettings: VoiceSettings
    private var voiceReminderHelper: VoiceReminderHelper? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        voiceSettings = VoiceSettings(requireContext())
        setupUserProfile()
        setupElderlyMode()
        setupFamilyProfiles()
        setupLanguageSelection()
        setupVoiceCustomization()
        setupMorningBriefing()
        setupTestVoiceButton()
    }

    private fun setupUserProfile() {
        val userName = voiceSettings.getUserName()
        binding.tvProfileName.text = if (userName.isNotBlank()) "Name: $userName" else "Name: Friend"

        binding.btnEditProfileName.setOnClickListener {
            NameSetupDialogFragment { newName ->
                binding.tvProfileName.text = "Name: $newName"
            }.show(childFragmentManager, "NameSetupDialog")
        }
    }

    private fun setupElderlyMode() {
        binding.switchElderlyMode.isChecked = voiceSettings.isElderlyModeEnabled()
        binding.switchElderlyMode.setOnCheckedChangeListener { _, isChecked ->
            voiceSettings.setElderlyModeEnabled(isChecked)
            val msg = if (isChecked) "Elderly-Friendly Mode enabled" else "Elderly-Friendly Mode disabled"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFamilyProfiles() {
        binding.btnManageFamily.setOnClickListener {
            FamilyProfilesDialogFragment().show(childFragmentManager, "FamilyProfilesDialog")
        }
    }

    private fun setupLanguageSelection() {
        val currentLanguage = voiceSettings.getVoiceLanguage()
        if (currentLanguage == VoiceSettings.LANGUAGE_HINDI) {
            binding.rbHindi.isChecked = true
        } else {
            binding.rbEnglish.isChecked = true
        }

        binding.rgVoiceLanguage.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == binding.rbHindi.id) {
                voiceSettings.setVoiceLanguage(VoiceSettings.LANGUAGE_HINDI)
            } else {
                voiceSettings.setVoiceLanguage(VoiceSettings.LANGUAGE_ENGLISH)
            }
        }
    }

    private fun setupVoiceCustomization() {
        val repeatOptions = resources.getStringArray(R.array.voice_repeats_array)
        val repeatAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, repeatOptions)
        binding.actvDefaultVoiceRepeat.setAdapter(repeatAdapter)

        val currentCount = voiceSettings.getVoiceRepeatCount()
        binding.actvDefaultVoiceRepeat.setText("${currentCount} Time" + if (currentCount > 1) "s" else "", false)

        binding.actvDefaultVoiceRepeat.setOnItemClickListener { _, _, position, _ ->
            val count = when (position) {
                0 -> 1
                1 -> 2
                2 -> 3
                3 -> 5
                else -> 2
            }
            voiceSettings.setVoiceRepeatCount(count)
            Toast.makeText(requireContext(), "Default voice repeats set to $count", Toast.LENGTH_SHORT).show()
        }

        val toneOptions = resources.getStringArray(R.array.voice_tones_array)
        val toneAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, toneOptions)
        binding.actvVoiceTone.setAdapter(toneAdapter)

        val currentTone = voiceSettings.getVoiceTone()
        val toneText = when (currentTone) {
            VoiceSettings.TONE_HIGH_CLEAR -> "Polite / Clear High Tone"
            VoiceSettings.TONE_DEEP -> "Deep Tone"
            else -> "Natural Warm Tone"
        }
        binding.actvVoiceTone.setText(toneText, false)

        binding.actvVoiceTone.setOnItemClickListener { _, _, position, _ ->
            val toneKey = when (position) {
                1 -> VoiceSettings.TONE_HIGH_CLEAR
                2 -> VoiceSettings.TONE_DEEP
                else -> VoiceSettings.TONE_WARM
            }
            voiceSettings.setVoiceTone(toneKey)
            Toast.makeText(requireContext(), "Voice tone updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMorningBriefing() {
        binding.switchMorningBriefing.isChecked = voiceSettings.isMorningBriefingEnabled()
        updateBriefingTimeDisplay()

        binding.switchMorningBriefing.setOnCheckedChangeListener { _, isChecked ->
            voiceSettings.setMorningBriefingEnabled(isChecked)
            val alarmScheduler = AlarmScheduler(requireContext())
            if (isChecked) {
                alarmScheduler.scheduleMorningBriefing(
                    voiceSettings.getBriefingHour(),
                    voiceSettings.getBriefingMinute()
                )
                Toast.makeText(requireContext(), "Morning Briefing enabled", Toast.LENGTH_SHORT).show()
            } else {
                alarmScheduler.cancelMorningBriefing()
                Toast.makeText(requireContext(), "Morning Briefing disabled", Toast.LENGTH_SHORT).show()
            }
        }

        binding.layoutBriefingTime.setOnClickListener {
            val hour = voiceSettings.getBriefingHour()
            val minute = voiceSettings.getBriefingMinute()

            TimePickerDialog(
                requireContext(),
                { _, selectedHour, selectedMinute ->
                    voiceSettings.setBriefingHour(selectedHour)
                    voiceSettings.setBriefingMinute(selectedMinute)
                    updateBriefingTimeDisplay()

                    if (voiceSettings.isMorningBriefingEnabled()) {
                        AlarmScheduler(requireContext()).scheduleMorningBriefing(selectedHour, selectedMinute)
                    }
                },
                hour,
                minute,
                false
            ).show()
        }

        binding.btnTestBriefing.setOnClickListener {
            val languageCode = voiceSettings.getVoiceLanguage()

            Toast.makeText(requireContext(), "Testing Spoken Briefing...", Toast.LENGTH_SHORT).show()

            voiceReminderHelper?.shutdownTts()
            voiceReminderHelper = VoiceReminderHelper(requireContext()).apply {
                speakReminder(
                    title = "Morning Briefing Test",
                    message = "Today you have 3 routines scheduled and 2 reminders. Get ready for a great day!",
                    repeatCount = voiceSettings.getVoiceRepeatCount(),
                    languageCode = languageCode
                )
            }
        }
    }

    private fun updateBriefingTimeDisplay() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, voiceSettings.getBriefingHour())
            set(Calendar.MINUTE, voiceSettings.getBriefingMinute())
        }
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        binding.tvBriefingTime.text = sdf.format(calendar.time)
    }

    private fun setupTestVoiceButton() {
        binding.btnTestVoice.setOnClickListener {
            val languageCode = voiceSettings.getVoiceLanguage()
            val sampleTitle = "Take Medicine"
            val sampleMessage = "It is time to take your evening medicine."

            Toast.makeText(requireContext(), "Testing voice reminder with ${voiceSettings.getVoiceRepeatCount()} repeats...", Toast.LENGTH_SHORT).show()

            voiceReminderHelper?.shutdownTts()
            voiceReminderHelper = VoiceReminderHelper(requireContext()).apply {
                speakReminder(
                    title = sampleTitle,
                    message = sampleMessage,
                    repeatCount = voiceSettings.getVoiceRepeatCount(),
                    languageCode = languageCode
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        voiceReminderHelper?.stop()
        _binding = null
    }
}
