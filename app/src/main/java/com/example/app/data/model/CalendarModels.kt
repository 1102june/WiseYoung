package com.example.app.data.model

import com.google.gson.annotations.SerializedName

data class CalendarEventRequest(
    @SerializedName("userId") val userId: String,
    @SerializedName("title") val title: String,
    @SerializedName("eventType") val eventType: String, // "policy" or "housing"
    @SerializedName("endDate") val endDate: String, // "yyyy-MM-dd"
    // 알림 설정
    @SerializedName("isSevenDaysAlert") val isSevenDaysAlert: Boolean = false,
    @SerializedName("sevenDaysAlertTime") val sevenDaysAlertTime: String? = null,
    @SerializedName("isOneDayAlert") val isOneDayAlert: Boolean = false,
    @SerializedName("oneDayAlertTime") val oneDayAlertTime: String? = null,
    @SerializedName("isCustomAlert") val isCustomAlert: Boolean = false,
    @SerializedName("customAlertDays") val customAlertDays: Int? = null,
    @SerializedName("customAlertTime") val customAlertTime: String? = null
)

data class CalendarEventResponse(
    val eventId: Int,
    val userId: String,
    val title: String,
    val eventType: String,
    val endDate: String,
    val createdAt: String
)
