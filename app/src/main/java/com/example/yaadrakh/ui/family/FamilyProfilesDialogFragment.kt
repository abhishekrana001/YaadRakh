package com.example.yaadrakh.ui.family

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.yaadrakh.R
import com.example.yaadrakh.data.local.FamilyProfileEntity
import com.example.yaadrakh.databinding.DialogFamilyProfilesBinding
import com.example.yaadrakh.viewmodel.ReminderViewModel

class FamilyProfilesDialogFragment : DialogFragment() {

    private var _binding: DialogFamilyProfilesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReminderViewModel by activityViewModels()
    private lateinit var adapter: FamilyProfileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_YaadRakh)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFamilyProfilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeProfiles()
    }

    private fun setupRecyclerView() {
        adapter = FamilyProfileAdapter(
            onEditClick = { profile ->
                AddEditFamilyProfileDialogFragment.newInstance(profile.id)
                    .show(childFragmentManager, "AddEditFamilyProfileDialog")
            },
            onDeleteClick = { profile ->
                showDeleteConfirmationDialog(profile)
            }
        )
        binding.rvFamilyProfiles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFamilyProfiles.adapter = adapter
    }

    private fun observeProfiles() {
        viewModel.allFamilyProfiles.observe(viewLifecycleOwner) { profiles ->
            adapter.submitList(profiles)
        }
    }

    private fun showDeleteConfirmationDialog(profile: FamilyProfileEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Profile")
            .setMessage("Are you sure you want to delete '${profile.name}'? Existing reminders will be preserved.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteFamilyProfile(profile) {
                    Toast.makeText(requireContext(), "Profile deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupClickListeners() {
        binding.btnAddProfile.setOnClickListener {
            AddEditFamilyProfileDialogFragment.newInstance()
                .show(childFragmentManager, "AddEditFamilyProfileDialog")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
