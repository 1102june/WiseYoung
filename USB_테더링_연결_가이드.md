# USB 테더링 환경에서 서버 연결 가이드

## 문제
USB 테더링을 사용할 때 Android 앱에서 Spring Boot 서버 연결 실패

## 해결 방법

### 1. Config.kt 설정 확인 ✅
- 현재 설정: `http://172.16.2.178:8080` (컴퓨터 IP 주소)
- USB 테더링 사용 시 컴퓨터 IP 주소를 직접 사용하는 것이 가장 안정적입니다.

### 2. 컴퓨터 IP 주소 확인
```powershell
ipconfig | findstr IPv4
```
- 결과 예: `172.16.2.178`
- IP가 변경되면 Config.kt의 `BASE_URL_DEV`도 업데이트 필요

### 3. Spring Boot 서버 설정 확인
`application.yml`에서 서버가 모든 인터페이스에서 수신하도록 설정:
```yaml
server:
  port: 8080
  # address: 0.0.0.0  # 기본값이므로 명시하지 않아도 됨
```

### 4. Windows 방화벽 설정
USB 테더링 사용 시 Windows 방화벽에서 8080 포트를 허용해야 할 수 있습니다:

**방법 1: PowerShell에서 방화벽 규칙 추가**
```powershell
# 인바운드 규칙 추가 (관리자 권한 필요)
New-NetFirewallRule -DisplayName "Spring Boot 8080" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow
```

**방법 2: Windows 방화벽 GUI에서 설정**
1. Windows 설정 > 네트워크 및 인터넷 > Windows 방화벽
2. 고급 설정
3. 인바운드 규칙 > 새 규칙
4. 포트 선택 > TCP > 특정 로컬 포트: 8080
5. 연결 허용
6. 모든 프로필 적용
7. 이름: "Spring Boot 8080"

### 5. Spring Boot 서버 실행 확인
서버가 실행 중인지 확인:
```powershell
# 브라우저에서 접속 테스트
# http://172.16.2.178:8080
```

### 6. 앱 재빌드 및 재시작
1. Android Studio에서 앱 재빌드 (Build > Rebuild Project)
2. 앱 재설치 및 재시작

## 문제 해결 체크리스트

- [x] Config.kt에 컴퓨터 IP 주소 설정 (`172.16.2.178:8080`)
- [ ] Spring Boot 서버 실행 중 확인
- [ ] Windows 방화벽에서 8080 포트 허용
- [ ] 앱 재빌드 및 재시작
- [ ] 브라우저에서 `http://172.16.2.178:8080` 접속 테스트

## 자주 발생하는 문제

### 1. "Unable to resolve host" 오류
- **원인**: IP 주소가 잘못되었거나 네트워크 연결 문제
- **해결**: 
  - 컴퓨터 IP 주소 재확인: `ipconfig | findstr IPv4`
  - Config.kt의 IP 주소 업데이트
  - USB 테더링 연결 확인

### 2. "Connection refused" 오류
- **원인**: Spring Boot 서버가 실행되지 않았거나 방화벽 차단
- **해결**: 
  - Spring Boot 서버 실행 확인
  - Windows 방화벽에서 8080 포트 허용
  - 서버 로그 확인

### 3. "Timeout" 오류
- **원인**: 방화벽 또는 네트워크 문제
- **해결**: 
  - Windows 방화벽 설정 확인
  - USB 테더링 연결 안정성 확인
  - 컴퓨터와 기기가 같은 네트워크에 있는지 확인 (USB 테더링 사용 시 자동)

## USB 테더링 vs 일반 USB 연결

### USB 테더링 사용 시 (현재 설정)
- ✅ 컴퓨터 IP 주소 직접 사용 (`172.16.2.178:8080`)
- ✅ ADB 포트 포워딩 불필요
- ⚠️ Windows 방화벽 설정 필요할 수 있음
- ⚠️ 컴퓨터 IP가 변경되면 Config.kt 업데이트 필요

### 일반 USB 연결 시 (ADB 포트 포워딩)
- ✅ `127.0.0.1:8080` 사용
- ✅ ADB 포트 포워딩 필요: `adb reverse tcp:8080 tcp:8080`
- ✅ 방화벽 설정 불필요
- ⚠️ USB 테더링 사용 시 불안정할 수 있음

## 현재 설정 요약

- **Config.kt**: `http://172.16.2.178:8080`
- **연결 방식**: USB 테더링 (컴퓨터 IP 직접 사용)
- **필요 작업**: Windows 방화벽 설정 확인

