# FCM 토큰 확인 가이드

## 🔍 FCM 등록 토큰 확인 방법

Firebase 콘솔에서 알림 테스트를 하려면 FCM 등록 토큰이 필요합니다.

### 방법 1: Android Studio Logcat에서 확인 (가장 쉬움) ⭐

1. **앱 실행**
   - 앱을 실행하고 로그인하세요
   - MainActivity가 시작되면 자동으로 FCM 토큰이 생성됩니다

2. **Logcat 열기**
   - Android Studio 하단의 `Logcat` 탭 클릭
   - 필터: `FcmTokenService` 입력

3. **토큰 확인**
   - 다음과 같은 로그를 찾으세요:
   ```
   I/FcmTokenService: ═══════════════════════════════════════════════════════
   I/FcmTokenService: FCM 등록 토큰 (Firebase 콘솔에 복사하세요):
   I/FcmTokenService: [여기에 긴 토큰 문자열이 표시됩니다]
   I/FcmTokenService: ═══════════════════════════════════════════════════════
   ```

4. **토큰 복사**
   - 토큰 문자열을 길게 눌러 선택
   - 복사 (Ctrl+C)
   - Firebase 콘솔에 붙여넣기

### 방법 2: 프로필 화면에서 확인 (추가 예정)

프로필 화면에 FCM 토큰을 표시하는 기능을 추가할 예정입니다.

## 📱 Firebase 콘솔에서 테스트하기

1. **Firebase Console 접속**
   - https://console.firebase.google.com
   - 프로젝트 선택

2. **Cloud Messaging 메뉴**
   - 왼쪽 메뉴에서 `Cloud Messaging` 클릭
   - `새 알림 작성` 또는 `테스트 메시지 전송` 클릭

3. **FCM 등록 토큰 입력**
   - "FCM 등록 토큰 추가" 섹션
   - Logcat에서 복사한 토큰 붙여넣기
   - 또는 프로필 화면에서 복사한 토큰 붙여넣기

4. **알림 테스트**
   - 알림 제목, 내용 입력
   - `테스트` 버튼 클릭
   - 기기에서 알림 수신 확인

## ⚠️ 주의사항

1. **토큰은 기기별로 다릅니다**
   - 각 기기마다 고유한 토큰이 생성됩니다
   - 앱을 재설치하면 새로운 토큰이 생성됩니다

2. **토큰 갱신**
   - 토큰은 자동으로 갱신될 수 있습니다
   - 최신 토큰을 사용해야 합니다

3. **로그인 필요**
   - FCM 토큰은 로그인한 사용자에게만 생성됩니다
   - 로그인 후 MainActivity가 시작되면 자동으로 토큰이 생성됩니다

## 🔧 문제 해결

### 토큰이 표시되지 않는 경우

1. **로그인 확인**
   - 앱에 로그인되어 있는지 확인
   - 로그아웃 후 다시 로그인

2. **앱 재시작**
   - 앱을 완전히 종료 후 다시 실행
   - MainActivity가 시작되면 토큰이 자동 생성됩니다

3. **Logcat 필터 확인**
   - `FcmTokenService` 태그로 필터링
   - 또는 `FCM` 키워드로 검색

4. **로그 레벨 확인**
   - Logcat에서 `Info` 레벨 이상 표시 확인
   - `Verbose`, `Debug`, `Info`, `Warn`, `Error` 모두 확인

## 📝 요약

**가장 쉬운 방법:**
1. 앱 실행 및 로그인
2. Android Studio Logcat 열기
3. `FcmTokenService` 필터 적용
4. 토큰 복사
5. Firebase 콘솔에 붙여넣기

