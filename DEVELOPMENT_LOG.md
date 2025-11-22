# 개발 로그 (2025-11-21)

## 주요 수정 사항

### 1. 네트워크 연결 설정
- **문제**: Android 앱에서 Spring Boot 서버로 데이터 전송 실패
- **해결**: 
  - `Config.kt`에 API 엔드포인트 중앙 관리
  - USB 연결 시 ADB 포트 포워딩 사용 (`127.0.0.1:8080`)
  - `network_security_config.xml`에 cleartext traffic 허용

### 2. Google 로그인 사용자 자동 생성
- **문제**: Google 로그인 사용자가 프로필 저장 시 "사용자를 찾을 수 없습니다" 오류
- **해결**: 
  - `/auth/profile` 엔드포인트에서 사용자 없으면 자동 생성
  - 이메일 중복 시 기존 사용자 사용하도록 처리

### 3. 데이터베이스 스키마 대응
- `password_hash` nullable 처리 (Google 로그인 사용자 대응)
- `app_version`, `device_id` 필드 추가 및 저장 로직 구현
- `region` 필드 VARCHAR(10) 제약에 맞춰 `province`만 저장

### 4. Enum 값 수정
- `LoginType.GOOGLE` → `LoginType.google`
- `OSType.ANDROID` → `OSType.android`

### 5. FCM 알림 기능 추가
- FCM 토큰 수집 및 서버 저장
- 알림 수신 서비스 구현
- `/auth/push-token` 엔드포인트 추가

### 6. UI 개선
- 하단 네비게이션 바 위치 조정 (시스템 버튼과 겹침 방지)
- 북마크 아이콘 → 하트 아이콘 변경
- 선택된 네비게이션 버튼 시각적 피드백 강화

## 수정된 파일

### Android
- `Config.kt` - API 엔드포인트 중앙 관리
- `ProfileSetupActivity.kt` - appVersion, deviceId 전송 추가
- `RegisterActivity.kt` - appVersion, deviceId 전송 추가
- `LoginActivity.kt` - Google 로그인 오류 처리 개선, FCM 토큰 저장
- `DeviceInfo.kt` - 앱 버전 및 디바이스 ID 조회 유틸리티
- `FcmTokenService.kt` - FCM 토큰 관리 서비스 (신규)
- `MyFirebaseMessagingService.kt` - FCM 알림 수신 서비스 (신규)
- `BottomNavigationBar.kt` - UI 개선

### Spring Boot
- `UserController.java` - 사용자 자동 생성, push-token 엔드포인트 추가
- `UserService.java` - updateUser 메서드 추가, region 필드 처리 수정
- `User.java` - passwordHash nullable, appVersion/deviceId 필드 추가
- `ProfileRequest.java` - appVersion, deviceId 필드 추가
- `PushTokenRequest.java` - FCM 토큰 저장용 DTO (신규)


