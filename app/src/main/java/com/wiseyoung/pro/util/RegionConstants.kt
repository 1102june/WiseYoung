package com.wiseyoung.pro.util

/**
 * 프로필 지역 선택 — 도(광역시) 단위만 선택.
 */
object RegionConstants {

    val provinceCities: Map<String, List<String>> = mapOf(
        "서울" to listOf("서울특별시"),
        "부산" to listOf("부산광역시"),
        "경기" to listOf("경기도"),
        "인천" to listOf("인천광역시"),
        "대구" to listOf("대구광역시"),
        "광주" to listOf("광주광역시"),
        "대전" to listOf("대전광역시"),
        "울산" to listOf("울산광역시"),
        "강원" to listOf("강릉시", "동해시", "삼척시", "속초시", "원주시", "춘천시", "태백시"),
        "충북" to listOf("제천시", "청주시", "충주시"),
        "충남" to listOf("계룡시", "공주시", "논산시", "당진시", "보령시", "서산시", "아산시", "천안시"),
        "전북" to listOf("군산시", "김제시", "남원시", "익산시", "전주시", "정읍시"),
        "전남" to listOf("광양시", "나주시", "목포시", "순천시", "여수시"),
        "경북" to listOf("경산시", "경주시", "구미시", "김천시", "문경시", "상주시", "안동시", "영주시", "영천시", "포항시"),
        "경남" to listOf("거제시", "김해시", "밀양시", "사천시", "양산시", "진주시", "창원시", "통영시"),
        "제주" to listOf("제주시", "서귀포시")
    )

    val provinceDisplayNames: Map<String, String> = mapOf(
        "서울" to "서울특별시",
        "부산" to "부산광역시",
        "경기" to "경기도",
        "인천" to "인천광역시",
        "대구" to "대구광역시",
        "광주" to "광주광역시",
        "대전" to "대전광역시",
        "울산" to "울산광역시",
        "강원" to "강원도",
        "충북" to "충청북도",
        "충남" to "충청남도",
        "전북" to "전라북도",
        "전남" to "전라남도",
        "경북" to "경상북도",
        "경남" to "경상남도",
        "제주" to "제주특별자치도"
    )

    /** 도(광역시) 선택 시 자동으로 채울 시/도 표기 */
    fun defaultCityForProvince(province: String): String {
        if (province.isBlank()) return ""
        return provinceCities[province]?.singleOrNull()
            ?: provinceDisplayNames[province]
            ?: ""
    }
}
