package com.example.lendmark.ui.my

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.lendmark.databinding.DialogReservationDetailBinding

class ReservationDetailDialog(
    private val reservation: Reservation,
    private val onCancelClick: (Int) -> Unit // 콜백 전달받기
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogReservationDetailBinding.inflate(LayoutInflater.from(context))

        // 예약 정보 세팅
        binding.tvBuildingName.text = reservation.building
        binding.tvRoomName.text = reservation.room
        binding.tvDate.text = reservation.date
        binding.tvTime.text = reservation.time
        binding.tvAttendees.text = "${reservation.attendees} people"
        binding.tvPurpose.text = reservation.purpose

        // 상태에 따라 버튼 표시 제어
        when (reservation.status) {
            "Finished" -> {
                binding.btnCancel.visibility = View.GONE
                binding.btnRegisterInfo.visibility = View.VISIBLE
            }
            "Cancelled" -> {
                binding.btnCancel.visibility = View.GONE
                binding.btnRegisterInfo.visibility = View.GONE
            }
            else -> {
                binding.btnCancel.visibility = View.VISIBLE
                binding.btnRegisterInfo.visibility = View.GONE
            }
        }

        // 취소 버튼 클릭 시 콜백 호출
        binding.btnCancel.setOnClickListener {
            onCancelClick(reservation.id)
            dismiss()
        }

        // 닫기 버튼
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // 다이얼로그 생성
        val dialog = AlertDialog.Builder(requireActivity())
            .setView(binding.root)
            .create()

        // 배경 투명 처리
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }
}
