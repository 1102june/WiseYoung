package com.example.app.data.model

/**
 * Passkey 로그인 요청 생성 응답
 * 서버에서 받아오는 challenge 및 설정
 */
data class PasskeyLoginRequestResponse(
    val challenge: String,              // Base64 URL-safe 인코딩된 challenge
    val rpId: String,                   // Relying Party ID (도메인)
    val timeout: Long? = null,          // 타임아웃 (밀리초)
    val userVerification: String? = null, // "required", "preferred", "discouraged"
    val allowCredentials: List<String>? = null // 허용할 credential ID 목록
)

