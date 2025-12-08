package com.example.app.data.model

/**
 * 회원탈퇴 요청
 * 이메일과 OTP 인증번호를 받아서 검증 후 탈퇴 처리
 */
data class DeleteAccountRequest(
    val email: String,
    val otp: String
)

