# Google Passkey 로그인 구현 가이드

## 📋 개요

Google Passkey는 FIDO2/WebAuthn 표준을 기반으로 한 비밀번호 없는 인증 방식입니다. Android에서는 **Credential Manager API**를 사용하여 구현합니다.

## 🔧 구현 단계

### 1단계: 의존성 추가

`app/build.gradle.kts`에 다음 의존성을 추가합니다:

```kotlin
// Google Credential Manager (Passkey 지원)
implementation("androidx.credentials:credentials:1.3.0")
implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
implementation("com.google.android.gms:play-services-auth:21.4.0")
```

### 2단계: PasskeyService 클래스 생성

Passkey 등록 및 로그인 로직을 처리하는 서비스 클래스를 생성합니다.

**주요 기능:**
- Passkey 등록 (회원가입 시)
- Passkey 로그인
- Passkey 삭제 (계정 삭제 시)

### 3단계: LoginActivity에 Passkey 로그인 구현

`onGoogleKeyLogin` 콜백에 Passkey 로그인 로직을 추가합니다.

### 4단계: 백엔드 연동

백엔드에서 Passkey 인증을 처리할 엔드포인트가 필요합니다:
- `/api/auth/passkey/register` - Passkey 등록
- `/api/auth/passkey/login` - Passkey 로그인

## 📱 Passkey 동작 방식

1. **등록 (Register)**
   - 사용자가 회원가입 시 Passkey를 생성
   - 공개키를 서버에 저장
   - 기기에 개인키 저장 (생체인증으로 보호)

2. **로그인 (Sign In)**
   - 사용자가 Passkey 로그인 버튼 클릭
   - 기기의 생체인증 (지문/얼굴) 요청
   - 인증 성공 시 서버에 서명 전송
   - 서버에서 검증 후 로그인 완료

## ⚠️ 주의사항

1. **최소 SDK 버전**: Android 9 (API 28) 이상 필요
2. **Google Play Services**: 최신 버전 필요
3. **백엔드 지원**: Passkey 검증을 위한 서버 엔드포인트 필요
4. **테스트**: 실제 기기에서만 테스트 가능 (에뮬레이터 제한적)

## 🔗 참고 자료

- [Android Credential Manager 공식 문서](https://developer.android.com/training/sign-in/passkeys)
- [Google Passkey 가이드](https://developers.google.com/identity/passkeys)

