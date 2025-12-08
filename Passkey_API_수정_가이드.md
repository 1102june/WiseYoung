# Passkey API 수정 가이드

## 문제점

현재 코드에서 사용한 클래스들이 실제 Credential Manager API에 존재하지 않습니다:
- `PublicKeyCredentialRequestOptions` - 존재하지 않음
- `PublicKeyCredentialCreationOptions` - 존재하지 않음
- `PublicKeyCredentialUserEntity` - 존재하지 않음
- `PublicKeyCredentialRpEntity` - 존재하지 않음

## 해결 방법

Credential Manager API는 **JSON 문자열을 직접 생성**하여 사용해야 합니다.
`GetPublicKeyCredentialOption`과 `CreatePublicKeyCredentialOption`은 `requestJson: String` 파라미터만 받습니다.

## 올바른 구현 방법

### 1. Passkey 로그인 (Get)

```kotlin
// JSON 문자열 직접 생성
val requestJson = JSONObject().apply {
    put("challenge", Base64.encodeToString(challenge, Base64.URL_SAFE or Base64.NO_PADDING))
    put("rpId", "your-domain.com")
    put("timeout", 60000)
    put("userVerification", "required")
    put("allowCredentials", JSONArray()) // 빈 배열 = 모든 Passkey 허용
}.toString()

val option = GetPublicKeyCredentialOption(requestJson = requestJson)
val request = GetCredentialRequest(listOf(option))
val result = credentialManager.getCredential(request = request, context = context)
```

### 2. Passkey 등록 (Create)

```kotlin
// JSON 문자열 직접 생성
val requestJson = JSONObject().apply {
    put("rp", JSONObject().apply {
        put("id", "your-domain.com")
        put("name", "Your App Name")
    })
    put("user", JSONObject().apply {
        put("id", Base64.encodeToString(userIdBytes, Base64.URL_SAFE or Base64.NO_PADDING))
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
}.toString()

val option = CreatePublicKeyCredentialOption(requestJson = requestJson)
val request = CreateCredentialRequest(credential = option)
val result = credentialManager.createCredential(request = request, context = context)
```

## 참고 자료

- [Android Credential Manager 공식 문서](https://developer.android.com/training/sign-in/passkeys)
- [WebAuthn 표준](https://www.w3.org/TR/webauthn-2/)

