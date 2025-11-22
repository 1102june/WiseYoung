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
    // 🔥 실제 기기 + USB 연결 시:
    //    1. ADB 포트 포워딩 실행 (Android Studio Terminal):
    //       C:\Users\USER\AppData\Local\Android\Sdk\platform-tools\adb.exe reverse tcp:8080 tcp:8080
    //    2. 포트 포워딩 확인: adb reverse --list
    //    3. 앱 재시작
    //    → 현재 설정: "http://127.0.0.1:8080" (ADB 포트 포워딩 필요)
    //
    // 🔥 ADB 포트 포워딩이 안 될 때 (대안):
    //    아래 주석을 해제하고 위의 BASE_URL_DEV를 주석 처리
    //    컴퓨터와 기기가 같은 Wi-Fi 네트워크에 연결되어 있어야 함
    private const val BASE_URL_DEV = "http://127.0.0.1:8080"  // USB 연결 시 localhost (ADB 포트 포워딩 필요)
    // private const val BASE_URL_DEV = "http://172.29.121.3:8080"  // 컴퓨터 IP 사용 (USB 연결 시 대안)
    // private const val BASE_URL_DEV = "http://10.0.2.2:8080"  // Android 에뮬레이터 사용 시
    // private const val BASE_URL_DEV = "http://192.168.x.x:8080"  // Wi-Fi 연결 시 (컴퓨터 IP 주소로 변경)
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
        const val PUSH_TOKEN = "/auth/push-token" // FCM 토큰 저장
        const val EMAIL_CHECK = "/auth/otp/email/check" // 이메일 중복 확인
        const val OTP_SEND = "/auth/otp/send" // 이메일 인증번호 발송
        const val OTP_VERIFY = "/auth/otp/verify" // 이메일 인증번호 확인
    }
    
    // 전체 URL 생성 헬퍼 함수
    fun getUrl(endpoint: String): String {
        return "$BASE_URL$endpoint"
    }
}

