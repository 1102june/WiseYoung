# WiseYoung 앱 흐름 분석 및 문제점 정리

## 📱 전체 앱 흐름도

```
1. SplashActivity (2초)
   ↓
   [프로필 완료 여부 확인]
   ├─ 완료됨 → CompleteActivity
   └─ 미완료 → WelcomeActivity
   
2. WelcomeActivity
   ↓ (다음 버튼)
   AuthActivity
   
3. AuthActivity
   ├─ 로그인 → LoginActivity
   └─ 회원가입 → RegisterActivity
   
4. LoginActivity
   ├─ 이메일/비밀번호 로그인
   │   └─ 성공 → [프로필 확인]
   │       ├─ 완료됨 → MainActivity
   │       └─ 미완료 → ProfileSetupActivity
   │
   └─ Google 로그인
       └─ [프로필 확인]
           ├─ 완료됨 → MainActivity
           └─ 미완료 → ProfileSetupActivity
   
5. RegisterActivity
   └─ 회원가입 성공 → ProfileSetupActivity
   
6. ProfileSetupActivity
   └─ 프로필 저장 성공 → CompleteActivity
   
7. CompleteActivity
   └─ 시작하기 버튼 → MainActivity
   
8. MainActivity (홈 화면)
   ├─ 알림 버튼 → NotificationActivity
   ├─ 맞춤 청년정책 → PolicyListActivity
   ├─ 맞춤 임대주택 → HousingMapActivity
   ├─ 하단 네비게이션
   │   ├─ 홈 → MainActivity (현재)
   │   ├─ 캘린더 → CalendarActivity
   │   ├─ 챗봇 → ChatbotDialog (다이얼로그)
   │   ├─ 북마크 → BookmarkActivity
   │   └─ 내정보 → ProfileActivity
   └─ 정책 카드 좋아요 → 알림 설정 다이얼로그
```

## ⚠️ 발견된 문제점들

### 🔴 **심각한 문제**

1. **Google 로그인 미구현**
   - `LoginActivity.handleGoogleLogin()`이 실제 Google 로그인 로직 없이 프로필 확인만 수행
   - Firebase Google Sign-In 연동 필요
   - 위치: `LoginActivity.kt:119-126`

2. **이메일 로그인 후 네비게이션 누락**
   - `LoginActivity.loginUser()`에서 로그인 성공 후 MainActivity로 이동하지 않음
   - 서버 응답만 처리하고 화면 전환 없음
   - 위치: `LoginActivity.kt:65-118`

3. **챗봇 다이얼로그 미연결**
   - `MainActivity`에서 `onNavigateChatbot = {}`로 빈 함수
   - `HomeScreen`에서 챗봇 버튼 클릭 시 다이얼로그가 표시되지 않음
   - 위치: `MainActivity.kt:37`

4. **데이터 영속성 부족**
   - 북마크, 알림 설정 등이 메모리에만 저장됨 (앱 재시작 시 초기화)
   - Room Database 또는 SharedPreferences로 저장 필요

5. **서버 URL 하드코딩**
   - `LoginActivity`: `"http://your_server_url/auth/login"` (플레이스홀더)
   - `RegisterActivity`: `"http://172.16.1.42:8080/auth/signup"` (로컬 IP)
   - 환경별 설정 파일로 분리 필요

### 🟡 **중요한 문제**

6. **프로필 완료 상태 불일치**
   - `SplashActivity`에서 프로필 완료 여부로 CompleteActivity/WelcomeActivity 분기
   - 하지만 실제 로그인 상태는 확인하지 않음
   - 로그인 상태와 프로필 완료 상태를 함께 확인해야 함

7. **Activity 스택 관리**
   - 여러 화면에서 `finish()` 호출이 일관되지 않음
   - 뒤로가기 버튼 동작이 예상과 다를 수 있음

8. **에러 처리 부족**
   - 네트워크 오류, 서버 오류에 대한 사용자 친화적 메시지 부족
   - 로딩 상태 표시 없음

9. **북마크 데이터 공유 불가**
   - 각 Activity에서 독립적인 북마크 상태 관리
   - HomeScreen에서 북마크한 정책이 BookmarkActivity에 반영되지 않음

10. **캘린더 기능 미완성**
    - 실제 날짜 선택 기능 없음 ("날짜 선택 기능은 추후 구현 예정" 메시지만)
    - 날짜별 정책/주택 표시 기능 없음

### 🟢 **개선 권장 사항**

11. **코드 중복**
    - NotificationDialog가 여러 파일에 중복 정의됨
    - 공통 컴포넌트로 분리 필요

12. **하드코딩된 데이터**
    - 정책 리스트, 아파트 리스트가 코드에 하드코딩됨
    - 서버 API 연동 필요

13. **패키지명 불일치**
    - 일부 파일: `com.wiseyoung.app`
    - 일부 파일: `com.example.app`
    - 통일 필요

14. **로그아웃 기능 미구현**
    - `ProfileActivity`에서 로그아웃 버튼 클릭 시 아무 동작 없음
    - Firebase 로그아웃 및 프로필 상태 초기화 필요

15. **회원탈퇴 기능 미구현**
    - 다이얼로그만 있고 실제 탈퇴 로직 없음

## 🔧 수정 우선순위

### 즉시 수정 필요
1. ✅ Google 로그인 구현
2. ✅ 이메일 로그인 후 MainActivity 이동
3. ✅ 챗봇 다이얼로그 연결
4. ✅ 프로필 완료 상태와 로그인 상태 동기화

### 단기 개선
5. ✅ 데이터 영속성 (Room/SharedPreferences)
6. ✅ 서버 URL 설정 파일화
7. ✅ Activity 스택 관리 개선
8. ✅ 북마크 데이터 공유 (ViewModel/Repository)

### 중기 개선
9. ✅ 에러 처리 및 로딩 상태
10. ✅ 캘린더 기능 완성
11. ✅ 코드 중복 제거
12. ✅ 로그아웃/회원탈퇴 구현

## 📋 체크리스트

- [ ] Google 로그인 Firebase 연동
- [ ] 이메일 로그인 성공 후 네비게이션 추가
- [ ] 챗봇 다이얼로그 MainActivity 연결
- [ ] 데이터 영속성 구현 (북마크, 알림 설정)
- [ ] 서버 URL 설정 파일화
- [ ] 로그인 상태 확인 로직 추가
- [ ] 로그아웃 기능 구현
- [ ] 회원탈퇴 기능 구현
- [ ] 에러 처리 개선
- [ ] 로딩 상태 표시 추가

