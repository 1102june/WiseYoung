# 수정 사항 요약

## ✅ 완료된 수정 사항

### 1. 서버 URL 설정 파일화
- **파일**: `app/src/main/java/com/example/app/Config.kt` 생성
- **변경 사항**:
  - 개발/프로덕션 환경별 URL 관리
  - API 엔드포인트 중앙 관리
  - `LoginActivity`와 `RegisterActivity`에서 `Config` 사용

### 2. 이메일 로그인 후 네비게이션 추가
- **파일**: `app/src/main/java/com/example/app/LoginActivity.kt`
- **변경 사항**:
  - `sendIdTokenToServer()`에서 비동기 처리로 변경 (`enqueue` 사용)
  - `navigateAfterLogin()` 함수 추가
  - 프로필 완료 여부에 따라 `CompleteActivity` 또는 `ProfileSetupActivity`로 이동
  - 로그인 성공 후 자동 네비게이션

### 3. Google 로그인 Firebase 연동
- **파일**: `app/src/main/java/com/example/app/LoginActivity.kt`
- **변경 사항**:
  - `GoogleSignInClient` 초기화
  - `signInWithGoogle()` 함수 구현
  - `firebaseAuthWithGoogle()` 함수 구현
  - `ActivityResultLauncher`로 Google 로그인 결과 처리
  - Google 로그인 성공 후 서버에 ID Token 전송 및 네비게이션
- **추가 파일**: `app/src/main/res/values/strings.xml`
  - `default_web_client_id` 추가 (Google Sign-In용)

### 4. SplashActivity에서 로그인 상태 확인
- **파일**: `app/src/main/java/com/example/app/SplashActivity.kt`
- **변경 사항**:
  - `FirebaseAuth.getInstance().currentUser`로 로그인 상태 확인
  - 로그인 상태와 프로필 완료 여부를 모두 확인하여 네비게이션
  - 로직:
    - 로그인됨 + 프로필 완료 → `CompleteActivity`
    - 로그인됨 + 프로필 미완료 → `ProfileSetupActivity`
    - 로그인 안됨 → `WelcomeActivity`

### 5. 챗봇 다이얼로그 연결
- **파일**: `app/src/main/java/com/example/app/MainActivity.kt`
- **상태**: 이미 `HomeScreen` 내부에서 챗봇 다이얼로그를 관리하고 있음
- **설명**: `HomeScreen`의 `BottomNavigationBar`에서 `onNavigateChatbot` 호출 시 `showChatbotDialog = true`로 설정되어 다이얼로그가 표시됨

## 📝 북마크 데이터 공유 문제 설명

### 문제 원인
현재 각 Activity에서 북마크 상태를 독립적으로 관리하고 있어서, 한 화면에서 북마크한 항목이 다른 화면에 반영되지 않습니다.

### 현재 구조
- `HomeScreen`: 로컬 상태로 북마크 관리
- `BookmarkActivity`: 로컬 상태로 북마크 관리
- `PolicyListActivity`: 로컬 상태로 북마크 관리
- 각 화면 간 데이터 공유 없음

### 해결 방법 (SpringBoot 연동 시)
1. **ViewModel + Repository 패턴 사용**
   ```kotlin
   // BookmarkViewModel.kt
   class BookmarkViewModel : ViewModel() {
       private val repository = BookmarkRepository()
       val bookmarks = repository.getBookmarks().stateIn(...)
       
       fun toggleBookmark(item: BookmarkItem) {
           viewModelScope.launch {
               repository.toggleBookmark(item)
           }
       }
   }
   ```

2. **SharedPreferences 또는 Room Database 사용** (로컬 캐시)
   - 서버 동기화 전 로컬에 저장
   - 앱 재시작 시에도 유지

3. **Singleton 객체 사용** (임시 해결책)
   ```kotlin
   object BookmarkManager {
       private val _bookmarks = mutableStateOf<List<BookmarkItem>>(emptyList())
       val bookmarks: State<List<BookmarkItem>> = _bookmarks
       
       fun toggleBookmark(item: BookmarkItem) {
           // 로직
       }
   }
   ```

## 🔧 Activity 스택 관리 개선

### 현재 문제점
- 일부 Activity에서 `finish()` 호출이 일관되지 않음
- 뒤로가기 버튼 동작이 예상과 다를 수 있음

### 권장 사항
1. **로그인/회원가입 플로우**: 
   - `WelcomeActivity` → `AuthActivity` → `LoginActivity`/`RegisterActivity`
   - 각 화면에서 다음 화면으로 이동 시 `finish()` 호출하여 스택 정리

2. **메인 플로우**:
   - `CompleteActivity` → `MainActivity` (finish 호출)
   - `MainActivity`에서 다른 화면으로 이동 시 `finish()` 호출하지 않음 (뒤로가기 가능)

3. **하위 화면**:
   - `NotificationActivity`, `BookmarkActivity` 등은 뒤로가기로 `MainActivity`로 돌아갈 수 있도록 `finish()` 호출하지 않음

### 수정 예시
```kotlin
// CompleteActivity에서 MainActivity로 이동 시
startActivity(Intent(this, MainActivity::class.java))
finish() // CompleteActivity는 더 이상 필요 없음

// MainActivity에서 다른 화면으로 이동 시
startActivity(Intent(this, BookmarkActivity::class.java))
// finish() 호출하지 않음 - 뒤로가기로 돌아올 수 있도록
```

## 🚀 다음 단계

1. **북마크 데이터 공유 구현**
   - ViewModel 또는 Singleton 패턴으로 전역 상태 관리
   - SpringBoot API 연동 시 Repository 패턴 적용

2. **에러 처리 개선**
   - 네트워크 오류 시 사용자 친화적 메시지
   - 로딩 상태 표시 (CircularProgressIndicator)

3. **로그아웃/회원탈퇴 구현**
   - `ProfileActivity`에서 Firebase 로그아웃
   - 프로필 상태 초기화

4. **데이터 영속성**
   - 북마크, 알림 설정을 SharedPreferences 또는 Room에 저장
   - 앱 재시작 시에도 유지

