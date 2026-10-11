package com.example.yaadrakh.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.yaadrakh.R
import com.example.yaadrakh.databinding.DialogNameSetupBinding
import com.example.yaadrakh.utils.VoiceSettings

class NameSetupDialogFragment(
    private val onNameSaved: ((String) -> Unit)? = null
) : DialogFragment() {

    private var _binding: DialogNameSetupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogNameSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val voiceSettings = VoiceSettings(requireContext())
        val existingName = voiceSettings.getUserName()
        if (existingName.isNotBlank()) {
            binding.etUserName.setText(existingName)
        }

        isCancelable = existingName.isNotBlank()

        binding.btnSaveName.setOnClickListener {
            val name = binding.etUserName.text.toString().trim()
            if (name.isBlank()) {
                binding.tilUserName.error = "Please enter your name"
                return@setOnClickListener
            }

            voiceSettings.setUserName(name)
            Toast.makeText(requireContext(), "Welcome, $name!", Toast.LENGTH_SHORT).show()
            onNameSaved?.invoke(name)
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
