package com.example.yaadrakh.ui.family

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.yaadrakh.data.local.FamilyProfileEntity
import com.example.yaadrakh.databinding.ItemFamilyProfileBinding

class FamilyProfileAdapter(
    private val onEditClick: (FamilyProfileEntity) -> Unit,
    private val onDeleteClick: (FamilyProfileEntity) -> Unit
) : ListAdapter<FamilyProfileEntity, FamilyProfileAdapter.ProfileViewHolder>(DiffCallback) {

    inner class ProfileViewHolder(private val binding: ItemFamilyProfileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(profile: FamilyProfileEntity) {
            binding.tvProfileName.text = profile.name
            binding.tvProfileRelationship.text = "Relationship: ${profile.relationship}"
            if (profile.notes.isNotBlank()) {
                binding.tvProfileNotes.text = profile.notes
                binding.tvProfileNotes.visibility = View.VISIBLE
            } else {
                binding.tvProfileNotes.visibility = View.GONE
            }

            binding.btnEditProfile.setOnClickListener { onEditClick(profile) }
            binding.btnDeleteProfile.setOnClickListener { onDeleteClick(profile) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val binding =
            ItemFamilyProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProfileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object DiffCallback : DiffUtil.ItemCallback<FamilyProfileEntity>() {
        override fun areItemsTheSame(oldItem: FamilyProfileEntity, newItem: FamilyProfileEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FamilyProfileEntity, newItem: FamilyProfileEntity): Boolean {
            return oldItem == newItem
        }
    }
}
