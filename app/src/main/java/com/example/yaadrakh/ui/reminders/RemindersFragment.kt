package com.example.yaadrakh.ui.reminders

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.yaadrakh.R
import com.example.yaadrakh.data.local.ReminderEntity
import com.example.yaadrakh.databinding.FragmentRemindersBinding
import com.example.yaadrakh.ui.home.ReminderAdapter
import com.example.yaadrakh.ui.reminder.AddReminderFragment
import com.example.yaadrakh.ui.reminder.ReminderDetailsFragment
import com.example.yaadrakh.viewmodel.ReminderViewModel

class RemindersFragment : Fragment() {

    private var _binding: FragmentRemindersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReminderViewModel by viewModels()
    private lateinit var adapter: ReminderAdapter

    private var currentQuery: String = ""
    private var currentFilter: ReminderViewModel.FilterType = ReminderViewModel.FilterType.ALL
    private var currentPersonFilter: String = "ALL"
    private var rawReminders: List<ReminderEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRemindersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupFilters()
        setupPersonFilter()
        setupClickListeners()
        observeReminders()
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

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s?.toString() ?: ""
                applyFilterAndSort()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when {
                checkedIds.contains(binding.chipToday.id) -> ReminderViewModel.FilterType.TODAY
                checkedIds.contains(binding.chipUpcoming.id) -> ReminderViewModel.FilterType.UPCOMING
                checkedIds.contains(binding.chipCompleted.id) -> ReminderViewModel.FilterType.COMPLETED
                checkedIds.contains(binding.chipMissed.id) -> ReminderViewModel.FilterType.MISSED
                else -> ReminderViewModel.FilterType.ALL
            }
            applyFilterAndSort()
        }
    }

    private fun setupPersonFilter() {
        viewModel.allFamilyProfiles.observe(viewLifecycleOwner) { profiles ->
            val personOptions = mutableListOf("All People", "Self / None")
            personOptions.addAll(profiles.map { it.name })

            val personAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                personOptions
            )
            binding.actvPersonFilter.setAdapter(personAdapter)

            binding.actvPersonFilter.setOnItemClickListener { _, _, position, _ ->
                currentPersonFilter = when (position) {
                    0 -> "ALL"
                    1 -> "Self / None"
                    else -> personOptions[position]
                }
                applyFilterAndSort()
            }
        }
    }

    private fun observeReminders() {
        viewModel.allReminders.observe(viewLifecycleOwner) { reminders ->
            rawReminders = reminders ?: emptyList()
            applyFilterAndSort()
        }
    }

    private fun applyFilterAndSort() {
        val filteredList = viewModel.filterAndSortReminders(
            rawReminders,
            currentQuery,
            currentFilter,
            currentPersonFilter
        )
        adapter.submitList(filteredList)

        if (filteredList.isEmpty()) {
            binding.rvReminders.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
            updateEmptyStateText()
        } else {
            binding.rvReminders.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
        }
    }

    private fun updateEmptyStateText() {
        when {
            currentQuery.isNotEmpty() -> {
                binding.tvEmptyTitle.text = "No reminders found"
                binding.tvEmptySubtitle.text = "Try searching for a different keyword."
            }
            currentFilter == ReminderViewModel.FilterType.COMPLETED -> {
                binding.tvEmptyTitle.text = "No completed reminders yet"
                binding.tvEmptySubtitle.text = "Completed reminders will appear here."
            }
            currentFilter == ReminderViewModel.FilterType.TODAY -> {
                binding.tvEmptyTitle.text = "No reminders today"
                binding.tvEmptySubtitle.text = "Enjoy your free day!"
            }
            currentFilter == ReminderViewModel.FilterType.UPCOMING -> {
                binding.tvEmptyTitle.text = "No upcoming reminders"
                binding.tvEmptySubtitle.text = "Schedule a future reminder to stay prepared."
            }
            currentFilter == ReminderViewModel.FilterType.MISSED -> {
                binding.tvEmptyTitle.text = "No missed reminders"
                binding.tvEmptySubtitle.text = "You are all caught up on your reminders!"
            }
            else -> {
                binding.tvEmptyTitle.text = "Your reminder list is empty"
                binding.tvEmptySubtitle.text = "Create your first reminder to get started."
            }
        }
    }

    private fun openReminderDetails(reminderId: Long) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, ReminderDetailsFragment.newInstance(reminderId))
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
