# ADB 포트 포워딩 설정 가이드

## 문제 상황
USB 테더링을 사용할 때 기기와 컴퓨터가 다른 서브넷에 있어서 연결이 안 되는 경우가 있습니다.
- 컴퓨터 IP: 172.16.2.178 (Wi-Fi)
- 기기 IP: 172.16.5.235 (USB 테더링)
- 오류: `EHOSTUNREACH (No route to host)`

## 해결 방법: ADB 포트 포워딩 사용 (권장)

ADB 포트 포워딩을 사용하면 네트워크 설정과 무관하게 안정적으로 연결할 수 있습니다.

### 1. USB로 기기 연결
- USB 케이블로 Android 기기를 컴퓨터에 연결
- 기기에서 "USB 디버깅" 활성화 (개발자 옵션)

### 2. ADB 포트 포워딩 설정
PowerShell 또는 명령 프롬프트에서 다음 명령어 실행:

```powershell
adb reverse tcp:8080 tcp:8080
```

### 3. 포트 포워딩 확인
```powershell
adb reverse --list
```

출력 예시:
```
8080 tcp:8080 tcp:8080
```

### 4. 앱 설정 확인
`Config.kt` 파일에서 다음 설정이 되어 있는지 확인:
```kotlin
private const val BASE_URL_DEV = "http://127.0.0.1:8080"
```

### 5. Spring Boot 서버 실행
IntelliJ IDEA에서 Spring Boot 서버를 실행합니다.

### 6. 앱 테스트
앱을 실행하고 프로필 저장을 시도합니다.

## 포트 포워딩 제거
포트 포워딩을 제거하려면:
```powershell
adb reverse --remove tcp:8080
```

## 문제 해결

### 포트 포워딩이 작동하지 않는 경우
1. **ADB 연결 확인:**
   ```powershell
   adb devices
   ```
   기기가 목록에 나타나야 합니다.

2. **ADB 재시작:**
   ```powershell
   adb kill-server
   adb start-server
   adb reverse tcp:8080 tcp:8080
   ```

3. **기기 재연결:**
   - USB 케이블을 뽑았다가 다시 연결
   - 기기에서 "USB 디버깅" 재인증

### USB 테더링을 계속 사용하려는 경우
1. 컴퓨터의 USB 테더링 네트워크 어댑터 IP 확인:
   ```powershell
   ipconfig /all
   ```
   "로컬 영역 연결" 또는 "이더넷" 어댑터에서 USB 테더링 관련 IP 찾기

2. 기기와 컴퓨터가 같은 서브넷에 있는지 확인:
   - 기기 IP: 172.16.5.235
   - 컴퓨터 USB 테더링 IP: 172.16.5.x (같은 서브넷)

3. `Config.kt`에서 컴퓨터의 USB 테더링 IP 사용:
   ```kotlin
   private const val BASE_URL_DEV = "http://172.16.5.XXX:8080"
   ```

4. Windows 방화벽에서 8080 포트 허용 확인

## 권장 사항
✅ **ADB 포트 포워딩 사용** (가장 안정적)
- 네트워크 설정과 무관
- USB만 연결하면 작동
- 방화벽 설정 불필요

❌ USB 테더링 직접 연결
- 네트워크 설정 복잡
- 서브넷 일치 필요
- 방화벽 설정 필요

