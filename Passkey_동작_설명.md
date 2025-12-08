# Passkey 로그인 동작 설명

## 현재 상황

구글 패스키 로그인을 누르면 다음 옵션들이 나타납니다:
- 다른 기기의 패스키
- Google 비밀번호 관리자 열기
- Samsung Pass

## 이것은 정상적인 동작입니다! ✅

이것은 **Passkey의 표준 동작**입니다. Passkey는 여러 기기에서 동기화될 수 있고, 사용자가 여러 옵션 중에서 선택할 수 있도록 설계되었습니다.

### Passkey의 특징

1. **크로스 플랫폼 동기화**
   - Passkey는 Google 계정을 통해 여러 기기에서 동기화됩니다
   - 같은 Google 계정으로 로그인한 다른 기기의 Passkey도 사용할 수 있습니다
   - 이것이 "다른 기기의 패스키" 옵션이 나타나는 이유입니다

2. **비밀번호 관리자 통합**
   - Google 비밀번호 관리자, Samsung Pass 등과 통합되어 있습니다
   - 사용자가 원하는 방식으로 Passkey를 관리할 수 있습니다

3. **보안**
   - 각 Passkey는 기기별로 고유한 키 쌍을 가지고 있습니다
   - 다른 기기의 Passkey를 사용해도 안전합니다

## 현재 구현 상태

### 서버 측 (LoginController.java)
```java
.allowCredentials(new ArrayList<>()) // 빈 리스트 = 모든 Passkey 허용
```

- `allowCredentials`가 빈 배열이면 **모든 등록된 Passkey**를 허용합니다
- 이것이 여러 옵션이 나타나는 이유입니다

### 클라이언트 측 (PasskeyService.kt)
- Android Credential Manager API를 사용하여 Passkey를 요청합니다
- 시스템이 자동으로 사용 가능한 모든 Passkey 옵션을 표시합니다

## 개선 방안 (선택사항)

만약 현재 기기의 Passkey만 사용하고 싶다면, 다음과 같이 개선할 수 있습니다:

### 옵션 1: 현재 기기의 Passkey만 사용 (권장하지 않음)

이렇게 하면 크로스 플랫폼 동기화의 이점을 잃게 됩니다.

### 옵션 2: 사용자에게 명확한 안내 제공 (권장)

현재 동작을 유지하되, 사용자에게 명확한 안내를 제공합니다:

```kotlin
// Passkey 로그인 전 안내 다이얼로그 표시
AlertDialog(
    title = "Passkey 로그인",
    message = "다음 중 하나를 선택하여 로그인할 수 있습니다:\n" +
              "• 현재 기기의 Passkey\n" +
              "• 다른 기기의 Passkey (Google 계정 동기화)\n" +
              "• Google 비밀번호 관리자\n" +
              "• Samsung Pass",
    positiveButton = "확인"
)
```

### 옵션 3: 현재 동작 유지 (가장 권장)

현재 동작은 Passkey의 표준 동작이며, 사용자에게 최대한의 유연성을 제공합니다.

## 결론

**현재 구현은 정상적으로 작동하고 있습니다!** ✅

여러 옵션이 나타나는 것은:
- ✅ Passkey의 표준 동작
- ✅ 크로스 플랫폼 동기화 기능
- ✅ 사용자 편의성 향상

사용자가 원하는 옵션을 선택하면 정상적으로 로그인됩니다.

## 참고 자료

- [Android Passkey 가이드](https://developer.android.com/identity/passkeys)
- [FIDO2/WebAuthn 표준](https://www.w3.org/TR/webauthn-2/)

