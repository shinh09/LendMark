package com.example.lendmark.ui.notification

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 화면에 보여줄 알림 리스트
    private val _notifications = MutableLiveData<List<NotificationItem>>()
    val notifications: LiveData<List<NotificationItem>> get() = _notifications

    // 선택된 알림 (다이얼로그용)
    private val _selectedNotification = MutableLiveData<NotificationItem?>()
    val selectedNotification: LiveData<NotificationItem?> get() = _selectedNotification

    // 인앱 알림 활성화 여부
    var isInAppEnabled: Boolean = true

    // 건물 ID 매핑용
    private var buildingNameMap = mapOf<String, String>()

    init {
        loadBuildingNames()
    }

    private fun loadBuildingNames() {
        db.collection("buildings").get()
            .addOnSuccessListener { result ->
                buildingNameMap = result.documents.associate { doc ->
                    val id = doc.id
                    val name = doc.getString("name") ?: "Building $id"
                    id to name
                }
                checkReservationsAndCreateNotifications()
            }
            .addOnFailureListener {
                Log.e("NotificationVM", "건물 데이터 로딩 실패", it)
                checkReservationsAndCreateNotifications()
            }
    }

    fun checkReservationsAndCreateNotifications() {
        if (!isInAppEnabled) {
            _notifications.value = emptyList()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            _notifications.value = emptyList()
            return
        }

        val userId = currentUser.uid

        db.collection("reservations")
            .whereEqualTo("userId", userId)
            // .whereEqualTo("status", "approved") // 필요 시 주석 해제
            .get()
            .addOnSuccessListener { documents ->
                val newNotifications = mutableListOf<NotificationItem>()
                val currentTime = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

                for (doc in documents) {
                    try {
                        val dateStr = doc.getString("date") ?: ""
                        // 0, 1, 2... 같은 정수값
                        val periodStart = doc.getLong("periodStart")?.toInt() ?: 0
                        val periodEnd = doc.getLong("periodEnd")?.toInt() ?: 0

                        // 데이터 유효성 체크
                        if (dateStr.isEmpty()) continue

                        val buildingId = doc.getString("buildingId") ?: ""
                        val buildingName = buildingNameMap[buildingId] ?: "Building $buildingId"
                        val roomId = doc.getString("roomId") ?: ""

                        // 정확한 시간 문자열 변환
                        val startTimeStr = convertPeriodToStartTime(periodStart)
                        val endTimeStr = convertPeriodToEndTime(periodEnd)

                        // 날짜 + 시간 파싱 ("2023-10-25 09:00")
                        val startDateTime = dateFormat.parse("$dateStr $startTimeStr")?.time ?: 0L
                        val endDateTime = dateFormat.parse("$dateStr $endTimeStr")?.time ?: 0L

                        val diffStart = startDateTime - currentTime
                        val diffEnd = endDateTime - currentTime

                        // 디버깅용 로그 (테스트할 때 유용)
                        // Log.d("NotiCheck", "Room: $roomId, Start: $startTimeStr, TimeDiff: ${TimeUnit.MILLISECONDS.toMinutes(diffStart)}min")

                        // 조건 1: 시작 30분 전 (0 < 남은시간 <= 30분)
                        if (diffStart > 0 && diffStart <= TimeUnit.MINUTES.toMillis(30)) {
                            val minsLeft = TimeUnit.MILLISECONDS.toMinutes(diffStart) + 1
                            newNotifications.add(
                                NotificationItem(
                                    id = doc.id.hashCode(),
                                    reservationId = doc.id,
                                    title = "Reservation starts in ${minsLeft} mins!",
                                    location = "$buildingName - Room $roomId",
                                    date = dateStr,
                                    startTime = startTimeStr,
                                    endTime = endTimeStr,
                                    remainingTime = "Starts in ${minsLeft} mins",
                                    type = "start",
                                    isRead = false
                                )
                            )
                        }

                        // 조건 2: 종료 10분 전
                        if (diffEnd > 0 && diffEnd <= TimeUnit.MINUTES.toMillis(10)) {
                            val minsLeft = TimeUnit.MILLISECONDS.toMinutes(diffEnd) + 1
                            newNotifications.add(
                                NotificationItem(
                                    id = doc.id.hashCode() + 1,
                                    reservationId = doc.id,
                                    title = "Reservation ends in ${minsLeft} mins. Please clean up!",
                                    location = "$buildingName - Room $roomId",
                                    date = dateStr,
                                    startTime = startTimeStr,
                                    endTime = endTimeStr,
                                    remainingTime = "Ends in ${minsLeft} mins",
                                    type = "end",
                                    isRead = false
                                )
                            )
                        }

                    } catch (e: Exception) {
                        Log.e("NotificationVM", "Error parsing reservation: ${e.message}")
                    }
                }

                newNotifications.sortBy { it.remainingTime }
                _notifications.value = newNotifications
            }
            .addOnFailureListener { e ->
                Log.e("NotificationVM", "Firestore error", e)
            }
    }

    // 아이템 클릭
    fun selectNotification(item: NotificationItem) {
        _selectedNotification.value = item
        _notifications.value = _notifications.value?.map {
            if (it.id == item.id) it.copy(isRead = true) else it
        }
    }

    // =================================================================
    // 정확한 시간 변환 (0 = 08:00)
    // =================================================================

    private fun convertPeriodToStartTime(period: Int): String {
        // DB: 0 -> 8시, 1 -> 9시 ...
        val hour = 8 + period
        return String.format(Locale.getDefault(), "%02d:00", hour)
    }

    private fun convertPeriodToEndTime(period: Int): String {
        // 종료 시간 = 시작시간 + 1시간
        val hour = 8 + period + 1
        return String.format(Locale.getDefault(), "%02d:00", hour)
    }
}