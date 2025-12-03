# Firebase 프로젝트 설정 가이드

## 옵션 1: 기존 프로젝트 공유 사용 (권장)

현재 `google-services.json` 파일은 **학교 컴퓨터의 Firebase 프로젝트** 설정입니다.

### 같은 프로젝트를 계속 사용하려면:
- ✅ **아무것도 변경할 필요 없습니다**
- 같은 Firebase 프로젝트를 사용하면 데이터를 공유할 수 있습니다
- 학교 컴퓨터와 동일한 사용자 데이터를 사용하게 됩니다

---

## 옵션 2: 새로운 Firebase 프로젝트 생성 (독립 프로젝트)

자신만의 독립적인 Firebase 프로젝트를 원하는 경우 다음 단계를 따르세요.

### 1단계: Firebase Console에서 프로젝트 생성

1. [Firebase Console](https://console.firebase.google.com/) 접속
2. "프로젝트 추가" 클릭
3. 프로젝트 이름 입력 (예: `WiseYoung-Personal`)
4. Google Analytics 설정 (선택 사항)
5. 프로젝트 생성 완료

### 2단계: Android 앱 추가

1. Firebase Console에서 생성한 프로젝트 선택
2. 왼쪽 메뉴에서 "프로젝트 개요" 클릭
3. Android 아이콘 (📱) 클릭하여 Android 앱 추가
4. 다음 정보 입력:
   - **Android 패키지 이름**: `com.wiseyoung.app` (변경하지 마세요!)
   - **앱 닉네임**: `WiseYoung` (선택 사항)
   - **디버그 서명 인증서 SHA-1**: (3단계에서 얻은 값)

### 3단계: SHA-1 지문 가져오기

#### Windows (PowerShell):
```powershell
cd C:\Users\subpa\.android
keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
```

#### 또는 Gradle 명령어:
```powershell
cd C:\Users\subpa\StudioProjects\WiseYoung
.\gradlew signingReport
```

**SHA-1 값**을 복사하여 Firebase Console에 입력합니다.

### 4단계: google-services.json 다운로드

1. Firebase Console에서 `google-services.json` 파일 다운로드
2. 다운로드한 파일을 다음 경로에 저장:
   ```
   app/google-services.json
   ```
   (기존 파일을 덮어씁니다)

### 5단계: strings.xml 업데이트

`app/src/main/res/values/strings.xml` 파일을 열고:

1. 다운로드한 `google-services.json` 파일을 열기
2. 다음 부분 찾기:
   ```json
   "oauth_client": [
     {
       "client_id": "XXXXX-hhhv4g5tckho264n2jv2mpnqtie0qf48.apps.googleusercontent.com",
       "client_type": 3
     }
   ]
   ```
3. `client_type: 3`인 `client_id` 값을 복사
4. `strings.xml`의 `default_web_client_id` 값을 업데이트:
   ```xml
   <string name="default_web_client_id">여기에_새로운_client_id_붙여넣기</string>
   ```

### 6단계: Firebase Authentication 활성화

1. Firebase Console > Authentication 이동
2. "시작하기" 클릭
3. "Sign-in method" 탭에서 다음 활성화:
   - ✅ **이메일/비밀번호** (Email/Password)
   - ✅ **Google** (Google Sign-In)

### 7단계: Firebase Realtime Database 설정 (필요시)

현재 프로젝트에서 Realtime Database를 사용하는 경우:

1. Firebase Console > Realtime Database 이동
2. "데이터베이스 만들기" 클릭
3. 위치 선택 (예: `asia-northeast3 (Seoul)`)
4. 테스트 모드로 시작 (개발 중)

### 8단계: 빌드 및 테스트

1. Android Studio에서 **Build > Clean Project**
2. **Build > Rebuild Project**
3. 앱 실행하여 로그인 테스트

---

## 비교

| 항목 | 옵션 1: 공유 프로젝트 | 옵션 2: 새 프로젝트 |
|------|---------------------|-------------------|
| 설정 필요 | ❌ 없음 | ✅ 8단계 필요 |
| 데이터 공유 | ✅ 학교 컴퓨터와 공유 | ❌ 독립적 |
| 테스트 안전성 | ⚠️ 데이터 섞일 수 있음 | ✅ 안전 |
| 초기 작업 시간 | 0분 | 10-15분 |

---

## 권장 사항

- **개인 개발/테스트 목적**: 옵션 2 (새 프로젝트) 권장
- **협업/데이터 공유 목적**: 옵션 1 (공유 프로젝트) 권장

---

## 문제 해결

### SHA-1 지문을 찾을 수 없을 때
- `C:\Users\subpa\.android\debug.keystore` 파일이 있는지 확인
- 없으면 Android Studio가 자동으로 생성하므로 앱을 한 번 실행해보세요

### Google 로그인이 작동하지 않을 때
- Firebase Console > Authentication > Sign-in method에서 Google이 활성화되어 있는지 확인
- SHA-1 지문이 올바르게 등록되었는지 확인
- `strings.xml`의 `default_web_client_id`가 올바른지 확인

### 빌드 오류가 발생할 때
- `google-services.json` 파일이 `app/` 폴더에 있는지 확인
- Android Studio > File > Invalidate Caches / Restart 실행

