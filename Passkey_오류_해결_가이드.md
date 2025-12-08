# Passkey "The incoming request cannot be validated" 오류 해결

## 🔴 오류 원인

"The incoming request cannot be validated" 오류는 다음과 같은 이유로 발생할 수 있습니다:

1. **rpId 형식 문제**: 패키지명을 사용하면 검증에 실패할 수 있습니다. 실제 도메인을 사용해야 합니다.
2. **JSON 형식 문제**: WebAuthn 표준에 맞지 않는 JSON 형식
3. **Challenge 인코딩 문제**: Base64 URL-safe 인코딩이 올바르지 않음

## ✅ 해결 방법

### 1. rpId를 도메인으로 변경

**문제:**
```kotlin
return context.packageName // "com.wiseyoung.app" - 검증 실패!
```

**해결:**
```kotlin
return "localhost" // 개발용
// 또는
return "wiseyoung.com" // 실제 배포 시
```

### 2. JSON 형식 확인

WebAuthn 표준에 맞는 JSON 형식:
```json
{
  "challenge": "base64url-encoded-challenge",
  "rpId": "localhost",
  "timeout": 60000,
  "userVerification": "preferred",
  "allowCredentials": []
}
```

### 3. 실제 도메인 사용 (프로덕션)

실제 배포 시에는 반드시 도메인을 사용해야 합니다:
```kotlin
private fun getRpId(context: Context): String {
    // 실제 배포 시
    return "wiseyoung.com" // 또는 실제 도메인
}
```

## 📝 다음 단계

1. **개발 환경**: `localhost` 사용 (현재 수정됨)
2. **테스트**: 다시 빌드하고 테스트
3. **프로덕션**: 실제 도메인으로 변경

## ⚠️ 중요 사항

- **rpId는 반드시 도메인이어야 합니다** (패키지명 X)
- **도메인은 실제로 존재해야 합니다**
- **HTTPS를 사용하는 경우 도메인과 일치해야 합니다**

