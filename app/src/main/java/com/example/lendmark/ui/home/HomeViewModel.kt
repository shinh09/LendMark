package com.example.lendmark.ui.home

import android.util.Log
import androidx.lifecycle.*
import com.example.lendmark.data.local.RecentRoomEntity
import com.example.lendmark.data.sources.announcement.*
import com.example.lendmark.ui.home.adapter.Room
import com.example.lendmark.ui.main.MyApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


data class UpcomingReservationInfo(
    val reservationId: String,
    val roomName: String,
    val time: String
)

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    // ⭐ ANNOUNCEMENT 슬라이드 구성 요소들
    private val announcementRepo = AnnouncementRepository(
        weatherRepo = WeatherRepository(),
        academicCrawler = AcademicCrawler()
    )

    private val _announcementSlides = MutableLiveData<List<AnnouncementItem>>()
    val announcementSlides: LiveData<List<AnnouncementItem>> = _announcementSlides



    private val _frequentlyUsedRooms = MutableLiveData<List<Room>>()
    val frequentlyUsedRooms: LiveData<List<Room>> = _frequentlyUsedRooms

    private val _upcomingReservation = MutableLiveData<UpcomingReservationInfo?>()
    val upcomingReservation: LiveData<UpcomingReservationInfo?> = _upcomingReservation

    private val _recentViewedRooms = MutableLiveData<List<RecentRoomEntity>>()
    val recentViewedRooms: LiveData<List<RecentRoomEntity>> = _recentViewedRooms

    private val _searchResults = MutableLiveData<List<String>>()
    val searchResults: LiveData<List<String>> = _searchResults


    fun loadHomeData() {
        loadAnnouncementSlides()
        loadFrequentlyUsedRooms()
        loadUpcomingReservation()
        loadRecentViewedRooms()
    }

    private fun loadAnnouncementSlides() {
        viewModelScope.launch {
            val slides = announcementRepo.loadAnnouncements()
            _announcementSlides.postValue(slides)
        }
    }

    fun loadRecentViewedRooms() {
        viewModelScope.launch {
            val dao = MyApp.database.recentRoomDao()
            _recentViewedRooms.postValue(dao.getRecentRooms())
        }
    }

    fun addRecentViewedRoom(roomId: String, buildingId: String, roomName: String) {
        viewModelScope.launch {
            val dao = MyApp.database.recentRoomDao()
            dao.insertRecentRoom(
                RecentRoomEntity(
                    roomId = roomId,
                    buildingId = buildingId,
                    roomName = roomName,
                    viewedAt = System.currentTimeMillis()
                )
            )
            dao.trimRecentRooms()
            loadRecentViewedRooms()
        }
    }

    private fun loadFrequentlyUsedRooms() {
        if (uid == null) {
            _frequentlyUsedRooms.value = emptyList()
            return
        }

        db.collection("reservations")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    _frequentlyUsedRooms.value = emptyList()
                    return@addOnSuccessListener
                }

                // 1. 가장 많이 쓴 강의실 top 3 계산
                val roomCounts = docs.mapNotNull { doc ->
                    val b = doc.getString("buildingId")
                    val r = doc.getString("roomId")
                    if (b != null && r != null) "$b $r" else null
                }.groupingBy { it }.eachCount()

                val top = roomCounts.entries.sortedByDescending { it.value }.take(3)
                val result = mutableListOf<Room>()

                // 2. 건물 정보 가져와서 이름으로 변환
                if (top.isEmpty()) {
                    _frequentlyUsedRooms.value = emptyList()
                } else {
                    top.forEach { entry ->
                        val (buildingId, roomId) = entry.key.split(" ")

                        db.collection("buildings").document(buildingId)
                            .get()
                            .addOnSuccessListener { doc ->
                                val img = doc.getString("imageUrl") ?: ""
                                val buildingName = doc.getString("name") ?: buildingId // 건물명 가져오기

                                // "건물명 호실" 형태로 저장 (예: Dasan Hall 110)
                                result.add(Room("$buildingName $roomId", img))

                                if (result.size == top.size) {
                                    _frequentlyUsedRooms.value = result
                                }
                            }
                    }
                }
            }
    }

    // HomeViewModel.kt
    private fun loadUpcomingReservation() {
        if (uid == null) {
            _upcomingReservation.value = null
            return
        }

        // 1. 예약 정보 가져오기
        db.collection("reservations")
            .whereEqualTo("userId", uid)
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    _upcomingReservation.value = null
                    return@addOnSuccessListener
                }

                val now = System.currentTimeMillis()
                val oneHourInMillis = 60 * 60 * 1000
                val oneHourLater = now + oneHourInMillis

                // 2. 1시간 이내 예약 찾기
                val targetDoc = snap.documents.firstOrNull { doc ->
                    val dateStr = doc.getString("date") ?: ""
                    val periodStart = doc.getLong("periodStart")?.toInt() ?: -1

                    if (dateStr.isNotEmpty() && periodStart != -1) {
                        val startTimeMillis = convertToMillis(dateStr, periodStart)
                        startTimeMillis in now..oneHourLater
                    } else {
                        false
                    }
                }

                if (targetDoc != null) {
                    val buildingId = targetDoc.getString("buildingId") ?: ""
                    val roomId = targetDoc.getString("roomId") ?: ""
                    val start = targetDoc.getLong("periodStart")?.toInt() ?: 0
                    val end = targetDoc.getLong("periodEnd")?.toInt() ?: 0
                    val date = targetDoc.getString("date") ?: ""

                    // 3. 건물 이름 가져오기 (Nested Query)
                    db.collection("buildings").document(buildingId).get()
                        .addOnSuccessListener { buildingDoc ->
                            val buildingName = buildingDoc.getString("name") ?: ""

                            // 포맷 변경: "2. Dasan Hall - no.110"
                            val formattedRoomName = "$buildingName $roomId"

                            // 시간 수정: 끝나는 교시에 +1 (예: 8-9교시 -> 16:00~18:00)
                            val formattedTime = "$date • ${periodToTime(start)} - ${periodToTime(end + 1)}"

                            _upcomingReservation.value = UpcomingReservationInfo(
                                reservationId = targetDoc.id,
                                roomName = formattedRoomName,
                                time = formattedTime
                            )
                        }
                        .addOnFailureListener {
                            // 건물 정보 실패 시 기존 방식대로
                            _upcomingReservation.value = UpcomingReservationInfo(
                                reservationId = targetDoc.id,
                                roomName = "$buildingId $roomId",
                                time = "$date • ${periodToTime(start)} - ${periodToTime(end + 1)}"
                            )
                        }
                } else {
                    _upcomingReservation.value = null
                }
            }
            .addOnFailureListener {
                _upcomingReservation.value = null
            }
    }

    // 날짜(String) + 교시(Int) -> 시간(Long) 변환 함수 추가
    private fun convertToMillis(dateStr: String, periodStart: Int): Long {
        return try {
            // 날짜 포맷 (DB에 저장된 형태가 "yyyy-MM-dd" 라고 가정)
            // 만약 "yyyy.MM.dd" 등을 쓴다면 아래 패턴을 수정해야 합니다.
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return 0L

            val calendar = Calendar.getInstance()
            calendar.time = date

            // 교시 -> 시간 변환 (0교시 = 08시, 1교시 = 09시 ...)
            val hourOfDay = 8 + periodStart

            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            calendar.timeInMillis
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    private fun periodToTime(period: Int): String {
        val hour = 8 + period
        return String.format("%02d:00", hour)
    }

    fun searchBuilding(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }

        db.collection("buildings")
            .get()
            .addOnSuccessListener { result ->
                val matched = result.documents.mapNotNull { doc ->
                    val name = doc.getString("name")
                    if (name != null && name.contains(clean, ignoreCase = true)) name else null
                }
                _searchResults.value = matched
            }
            .addOnFailureListener {
                _searchResults.value = emptyList()
            }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }
}
