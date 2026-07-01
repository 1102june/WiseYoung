package com.wiseyoung.pro.util

/**
 * 임대주택 공고 지역 필터 — API 원문(cnpCdNm) 대신 표준 시·도명 사용.
 * "광주광역시 외" 등은 정규화 후 매칭.
 */
object HousingRegionUtils {

    private val provinceFilterOrder: List<String> = listOf(
        "서울특별시",
        "부산광역시",
        "대구광역시",
        "인천광역시",
        "광주광역시",
        "대전광역시",
        "울산광역시",
        "세종특별자치시",
        "경기도",
        "강원특별자치도",
        "충청북도",
        "충청남도",
        "전북특별자치도",
        "전라남도",
        "경상북도",
        "경상남도",
        "제주특별자치도"
    )

    /** 공고 지역 필터 드롭다운 (전체 + 17개 시·도) */
    val provinceFilterOptions: List<String> = listOf("전체") + provinceFilterOrder

    private val canonicalByCompact: Map<String, String> = buildMap {
        provinceFilterOrder.forEach { name ->
            put(name.replace(" ", ""), name)
        }
        RegionConstants.provinceDisplayNames.forEach { (_, display) ->
            put(display.replace(" ", ""), display)
        }
        put("서울", "서울특별시")
        put("부산", "부산광역시")
        put("대구", "대구광역시")
        put("인천", "인천광역시")
        put("광주", "광주광역시")
        put("대전", "대전광역시")
        put("울산", "울산광역시")
        put("세종", "세종특별자치시")
        put("경기", "경기도")
        put("강원", "강원특별자치도")
        put("강원도", "강원특별자치도")
        put("충북", "충청북도")
        put("충남", "충청남도")
        put("전북", "전북특별자치도")
        put("전남", "전라남도")
        put("경북", "경상북도")
        put("경남", "경상남도")
        put("제주", "제주특별자치도")
    }

    /**
     * API 지역명 → 표준 시·도명. 예: "광주광역시 외" → "광주광역시"
     */
    fun normalizeRegion(raw: String?): String? {
        if (raw.isNullOrBlank()) return null

        var text = raw.trim()
            .replace(Regex("\\s*외$"), "")
            .replace(Regex("\\s+"), "")

        if (text.isEmpty()) return null

        canonicalByCompact[text]?.let { return it }

        provinceFilterOrder
            .sortedByDescending { it.length }
            .forEach { canonical ->
                val compact = canonical.replace(" ", "")
                if (text.contains(compact) || compact.contains(text)) {
                    return canonical
                }
            }

        RegionConstants.provinceDisplayNames.entries
            .sortedByDescending { it.value.length }
            .forEach { (key, display) ->
                if (text.contains(key) || text.contains(display.replace(" ", ""))) {
                    return display
                }
            }

        return null
    }

    fun matchesRegion(itemRegion: String, filterRegion: String): Boolean {
        if (filterRegion == "전체") return true
        if (itemRegion.isBlank()) return false

        val itemCanonical = normalizeRegion(itemRegion)
        val filterCanonical = normalizeRegion(filterRegion) ?: filterRegion

        if (itemCanonical != null && itemCanonical == filterCanonical) return true

        val itemCompact = itemRegion.replace(" ", "").replace(Regex("외$"), "")
        val filterCompact = filterRegion.replace(" ", "")
        return itemCompact.contains(filterCompact) || filterCompact.contains(itemCompact)
    }
}
