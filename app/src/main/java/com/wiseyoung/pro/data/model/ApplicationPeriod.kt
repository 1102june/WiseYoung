package com.wiseyoung.pro.data.model

/**
 * 신청기간 표시: applicationPeriodText → applicationPeriod → start/end 조합 순.
 * start/end가 null이면 " ~ "로 이어 붙이지 않음 (상시 신청 등).
 */
object ApplicationPeriodFormatter {
    fun format(
        applicationPeriodText: String? = null,
        applicationPeriod: String? = null,
        applicationStart: String? = null,
        applicationEnd: String? = null
    ): String {
        applicationPeriodText?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        applicationPeriod?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }

        val start = formatDate(applicationStart)
        val end = formatDate(applicationEnd)

        return when {
            start != null && end != null -> "$start ~ $end"
            start != null -> start
            end != null -> end
            else -> ""
        }
    }

    private fun formatDate(isoDate: String?): String? {
        return isoDate?.take(10)?.replace("-", ".")?.trim()?.takeIf { it.isNotEmpty() }
    }
}

fun PolicyResponse.displayApplicationPeriod(): String =
    ApplicationPeriodFormatter.format(
        applicationPeriodText = applicationPeriodText,
        applicationPeriod = applicationPeriod,
        applicationStart = applicationStart,
        applicationEnd = applicationEnd
    )

fun HousingNoticeResponse.displayApplicationPeriod(): String =
    ApplicationPeriodFormatter.format(
        applicationPeriodText = applicationPeriodText,
        applicationPeriod = applicationPeriod,
        applicationStart = applicationStart,
        applicationEnd = applicationEnd
    )

fun HousingResponse.displayApplicationPeriod(): String =
    ApplicationPeriodFormatter.format(
        applicationPeriodText = applicationPeriodText,
        applicationPeriod = applicationPeriod,
        applicationStart = applicationStart,
        applicationEnd = applicationEnd
    )
