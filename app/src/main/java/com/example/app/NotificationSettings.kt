package com.example.app

/**
 * 알림 설정 데이터 클래스
 * 여러 화면에서 공통으로 사용
 */
data class NotificationSettings(
    val sevenDays: Boolean = true,
    val sevenDaysTime: String = "09:00",
    val oneDay: Boolean = true,
    val oneDayTime: String = "10:00",
    val custom: Boolean = false,
    val customDays: Int = 3,
    val customTime: String = "09:00"
)

