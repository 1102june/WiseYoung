# Google 로그인 설정 가이드

## 문제
같은 Firebase 프로젝트를 사용하더라도, 각 컴퓨터마다 **SHA-1 지문**이 다릅니다.
- 학교 컴퓨터의 SHA-1: `263b2503fc33359a0b64ea0f1ca712e803ceb1ed`, `0f8f982b1eb4c103d38777ced81ad50f8f2c91eb`
- 본인 컴퓨터의 SHA-1: 확인 필요

## 해결 방법

### 1단계: SHA-1 지문 확인

#### 방법 1: PowerShell (권장)
```powershell
cd C:\Users\subpa\StudioProjects\WiseYoung
.\gradlew signingReport
```

출력에서 다음 부분을 찾으세요:
```
Variant: debug
Config: debug
Store: C:\Users\subpa\.android\debug.keystore
Alias: AndroidDebugKey
SHA1: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX
```

#### 방법 2: 직접 keytool 사용
```powershell
keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

**SHA1:** 뒤의 값을 복사하세요 (콜론 제거 필요)

#### 방법 3: Android Studio
1. Android Studio에서 프로젝트 열기
2. Gradle 탭 (오른쪽) > `app` > `Tasks` > `android` > `signingReport` 더블클릭
3. Run 창에서 SHA-1 값 확인

### 2단계: Firebase Console에 SHA-1 추가

1. [Firebase Console](https://console.firebase.google.com/) 접속
2. 프로젝트 선택: **wiseyoung-5eeaa**
3. 왼쪽 메뉴에서 **⚙️ 프로젝트 설정** 클릭
4. 아래로 스크롤하여 **내 앱** 섹션 찾기
5. Android 앱 (`com.wiseyoung.app`) 찾기
6. **SHA 인증서 지문 추가** 클릭
7. 1단계에서 확인한 SHA-1 값 입력 (콜론 포함 또는 제거 모두 가능)
   - 예: `26:3B:25:03:FC:33:35:9A:0B:64:EA:0F:1C:A7:12:E8:03:CE:B1:ED`
   - 또는: `263b2503fc33359a0b64ea0f1ca712e803ceb1ed`
8. **저장** 클릭

### 3단계: google-services.json 다시 다운로드

⚠️ **중요**: SHA-1을 추가한 후 반드시 google-services.json을 다시 다운로드해야 합니다!

1. Firebase Console > 프로젝트 설정
2. Android 앱 (`com.wiseyoung.app`) 섹션에서
3. **google-services.json 다운로드** 클릭
4. 다운로드한 파일을 다음 위치에 덮어쓰기:
   ```
   app/google-services.json
   ```

### 4단계: Google Sign-in 활성화 확인

1. Firebase Console > **Authentication** 이동
2. **Sign-in method** 탭 클릭
3. **Google** 항목 확인:
   - ✅ **활성화**되어 있어야 합니다
   - 비활성화되어 있으면 클릭하여 활성화
   - 프로젝트 지원 이메일 설정 (필요시)

### 5단계: strings.xml 확인

`app/src/main/res/values/strings.xml` 파일 확인:
```xml
<string name="default_web_client_id">609597104515-hhhv4g5tckho264n2jv2mpnqtie0qf48.apps.googleusercontent.com</string>
```

이 값은 `google-services.json`의 `client_type: 3`인 `client_id`와 일치해야 합니다.

### 6단계: 앱 재빌드 및 테스트

1. Android Studio에서 **Build > Clean Project**
2. **Build > Rebuild Project**
3. 앱 실행
4. Google 로그인 테스트

---

## 문제 해결

### SHA-1을 찾을 수 없을 때

debug.keystore 파일이 없으면:
1. Android Studio에서 앱을 한 번 실행해보세요 (자동 생성됨)
2. 또는 다음 위치 확인:
   - `C:\Users\subpa\.android\debug.keystore`
   - `C:\Users\subpa\Android\.android\debug.keystore`

### Google 로그인이 여전히 안 될 때

1. **SHA-1 지문 재확인**
   - 본인 컴퓨터의 SHA-1이 Firebase Console에 등록되어 있는지 확인
   - google-services.json을 다시 다운로드했는지 확인

2. **Firebase Console 확인**
   - Authentication > Sign-in method에서 Google이 활성화되어 있는지 확인
   - 프로젝트 지원 이메일이 설정되어 있는지 확인

3. **캐시 정리**
   ```powershell
   cd C:\Users\subpa\StudioProjects\WiseYoung
   .\gradlew clean
   ```
   Android Studio > File > Invalidate Caches / Restart

4. **앱 재설치**
   - 디바이스에서 앱 완전 제거 후 재설치

---

## 현재 등록된 SHA-1

google-services.json에 현재 등록된 SHA-1:
- `263b2503fc33359a0b64ea0f1ca712e803ceb1ed` (학교 컴퓨터)
- `0f8f982b1eb4c103d38777ced81ad50f8f2c91eb` (학교 컴퓨터)

본인의 SHA-1을 추가해야 합니다!

