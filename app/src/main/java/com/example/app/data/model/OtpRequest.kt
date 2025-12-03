package com.example.app.data.model

data class OtpRequest(
    val email: String,
    val otp: String? = null // verify에서만 사용
)

