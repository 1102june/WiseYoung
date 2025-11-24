# 테스팅 체크리스트

## ✅ 테스팅 전 확인 사항

### 1. 컴파일 확인
- [x] 린터 오류 없음 확인됨
- [x] Config.kt 파일 생성 확인됨
- [x] Google Sign-In 설정 확인됨

### 2. 필수 설정 확인
- [x] `strings.xml`에 `default_web_client_id` 추가됨
- [x] `LoginActivity`에 Google Sign-In 코드 추가됨
- [x] `SplashActivity`에 로그인 상태 확인 추가됨

## 🧪 테스팅 시나리오

### 시나리오 1: 최초 실행 (로그인 안됨)
1. 앱 실행
2. **예상 결과**: SplashActivity → WelcomeActivity
3. "다음" 버튼 클릭
4. **예상 결과**: AuthActivity 표시

### 시나리오 2: 이메일 로그인 (프로필 미완료)
1. AuthActivity → LoginActivity
2. 이메일/비밀번호 입력 후 로그인
3. **예상 결과**: 
   - 서버에 ID Token 전송
   - 프로필 미완료 시 → ProfileSetupActivity
   - 프로필 완료 시 → CompleteActivity

### 시나리오 3: Google 로그인 (프로필 미완료)
1. LoginActivity에서 "Google Login 로그인" 버튼 클릭
2. Google 계정 선택
3. **예상 결과**:
   - Firebase 인증 성공
   - 서버에 ID Token 전송
   - 프로필 미완료 시 → ProfileSetupActivity
   - 프로필 완료 시 → CompleteActivity

### 시나리오 4: 프로필 완료 후
1. ProfileSetupActivity에서 프로필 입력 후 "시작하기" 클릭
2. **예상 결과**: CompleteActivity 표시
3. "시작하기" 버튼 클릭
4. **예상 결과**: MainActivity 표시

### 시나리오 5: 재실행 (로그인됨 + 프로필 완료)
1. 앱 종료 후 재실행
2. **예상 결과**: SplashActivity → CompleteActivity (또는 MainActivity)

### 시나리오 6: 챗봇 다이얼로그
1. MainActivity에서 하단 네비게이션의 챗봇 버튼 클릭
2. **예상 결과**: ChatbotDialog 표시

## ⚠️ 주의사항

### 서버 연결 오류 시
- `Config.kt`의 `BASE_URL_DEV`가 실제 서버 주소와 일치하는지 확인
- 서버가 실행 중인지 확인
- 네트워크 권한이 있는지 확인

### Google 로그인 오류 시
- `google-services.json` 파일이 올바른 위치에 있는지 확인
- `strings.xml`의 `default_web_client_id`가 올바른지 확인
- SHA-1 인증서가 Firebase Console에 등록되어 있는지 확인

### 프로필 완료 상태 확인
- `ProfilePreferences.hasCompletedProfile()`이 올바르게 동작하는지 확인
- SharedPreferences에 값이 저장되는지 확인

## 🔍 디버깅 팁

1. **로그 확인**
   - Android Studio의 Logcat에서 에러 메시지 확인
   - Toast 메시지로 사용자 피드백 확인

2. **네트워크 오류**
   - `Config.getUrl()`이 올바른 URL을 반환하는지 확인
   - OkHttp의 `onFailure` 콜백에서 에러 메시지 확인

3. **네비게이션 오류**
   - Intent가 올바른 Activity를 가리키는지 확인
   - `finish()` 호출로 인한 스택 문제 확인

## 📝 테스팅 결과 기록

### 테스트 날짜: ___________

#### 시나리오 1: 최초 실행
- [ ] 성공
- [ ] 실패 (오류 내용: ________________)

#### 시나리오 2: 이메일 로그인
- [ ] 성공
- [ ] 실패 (오류 내용: ________________)

#### 시나리오 3: Google 로그인
- [ ] 성공
- [ ] 실패 (오류 내용: ________________)

#### 시나리오 4: 프로필 완료 후
- [ ] 성공
- [ ] 실패 (오류 내용: ________________)

#### 시나리오 5: 재실행
- [ ] 성공
- [ ] 실패 (오류 내용: ________________)

#### 시나리오 6: 챗봇 다이얼로그
- [ ] 성공
- [ ] 실패 (오류 내용: ________________)

## 🐛 발견된 버그

1. ________________
2. ________________
3. ________________

