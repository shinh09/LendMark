package com.example.lendmark.ui.room

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import com.example.lendmark.R
import com.example.lendmark.databinding.FragmentRoomScheduleBinding
import com.google.firebase.firestore.FirebaseFirestore

class RoomScheduleFragment : Fragment() {

    private var _binding: FragmentRoomScheduleBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()

    private lateinit var buildingName: String
    private lateinit var roomId: String

    // period → 시간표용 실제 시간
    private val periodTime = mapOf(
        1 to "09:00",
        2 to "10:00",
        3 to "11:00",
        4 to "12:00",
        5 to "13:00",
        6 to "14:00",
        7 to "15:00",
        8 to "16:00",
        9 to "17:00"
    )

    // 요일 순서
    private val days = listOf("시간", "월", "화", "수", "목", "금")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRoomScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buildingName = arguments?.getString("buildingName") ?: "건물"
        roomId = arguments?.getString("roomId") ?: "000"

        binding.tvRoomTitle.text = "${buildingName} ${roomId}호"

        setupHeader()
        setupTimeColumn()

        loadSchedule()
    }

    // -------------------------------
    //       1) 요일 헤더 추가
    // -------------------------------
    private fun setupHeader() {
        for (label in days) {
            addHeaderCell(label)
        }
    }

    private fun addHeaderCell(label: String) {
        val tv = TextView(requireContext()).apply {
            layoutParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                setMargins(0, 0, 0, 0)
                columnSpec = androidx.gridlayout.widget.GridLayout.spec(
                    androidx.gridlayout.widget.GridLayout.UNDEFINED,
                    1f
                )
            }

            text = label
            textSize = 11f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(4)
        }

        binding.gridSchedule.addView(tv)
    }

    // -------------------------------
    //       2) 왼쪽 시간 라인
    // -------------------------------
    private fun setupTimeColumn() {
        for (period in 1..13) {
            val tv = TextView(requireContext()).apply {
                layoutParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    height = ViewGroup.LayoutParams.WRAP_CONTENT

                    // 왼쪽 고정 열 = column 0
                    columnSpec = androidx.gridlayout.widget.GridLayout.spec(0, 1f)

                    // 각 period = 행 1~9 (row 0은 헤더)
                    rowSpec = androidx.gridlayout.widget.GridLayout.spec(period)
                }

                text = periodTime[period]
                textSize = 11f
                gravity = Gravity.CENTER
                setPadding(4)
                setTextColor(Color.DKGRAY)
            }

            binding.gridSchedule.addView(tv)
        }
    }


    // -------------------------------
    //       3) Firestore 시간표 로드
    // -------------------------------
    private fun loadSchedule() {

        val buildingCode = arguments?.getInt("buildingCode") ?: 0

        db.collection("buildings")
            .document(buildingCode.toString())
            .collection("timetable")
            .document(roomId)
            .get()
            .addOnSuccessListener { doc ->

                val raw = doc.get("schedule") as? List<Map<String, Any>> ?: emptyList()

                Log.d("ROOM_DEBUG", "Schedule loaded: $raw")

                for (item in raw) {
                    val day = item["day"] as? String ?: continue
                    val subject = item["subject"] as? String ?: ""
                    val start = (item["periodStart"] as? Long)?.toInt() ?: continue
                    val end = (item["periodEnd"] as? Long)?.toInt() ?: continue

                    addClassBlock(day, subject, start, end)
                }
            }
            .addOnFailureListener {
                Log.e("ROOM_DEBUG", "불러오기 실패", it)
            }
    }


    // -------------------------------
    //       4) 수업 블록 넣기
    // -------------------------------
    private fun addClassBlock(day: String, subject: String, start: Int, end: Int) {
        val dayIndex = when (day) {
            "Mon" -> 1
            "Tue" -> 2
            "Wed" -> 3
            "Thu" -> 4
            "Fri" -> 5
            else -> -1
        }



        for (p in start..end) {
            val tv = TextView(requireContext()).apply {
                layoutParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    height = ViewGroup.LayoutParams.WRAP_CONTENT

                    rowSpec = androidx.gridlayout.widget.GridLayout.spec(p)  // p번째 행
                    columnSpec = androidx.gridlayout.widget.GridLayout.spec(dayIndex, 1f)

                    setMargins(4, 4, 4, 4)
                }

                text = subject
                textSize = 11f
                gravity = Gravity.CENTER
                setPadding(4)
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_cell_class)
            }

            binding.gridSchedule.addView(tv)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

