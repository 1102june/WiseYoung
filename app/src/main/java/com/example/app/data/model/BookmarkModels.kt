package com.example.app.data.model

data class BookmarkRequest(
    val userId: String,
    val contentType: String, // "policy" or "housing"
    val contentId: String
)

data class BookmarkResponse(
    val bookmarkId: Int,
    val userId: String,
    val contentType: String,
    val contentId: String,
    val isActive: String,
    val createdAt: String,
    // 클라이언트에서 필요한 추가 정보
    val title: String? = null,
    val deadline: String? = null,
    val organization: String? = null
)

