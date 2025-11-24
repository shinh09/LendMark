package com.example.lendmark.ui.my

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.lendmark.databinding.DialogProfilePhotoOptionsBinding

class ProfilePhotoOptionsDialog(
    private val onChoose: (() -> Unit)? = null,
    private val onRemove: (() -> Unit)? = null
) : DialogFragment() {

    private var _binding: DialogProfilePhotoOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogProfilePhotoOptionsBinding.inflate(LayoutInflater.from(requireContext()))

        binding.btnChoosePhoto.setOnClickListener {
            onChoose?.invoke()
            dismiss()
        }

        binding.btnRemovePhoto.setOnClickListener {
            onRemove?.invoke()
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
