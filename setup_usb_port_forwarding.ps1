# USB 연결 시 ADB 포트 포워딩 설정 스크립트
# 사용법: PowerShell에서 이 스크립트 실행

Write-Host "=== USB 연결 및 포트 포워딩 설정 ===" -ForegroundColor Cyan
Write-Host ""

# ADB 경로
$adbPath = "C:\Users\USER\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# 1. 기기 연결 확인
Write-Host "1. 기기 연결 확인 중..." -ForegroundColor Yellow
$devices = & $adbPath devices
Write-Host $devices
Write-Host ""

# 기기가 연결되어 있는지 확인
if ($devices -match "device\s*$") {
    Write-Host "✓ 기기가 연결되어 있습니다." -ForegroundColor Green
    Write-Host ""
    
    # 2. 기존 포트 포워딩 확인
    Write-Host "2. 기존 포트 포워딩 확인 중..." -ForegroundColor Yellow
    $existingPorts = & $adbPath reverse --list
    if ($existingPorts) {
        Write-Host $existingPorts
    } else {
        Write-Host "포트 포워딩이 설정되어 있지 않습니다." -ForegroundColor Yellow
    }
    Write-Host ""
    
    # 3. 포트 포워딩 설정
    Write-Host "3. 포트 포워딩 설정 중 (8080 -> 8080)..." -ForegroundColor Yellow
    $result = & $adbPath reverse tcp:8080 tcp:8080
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ 포트 포워딩이 설정되었습니다!" -ForegroundColor Green
    } else {
        Write-Host "✗ 포트 포워딩 설정 실패" -ForegroundColor Red
        exit 1
    }
    Write-Host ""
    
    # 4. 포트 포워딩 확인
    Write-Host "4. 포트 포워딩 확인 중..." -ForegroundColor Yellow
    $ports = & $adbPath reverse --list
    Write-Host $ports
    Write-Host ""
    
    if ($ports -match "tcp:8080 tcp:8080") {
        Write-Host "✓ 성공! 이제 앱을 재시작하세요." -ForegroundColor Green
        Write-Host ""
        Write-Host "참고:" -ForegroundColor Cyan
        Write-Host "- USB 연결이 끊기면 포트 포워딩도 해제됩니다" -ForegroundColor Gray
        Write-Host "- USB를 다시 연결할 때마다 이 스크립트를 다시 실행하세요" -ForegroundColor Gray
    } else {
        Write-Host "✗ 포트 포워딩 확인 실패" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "✗ 기기가 연결되어 있지 않습니다." -ForegroundColor Red
    Write-Host ""
    Write-Host "해결 방법:" -ForegroundColor Yellow
    Write-Host "1. USB 케이블로 기기를 컴퓨터에 연결하세요" -ForegroundColor Gray
    Write-Host "2. 기기에서 USB 디버깅을 허용하세요" -ForegroundColor Gray
    Write-Host "3. 이 스크립트를 다시 실행하세요" -ForegroundColor Gray
    exit 1
}


