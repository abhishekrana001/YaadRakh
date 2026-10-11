package com.example.yaadrakh.ui.family

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.yaadrakh.R
import com.example.yaadrakh.data.local.FamilyProfileEntity
import com.example.yaadrakh.databinding.DialogAddEditFamilyProfileBinding
import com.example.yaadrakh.viewmodel.ReminderViewModel

class AddEditFamilyProfileDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_PROFILE_ID = "arg_profile_id"

        fun newInstance(profileId: Long? = null): AddEditFamilyProfileDialogFragment {
            return AddEditFamilyProfileDialogFragment().apply {
                arguments = Bundle().apply {
                    if (profileId != null) putLong(ARG_PROFILE_ID, profileId)
                }
            }
        }
    }

    private var _binding: DialogAddEditFamilyProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReminderViewModel by activityViewModels()
    private var profileId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_YaadRakh)
        arguments?.let {
            if (it.containsKey(ARG_PROFILE_ID)) {
                profileId = it.getLong(ARG_PROFILE_ID)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditFamilyProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (profileId != null) {
            binding.tvDialogTitle.text = "Edit Family Member"
            binding.btnSaveProfile.text = "Update Family Member"
            loadProfileData(profileId!!)
        } else {
            binding.tvDialogTitle.text = "Add Family Member"
            binding.btnSaveProfile.text = "Save Family Member"
        }

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadProfileData(id: Long) {
        viewModel.allFamilyProfiles.observe(viewLifecycleOwner) { profiles ->
            val profile = profiles.find { it.id == id }
            if (profile != null) {
                binding.etName.setText(profile.name)
                binding.etRelationship.setText(profile.relationship)
                binding.etNotes.setText(profile.notes)
            }
        }
    }

    private fun saveProfile() {
        val name = binding.etName.text?.toString()?.trim() ?: ""
        val relationship = binding.etRelationship.text?.toString()?.trim() ?: ""
        val notes = binding.etNotes.text?.toString()?.trim() ?: ""

        if (name.isEmpty()) {
            binding.tilName.error = "Name is required"
            return
        }
        binding.tilName.error = null

        val profile = FamilyProfileEntity(
            id = profileId ?: 0,
            name = name,
            relationship = if (relationship.isNotBlank()) relationship else name,
            notes = notes
        )

        if (profileId != null) {
            viewModel.updateFamilyProfile(profile) {
                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        } else {
            viewModel.addFamilyProfile(profile) {
                Toast.makeText(requireContext(), "Profile added", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
