package com.example.yaadrakh.ui.routine

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.yaadrakh.R
import com.example.yaadrakh.data.local.RoutineEntity
import com.example.yaadrakh.databinding.DialogAddEditRoutineBinding
import com.example.yaadrakh.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddEditRoutineDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_ROUTINE_ID = "arg_routine_id"

        fun newInstance(routineId: Long? = null): AddEditRoutineDialogFragment {
            return AddEditRoutineDialogFragment().apply {
                arguments = Bundle().apply {
                    if (routineId != null) putLong(ARG_ROUTINE_ID, routineId)
                }
            }
        }
    }

    private var _binding: DialogAddEditRoutineBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReminderViewModel by activityViewModels()

    private var routineId: Long? = null
    private var selectedHour: Int = 8
    private var selectedMinute: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_YaadRakh)
        arguments?.let {
            if (it.containsKey(ARG_ROUTINE_ID)) {
                routineId = it.getLong(ARG_ROUTINE_ID)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditRoutineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val calendar = Calendar.getInstance()
        selectedHour = calendar.get(Calendar.HOUR_OF_DAY)
        selectedMinute = calendar.get(Calendar.MINUTE)
        updateTimeText()

        if (routineId != null) {
            binding.tvDialogTitle.text = "Edit Daily Routine"
            binding.btnSaveRoutine.text = "Update Routine"
            loadRoutineData(routineId!!)
        } else {
            binding.tvDialogTitle.text = "Add Daily Routine"
            binding.btnSaveRoutine.text = "Save Routine"
        }

        binding.cardTimePicker.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    selectedHour = hourOfDay
                    selectedMinute = minute
                    updateTimeText()
                },
                selectedHour,
                selectedMinute,
                false
            ).show()
        }

        binding.btnSaveRoutine.setOnClickListener {
            saveRoutine()
        }
    }

    private fun loadRoutineData(id: Long) {
        viewModel.allRoutines.observe(viewLifecycleOwner) { routines ->
            val routine = routines.find { it.id == id }
            if (routine != null) {
                binding.etTitle.setText(routine.title)
                binding.etDescription.setText(routine.description)
                selectedHour = routine.hour
                selectedMinute = routine.minute
                updateTimeText()
                binding.switchVoice.isChecked = routine.voiceEnabled
                selectCategoryChip(routine.category)
            }
        }
    }

    private fun updateTimeText() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
        }
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        binding.tvSelectedTime.text = timeFormat.format(calendar.time)
    }

    private fun getSelectedCategory(): String {
        return when (binding.chipGroupCategory.checkedChipId) {
            R.id.chipExercise -> "Exercise"
            R.id.chipFood -> "Food"
            R.id.chipMedicine -> "Medicine"
            R.id.chipWork -> "Work"
            R.id.chipStudy -> "Study"
            R.id.chipPersonal -> "Personal"
            else -> "Sleep"
        }
    }

    private fun selectCategoryChip(category: String) {
        val chipId = when (category) {
            "Exercise" -> R.id.chipExercise
            "Food" -> R.id.chipFood
            "Medicine" -> R.id.chipMedicine
            "Work" -> R.id.chipWork
            "Study" -> R.id.chipStudy
            "Personal" -> R.id.chipPersonal
            else -> R.id.chipSleep
        }
        binding.chipGroupCategory.check(chipId)
    }

    private fun saveRoutine() {
        val title = binding.etTitle.text?.toString()?.trim() ?: ""
        val description = binding.etDescription.text?.toString()?.trim() ?: ""

        if (title.isEmpty()) {
            binding.tilTitle.error = "Title is required"
            return
        }
        binding.tilTitle.error = null

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
        }
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(calendar.time)

        val routine = RoutineEntity(
            id = routineId ?: 0,
            title = title,
            description = description,
            category = getSelectedCategory(),
            time = formattedTime,
            hour = selectedHour,
            minute = selectedMinute,
            enabled = true,
            notificationEnabled = true,
            voiceEnabled = binding.switchVoice.isChecked
        )

        if (routineId != null) {
            viewModel.updateRoutine(routine) {
                Toast.makeText(requireContext(), "Routine updated", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        } else {
            viewModel.addRoutine(routine) {
                Toast.makeText(requireContext(), "Routine saved", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
