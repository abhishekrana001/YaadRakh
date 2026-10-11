package com.example.yaadrakh.ui.routine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.yaadrakh.R
import com.example.yaadrakh.data.local.RoutineEntity
import com.example.yaadrakh.data.local.RoutineOccurrenceEntity
import com.example.yaadrakh.databinding.FragmentRoutinesBinding
import com.example.yaadrakh.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RoutinesFragment : Fragment() {

    private var _binding: FragmentRoutinesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReminderViewModel by activityViewModels()
    private lateinit var adapter: RoutineAdapter

    private var routinesList: List<RoutineEntity> = emptyList()
    private var occurrencesList: List<RoutineOccurrenceEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoutinesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeRoutines()
    }

    private fun setupRecyclerView() {
        adapter = RoutineAdapter(
            onItemClick = { routine ->
                AddEditRoutineDialogFragment.newInstance(routine.id)
                    .show(childFragmentManager, "AddEditRoutineDialog")
            },
            onToggleComplete = { routine, isCompleted ->
                viewModel.markRoutineOccurrence(routine.id, isCompleted)
            },
            onToggleEnable = { routine, isEnabled ->
                viewModel.toggleRoutineEnabled(routine, isEnabled)
            },
            onDeleteClick = { routine ->
                showDeleteConfirmationDialog(routine)
            }
        )
        binding.rvRoutines.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRoutines.adapter = adapter
    }

    private fun observeRoutines() {
        viewModel.allRoutines.observe(viewLifecycleOwner) { list ->
            routinesList = list ?: emptyList()
            adapter.submitList(routinesList)
            updateProgressCard()

            if (routinesList.isEmpty()) {
                binding.rvRoutines.visibility = View.GONE
                binding.layoutEmptyRoutines.visibility = View.VISIBLE
            } else {
                binding.rvRoutines.visibility = View.VISIBLE
                binding.layoutEmptyRoutines.visibility = View.GONE
            }
        }

        viewModel.todayRoutineOccurrences.observe(viewLifecycleOwner) { occurrences ->
            occurrencesList = occurrences ?: emptyList()
            val completedIds = occurrences.filter { it.status == "COMPLETED" }.map { it.routineId }.toSet()
            adapter.setTodayCompletedRoutineIds(completedIds)
            updateProgressCard()
        }
    }

    private fun updateProgressCard() {
        val totalRoutines = routinesList.size
        val completedIds = occurrencesList.filter { it.status == "COMPLETED" }.map { it.routineId }.toSet()
        val completedRoutines = routinesList.count { completedIds.contains(it.id) }
        val percent = if (totalRoutines > 0) (completedRoutines * 100) / totalRoutines else 0

        val dateFormat = SimpleDateFormat("EEEE, d MMM", Locale.getDefault())
        val todayDateFormatted = dateFormat.format(Date()).uppercase(Locale.ROOT)

        binding.tvTodayRoutineDate.text = "TODAY'S ROUTINES • $todayDateFormatted"
        binding.tvRoutineProgressStatus.text = "$completedRoutines of $totalRoutines completed"
        binding.progressBarRoutines.progress = percent
        binding.tvRoutineProgressPercent.text = "$percent%"
    }

    private fun showDeleteConfirmationDialog(routine: RoutineEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Routine")
            .setMessage("Are you sure you want to delete '${routine.title}'? Past occurrences will also be deleted.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteRoutine(routine) {
                    Toast.makeText(requireContext(), "Routine deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupClickListeners() {
        val openAdd = {
            AddEditRoutineDialogFragment.newInstance()
                .show(childFragmentManager, "AddEditRoutineDialog")
        }

        binding.fabAddRoutine.setOnClickListener { openAdd() }
        binding.btnEmptyAddRoutine.setOnClickListener { openAdd() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
