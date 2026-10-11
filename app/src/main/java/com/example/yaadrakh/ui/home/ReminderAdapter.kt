package com.example.yaadrakh.ui.home

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.yaadrakh.R
import com.example.yaadrakh.data.local.ReminderEntity
import com.example.yaadrakh.databinding.ItemReminderBinding
import com.example.yaadrakh.utils.DateTimeUtils

class ReminderAdapter(
    private val onItemClick: (ReminderEntity) -> Unit,
    private val onToggleComplete: (ReminderEntity) -> Unit,
    private val onToggleEnable: ((ReminderEntity, Boolean) -> Unit)? = null
) : ListAdapter<ReminderEntity, ReminderAdapter.ReminderViewHolder>(DiffCallback) {

    inner class ReminderViewHolder(private val binding: ItemReminderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(reminder: ReminderEntity) {
            val context = binding.root.context

            val emoji = DateTimeUtils.getCategoryEmoji(reminder.category)
            binding.tvCategoryEmoji.text = emoji
            binding.tvReminderTitle.text = reminder.title
            binding.tvReminderMessage.text = reminder.message
            binding.tvCategoryName.text = reminder.category

            if (reminder.forWhom.isNotBlank()) {
                binding.tvForWhom.text = "👤 ${reminder.forWhom}"
                binding.tvForWhom.visibility = View.VISIBLE
            } else {
                binding.tvForWhom.visibility = View.GONE
            }

            val dateTimeRepeatText = "${reminder.date} • ${reminder.time} • ${reminder.repeatType}"
            binding.tvDateTimeRepeat.text = dateTimeRepeatText

            binding.tvNotificationBadge.visibility =
                if (reminder.isNotificationEnabled) View.VISIBLE else View.GONE
            binding.tvVoiceBadge.visibility =
                if (reminder.isVoiceEnabled) View.VISIBLE else View.GONE

            binding.switchEnable.setOnCheckedChangeListener(null)
            binding.switchEnable.isChecked = reminder.isNotificationEnabled
            binding.switchEnable.setOnCheckedChangeListener { _, isChecked ->
                onToggleEnable?.invoke(reminder, isChecked)
            }

            if (reminder.isCompleted) {
                binding.cardReminder.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.card_completed_bg)
                )
                binding.tvReminderTitle.paintFlags =
                    binding.tvReminderTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvReminderTitle.setTextColor(
                    ContextCompat.getColor(context, R.color.text_muted)
                )
                binding.btnToggleComplete.text = context.getString(R.string.status_completed)
                binding.btnToggleComplete.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.status_completed_bg)
                )
                binding.btnToggleComplete.setTextColor(
                    ContextCompat.getColor(context, R.color.status_completed_text)
                )
            } else {
                binding.cardReminder.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.surface_light)
                )
                binding.tvReminderTitle.paintFlags =
                    binding.tvReminderTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvReminderTitle.setTextColor(
                    ContextCompat.getColor(context, R.color.text_primary)
                )
                binding.btnToggleComplete.text = context.getString(R.string.status_pending)
                binding.btnToggleComplete.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.status_pending_bg)
                )
                binding.btnToggleComplete.setTextColor(
                    ContextCompat.getColor(context, R.color.status_pending_text)
                )
            }

            binding.root.setOnClickListener { onItemClick(reminder) }
            binding.btnToggleComplete.setOnClickListener { onToggleComplete(reminder) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val binding =
            ItemReminderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReminderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object DiffCallback : DiffUtil.ItemCallback<ReminderEntity>() {
        override fun areItemsTheSame(
            oldItem: ReminderEntity,
            newItem: ReminderEntity
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: ReminderEntity,
            newItem: ReminderEntity
        ): Boolean {
            return oldItem == newItem
        }
    }
}
