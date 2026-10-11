package com.example.yaadrakh.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.yaadrakh.R
import com.example.yaadrakh.data.local.ReminderEntity
import com.example.yaadrakh.data.local.ReminderHistoryEntity
import com.example.yaadrakh.data.local.RoutineEntity
import com.example.yaadrakh.data.local.RoutineOccurrenceEntity
import com.example.yaadrakh.databinding.FragmentHomeBinding
import com.example.yaadrakh.ui.auth.NameSetupDialogFragment
import com.example.yaadrakh.ui.reminder.AddReminderFragment
import com.example.yaadrakh.ui.reminder.ReminderDetailsFragment
import com.example.yaadrakh.ui.routine.RoutinesFragment
import com.example.yaadrakh.utils.DateTimeUtils
import com.example.yaadrakh.utils.VoiceSettings
import com.example.yaadrakh.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReminderViewModel by viewModels()
    private lateinit var adapter: ReminderAdapter

    private var todayRemindersList: List<ReminderEntity> = emptyList()
    private var todayHistoryList: List<ReminderHistoryEntity> = emptyList()
    private var routinesList: List<RoutineEntity> = emptyList()
    private var occurrencesList: List<RoutineOccurrenceEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
        setupRecyclerView()
        setupClickListeners()
        observeReminders()
    }

    private fun setupHeader() {
        val voiceSettings = VoiceSettings(requireContext())
        val userName = voiceSettings.getUserName()

        binding.tvGreeting.text = getGreetingMessage()
        binding.tvUserName.text = if (userName.isNotBlank()) userName else "Friend"

        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        binding.tvTodayDate.text = dateFormat.format(Date())

        if (!voiceSettings.isUserLoggedIn()) {
            NameSetupDialogFragment { newName ->
                binding.tvUserName.text = newName
            }.show(childFragmentManager, "NameSetupDialog")
        }

        binding.tvUserName.setOnClickListener {
            NameSetupDialogFragment { newName ->
                binding.tvUserName.text = newName
            }.show(childFragmentManager, "NameSetupDialog")
        }
    }

    private fun getGreetingMessage(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> "Good Morning 👋"
            in 12..16 -> "Good Afternoon 👋"
            in 17..21 -> "Good Evening 👋"
            else -> "Good Night 🌙"
        }
    }

    private fun setupRecyclerView() {
        adapter = ReminderAdapter(
            onItemClick = { reminder ->
                openReminderDetails(reminder.id)
            },
            onToggleComplete = { reminder ->
                viewModel.markReminderCompleted(reminder.id, !reminder.isCompleted)
            },
            onToggleEnable = { reminder, isEnabled ->
                viewModel.toggleReminderEnable(reminder, isEnabled)
            }
        )
        binding.rvReminders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReminders.adapter = adapter
    }

    private fun observeReminders() {
        viewModel.todayReminders.observe(viewLifecycleOwner) { reminders ->
            todayRemindersList = reminders ?: emptyList()
            adapter.submitList(todayRemindersList)

            updateProgressCard()
            updateUpcomingCard(todayRemindersList)
            updateMorningBriefingCard()

            if (todayRemindersList.isEmpty()) {
                binding.rvReminders.visibility = View.GONE
                binding.layoutEmptyState.visibility = View.VISIBLE
            } else {
                binding.rvReminders.visibility = View.VISIBLE
                binding.layoutEmptyState.visibility = View.GONE
            }
        }

        viewModel.todayHistory.observe(viewLifecycleOwner) { history ->
            todayHistoryList = history ?: emptyList()
            updateProgressCard()
        }

        viewModel.missedReminders.observe(viewLifecycleOwner) { missedList ->
            for (missed in missedList) {
                viewModel.recordMissed(missed)
            }
            updateMissedCard(missedList)
        }

        viewModel.allRoutines.observe(viewLifecycleOwner) { routines ->
            routinesList = routines ?: emptyList()
            updateMorningBriefingCard()
        }

        viewModel.todayRoutineOccurrences.observe(viewLifecycleOwner) { occurrences ->
            occurrencesList = occurrences ?: emptyList()
            updateMorningBriefingCard()
        }
    }

    private fun updateMorningBriefingCard() {
        val enabledRoutines = routinesList.filter { it.enabled }
        val totalRoutines = enabledRoutines.size
        val completedIds = occurrencesList.filter { it.status == "COMPLETED" }.map { it.routineId }.toSet()
        val completedRoutines = enabledRoutines.count { completedIds.contains(it.id) }
        val remainingRoutines = maxOf(0, totalRoutines - completedRoutines)

        if (totalRoutines > 0) {
            binding.tvBriefingSummary.text =
                "$totalRoutines routines scheduled • $completedRoutines completed • $remainingRoutines remaining"
        } else {
            binding.tvBriefingSummary.text =
                "No routines set for today. Tap below to manage daily routines!"
        }

        // Calculate Next Up
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val nowMinuteOfDay = currentHour * 60 + currentMinute

        val upcomingRoutine = enabledRoutines
            .filter { !completedIds.contains(it.id) }
            .filter { (it.hour * 60 + it.minute) >= nowMinuteOfDay }
            .minByOrNull { it.hour * 60 + it.minute }

        val nowMillis = System.currentTimeMillis()
        val upcomingReminder = todayRemindersList
            .filter { !it.isCompleted && it.scheduledTimeMillis > nowMillis }
            .minByOrNull { it.scheduledTimeMillis }

        if (upcomingRoutine != null) {
            val emoji = DateTimeUtils.getCategoryEmoji(upcomingRoutine.category)
            binding.tvBriefingNextUp.text = "$emoji ${upcomingRoutine.title} — ${upcomingRoutine.time}"
            binding.layoutNextUp.visibility = View.VISIBLE
        } else if (upcomingReminder != null) {
            val emoji = DateTimeUtils.getCategoryEmoji(upcomingReminder.category)
            binding.tvBriefingNextUp.text = "$emoji ${upcomingReminder.title} — ${upcomingReminder.time}"
            binding.layoutNextUp.visibility = View.VISIBLE
        } else {
            binding.layoutNextUp.visibility = View.GONE
        }
    }

    private fun updateMissedCard(missedList: List<ReminderEntity>) {
        val missed = missedList.firstOrNull()
        if (missed != null) {
            binding.cardMissedReminders.visibility = View.VISIBLE
            binding.tvMissedTime.text = "${missed.date} ${missed.time}"
            val emoji = DateTimeUtils.getCategoryEmoji(missed.category)
            binding.tvMissedTitle.text = "$emoji ${missed.title}"
            binding.tvMissedMessage.text = missed.message

            binding.btnMissedDone.setOnClickListener {
                viewModel.markReminderCompleted(missed.id, true)
            }

            binding.btnMissedSnooze.setOnClickListener {
                showSnoozeDialog(missed)
            }
        } else {
            binding.cardMissedReminders.visibility = View.GONE
        }
    }

    private fun showSnoozeDialog(reminder: ReminderEntity) {
        val options = arrayOf("5 Minutes", "10 Minutes", "30 Minutes")
        AlertDialog.Builder(requireContext())
            .setTitle("Snooze Reminder")
            .setItems(options) { _, which ->
                val minutes = when (which) {
                    0 -> 5
                    1 -> 10
                    2 -> 30
                    else -> 10
                }
                viewModel.snoozeReminder(reminder, minutes)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateProgressCard() {
        val now = System.currentTimeMillis()

        val completedRemindersCount = todayRemindersList.count { it.isCompleted }
        val completedHistoryCount = todayHistoryList.count { it.status == "COMPLETED" }
        val completedCount = maxOf(completedRemindersCount, completedHistoryCount)

        val missedHistoryCount = todayHistoryList.count { it.status == "MISSED" }
        val missedRemindersCount = todayRemindersList.count { !it.isCompleted && it.scheduledTimeMillis < now && it.snoozedUntil <= now }
        val missedCount = maxOf(missedHistoryCount, missedRemindersCount)

        val totalCount = maxOf(todayRemindersList.size, completedCount + missedCount)

        val percent = if (totalCount > 0) (completedCount * 100) / totalCount else 0

        binding.tvProgressStatus.text =
            getString(R.string.progress_status, completedCount, totalCount)
        binding.tvProgressSummary.text =
            getString(R.string.progress_summary, completedCount, missedCount)
        binding.progressBarReminders.progress = percent
        binding.tvProgressPercent.text = "$percent%"
    }

    private fun updateUpcomingCard(reminders: List<ReminderEntity>) {
        val now = System.currentTimeMillis()
        val upcoming = reminders.filter { !it.isCompleted && it.scheduledTimeMillis > now }
            .minByOrNull { it.scheduledTimeMillis }

        if (upcoming != null) {
            binding.cardUpcoming.visibility = View.VISIBLE
            binding.tvUpcomingTime.text = upcoming.time
            val emoji = DateTimeUtils.getCategoryEmoji(upcoming.category)
            binding.tvUpcomingTitle.text = "$emoji ${upcoming.title}"
            binding.tvUpcomingMessage.text = upcoming.message

            val diffMinutes = (upcoming.scheduledTimeMillis - now) / (1000 * 60)
            binding.tvUpcomingCountdown.text = if (diffMinutes > 0) "in ${diffMinutes}m" else "due now"
        } else {
            binding.cardUpcoming.visibility = View.GONE
        }
    }

    private fun openReminderDetails(reminderId: Long) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, ReminderDetailsFragment.newInstance(reminderId))
            .addToBackStack(null)
            .commit()
    }

    private fun openRoutinesFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, RoutinesFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun setupClickListeners() {
        val openAdd = {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddReminderFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.fabAddReminder.setOnClickListener { openAdd() }
        binding.btnEmptyAdd.setOnClickListener { openAdd() }
        binding.btnManageRoutines.setOnClickListener { openRoutinesFragment() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
