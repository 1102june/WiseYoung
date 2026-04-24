package com.wiseyoung.pro.data.model

/**
 * Passkey 등록 요청
 * 서버로 전송하는 Passkey credential
 */
data class PasskeyRegisterRequest(
    val credential: String,  // PublicKeyCredential의 registrationResponseJson
    val email: String        // 사용자 이메일 (검증용)
)

