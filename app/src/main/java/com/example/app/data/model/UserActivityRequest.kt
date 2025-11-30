package com.example.app.data.model

data class UserActivityRequest(
    val activityType: String,  // "CLICK", "VIEW", "SEARCH", "BOOKMARK" 등
    val contentType: String? = null,  // "policy" 또는 "housing"
    val contentId: String? = null,    // 정책 ID 또는 주택 ID
    val searchKeyword: String? = null,  // 검색 키워드 (검색 활동인 경우)
    val metadata: String? = null  // 추가 메타데이터 (JSON 형식, 선택)
)

