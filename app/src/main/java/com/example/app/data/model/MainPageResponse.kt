package com.example.app.data.model

data class MainPageResponse(
    val aiRecommendedPolicies: List<AIRecommendationResponse>,
    val unreadNotificationCount: Int
)

