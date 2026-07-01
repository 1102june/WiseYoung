package com.wiseyoung.pro.util

/**
 * 시·도 단위 지도 중심 좌표 (카카오맵).
 */
object ProvinceMapUtils {

    private val provinceCenters: Map<String, Pair<Double, Double>> = mapOf(
        "서울" to (37.5665 to 126.9780),
        "부산" to (35.1796 to 129.0756),
        "대구" to (35.8714 to 128.6014),
        "인천" to (37.4563 to 126.7052),
        "광주" to (35.1595 to 126.8526),
        "대전" to (36.3504 to 127.3845),
        "울산" to (35.5384 to 129.3114),
        "세종" to (36.4800 to 127.2890),
        "경기" to (37.4138 to 127.5183),
        "강원" to (37.8228 to 128.1555),
        "충북" to (36.6357 to 127.4917),
        "충남" to (36.5184 to 126.8000),
        "전북" to (35.8242 to 127.1480),
        "전남" to (34.8161 to 126.4629),
        "경북" to (36.4919 to 128.8889),
        "경남" to (35.4606 to 128.2132),
        "제주" to (33.4996 to 126.5312)
    )

    private val aliases = listOf(
        "서울특별시" to "서울",
        "부산광역시" to "부산",
        "대구광역시" to "대구",
        "인천광역시" to "인천",
        "광주광역시" to "광주",
        "대전광역시" to "대전",
        "울산광역시" to "울산",
        "세종특별자치시" to "세종",
        "경기도" to "경기",
        "강원특별자치도" to "강원",
        "강원도" to "강원",
        "충청북도" to "충북",
        "충청남도" to "충남",
        "전북특별자치도" to "전북",
        "전라북도" to "전북",
        "전라남도" to "전남",
        "경상북도" to "경북",
        "경상남도" to "경남",
        "제주특별자치도" to "제주"
    )

    fun findCenter(regionText: String?): Pair<Double, Double>? {
        if (regionText.isNullOrBlank()) return null
        val normalized = regionText.replace(" ", "")
        for ((alias, key) in aliases) {
            if (normalized.contains(alias.replace(" ", ""))) {
                return provinceCenters[key]
            }
        }
        for ((key, center) in provinceCenters) {
            if (normalized.contains(key)) return center
        }
        return null
    }

    fun findCenterFromUserRegion(userRegion: String?): Pair<Double, Double>? {
        if (userRegion.isNullOrBlank()) return null
        return findCenter(userRegion) ?: userRegion.split(" ").firstOrNull()?.let { findCenter(it) }
    }
}
