# FCM 알림 테스트 가이드

## ✅ 구현 완료

Firebase 콘솔에서 토큰을 직접 등록할 수 없는 문제를 해결하기 위해 **서버에서 직접 FCM 알림을 발송**하는 기능을 구현했습니다.

## 📱 구현된 기능

1. **FCM 토큰 저장** (`/auth/push-token`)
   - 앱에서 FCM 토큰을 서버에 저장
   - 이미 구현되어 있음

2. **FCM 알림 발송 서비스** (`FcmService.java`)
   - 단일 기기에 알림 발송
   - 특정 사용자에게 알림 발송
   - 여러 기기에 알림 발송 (멀티캐스트)

3. **테스트용 API 엔드포인트**
   - `/auth/test-notification` - 사용자 ID로 알림 발송
   - `/auth/test-notification-by-token` - FCM 토큰으로 직접 알림 발송

## 🧪 테스트 방법

### 방법 1: FCM 토큰으로 직접 테스트 (가장 쉬움) ⭐

1. **앱에서 FCM 토큰 확인**
   - 앱 실행 및 로그인
   - Android Studio Logcat에서 `FcmTokenService` 필터 적용
   - FCM 토큰 복사

2. **Postman 또는 curl로 테스트**
   ```bash
   POST http://localhost:8080/auth/test-notification-by-token
   Content-Type: application/json
   
   {
     "fcmToken": "여기에_FCM_토큰_붙여넣기",
     "title": "테스트 알림",
     "body": "알림이 정상적으로 작동합니다!"
   }
   ```

3. **기기에서 알림 확인**
   - 앱이 실행 중이거나 백그라운드에 있어야 함
   - 알림이 표시되는지 확인

### 방법 2: 사용자 ID로 테스트

1. **앱에서 로그인**
   - FCM 토큰이 서버에 저장되어 있어야 함

2. **ID Token 가져오기**
   - 앱에서 로그인 시 ID Token이 생성됨
   - 또는 Logcat에서 확인

3. **Postman 또는 curl로 테스트**
   ```bash
   POST http://localhost:8080/auth/test-notification
   Content-Type: application/json
   
   {
     "idToken": "여기에_ID_Token_붙여넣기",
     "title": "테스트 알림",
     "body": "알림이 정상적으로 작동합니다!"
   }
   ```

## 📋 API 엔드포인트 상세

### 1. FCM 토큰 저장 (기존)
```
POST /auth/push-token
Content-Type: application/json

{
  "idToken": "Firebase ID Token",
  "pushToken": "FCM 등록 토큰"
}
```

### 2. 테스트 알림 발송 (사용자 ID)
```
POST /auth/test-notification
Content-Type: application/json

{
  "idToken": "Firebase ID Token",
  "title": "알림 제목",
  "body": "알림 내용"
}
```

### 3. 테스트 알림 발송 (FCM 토큰 직접)
```
POST /auth/test-notification-by-token
Content-Type: application/json

{
  "fcmToken": "FCM 등록 토큰",
  "title": "알림 제목",
  "body": "알림 내용"
}
```

## 🔧 PowerShell로 테스트하기

### FCM 토큰으로 직접 테스트
```powershell
$fcmToken = "여기에_FCM_토큰_붙여넣기"

$body = @{
    fcmToken = $fcmToken
    title = "테스트 알림"
    body = "알림이 정상적으로 작동합니다!"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/auth/test-notification-by-token" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

### 사용자 ID로 테스트
```powershell
$idToken = "여기에_ID_Token_붙여넣기"

$body = @{
    idToken = $idToken
    title = "테스트 알림"
    body = "알림이 정상적으로 작동합니다!"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/auth/test-notification" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

## ⚠️ 주의사항

1. **FCM 토큰이 서버에 저장되어 있어야 함**
   - 앱 실행 시 자동으로 저장됨
   - MainActivity 시작 시 `FcmTokenService.getAndSaveToken()` 호출

2. **앱이 실행 중이거나 백그라운드에 있어야 함**
   - 앱이 완전히 종료되어 있으면 알림을 받을 수 없음
   - 백그라운드에서도 알림 수신 가능

3. **Firebase Admin SDK 설정 확인**
   - `firebase-admin.json` 파일이 `src/main/resources/`에 있어야 함
   - Firebase 프로젝트의 서비스 계정 키 파일

## 🎯 실제 사용 예시

### 정책 알림 발송
```java
// PolicyService에서 사용
fcmService.sendNotificationToUser(
    userId,
    "새로운 청년정책",
    "청년주거지원사업 신청이 시작되었습니다."
);
```

### 여러 사용자에게 알림 발송
```java
List<String> userIds = Arrays.asList("user1", "user2", "user3");
for (String userId : userIds) {
    fcmService.sendNotificationToUser(
        userId,
        "공지사항",
        "앱 업데이트가 있습니다."
    );
}
```

## 📝 요약

**Firebase 콘솔 대신 서버 API를 사용하여 FCM 알림을 테스트할 수 있습니다:**

1. ✅ 앱에서 FCM 토큰 확인 (Logcat)
2. ✅ 서버 API로 알림 발송 (`/auth/test-notification-by-token`)
3. ✅ 기기에서 알림 확인

이제 Firebase 콘솔의 UI 없이도 FCM 알림을 테스트할 수 있습니다!

