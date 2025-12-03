package com.example.app.data.model

data class ChatResponse(
    val response: String,
    val conversationId: String? = null,
    val actionLinks: List<ActionLink> = emptyList()
) {
    data class ActionLink(
        val type: String,  // "policy" 또는 "housing"
        val id: String,    // 정책 ID 또는 주택 ID
        val title: String, // 링크 제목
        val summary: String? = null, // 간단 요약
        val url: String? = null  // 신청 링크 URL
    )
}

