package com.example.lendmark.ui.notification

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.lendmark.databinding.DialogNotificationDetailBinding

class NotificationDetailDialog(
    private val item: NotificationItem
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogNotificationDetailBinding.inflate(LayoutInflater.from(context))

        binding.tvTitle.text = item.title
        binding.tvDetail.text = item.detail

        binding.btnGoReservation.setOnClickListener {
            // TODO: 예약 상세 페이지로 이동 (추후 구현)
            dismiss()
        }

        binding.btnConfirm.setOnClickListener {
            dismiss()
        }

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }
}
