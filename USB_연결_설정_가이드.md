# USB 연결 시 서버 연결 설정 가이드

## 문제
USB로 연결된 Android 기기에서 Spring Boot 서버(8080 포트) 연결 실패

## 해결 방법

### 방법 1: ADB 포트 포워딩 (권장) ✅ 실제 기기 + USB 연결 시 사용

1. **Android Studio의 Terminal 탭 열기**
   - Android Studio 하단의 Terminal 탭 클릭

2. **ADB 포트 포워딩 실행**
   ```powershell
   # ADB 경로를 직접 지정 (실제 기기 + USB)
   C:\Users\USER\AppData\Local\Android\Sdk\platform-tools\adb.exe reverse tcp:8080 tcp:8080
   
   # 또는 (ADB가 PATH에 설정된 경우)
   adb reverse tcp:8080 tcp:8080
   ```

3. **포트 포워딩 확인**
   ```powershell
   C:\Users\USER\AppData\Local\Android\Sdk\platform-tools\adb.exe reverse --list
   ```
   - 결과에 `UsbFfs tcp:8080 tcp:8080` 또는 `tcp:8080 tcp:8080`이 표시되면 성공 ✅

4. **기기 연결 확인**
   ```powershell
   C:\Users\USER\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
   ```
   - 기기가 `device` 상태로 표시되어야 함

5. **앱 재시작**
   - Android Studio에서 앱 재실행 또는 기기에서 앱 재시작

**주의사항:**
- USB 연결이 끊기면 포트 포워딩도 해제됩니다
- USB를 다시 연결할 때마다 위 명령어를 다시 실행해야 합니다
- 여러 기기를 연결한 경우: `adb -s [기기ID] reverse tcp:8080 tcp:8080`

### 방법 2: 컴퓨터 IP 주소 사용 (대안)

ADB 포트 포워딩이 작동하지 않는 경우:

1. **컴퓨터의 IP 주소 확인**
   ```powershell
   # Windows PowerShell
   ipconfig
   
   # 또는
   ipconfig | findstr IPv4
   ```
   - 결과 예: `172.29.121.3` 또는 `192.168.x.x`

2. **Config.kt 수정**
   ```kotlin
   // Config.kt에서
   private const val BASE_URL_DEV = "http://[컴퓨터IP]:8080"  
   // 예: "http://172.29.121.3:8080"
   ```

3. **네트워크 보안 설정 확인**
   - `network_security_config.xml`에 해당 IP가 허용되어 있는지 확인
   - 또는 `cleartextTrafficPermitted="true"`로 설정되어 있으면 모든 IP 허용

4. **방화벽 확인**
   - Windows 방화벽에서 8080 포트 허용 필요
   - Spring Boot 서버가 모든 인터페이스에서 수신하도록 설정 확인

### 방법 3: Android Studio Terminal 위치 확인

ADB 명령어가 인식되지 않는 경우:

1. **Android Studio SDK 경로 확인**
   - File → Settings → Appearance & Behavior → System Settings → Android SDK
   - SDK Location 확인 (예: `C:\Users\USER\AppData\Local\Android\Sdk`)

2. **전체 경로로 실행**
   ```powershell
   C:\Users\USER\AppData\Local\Android\Sdk\platform-tools\adb.exe reverse tcp:8080 tcp:8080
   ```

3. **또는 환경 변수 PATH에 추가**
   - SDK의 `platform-tools` 폴더를 시스템 PATH에 추가

## 현재 설정 확인

### Config.kt
- 현재 설정: `http://127.0.0.1:8080` (USB 연결 시 ADB 포트 포워딩 필요)

### network_security_config.xml
- `localhost` 허용됨
- `10.0.2.2` 허용됨 (에뮬레이터용)
- 모든 cleartext traffic 허용됨

## 문제 해결 체크리스트

- [ ] USB 연결이 되어 있는지 확인
- [ ] Android 기기가 인식되는지 확인: `adb devices`
- [ ] ADB 포트 포워딩 실행: `adb reverse tcp:8080 tcp:8080`
- [ ] Spring Boot 서버가 실행 중인지 확인 (http://localhost:8080)
- [ ] 앱을 재시작했는지 확인
- [ ] 로그에서 정확한 에러 메시지 확인 (Logcat)

## 자주 발생하는 문제

### 1. "Unable to resolve host" 오류
- **원인**: ADB 포트 포워딩이 설정되지 않음
- **해결**: `adb reverse tcp:8080 tcp:8080` 실행

### 2. "Connection refused" 오류
- **원인**: Spring Boot 서버가 실행되지 않았거나 다른 포트 사용
- **해결**: 서버 실행 확인 및 포트 확인

### 3. "Timeout" 오류
- **원인**: 방화벽 또는 네트워크 문제
- **해결**: 방화벽 설정 확인, 컴퓨터 IP 주소 사용

## 참고

- **에뮬레이터 사용 시**: `10.0.2.2:8080` 사용 (자동으로 호스트 컴퓨터를 가리킴)
- **실제 기기 + USB**: ADB 포트 포워딩 또는 컴퓨터 IP 주소 사용
- **실제 기기 + Wi-Fi**: 컴퓨터 IP 주소 사용 (같은 Wi-Fi 네트워크 필수)

