package com.wiseyoung.pro.data.model

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
    // 클라이언트 평탄 필드 (레거시)
    val title: String? = null,
    val deadline: String? = null,
    val organization: String? = null,
    // 서버 응답 nested (실제 API)
    val policy: PolicyResponse? = null,
    val housing: HousingResponse? = null
)

fun BookmarkResponse.displayTitle(): String? =
    title?.takeIf { it.isNotBlank() }
        ?: policy?.title?.takeIf { it.isNotBlank() }
        ?: housing?.name?.takeIf { it.isNotBlank() }

fun BookmarkResponse.displayOrganization(): String? =
    organization?.takeIf { it.isNotBlank() }
        ?: policy?.region?.takeIf { it.isNotBlank() }
        ?: housing?.organization?.takeIf { it.isNotBlank() }

fun BookmarkResponse.displayDeadline(): String? {
    deadline?.takeIf { it.isNotBlank() }?.let { return it }
    policy?.applicationEnd?.takeIf { it.isNotBlank() }?.let { return it.take(10) }
    housing?.applicationEnd?.takeIf { it.isNotBlank() }?.let { return it.take(10) }
    return null
}

