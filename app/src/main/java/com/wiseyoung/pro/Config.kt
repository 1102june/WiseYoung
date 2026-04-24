package com.wiseyoung.pro

/**
 * 서버 URL 및 API 엔드포인트 설정
 * 
 * 사용법:
 * - 개발 환경: isDebug를 true로 설정
 * - 프로덕션: isDebug를 false로 설정
 */
object Config {
    // 개발/프로덕션 모드 전환
    // 로컬 개발용: true, 서버 배포용: false
    private const val isDebug = true
    
    // 서버 기본 URL
    // 
    // ⚠️ 중요: USB 테더링 사용 시 설정 방법
    //
    // 방법 1 (권장): ADB 포트 포워딩 사용
    //    1. USB로 기기를 연결
    //    2. PowerShell에서 실행: adb reverse tcp:8080 tcp:8080
    //    3. 아래 BASE_URL_DEV를 "http://127.0.0.1:8080"으로 유지
    //    ✅ 이 방법이 가장 안정적이며, 네트워크 설정과 무관하게 작동합니다.
    //
    // 방법 2: USB 테더링 IP 주소 사용
    //    1. 컴퓨터 IP 확인: PowerShell에서 "ipconfig | findstr IPv4" 실행
    //       또는 "ipconfig /all" 실행 후 USB 테더링 어댑터 찾기
    //    2. 아래 BASE_URL_DEV를 찾은 IP 주소로 변경
    //       예: "http://172.16.2.178:8080"
    //    3. Spring Boot 서버가 모든 인터페이스에서 수신하도록 설정 확인
    //       (application.yml에서 server.address=0.0.0.0 또는 주석 처리)
    //    4. Windows 방화벽에서 8080 포트 허용 확인
    //
    // 현재 설정: 서버 URL 사용 (Debug APK에서 서버 연동 테스트용)
    private const val BASE_URL_DEV = "http://127.0.0.1:8080"  // 로컬 개발용 (ADB 포트 포워딩)
    
    // 🔥 USB 테더링 IP 사용 시 (방법 2):
    //    아래 주석을 해제하고 컴퓨터의 USB 테더링 네트워크 어댑터 IP 주소 사용
    //    컴퓨터 IP 확인: PowerShell에서 "ipconfig | findstr IPv4" 실행
    //    기기 IP 확인: Android 기기 설정 > 네트워크 > USB 테더링에서 확인
    //    컴퓨터와 기기가 같은 서브넷에 있어야 함 (예: 172.16.5.x)
    // private const val BASE_URL_DEV = "http://172.16.2.178:8080"  // USB 테더링 IP 사용 시 (위의 127.0.0.1 주석 처리)
    // private const val BASE_URL_DEV = "http://10.0.2.2:8080"  // Android 에뮬레이터 사용 시
    private const val BASE_URL_PROD = "http://210.104.76.139:8080"
    
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
        const val PROFILE = "/auth/profile" // GET: 프로필 조회, POST: 프로필 저장
        const val BOOKMARKS = "/bookmarks"
        const val NOTIFICATIONS = "/notifications"
        const val PUSH_TOKEN = "/auth/push-token" // FCM 토큰 저장
        const val EMAIL_CHECK = "/auth/otp/email/check" // 이메일 중복 확인
        const val OTP_SEND = "/auth/otp/send" // 이메일 인증번호 발송
        const val OTP_VERIFY = "/auth/otp/verify" // 이메일 인증번호 확인
        const val PASSWORD_RESET = "/auth/password-reset" // 비밀번호 재설정
    }
    
    // 전체 URL 생성 헬퍼 함수
    fun getUrl(endpoint: String): String {
        return "$BASE_URL$endpoint"
    }
}

