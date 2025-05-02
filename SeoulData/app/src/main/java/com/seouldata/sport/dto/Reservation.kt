package com.example.seouldata.dto
data class Reservation(
    val placeName: String = "",
    val dateTime: String = "",    // "2025년 5월 10일 (토) 11:00 ~ 13:00"
    val status: String = ""       // 디폴트 "예약 대기"
)