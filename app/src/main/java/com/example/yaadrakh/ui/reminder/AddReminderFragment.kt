package com.example.yaadrakh.ui.reminder

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.yaadrakh.R
import com.example.yaadrakh.alarm.AlarmScheduler
import com.example.yaadrakh.data.local.ReminderEntity
import com.example.yaadrakh.databinding.FragmentAddReminderBinding
import com.example.yaadrakh.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddReminderFragment : Fragment() {

    companion object {
        const val ARG_REMINDER_ID = "arg_reminder_id"

        fun newInstance(reminderId: Long = -1L): AddReminderFragment {
            val fragment = AddReminderFragment()
            val args = Bundle().apply {
                putLong(ARG_REMINDER_ID, reminderId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentAddReminderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReminderViewModel by viewModels()

    private var reminderId: Long = -1L
    private val selectedCalendar: Calendar = Calendar.getInstance()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(
                    requireContext(),
                    "Notification permission is needed to display reminder alerts",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reminderId = arguments?.getLong(ARG_REMINDER_ID, -1L) ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddReminderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupDropdowns()
        setupDateAndTimePickers()
        setupClickListeners()
        setupBackPressHandler()
        checkAndRequestPermissions()

        if (reminderId != -1L) {
            loadReminderForEditing(reminderId)
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmScheduler = AlarmScheduler(requireContext())
            if (!alarmScheduler.canScheduleExactAlarms()) {
                showExactAlarmPermissionDialog()
            }
        }
    }

    private fun showExactAlarmPermissionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Exact Alarm Permission Required")
            .setMessage("YaadRakh requires exact alarm permission to deliver reminders precisely on time. Please enable it in Settings.")
            .setPositiveButton("Settings") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${requireContext().packageName}")
                    }
                    startActivity(intent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            handleBackNavigation()
        }

        if (reminderId != -1L) {
            binding.toolbar.title = getString(R.string.edit_reminder)
            binding.btnSaveReminder.text = getString(R.string.update_reminder)
        } else {
            binding.toolbar.title = getString(R.string.add_reminder_title)
            binding.btnSaveReminder.text = getString(R.string.save_reminder)
        }
    }

    private fun loadReminderForEditing(id: Long) {
        viewModel.getReminderById(id) { reminder ->
            reminder?.let {
                binding.etTitle.setText(it.title)
                binding.etMessage.setText(it.message)
                binding.actvCategory.setText(it.category, false)
                binding.actvRepeat.setText(it.repeatType, false)
                binding.actvForWhom.setText(if (it.forWhom.isNotBlank()) it.forWhom else "Self / None", false)
                binding.switchNotification.isChecked = it.isNotificationEnabled
                binding.switchVoice.isChecked = it.isVoiceEnabled

                val countText = when (it.voiceRepeatCount) {
                    1 -> "1 Time"
                    3 -> "3 Times"
                    5 -> "5 Times"
                    else -> "2 Times"
                }
                binding.actvVoiceRepeat.setText(countText, false)

                selectedCalendar.timeInMillis = it.scheduledTimeMillis
                updateDateDisplay()
                updateTimeDisplay()
            }
        }
    }

    private fun setupDropdowns() {
        val categories = resources.getStringArray(R.array.categories_array)
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        binding.actvCategory.setAdapter(categoryAdapter)

        val repeatOptions = resources.getStringArray(R.array.repeat_array)
        val repeatAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            repeatOptions
        )
        binding.actvRepeat.setAdapter(repeatAdapter)

        val voiceRepeatOptions = resources.getStringArray(R.array.voice_repeats_array)
        val voiceRepeatAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            voiceRepeatOptions
        )
        binding.actvVoiceRepeat.setAdapter(voiceRepeatAdapter)

        viewModel.allFamilyProfiles.observe(viewLifecycleOwner) { profiles ->
            val personOptions = mutableListOf("Self / None")
            personOptions.addAll(profiles.map { it.name })
            val forWhomAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                personOptions
            )
            binding.actvForWhom.setAdapter(forWhomAdapter)
        }
    }

    private fun setupDateAndTimePickers() {
        updateDateDisplay()
        updateTimeDisplay()

        binding.cardDatePicker.setOnClickListener { showDatePicker() }
        binding.cardTimePicker.setOnClickListener { showTimePicker() }
    }

    private fun showDatePicker() {
        val year = selectedCalendar.get(Calendar.YEAR)
        val month = selectedCalendar.get(Calendar.MONTH)
        val day = selectedCalendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedCalendar.set(Calendar.YEAR, selectedYear)
                selectedCalendar.set(Calendar.MONTH, selectedMonth)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, selectedDay)
                updateDateDisplay()
            },
            year,
            month,
            day
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val hour = selectedCalendar.get(Calendar.HOUR_OF_DAY)
        val minute = selectedCalendar.get(Calendar.MINUTE)

        TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                selectedCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                selectedCalendar.set(Calendar.MINUTE, selectedMinute)
                selectedCalendar.set(Calendar.SECOND, 0)
                selectedCalendar.set(Calendar.MILLISECOND, 0)
                updateTimeDisplay()
            },
            hour,
            minute,
            false
        ).show()
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        binding.tvSelectedDate.text = dateFormat.format(selectedCalendar.time)
    }

    private fun updateTimeDisplay() {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        binding.tvSelectedTime.text = timeFormat.format(selectedCalendar.time)
    }

    private fun setupClickListeners() {
        binding.btnSaveReminder.setOnClickListener {
            validateAndSave()
        }
    }

    private fun validateAndSave() {
        val title = binding.etTitle.text.toString().trim()
        val message = binding.etMessage.text.toString().trim()
        val repeatType = binding.actvRepeat.text.toString()
        val rawForWhom = binding.actvForWhom.text.toString().trim()
        val forWhom = if (rawForWhom.equals("Self / None", ignoreCase = true)) "" else rawForWhom

        val voiceRepeatCount = when (binding.actvVoiceRepeat.text.toString()) {
            "1 Time" -> 1
            "3 Times" -> 3
            "5 Times" -> 5
            else -> 2
        }

        var isValid = true

        if (title.isEmpty()) {
            binding.tilTitle.error = getString(R.string.title_required)
            isValid = false
        } else {
            binding.tilTitle.error = null
        }

        if (message.isEmpty()) {
            binding.tilMessage.error = getString(R.string.message_required)
            isValid = false
        } else {
            binding.tilMessage.error = null
        }

        if (!isValid) return

        if (selectedCalendar.timeInMillis <= System.currentTimeMillis() && repeatType == "Doesn't repeat") {
            Toast.makeText(
                requireContext(),
                "Please select a date and time in the future",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val reminder = ReminderEntity(
            id = if (reminderId != -1L) reminderId else 0,
            title = title,
            message = message,
            category = binding.actvCategory.text.toString(),
            date = binding.tvSelectedDate.text.toString(),
            time = binding.tvSelectedTime.text.toString(),
            scheduledTimeMillis = selectedCalendar.timeInMillis,
            repeatType = repeatType,
            isNotificationEnabled = binding.switchNotification.isChecked,
            isVoiceEnabled = binding.switchVoice.isChecked,
            isCompleted = false,
            forWhom = forWhom,
            voiceRepeatCount = voiceRepeatCount
        )

        if (reminderId != -1L) {
            viewModel.updateReminder(reminder) {
                Toast.makeText(
                    requireContext(),
                    "Reminder updated successfully",
                    Toast.LENGTH_SHORT
                ).show()
                parentFragmentManager.popBackStack()
            }
        } else {
            viewModel.addReminder(reminder) {
                Toast.makeText(
                    requireContext(),
                    "Reminder saved successfully",
                    Toast.LENGTH_SHORT
                ).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackNavigation()
                }
            }
        )
    }

    private fun handleBackNavigation() {
        val hasData = binding.etTitle.text.toString().trim().isNotEmpty() ||
                binding.etMessage.text.toString().trim().isNotEmpty()

        if (hasData && reminderId == -1L) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.discard_dialog_title)
                .setMessage(R.string.discard_dialog_msg)
                .setPositiveButton(R.string.discard) { _, _ ->
                    parentFragmentManager.popBackStack()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } else {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
