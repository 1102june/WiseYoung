package com.example.app.data.model

import com.google.gson.annotations.SerializedName

data class HousingComplexResponse(
    @SerializedName("complexId")
    val complexId: String? = null,
    
    @SerializedName("hsmpNm")
    val hsmpNm: String? = null, // 단지명
    
    @SerializedName("insttNm")
    val insttNm: String? = null, // 기관명
    
    @SerializedName("brtcNm")
    val brtcNm: String? = null, // 광역시도명
    
    @SerializedName("signguNm")
    val signguNm: String? = null, // 시군구명
    
    @SerializedName("rnAdres")
    val rnAdres: String? = null, // 도로명 주소
    
    @SerializedName("completeDate")
    val completeDate: String? = null, // 준공 일자
    
    @SerializedName("totalUnits")
    val totalUnits: Int? = null, // 세대수
    
    @SerializedName("suplyTyNm")
    val suplyTyNm: String? = null, // 공급 유형명
    
    @SerializedName("styleNm")
    val styleNm: String? = null, // 형명
    
    @SerializedName("supplyArea")
    val supplyArea: Double? = null, // 공급 면적
    
    @SerializedName("houseTyNm")
    val houseTyNm: String? = null, // 주택 유형명
    
    @SerializedName("heatMthdDetailNm")
    val heatMthdDetailNm: String? = null, // 난방 방식
    
    @SerializedName("buldStleNm")
    val buldStleNm: String? = null, // 건물 형태
    
    @SerializedName("elevator")
    val elevator: Boolean? = null, // 승강기 설치여부
    
    @SerializedName("parkingSpaces")
    val parkingSpaces: Int? = null, // 주차수
    
    @SerializedName("deposit")
    val deposit: Int? = null, // 기본 임대보증금
    
    @SerializedName("monthlyRent")
    val monthlyRent: Int? = null, // 기본 월임대료
    
    @SerializedName("latitude")
    val latitude: Double? = null, // 위도
    
    @SerializedName("longitude")
    val longitude: Double? = null, // 경도
    
    @SerializedName("isBookmarked")
    val isBookmarked: Boolean? = null, // 북마크 여부
    
    @SerializedName("distanceFromUser")
    val distanceFromUser: Double? = null // 사용자로부터의 거리 (미터 단위)
)

