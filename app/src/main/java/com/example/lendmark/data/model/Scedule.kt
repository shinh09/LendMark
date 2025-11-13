package com.example.lendmark.data.model

data class Schedule(
    val day: String = "",       // "Mon", "Tue" ...
    val periodStart: Int = 0,   // 1교시
    val periodEnd: Int = 0,     // 2교시
    val subject: String = ""
)
