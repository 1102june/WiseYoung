package com.example.app.data.model

data class CalendarEventRequest(
    val userId: String,
    val title: String,
    val eventType: String, // "policy" or "housing"
    val endDate: String // "yyyy-MM-dd"
)

data class CalendarEventResponse(
    val eventId: Int,
    val userId: String,
    val title: String,
    val eventType: String,
    val endDate: String,
    val createdAt: String
)

