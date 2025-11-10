package com.example.lendmark.ui.auth.signup

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.lendmark.databinding.DialogEmailVerifyBinding

class EmailVerifyDialog(
    private val email: String,
    private val onVerify: (String) -> Unit
) : DialogFragment() {

    private var _binding: DialogEmailVerifyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEmailVerifyBinding.inflate(LayoutInflater.from(context))

        val builder = AlertDialog.Builder(requireContext())
            .setView(binding.root)

        binding.tvDescription.text = "Please enter the verification code sent to $email."

        // Confirm button
        binding.btnConfirm.setOnClickListener {
            val code = binding.etCode.text.toString()
            onVerify(code)  // Pass to parent fragment (SignupFragment)
            dismiss()
        }

        // Close button (X)
        binding.btnClose.setOnClickListener { dismiss() }

        return builder.create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
