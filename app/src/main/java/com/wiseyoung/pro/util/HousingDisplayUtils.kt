package com.wiseyoung.pro.util

import com.wiseyoung.pro.data.model.HousingComplexResponse
import com.wiseyoung.pro.data.model.HousingNoticeResponse
import kotlin.math.roundToInt

object HousingDisplayUtils {

    fun formatMoneyWon(amountWon: Int?): String {
        if (amountWon == null || amountWon <= 0) return "미정"
        val manWon = amountWon / 10_000L
        return when {
            manWon >= 10_000L -> {
                val eok = manWon / 10_000L
                val rest = manWon % 10_000L
                if (rest == 0L) "%,d억원".format(eok) else "%,d억 %,d만원".format(eok, rest)
            }
            else -> "%,d만원".format(manWon)
        }
    }

    fun formatDepositMonthly(depositWon: Int?, monthlyRentWon: Int?): String {
        return "보증금 ${formatMoneyWon(depositWon)} · 월세 ${formatMoneyWon(monthlyRentWon)}"
    }

    fun formatSupplyArea(supplyArea: Double?): String {
        if (supplyArea == null || supplyArea <= 0) return "면적 미정"
        val sqm = supplyArea.roundToInt()
        val pyeong = supplyArea / 3.3058
        return "${sqm}㎡ (${String.format("%.1f", pyeong)}평)"
    }

    fun formatDistanceMeters(distanceMeters: Double?): String? {
        if (distanceMeters == null || distanceMeters < 0) return null
        return if (distanceMeters < 1_000) {
            "${distanceMeters.roundToInt()}m"
        } else {
            String.format("%.1fkm", distanceMeters / 1_000.0)
        }
    }

    fun complexRentSummary(complex: HousingComplexResponse): String {
        return complex.rentSummary?.takeIf { it.isNotBlank() }
            ?: formatDepositMonthly(complex.deposit, complex.monthlyRent)
    }

    fun complexSummaryLine(complex: HousingComplexResponse): String {
        return complex.summaryLine?.takeIf { it.isNotBlank() }
            ?: buildString {
                val type = complex.displayHousingType().ifBlank { "임대주택" }
                append(type)
                complex.totalUnits?.takeIf { it > 0 }?.let { append(" · ${it}세대") }
                complex.supplyArea?.takeIf { it > 0 }?.let {
                    append(" · ${formatSupplyArea(it)}")
                }
            }
    }

    fun noticeTitle(notice: HousingNoticeResponse): String {
        return notice.title?.takeIf { it.isNotBlank() }
            ?: notice.panNm?.takeIf { it.isNotBlank() }
            ?: notice.hsmpNm?.let { "$it 입주자 모집" }
            ?: "임대주택 모집 공고"
    }

    fun noticeRegion(notice: HousingNoticeResponse): String {
        return notice.region?.takeIf { it.isNotBlank() }
            ?: notice.cnpCdNm?.let { HousingRegionUtils.normalizeRegion(it) ?: it.trim() }
            ?: ""
    }

    fun noticeStatus(notice: HousingNoticeResponse, fallback: String): String {
        return notice.status?.takeIf { it.isNotBlank() } ?: fallback
    }

    fun noticeRentSummary(notice: HousingNoticeResponse): String {
        return notice.rentSummary?.takeIf { it.isNotBlank() }
            ?: notice.matchedComplex?.let { complexRentSummary(it) }
            ?: "보증금 · 월세는 상세 공고에서 확인"
    }

    /** 단지 ID/이름과 매칭되는 활성 공고에서 가장 가까운 마감일 */
    fun nearestDeadlineForComplex(
        complexId: String?,
        complexName: String,
        notices: List<HousingNoticeResponse>
    ): String {
        val normalizedName = complexName.trim()
        return notices
            .filter { notice ->
                val matchedId = notice.matchedComplex?.complexId
                when {
                    complexId != null && matchedId == complexId -> true
                    complexId != null && notice.hsmpSn == complexId -> true
                    normalizedName.isNotBlank() && !notice.hsmpNm.isNullOrBlank() ->
                        notice.hsmpNm!!.contains(normalizedName) || normalizedName.contains(notice.hsmpNm!!)
                    else -> false
                }
            }
            .mapNotNull { notice ->
                notice.applicationEnd?.take(10)?.replace("-", ".")?.takeIf { it.isNotBlank() }
            }
            .firstOrNull()
            .orEmpty()
    }
}
