package com.example.app.data.model

data class HousingResponse(
    val housingId: String,
    val name: String,
    val address: String? = null,
    val supplyArea: Double? = null,
    val completeDate: String? = null,  // ISO 형식 날짜 문자열
    val organization: String? = null,
    val applicationStart: String? = null,  // ISO 형식 날짜 문자열
    val applicationEnd: String? = null,    // ISO 형식 날짜 문자열
    val heatingType: String? = null,
    val elevator: Boolean? = null,
    val parkingSpaces: Int? = null,
    val deposit: Int? = null,
    val monthlyRent: Int? = null,
    val totalUnits: Int? = null,
    val link: String? = null,
    val isBookmarked: Boolean = false,
    // 지도 표시용
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distanceFromUser: Double? = null,  // 사용자로부터의 거리 (미터 단위)
    // 상세 정보
    val housingType: String? = null
)

