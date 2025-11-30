@echo off
REM USB 연결 시 ADB 포트 포워딩 설정 배치 파일
REM 사용법: 더블클릭 또는 명령 프롬프트에서 실행

echo === USB 연결 및 포트 포워딩 설정 ===
echo.

REM ADB 경로
set ADB_PATH=C:\Users\USER\AppData\Local\Android\Sdk\platform-tools\adb.exe

REM 1. 기기 연결 확인
echo 1. 기기 연결 확인 중...
%ADB_PATH% devices
echo.

REM 2. 포트 포워딩 설정
echo 2. 포트 포워딩 설정 중 (8080 -^> 8080)...
%ADB_PATH% reverse tcp:8080 tcp:8080
if %ERRORLEVEL% NEQ 0 (
    echo 포트 포워딩 설정 실패
    pause
    exit /b 1
)
echo.

REM 3. 포트 포워딩 확인
echo 3. 포트 포워딩 확인 중...
%ADB_PATH% reverse --list
echo.

echo 성공! 이제 앱을 재시작하세요.
echo.
echo 참고:
echo - USB 연결이 끊기면 포트 포워딩도 해제됩니다
echo - USB를 다시 연결할 때마다 이 스크립트를 다시 실행하세요
echo.
pause


