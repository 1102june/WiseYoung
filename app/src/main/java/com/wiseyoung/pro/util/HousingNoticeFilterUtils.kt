package com.wiseyoung.pro.util

/**
 * 임대주택 **공고** 탭 전용 필터 (단지 탭과 독립).
 * aisTpCdNm / uppAisTpNm 기준.
 */
object HousingNoticeFilterUtils {

    val mainCategories: List<String> = listOf("전체", "주택", "상가", "토지/분양")

    fun subCategories(mainCategory: String): List<String> = when (mainCategory) {
        "주택" -> listOf(
            "전체",
            "행복주택 / 신혼희망타운",
            "매입임대 / 집주인임대",
            "영구 / 통합공공임대"
        )
        "상가" -> listOf(
            "전체",
            "일반 경쟁 입찰",
            "추첨 / 공모 심사"
        )
        "토지/분양" -> listOf(
            "전체",
            "토지 및 분양 공고"
        )
        else -> listOf("전체")
    }

    fun matchesMainCategory(aisTpCdNm: String, uppAisTpNm: String, mainCategory: String): Boolean {
        if (mainCategory == "전체") return true
        val type = aisTpCdNm.trim()
        val upper = uppAisTpNm.trim()
        return when (mainCategory) {
            "주택" -> upper.contains("임대주택") ||
                matchesAny(type, HAPPY_HOME_TYPES + PURCHASE_TYPES + PERMANENT_TYPES)
            "상가" -> upper.contains("상가") || matchesAny(type, SHOP_BID_TYPES + SHOP_LOTTERY_TYPES)
            "토지/분양" -> upper.contains("토지") || upper.contains("분양") ||
                type.contains("토지") || type.contains("분양") ||
                matchesAny(type, LAND_TYPES)
            else -> true
        }
    }

    fun matchesSubCategory(aisTpCdNm: String, mainCategory: String, subCategory: String): Boolean {
        if (subCategory == "전체" || mainCategory == "전체") return true
        val type = aisTpCdNm.trim()
        return when (subCategory) {
            "행복주택 / 신혼희망타운" -> matchesAny(type, HAPPY_HOME_TYPES)
            "매입임대 / 집주인임대" -> matchesAny(type, PURCHASE_TYPES)
            "영구 / 통합공공임대" -> matchesAny(type, PERMANENT_TYPES)
            "일반 경쟁 입찰" -> matchesAny(type, SHOP_BID_TYPES)
            "추첨 / 공모 심사" -> matchesAny(type, SHOP_LOTTERY_TYPES)
            "토지 및 분양 공고" -> matchesAny(type, LAND_TYPES) ||
                type.contains("분양")
            else -> true
        }
    }

    fun matchesCnpRegion(itemRegion: String, filterRegion: String): Boolean {
        if (filterRegion == "전체") return true
        if (itemRegion.isBlank()) return false
        val item = itemRegion.trim()
        val filter = filterRegion.trim()
        return item == filter ||
            item.contains(filter) ||
            filter.contains(item) ||
            HousingRegionUtils.matchesRegion(item, filter)
    }

    private fun matchesAny(value: String, candidates: List<String>): Boolean =
        candidates.any { c -> value.contains(c) || c.contains(value) }

    private val HAPPY_HOME_TYPES = listOf("행복주택", "행복주택(신혼희망)")
    private val PURCHASE_TYPES = listOf("매입임대", "집주인임대")
    private val PERMANENT_TYPES = listOf("영구임대", "통합공공임대")
    private val SHOP_BID_TYPES = listOf("임대상가(입찰)", "(구)임대상가(입찰)")
    private val SHOP_LOTTERY_TYPES = listOf("임대상가(추첨)", "임대상가(공모ㆍ심사)", "임대상가(공모·심사)")
    private val LAND_TYPES = listOf("토지", "분양")
}
