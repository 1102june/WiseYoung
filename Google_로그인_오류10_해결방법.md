# Google 로그인 오류 10 (DEVELOPER_ERROR) 해결 방법

## 🔴 문제 원인

Google 로그인 오류 코드 10은 `DEVELOPER_ERROR`로, **SHA-1 인증서 지문이 Firebase Console에 등록되지 않았거나 일치하지 않을 때** 발생합니다.

## ✅ 확인된 정보

### 현재 상황
- **실제 디버그 키스토어 SHA-1**: `26:3B:25:03:FC:33:35:9A:0B:64:EA:0F:1C:A7:12:E8:03:CE:B1:ED`
- **google-services.json에 등록된 SHA-1**: `0f8f982b1eb4c103d38777ced81ad50f8f2c91eb`
- **일치 여부**: ❌ **일치하지 않음**

## 🔧 해결 방법

### 1단계: Firebase Console에 SHA-1 지문 추가

1. **Firebase Console 접속**
   - https://console.firebase.google.com/
   - 프로젝트: `wiseyoung-5eeaa` 선택

2. **프로젝트 설정 열기**
   - 좌측 톱니바퀴 아이콘 클릭
   - "프로젝트 설정" 선택

3. **앱 선택**
   - "내 앱" 섹션에서 Android 앱 (`com.wiseyoung.app`) 선택

4. **SHA 인증서 지문 추가**
   - "SHA 인증서 지문" 섹션으로 스크롤
   - "지문 추가" 버튼 클릭
   - 다음 SHA-1 지문을 추가:
     ```
     26:3B:25:03:FC:33:35:9A:0B:64:EA:0F:1C:A7:12:E8:03:CE:B1:ED
     ```
   - 또는 콜론 없이:
     ```
     263B2503FC33359A0B64EA0F1CA712E803CEB1ED
     ```

5. **google-services.json 다시 다운로드**
   - "google-services.json 다운로드" 버튼 클릭
   - 다운로드한 파일을 `app/google-services.json`에 덮어쓰기

### 2단계: 앱 재빌드 및 테스트

1. **프로젝트 클린**
   ```bash
   ./gradlew clean
   ```

2. **앱 재빌드**
   - Android Studio에서 "Build > Rebuild Project"

3. **앱 재설치**
   - 기존 앱 삭제 후 다시 설치

4. **Google 로그인 테스트**

## 📝 참고사항

### SHA-1 지문 확인 방법

디버그 키스토어의 SHA-1 지문을 다시 확인하려면:

**Windows PowerShell:**
```powershell
keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -storepass android -alias androiddebugkey
```

**macOS/Linux:**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -storepass android -alias androiddebugkey
```

### 릴리즈 빌드용 SHA-1

나중에 릴리즈 빌드를 배포할 때는 릴리즈 키스토어의 SHA-1도 추가해야 합니다:

```bash
keytool -list -v -keystore [릴리즈_키스토어_경로] -alias [앨리어스명]
```

## ⚠️ 주의사항

- SHA-1 지문을 추가한 후 **google-services.json 파일을 반드시 다시 다운로드**해야 합니다.
- 파일을 업데이트한 후 **앱을 완전히 삭제하고 재설치**해야 변경사항이 적용됩니다.
- Firebase Console에서 변경사항이 반영되는 데 몇 분이 걸릴 수 있습니다.

## 🔍 추가 확인사항

### default_web_client_id 확인
- `app/src/main/res/values/strings.xml`에 `default_web_client_id`가 올바르게 설정되어 있는지 확인
- 현재 값: `609597104515-hhhv4g5tckho264n2jv2mpnqtie0qf48.apps.googleusercontent.com`
- 이 값은 `google-services.json`의 `oauth_client` 중 `client_type: 3`인 항목의 `client_id`와 일치해야 합니다.

### OAuth 클라이언트 ID 확인
- Google Cloud Console (https://console.cloud.google.com/)에서도 확인 가능
- 프로젝트: `wiseyoung-5eeaa`
- "API 및 서비스" > "사용자 인증 정보"에서 OAuth 2.0 클라이언트 ID 확인

