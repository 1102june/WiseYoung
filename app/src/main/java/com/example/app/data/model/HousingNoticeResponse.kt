package com.example.app.data.model

import com.google.gson.annotations.SerializedName

data class HousingNoticeResponse(
    @SerializedName("noticeId")
    val noticeId: String? = null,
    
    @SerializedName("hsmpSn")
    val hsmpSn: String? = null, // 단지 식별자
    
    @SerializedName("hsmpNm")
    val hsmpNm: String? = null, // 단지명
    
    @SerializedName("panId")
    val panId: String? = null, // 공고ID
    
    @SerializedName("panNm")
    val panNm: String? = null, // 공고명
    
    @SerializedName("dtlUrl")
    val dtlUrl: String? = null, // 공고 상세 URL
    
    @SerializedName("panNtStDt")
    val panNtStDt: String? = null, // 공고게시일
    
    @SerializedName("clsgDt")
    val clsgDt: String? = null, // 공고마감일
    
    @SerializedName("panDt")
    val panDt: String? = null, // 공고일
    
    @SerializedName("applicationStart")
    val applicationStart: String? = null, // 신청 시작일
    
    @SerializedName("applicationEnd")
    val applicationEnd: String? = null, // 신청 종료일
    
    @SerializedName("cnpCdNm")
    val cnpCdNm: String? = null, // 지역명
    
    @SerializedName("uppAisTpNm")
    val uppAisTpNm: String? = null, // 상위 공고유형명
    
    @SerializedName("aisTpCdNm")
    val aisTpCdNm: String? = null, // 공고유형명
    
    @SerializedName("panSs")
    val panSs: String? = null, // 공고상태
    
    @SerializedName("isBookmarked")
    val isBookmarked: Boolean? = null, // 북마크 여부
    
    @SerializedName("matchedComplex")
    val matchedComplex: HousingComplexResponse? = null // 매칭된 단지정보 (있는 경우)
)

