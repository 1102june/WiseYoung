# Spring Boot 서버 연결 테스트 스크립트

Write-Host "=== Spring Boot 서버 연결 테스트 ===" -ForegroundColor Cyan
Write-Host ""

# 컴퓨터 IP 주소 확인
Write-Host "1. 컴퓨터 IP 주소 확인 중..." -ForegroundColor Yellow
$ipAddress = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object {$_.IPAddress -like "172.16.*" -or $_.IPAddress -like "192.168.*"} | Select-Object -First 1).IPAddress
if ($ipAddress) {
    Write-Host "   확인된 IP: $ipAddress" -ForegroundColor Green
} else {
    Write-Host "   IP 주소를 찾을 수 없습니다." -ForegroundColor Red
    $ipAddress = "172.16.2.178"  # 기본값
    Write-Host "   기본 IP 사용: $ipAddress" -ForegroundColor Yellow
}
Write-Host ""

# 서버 포트 확인
Write-Host "2. 서버 포트 8080 확인 중..." -ForegroundColor Yellow
$portCheck = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($portCheck) {
    Write-Host "   포트 8080이 열려 있습니다." -ForegroundColor Green
    Write-Host "   상태: $($portCheck.State)" -ForegroundColor Green
} else {
    Write-Host "   포트 8080이 열려 있지 않습니다." -ForegroundColor Red
    Write-Host "   Spring Boot 서버가 실행 중인지 확인하세요." -ForegroundColor Yellow
}
Write-Host ""

# HTTP 연결 테스트
Write-Host "3. HTTP 연결 테스트 중..." -ForegroundColor Yellow
$testUrl = "http://${ipAddress}:8080"
Write-Host "   테스트 URL: $testUrl" -ForegroundColor Cyan

try {
    $response = Invoke-WebRequest -Uri $testUrl -Method GET -TimeoutSec 5 -ErrorAction Stop
    Write-Host "   ✅ 연결 성공! (상태 코드: $($response.StatusCode))" -ForegroundColor Green
} catch {
    $errorMessage = $_.Exception.Message
    Write-Host "   ❌ 연결 실패: $errorMessage" -ForegroundColor Red
    
    if ($errorMessage -like "*refused*") {
        Write-Host ""
        Write-Host "   해결 방법:" -ForegroundColor Yellow
        Write-Host "   1. Spring Boot 서버가 실행 중인지 확인" -ForegroundColor White
        Write-Host "   2. application.yml에서 server.port=8080 확인" -ForegroundColor White
    } elseif ($errorMessage -like "*timeout*") {
        Write-Host ""
        Write-Host "   해결 방법:" -ForegroundColor Yellow
        Write-Host "   1. Windows 방화벽에서 8080 포트 허용 확인" -ForegroundColor White
        Write-Host "   2. 네트워크 연결 상태 확인" -ForegroundColor White
    } elseif ($errorMessage -like "*resolve*") {
        Write-Host ""
        Write-Host "   해결 방법:" -ForegroundColor Yellow
        Write-Host "   1. IP 주소가 올바른지 확인: $ipAddress" -ForegroundColor White
        Write-Host "   2. USB 테더링 연결 확인" -ForegroundColor White
    }
}
Write-Host ""

# 방화벽 규칙 확인
Write-Host "4. 방화벽 규칙 확인 중..." -ForegroundColor Yellow
$firewallRule = Get-NetFirewallRule -DisplayName "Spring Boot 8080" -ErrorAction SilentlyContinue
if ($firewallRule) {
    Write-Host "   ✅ 방화벽 규칙이 설정되어 있습니다." -ForegroundColor Green
} else {
    Write-Host "   ⚠️  방화벽 규칙이 없습니다." -ForegroundColor Yellow
    Write-Host "   다음 명령어로 방화벽 규칙을 추가하세요 (관리자 권한 필요):" -ForegroundColor White
    Write-Host "   New-NetFirewallRule -DisplayName 'Spring Boot 8080' -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow" -ForegroundColor Cyan
}
Write-Host ""

# Config.kt 설정 확인
Write-Host "5. Config.kt 설정 확인..." -ForegroundColor Yellow
$configPath = "app\src\main\java\com\example\app\Config.kt"
if (Test-Path $configPath) {
    $configContent = Get-Content $configPath -Raw
    if ($configContent -match "BASE_URL_DEV\s*=\s*`"http://([^`"]+)`"") {
        $configIp = $matches[1]
        Write-Host "   Config.kt의 IP: $configIp" -ForegroundColor Cyan
        if ($configIp -eq $ipAddress) {
            Write-Host "   ✅ IP 주소가 일치합니다." -ForegroundColor Green
        } else {
            Write-Host "   ⚠️  IP 주소가 일치하지 않습니다!" -ForegroundColor Red
            Write-Host "   Config.kt를 업데이트하세요: $ipAddress" -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "   Config.kt 파일을 찾을 수 없습니다." -ForegroundColor Red
}
Write-Host ""

Write-Host "=== 테스트 완료 ===" -ForegroundColor Cyan

