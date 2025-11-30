package com.example.app.data.model

data class AIRecommendationResponse(
    val recId: Long,
    val contentType: String,  // "policy" 또는 "housing"
    val contentId: String,
    val createdAt: String,  // ISO 형식 날짜 문자열
    val policy: PolicyResponse? = null,
    val housing: HousingResponse? = null
)

