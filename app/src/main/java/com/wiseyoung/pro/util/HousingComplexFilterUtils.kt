package com.wiseyoung.pro.util

/**
 * 임대주택 **단지** 탭 전용 — brtcNm(광역시·도) 기준.
 */
object HousingComplexFilterUtils {

    fun matchesBrtcRegion(brtcNm: String, filterRegion: String): Boolean {
        if (filterRegion == "전체") return true
        if (brtcNm.isBlank()) return false
        return HousingRegionUtils.matchesRegion(brtcNm.trim(), filterRegion.trim())
    }

    /** 프로필 region → 단지 필터용 brtcNm(시·도) */
    fun profileToBrtcFilter(profileRegion: String?): String? {
        if (profileRegion.isNullOrBlank()) return null
        return HousingRegionUtils.profileRegionToProvinceFilter(profileRegion)
            ?: HousingRegionUtils.normalizeRegion(profileRegion.split("\\s+".toRegex()).firstOrNull())
    }
}
