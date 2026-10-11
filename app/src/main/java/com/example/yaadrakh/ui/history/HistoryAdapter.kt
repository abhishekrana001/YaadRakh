package com.example.yaadrakh.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.yaadrakh.R
import com.example.yaadrakh.data.local.ReminderHistoryEntity
import com.example.yaadrakh.databinding.ItemHistoryBinding
import com.example.yaadrakh.utils.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter : ListAdapter<ReminderHistoryEntity, HistoryAdapter.HistoryViewHolder>(DiffCallback) {

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(history: ReminderHistoryEntity, showHeader: Boolean, headerText: String) {
            val context = binding.root.context

            if (showHeader) {
                binding.tvDateHeader.visibility = View.VISIBLE
                binding.tvDateHeader.text = headerText
            } else {
                binding.tvDateHeader.visibility = View.GONE
            }

            binding.tvHistoryTitle.text = history.title
            binding.tvHistoryMessage.text = history.message

            val emoji = DateTimeUtils.getCategoryEmoji(history.category)
            binding.tvHistoryCategory.text = "$emoji ${history.category}"

            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val scheduledTimeString = timeFormat.format(Date(history.scheduledTimeMillis))
            binding.tvHistoryScheduledTime.text = "Scheduled: $scheduledTimeString"

            when (history.status.uppercase(Locale.ROOT)) {
                "COMPLETED" -> {
                    binding.tvHistoryStatusBadge.text = "✓ COMPLETED"
                    binding.tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_completed)
                    binding.tvHistoryStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.accent))

                    val actionTimeString = timeFormat.format(Date(history.actionTimeMillis))
                    binding.tvHistoryActionDetail.text = "Completed at $actionTimeString"
                    binding.tvHistoryActionDetail.setTextColor(ContextCompat.getColor(context, R.color.accent))
                }
                "MISSED" -> {
                    binding.tvHistoryStatusBadge.text = "⚠ MISSED"
                    binding.tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_status_pending)
                    binding.tvHistoryStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.red))

                    binding.tvHistoryActionDetail.text = "Missed"
                    binding.tvHistoryActionDetail.setTextColor(ContextCompat.getColor(context, R.color.red))
                }
                "SNOOZED" -> {
                    binding.tvHistoryStatusBadge.text = "⏰ SNOOZED"
                    binding.tvHistoryStatusBadge.setBackgroundResource(R.drawable.bg_category_chip)
                    binding.tvHistoryStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.primary))

                    binding.tvHistoryActionDetail.text = if (history.note.isNotBlank()) history.note else "Snoozed"
                    binding.tvHistoryActionDetail.setTextColor(ContextCompat.getColor(context, R.color.primary))
                }
                else -> {
                    binding.tvHistoryStatusBadge.text = history.status
                    binding.tvHistoryActionDetail.text = ""
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val currentItem = getItem(position)
        val currentHeader = getHeaderForPosition(position)
        val previousHeader = if (position > 0) getHeaderForPosition(position - 1) else null

        val showHeader = position == 0 || currentHeader != previousHeader
        holder.bind(currentItem, showHeader, currentHeader)
    }

    private fun getHeaderForPosition(position: Int): String {
        val item = getItem(position)
        val startOfToday = DateTimeUtils.getStartOfDay()
        val endOfToday = DateTimeUtils.getEndOfDay()
        val startOfYesterday = startOfToday - (24 * 60 * 60 * 1000L)

        return when {
            item.scheduledTimeMillis in startOfToday..endOfToday -> "TODAY"
            item.scheduledTimeMillis in startOfYesterday until startOfToday -> "YESTERDAY"
            else -> {
                val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                dateFormat.format(Date(item.scheduledTimeMillis)).uppercase(Locale.ROOT)
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<ReminderHistoryEntity>() {
        override fun areItemsTheSame(oldItem: ReminderHistoryEntity, newItem: ReminderHistoryEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ReminderHistoryEntity, newItem: ReminderHistoryEntity): Boolean {
            return oldItem == newItem
        }
    }
}
