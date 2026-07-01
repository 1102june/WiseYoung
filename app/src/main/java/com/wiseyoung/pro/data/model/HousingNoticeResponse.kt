package com.wiseyoung.pro.data.model

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

    @SerializedName("applicationPeriodText")
    val applicationPeriodText: String? = null,

    @SerializedName("applicationPeriod")
    val applicationPeriod: String? = null,
    
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
    val matchedComplex: HousingComplexResponse? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("region")
    val region: String? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("recruitmentPeriodDisplay")
    val recruitmentPeriodDisplay: String? = null,

    @SerializedName("depositDisplay")
    val depositDisplay: String? = null,

    @SerializedName("monthlyRentDisplay")
    val monthlyRentDisplay: String? = null,

    @SerializedName("rentSummary")
    val rentSummary: String? = null,

    @SerializedName("address")
    val address: String? = null,

    @SerializedName("supplyAreaDisplay")
    val supplyAreaDisplay: String? = null,

    @SerializedName("hasApplicationLink")
    val hasApplicationLink: Boolean? = null
)

