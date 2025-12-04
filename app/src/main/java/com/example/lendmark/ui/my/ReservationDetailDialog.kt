package com.example.lendmark.ui.my

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.lendmark.R
import com.example.lendmark.databinding.DialogReservationDetailBinding
import com.google.firebase.firestore.FirebaseFirestore

class ReservationDetailDialogFS(
    private val reservation: ReservationFS,
    private val onCancelClick: (String) -> Unit,
    private val onRegisterClick: (String) -> Unit
) : DialogFragment() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogReservationDetailBinding.inflate(LayoutInflater.from(context))

        // ──────────────────────────────────────
        // 1) Firestore에서 building name 불러오기
        // ──────────────────────────────────────
        db.collection("buildings").document(reservation.buildingId)
            .get()
            .addOnSuccessListener { doc ->
                val buildingName = doc.getString("name") ?: "Unknown Building"
                binding.tvBuildingName.text = "${reservation.buildingId}. $buildingName"
            }
            .addOnFailureListener {
                binding.tvBuildingName.text = reservation.buildingId
            }

        // 강의실 번호, 날짜, 인원, 목적 바인딩
        binding.tvRoomName.text = "No. ${reservation.roomId}"
        binding.tvDate.text = reservation.date
        binding.tvTime.text =
            "${periodToTime(reservation.periodStart)} - ${periodToTime(reservation.periodEnd+1)}"
        binding.tvAttendees.text = "${reservation.attendees} people"
        binding.tvPurpose.text = reservation.purpose

        // 닫기 버튼
        binding.btnClose.setOnClickListener { dismiss() }

        // ──────────────────────────────────────
        // 2) 상태별 버튼 UI
        // ──────────────────────────────────────
        val grayColor = ContextCompat.getColor(requireContext(), R.color.gray)
        val redColor = ContextCompat.getColor(requireContext(), R.color.red)

        when (reservation.status) {

            "approved" -> {
                // 예약 확정: 취소 버튼 활성화
                binding.btnCancel.visibility = View.VISIBLE
                binding.btnRegisterInfo.visibility = View.GONE

                binding.btnCancel.text = "Cancel Reservation"
                binding.btnCancel.isEnabled = true
                binding.btnCancel.setTextColor(redColor)
                binding.btnCancel.strokeColor =
                    ContextCompat.getColorStateList(requireContext(), R.color.red)

                binding.btnCancel.setOnClickListener {
                    onCancelClick(reservation.id)
                    dismiss()
                }
            }

            "finished" -> {
                // 이용 완료: 정보 등록 버튼 활성화
                binding.btnCancel.visibility = View.GONE
                binding.btnRegisterInfo.visibility = View.VISIBLE

                binding.btnRegisterInfo.text = "Register Classroom Info"
                binding.btnRegisterInfo.isEnabled = true
                // 배경색, 텍스트 색상은 XML 기본값 혹은 아래처럼 세팅
                binding.btnRegisterInfo.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.colorPrimary)
                binding.btnRegisterInfo.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.white)
                )

                binding.btnRegisterInfo.setOnClickListener {
                    onRegisterClick(reservation.id)
                    dismiss()
                }
            }

            "canceled" -> {
                // 취소됨: 버튼 비활성화 + 회색 처리
                binding.btnRegisterInfo.visibility = View.GONE
                binding.btnCancel.visibility = View.VISIBLE

                binding.btnCancel.text = "Canceled"
                binding.btnCancel.isEnabled = false // 비활성화
                binding.btnCancel.setTextColor(grayColor)
                binding.btnCancel.strokeColor =
                    ContextCompat.getColorStateList(requireContext(), R.color.gray)

                // (선택) 타이틀 등 텍스트도 회색으로 처리
                binding.tvBuildingName.setTextColor(grayColor)
            }

            "reviewed", "expired" -> {
                // 리뷰 완료 or 기간 만료: 버튼 둘 다 숨김
                binding.btnCancel.visibility = View.GONE
                binding.btnRegisterInfo.visibility = View.GONE

                // 리뷰 완료/만료 상태임을 시각적으로 표현 (회색)
                binding.tvBuildingName.setTextColor(grayColor)
            }
        }

        val dialog = AlertDialog.Builder(requireActivity())
            .setView(binding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    private fun periodToTime(period: Int): String {
        val hour = 8 + period
        return String.format("%02d:00", hour)
    }
}