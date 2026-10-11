package com.example.yaadrakh.ui.reminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.yaadrakh.R
import com.example.yaadrakh.data.local.ReminderEntity
import com.example.yaadrakh.databinding.FragmentReminderDetailsBinding
import com.example.yaadrakh.utils.DateTimeUtils
import com.example.yaadrakh.viewmodel.ReminderViewModel

class ReminderDetailsFragment : Fragment() {

    companion object {
        private const val ARG_REMINDER_ID = "arg_reminder_id"

        fun newInstance(reminderId: Long): ReminderDetailsFragment {
            val fragment = ReminderDetailsFragment()
            val args = Bundle().apply {
                putLong(ARG_REMINDER_ID, reminderId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentReminderDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReminderViewModel by viewModels()

    private var reminderId: Long = -1L
    private var currentReminder: ReminderEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reminderId = arguments?.getLong(ARG_REMINDER_ID, -1L) ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReminderDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadReminderDetails()
    }

    private fun loadReminderDetails() {
        if (reminderId == -1L) return

        viewModel.getReminderById(reminderId) { reminder ->
            if (reminder == null) {
                parentFragmentManager.popBackStack()
                return@getReminderById
            }

            currentReminder = reminder
            bindReminderData(reminder)
        }
    }

    private fun bindReminderData(reminder: ReminderEntity) {
        binding.tvCategoryEmoji.text = DateTimeUtils.getCategoryEmoji(reminder.category)
        binding.tvTitle.text = reminder.title
        binding.tvMessage.text = reminder.message
        binding.tvForWhomDetail.text = if (reminder.forWhom.isNotBlank()) reminder.forWhom else "Self / None"
        binding.tvCategory.text = reminder.category
        binding.tvDate.text = reminder.date
        binding.tvTime.text = reminder.time
        binding.tvRepeat.text = reminder.repeatType

        binding.tvNotificationStatus.text =
            if (reminder.isNotificationEnabled) "Enabled" else "Disabled"
        binding.tvVoiceStatus.text =
            if (reminder.isVoiceEnabled) "Enabled" else "Disabled"

        if (reminder.isCompleted) {
            binding.tvStatusBadge.text = getString(R.string.status_completed)
            binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_completed)
            binding.btnToggleComplete.text = "Mark as Pending"
        } else if (reminder.snoozedUntil > System.currentTimeMillis()) {
            binding.tvStatusBadge.text = "Snoozed 💤"
            binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
            binding.btnToggleComplete.text = "Mark as Completed"
        } else {
            binding.tvStatusBadge.text = getString(R.string.status_pending)
            binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
            binding.btnToggleComplete.text = "Mark as Completed"
        }

        binding.btnToggleEnable.text =
            if (reminder.isNotificationEnabled) "Disable Reminder" else "Enable Reminder"

        setupClickListeners(reminder)
    }

    private fun setupClickListeners(reminder: ReminderEntity) {
        binding.btnEditReminder.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddReminderFragment.newInstance(reminder.id))
                .addToBackStack(null)
                .commit()
        }

        binding.btnSnooze.setOnClickListener {
            showSnoozeDialog(reminder)
        }

        binding.btnToggleComplete.setOnClickListener {
            viewModel.markReminderCompleted(reminder.id, !reminder.isCompleted) {
                loadReminderDetails()
            }
        }

        binding.btnToggleEnable.setOnClickListener {
            viewModel.toggleReminderEnable(reminder, !reminder.isNotificationEnabled)
            loadReminderDetails()
        }

        binding.btnDeleteReminder.setOnClickListener {
            showDeleteConfirmation(reminder)
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
                viewModel.snoozeReminder(reminder, minutes) {
                    Toast.makeText(requireContext(), "Reminder snoozed for $minutes minutes", Toast.LENGTH_SHORT).show()
                    loadReminderDetails()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation(reminder: ReminderEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_reminder)
            .setMessage("Are you sure you want to delete this reminder?")
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteReminder(reminder) {
                    Toast.makeText(requireContext(), "Reminder deleted", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
