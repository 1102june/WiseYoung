package com.example.app.data.model

data class RegisterRequest(
    val userId: String,
    val email: String,
    val passwordHash: String,
    val emailVerified: Boolean = false,
    val loginType: String,
    val osType: String,
    val appVersion: String,
    val pushToken: String = "",
    val deviceId: String,
    val createdAt: String  // LocalDateTime을 문자열로 전송 (ISO 형식)
)

