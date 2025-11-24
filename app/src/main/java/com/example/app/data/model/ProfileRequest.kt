package com.example.app.data.model

data class ProfileRequest(
    val idToken: String,
    val birthDate: String? = null, // "1999-01-01" 형식
    val gender: String? = null, // "male" or "female"
    val province: String? = null, // "강원"
    val city: String? = null, // "춘천시"
    val education: String? = null, // "대학교 재학"
    val employment: String? = null, // "학생"
    val interests: List<String>? = null, // ["창업", "취업"]
    val appVersion: String? = null, // 앱 버전
    val deviceId: String? = null // 디바이스 ID
)

