package com.example.app.data.model

/**
 * Passkey 등록 요청 생성 응답
 * 서버에서 받아오는 challenge 및 설정
 */
data class PasskeyRegisterRequestResponse(
    val challenge: String,              // Base64 URL-safe 인코딩된 challenge
    val rpId: String,                   // Relying Party ID (도메인)
    val rpName: String? = null,         // Relying Party 이름
    val userId: String,                 // Base64 URL-safe 인코딩된 사용자 ID
    val userName: String? = null,       // 사용자 이름 (이메일)
    val userDisplayName: String? = null, // 사용자 표시 이름
    val timeout: Long? = null,          // 타임아웃 (밀리초)
    val userVerification: String? = null // "required", "preferred", "discouraged"
)

