package com.example.lendmark.data.model

data class ClassSchedule(
    val day: String = "",
    val periodStart: Int = 0,
    val periodEnd: Int = 0,
    val subject: String = ""
)

data class RoomSchedule(
    val schedule: List<ClassSchedule> = emptyList()
)

data class Building(
    var id: String = "",
    val name: String = "",
    val code: Int = 0,
    val roomCount: Int = 0,
    val imageUrl: String = "",
    val naverMapLat: Double = 0.0,
    val naverMapLng: Double = 0.0,

    // Firestore의 timetable(강의실별 강의 시간표)
    val timetable: Map<String, RoomSchedule> = emptyMap()
)
