# Google 로그인 디버깅 가이드

## 🔴 현재 문제

Google 로그인이 계속 취소되고 있습니다 (result code: 0 - RESULT_CANCELED)

## ✅ 확인 사항

### 1. Google Play Services 설치 확인

**에뮬레이터 사용 시:**
- Google Play Services가 설치되어 있는지 확인
- Google Play Store가 있는 에뮬레이터를 사용해야 합니다
- AVD Manager에서 "Google Play" 아이콘이 있는 이미지 선택

**실제 기기 사용 시:**
- Google Play Services가 최신 버전인지 확인
- Play Store에서 Google Play Services 업데이트

### 2. SHA-1 인증서 지문 확인

**현재 등록된 SHA-1 (google-services.json):**
- `263b2503fc33359a0b64ea0f1ca712e803ceb1ed` (소문자)
- `0f8f982b1eb4c103d38777ced81ad50f8f2c91eb` (소문자)

**SHA-1 확인 방법 (PowerShell):**
```powershell
keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -storepass android -alias androiddebugkey
```

**SHA-1 확인 방법 (Android Studio):**
1. Gradle 탭 열기
2. `app` > `Tasks` > `android` > `signingReport` 실행
3. 콘솔에서 SHA-1 값 확인

**Firebase Console에 SHA-1 추가:**
1. https://console.firebase.google.com/ 접속
2. 프로젝트: `wiseyoung-5eeaa` 선택
3. 프로젝트 설정 > 앱 > Android 앱 (`com.wiseyoung.app`) 선택
4. "SHA 인증서 지문" 섹션에서 현재 SHA-1이 등록되어 있는지 확인
5. 없으면 "지문 추가" 버튼 클릭하여 추가

### 3. OAuth 클라이언트 ID 확인

**현재 설정 (strings.xml):**
```
default_web_client_id: 609597104515-hhhv4g5tckho264n2jv2mpnqtie0qf48.apps.googleusercontent.com
```

**이 값은 google-services.json의 다음 항목과 일치해야 함:**
```json
{
  "client_id": "609597104515-hhhv4g5tckho264n2jv2mpnqtie0qf48.apps.googleusercontent.com",
  "client_type": 3
}
```

### 4. google-services.json 확인

**위치:** `app/google-services.json`

**확인 사항:**
- `package_name`이 `com.wiseyoung.app`인지 확인
- `oauth_client`에 `client_type: 3` 항목이 있는지 확인
- 최신 파일인지 확인 (SHA-1 추가 후 다시 다운로드)

### 5. 앱 재빌드 및 재설치

1. **프로젝트 클린:**
   ```bash
   ./gradlew clean
   ```

2. **프로젝트 재빌드:**
   - Android Studio: Build > Rebuild Project

3. **앱 완전 삭제:**
   - 기존 앱을 완전히 삭제

4. **앱 재설치:**
   - Android Studio에서 Run 또는
   - APK를 직접 설치

### 6. 로그 확인

**Logcat 필터:**
```
tag:LoginActivity
```

**확인할 로그:**
- "Google Sign-In Web Client ID: ..." - Web Client ID가 올바르게 로드되는지
- "Google Sign-In 클라이언트 초기화 완료" - 초기화가 성공했는지
- "Google 로그인 시작..." - 로그인 시작 로그
- "Google 로그인 결과 수신" - 결과 수신 로그
- 오류 메시지나 상태 코드

## 🔧 추가 확인사항

### 에뮬레이터 사용 시

Google 로그인은 에뮬레이터에서 제대로 작동하지 않을 수 있습니다. 
**실제 기기에서 테스트**하는 것을 권장합니다.

### 네트워크 확인

- 인터넷 연결 확인
- 방화벽이나 VPN이 Google 서비스 접근을 차단하지 않는지 확인

### Google 계정 확인

- 기기에 Google 계정이 추가되어 있는지 확인
- Settings > Accounts에서 Google 계정 확인

## 📝 테스트 절차

1. 앱 완전 삭제
2. 프로젝트 클린 및 재빌드
3. 앱 재설치
4. 로그인 화면에서 "Google Login" 버튼 클릭
5. Logcat에서 로그 확인
6. Google 계정 선택 화면이 나타나는지 확인

## ⚠️ 주의사항

- SHA-1을 Firebase Console에 추가한 후 **google-services.json을 다시 다운로드**해야 합니다
- google-services.json을 업데이트한 후 **앱을 완전히 삭제하고 재설치**해야 합니다
- Firebase Console 변경사항이 반영되는 데 몇 분이 걸릴 수 있습니다

## 🐛 문제가 계속되면

1. Logcat의 전체 에러 로그를 확인
2. Firebase Console의 OAuth 클라이언트 설정 확인
3. Google Cloud Console (https://console.cloud.google.com/)에서 OAuth 동의 화면 설정 확인

