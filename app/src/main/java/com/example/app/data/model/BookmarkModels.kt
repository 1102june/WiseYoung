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
    val createdAt: String
)

