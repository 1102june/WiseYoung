package com.example.app

/**
 * 서버 URL 및 API 엔드포인트 설정
 * 
 * 사용법:
 * - 개발 환경: isDebug를 true로 설정
 * - 프로덕션: isDebug를 false로 설정
 */
object Config {
    // 개발/프로덕션 모드 전환
    private const val isDebug = true
    
    // 서버 기본 URL
    // 서버 컴퓨터 IP: 192.168.123.163 (ipconfig 확인 결과)
    private const val BASE_URL_DEV = "http://192.168.123.163:8080"
    private const val BASE_URL_PROD = "https://your-production-server.com"
    
    // 현재 사용할 서버 URL
    val BASE_URL: String = if (isDebug) {
        BASE_URL_DEV
    } else {
        BASE_URL_PROD
    }
    
    // API 엔드포인트
    object Api {
        const val SIGNUP = "/auth/signup"
        const val LOGIN = "/auth/login"
        const val LOGOUT = "/auth/logout"
        const val PROFILE = "/auth/profile"
        const val BOOKMARKS = "/bookmarks"
        const val NOTIFICATIONS = "/notifications"
    }
    
    // 전체 URL 생성 헬퍼 함수
    fun getUrl(endpoint: String): String {
        return "$BASE_URL$endpoint"
    }
}

