package com.example.app.data.model

data class ProfileResponse(
    val userId: String? = null,
    val birthDate: String? = null, // "1999-01-01" 형식
    val nickname: String? = null,
    val gender: String? = null, // "male" or "female"
    val province: String? = null, // "경기"
    val city: String? = null, // "수원시"
    val education: String? = null, // "대학교 재학"
    val employment: String? = null, // "학생", "취업준비생" 등
    val interests: List<String>? = null // ["창업", "취업"]
)

