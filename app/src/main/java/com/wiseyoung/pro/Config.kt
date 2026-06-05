package com.wiseyoung.pro

/**
 * API 엔드포인트 및 서버 URL.
 * BASE_URL은 [BuildConfig.BASE_URL]과 동기화 (debug=로컬, release=배포 서버).
 */
object Config {

    val BASE_URL: String = BuildConfig.BASE_URL.trimEnd('/')

    object Api {
        const val SIGNUP = "/auth/signup"
        const val LOGIN = "/auth/login"
        const val LOGOUT = "/auth/logout"
        const val PROFILE = "/auth/profile"
        const val BOOKMARKS = "/bookmarks"
        const val NOTIFICATIONS = "/notifications"
        const val PUSH_TOKEN = "/auth/push-token"
        const val EMAIL_CHECK = "/auth/otp/email/check"
        const val OTP_SEND = "/auth/otp/send"
        const val OTP_VERIFY = "/auth/otp/verify"
        const val PASSWORD_RESET = "/auth/password-reset"
    }

    fun getUrl(endpoint: String): String = "$BASE_URL$endpoint"
}
