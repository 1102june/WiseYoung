package com.wiseyoung.pro.data.model

/**
 * POST /auth/profile 성공 시 data 필드 (백엔드 ProfileSaveResponse와 동일).
 */
data class ProfileSaveResponse(
    val recommendationsRefreshed: Boolean = false,
    val recommendationCount: Int = 0
)
