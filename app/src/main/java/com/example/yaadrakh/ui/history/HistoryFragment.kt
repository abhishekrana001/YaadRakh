package com.example.yaadrakh.ui.history

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.yaadrakh.data.local.ReminderHistoryEntity
import com.example.yaadrakh.databinding.FragmentHistoryBinding
import com.example.yaadrakh.viewmodel.ReminderViewModel
import com.example.yaadrakh.viewmodel.ReminderViewModel.HistoryDateFilter
import com.example.yaadrakh.viewmodel.ReminderViewModel.HistoryStatusFilter

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReminderViewModel by viewModels()
    private lateinit var adapter: HistoryAdapter

    private var currentRawHistoryList: List<ReminderHistoryEntity> = emptyList()
    private var searchQuery: String = ""
    private var currentStatusFilter: HistoryStatusFilter = HistoryStatusFilter.ALL
    private var currentDateFilter: HistoryDateFilter = HistoryDateFilter.ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupFilters()
        observeHistory()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter()
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter
    }

    private fun setupSearch() {
        binding.etHistorySearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString().orEmpty()
                applyFiltersAndDisplay()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilters() {
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            currentStatusFilter = when {
                checkedIds.contains(binding.chipStatusCompleted.id) -> HistoryStatusFilter.COMPLETED
                checkedIds.contains(binding.chipStatusMissed.id) -> HistoryStatusFilter.MISSED
                checkedIds.contains(binding.chipStatusSnoozed.id) -> HistoryStatusFilter.SNOOZED
                else -> HistoryStatusFilter.ALL
            }
            applyFiltersAndDisplay()
        }

        binding.chipGroupDate.setOnCheckedStateChangeListener { _, checkedIds ->
            currentDateFilter = when {
                checkedIds.contains(binding.chipDateToday.id) -> HistoryDateFilter.TODAY
                checkedIds.contains(binding.chipDateYesterday.id) -> HistoryDateFilter.YESTERDAY
                checkedIds.contains(binding.chipDateOlder.id) -> HistoryDateFilter.OLDER
                else -> HistoryDateFilter.ALL
            }
            applyFiltersAndDisplay()
        }
    }

    private fun observeHistory() {
        viewModel.allHistory.observe(viewLifecycleOwner) { historyList ->
            currentRawHistoryList = historyList ?: emptyList()
            applyFiltersAndDisplay()
        }
    }

    private fun applyFiltersAndDisplay() {
        val filteredList = viewModel.filterAndSearchHistory(
            currentRawHistoryList,
            searchQuery,
            currentStatusFilter,
            currentDateFilter
        )

        adapter.submitList(filteredList)

        if (filteredList.isEmpty()) {
            binding.rvHistory.visibility = View.GONE
            binding.layoutEmptyHistory.visibility = View.VISIBLE
        } else {
            binding.rvHistory.visibility = View.VISIBLE
            binding.layoutEmptyHistory.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
