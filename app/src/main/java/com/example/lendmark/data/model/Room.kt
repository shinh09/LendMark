package com.example.lendmark.data.model

data class Room(
    val roomId: String,
    val name: String = "",
    val capacity: Int = 0,
    val floor: String = "",
    val imageUrl: String = ""   // 나중에 교실 사진 넣을 수 있음
)
