package com.example.yaadrakh.ui.routine

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.yaadrakh.R
import com.example.yaadrakh.data.local.RoutineEntity
import com.example.yaadrakh.databinding.ItemRoutineBinding
import com.example.yaadrakh.utils.DateTimeUtils

class RoutineAdapter(
    private val onItemClick: (RoutineEntity) -> Unit,
    private val onToggleComplete: (RoutineEntity, Boolean) -> Unit,
    private val onToggleEnable: (RoutineEntity, Boolean) -> Unit,
    private val onDeleteClick: (RoutineEntity) -> Unit
) : ListAdapter<RoutineEntity, RoutineAdapter.RoutineViewHolder>(DiffCallback) {

    private var completedRoutineIdsForToday: Set<Long> = emptySet()

    fun setTodayCompletedRoutineIds(ids: Set<Long>) {
        completedRoutineIdsForToday = ids
        notifyDataSetChanged()
    }

    inner class RoutineViewHolder(private val binding: ItemRoutineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(routine: RoutineEntity) {
            val context = binding.root.context
            val isCompletedToday = completedRoutineIdsForToday.contains(routine.id)

            binding.tvRoutineTitle.text = routine.title
            binding.tvRoutineDescription.text = routine.description
            binding.tvRoutineDescription.visibility =
                if (routine.description.isNotBlank()) View.VISIBLE else View.GONE

            binding.tvRoutineTime.text = routine.time

            val emoji = DateTimeUtils.getCategoryEmoji(routine.category)
            binding.tvRoutineCategory.text = "$emoji ${routine.category}"

            binding.tvVoiceIndicator.visibility =
                if (routine.voiceEnabled) View.VISIBLE else View.GONE

            binding.switchRoutineEnable.setOnCheckedChangeListener(null)
            binding.switchRoutineEnable.isChecked = routine.enabled
            binding.switchRoutineEnable.setOnCheckedChangeListener { _, isChecked ->
                onToggleEnable(routine, isChecked)
            }

            // Styling for completed vs pending today
            if (isCompletedToday) {
                binding.cardRoutine.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.card_completed_bg)
                )
                binding.tvRoutineTitle.paintFlags =
                    binding.tvRoutineTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvRoutineTitle.setTextColor(
                    ContextCompat.getColor(context, R.color.text_muted)
                )
                binding.btnToggleRoutineComplete.text = "Completed ✓"
                binding.btnToggleRoutineComplete.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.accent_light)
                )
                binding.btnToggleRoutineComplete.setTextColor(
                    ContextCompat.getColor(context, R.color.accent)
                )
            } else {
                binding.cardRoutine.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.surface_light)
                )
                binding.tvRoutineTitle.paintFlags =
                    binding.tvRoutineTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvRoutineTitle.setTextColor(
                    ContextCompat.getColor(context, R.color.text_primary)
                )
                binding.btnToggleRoutineComplete.text = "Mark Done"
                binding.btnToggleRoutineComplete.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.primary_light)
                )
                binding.btnToggleRoutineComplete.setTextColor(
                    ContextCompat.getColor(context, R.color.primary)
                )
            }

            // Clicks
            binding.root.setOnClickListener { onItemClick(routine) }
            binding.btnToggleRoutineComplete.setOnClickListener {
                onToggleComplete(routine, !isCompletedToday)
            }
            binding.btnRoutineDelete.setOnClickListener { onDeleteClick(routine) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutineViewHolder {
        val binding =
            ItemRoutineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoutineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoutineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object DiffCallback : DiffUtil.ItemCallback<RoutineEntity>() {
        override fun areItemsTheSame(oldItem: RoutineEntity, newItem: RoutineEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RoutineEntity, newItem: RoutineEntity): Boolean {
            return oldItem == newItem
        }
    }
}
