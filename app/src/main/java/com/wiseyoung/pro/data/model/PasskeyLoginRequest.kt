package com.wiseyoung.pro.data.model

/**
 * Passkey 로그인 요청
 * 서버로 전송하는 Passkey credential
 */
data class PasskeyLoginRequest(
    val credential: String  // PublicKeyCredential의 authenticationResponseJson
)

