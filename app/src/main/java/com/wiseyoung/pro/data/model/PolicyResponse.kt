package com.wiseyoung.pro.data.model

data class PolicyResponse(
    val policyId: String,
    val title: String,
    val summary: String? = null,
    val category: String? = null,
    val region: String? = null,
    val ageStart: Int? = null,
    val ageEnd: Int? = null,
    val eligibility: String? = null,
    val applicationStart: String? = null,  // ISO 형식 날짜 문자열
    val applicationEnd: String? = null,    // ISO 형식 날짜 문자열
    val applicationPeriodText: String? = null, // "상시 신청" 등 한 줄 표시
    val applicationPeriod: String? = null,     // applicationPeriodText 대체 필드
    val link1: String? = null,
    val link2: String? = null,
    val isBookmarked: Boolean = false
)

