# Passkey API 사용법 상세 가이드

## ✅ 수정 완료

코드를 Credential Manager API의 실제 구조에 맞게 수정했습니다.

## 🔑 핵심 포인트

### 1. JSON 문자열 직접 생성

Credential Manager API는 **클래스 기반이 아닌 JSON 문자열 기반**입니다.

**잘못된 방법 (존재하지 않는 클래스):**
```kotlin
// ❌ 이런 클래스는 존재하지 않습니다
val request = PublicKeyCredentialRequestOptions(...)
```

**올바른 방법 (JSON 문자열 직접 생성):**
```kotlin
// ✅ JSON 문자열을 직접 생성
val requestJson = JSONObject().apply {
    put("challenge", Base64.encodeToString(challenge, Base64.URL_SAFE or Base64.NO_PADDING))
    put("rpId", "your-domain.com")
    // ...
}.toString()

val option = GetPublicKeyCredentialOption(requestJson = requestJson)
```

### 2. API 사용 흐름

#### Passkey 로그인 (Get)

```kotlin
// 1. CredentialManager 생성
val credentialManager = CredentialManager.create(context)

// 2. JSON 요청 생성
val requestJson = JSONObject().apply {
    put("challenge", Base64.encodeToString(challenge, Base64.URL_SAFE or Base64.NO_PADDING))
    put("rpId", rpId)
    put("timeout", 60000)
    put("userVerification", "required")
    put("allowCredentials", JSONArray()) // 빈 배열 = 모든 Passkey 허용
}.toString()

// 3. GetPublicKeyCredentialOption 생성
val option = GetPublicKeyCredentialOption(requestJson = requestJson)

// 4. GetCredentialRequest 생성
val request = GetCredentialRequest(listOf(option))

// 5. Credential 요청
val result = credentialManager.getCredential(request = request, context = context)

// 6. 결과 확인
val credential = result.credential
if (credential is PublicKeyCredential) {
    // 성공!
}
```

#### Passkey 등록 (Create)

```kotlin
// 1. CredentialManager 생성
val credentialManager = CredentialManager.create(context)

// 2. JSON 요청 생성
val requestJson = JSONObject().apply {
    put("rp", JSONObject().apply {
        put("id", rpId)
        put("name", "앱 이름")
    })
    put("user", JSONObject().apply {
        put("id", Base64.encodeToString(userId, Base64.URL_SAFE or Base64.NO_PADDING))
        put("name", email)
        put("displayName", displayName)
    })
    put("challenge", Base64.encodeToString(challenge, Base64.URL_SAFE or Base64.NO_PADDING))
    put("pubKeyCredParams", JSONArray().apply {
        put(JSONObject().apply {
            put("type", "public-key")
            put("alg", -7) // ES256
        })
    })
    put("timeout", 60000)
    put("attestation", "none")
    put("authenticatorSelection", JSONObject().apply {
        put("userVerification", "required")
        put("requireResidentKey", false)
    })
}.toString()

// 3. CreatePublicKeyCredentialOption 생성
val option = CreatePublicKeyCredentialOption(requestJson = requestJson)

// 4. CreateCredentialRequest 생성
val request = CreateCredentialRequest(credential = option)

// 5. Credential 생성
val result = credentialManager.createCredential(request = request, context = context)

// 6. 결과 확인
val credential = result.credential
if (credential is PublicKeyCredential) {
    // 성공!
}
```

### 3. Base64 인코딩

**중요**: `challenge`와 `id`는 **Base64 URL-safe 인코딩**이 필요합니다.

```kotlin
Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
```

### 4. WebAuthn 표준 준수

JSON 형식은 **WebAuthn 표준**을 따라야 합니다:
- `challenge`: Base64 URL-safe 인코딩된 랜덤 바이트
- `rpId`: Relying Party ID (도메인 또는 패키지명)
- `timeout`: 밀리초 단위
- `userVerification`: "required", "preferred", "discouraged"
- `pubKeyCredParams`: 지원하는 알고리즘 목록
- `attestation`: "none", "indirect", "direct"

## 📚 참고 자료

- [Android Credential Manager 공식 문서](https://developer.android.com/training/sign-in/passkeys)
- [WebAuthn 표준](https://www.w3.org/TR/webauthn-2/)
- [Credential Manager API Reference](https://developer.android.com/reference/kotlin/androidx/credentials/package-summary)

## ⚠️ 주의사항

1. **실제 기기에서 테스트**: 에뮬레이터는 제한적
2. **Google Play Services**: 최신 버전 필요
3. **생체인증 설정**: 기기에 생체인증이 설정되어 있어야 함
4. **백엔드 지원**: Passkey 검증을 위한 서버 엔드포인트 필요

