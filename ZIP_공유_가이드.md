# 안드로이드 프로젝트 ZIP 공유 가이드

## ⚠️ 문제 원인

zip 파일로 프로젝트를 공유할 때 오류가 발생하는 주요 원인:

1. **빌드 캐시 파일 포함** (`build/`, `.gradle/`)
   - 로컬 빌드 결과물이 포함되어 다른 환경에서 충돌 발생
   
2. **로컬 경로 하드코딩** (`local.properties`)
   - 각 개발자의 SDK 경로가 다름
   - Windows/Mac/Linux 경로 차이

3. **IDE 설정 파일** (`.idea/` 일부)
   - Android Studio 설정이 포함되어 충돌 발생

4. **의존성 캐시** (`.gradle/caches/`)
   - 로컬 Gradle 캐시가 포함되어 문제 발생

## ✅ 올바른 ZIP 공유 방법

### 방법 1: .gitignore 기준으로 압축 (권장)

1. **제외할 폴더/파일:**
   ```
   - build/
   - .gradle/
   - app/build/
   - local.properties
   - .idea/caches/
   - .idea/libraries/
   - .idea/modules.xml
   - .idea/workspace.xml
   - *.iml
   ```

2. **포함해야 할 필수 파일:**
   ```
   - gradle/wrapper/ (Gradle Wrapper)
   - gradlew, gradlew.bat
   - build.gradle.kts
   - settings.gradle.kts
   - gradle.properties
   - app/src/
   - app/build.gradle.kts
   - app/google-services.json
   ```

### 방법 2: Git 사용 (가장 권장)

```bash
# Git 저장소로 공유
git clone <repository-url>

# 또는 zip으로 export (GitHub에서 Download ZIP)
```

### 방법 3: 수동으로 압축하기

**Windows (PowerShell):**
```powershell
# build 폴더 제외하고 압축
Compress-Archive -Path WiseYoung\* -DestinationPath WiseYoung_clean.zip -Exclude @('build', '.gradle', 'local.properties', '*.iml')
```

**또는 WinRAR/7-Zip 사용:**
1. WiseYoung 폴더 선택
2. 압축 시 제외 목록에 추가:
   - `build/`
   - `.gradle/`
   - `local.properties`
   - `*.iml`
   - `.idea/caches/`
   - `.idea/libraries/`

## 📋 받는 사람이 해야 할 작업

1. **압축 해제 후:**
   ```bash
   # local.properties 생성 (자동 생성됨)
   # 또는 수동으로 생성:
   sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
   ```

2. **프로젝트 열기:**
   - Android Studio에서 프로젝트 열기
   - "Sync Project with Gradle Files" 클릭
   - Gradle이 자동으로 의존성 다운로드

3. **빌드:**
   ```bash
   ./gradlew clean build
   ```

## 🔧 문제 해결

### 오류 1: "SDK location not found"
**해결:** `local.properties` 파일 생성
```
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```

### 오류 2: "Gradle sync failed"
**해결:**
1. `File` → `Invalidate Caches / Restart`
2. `Build` → `Clean Project`
3. `Build` → `Rebuild Project`

### 오류 3: "Unresolved reference"
**해결:**
1. `File` → `Sync Project with Gradle Files`
2. 의존성이 제대로 다운로드되었는지 확인

## 💡 권장 사항

1. **Git 사용:** 가장 안전하고 효율적
2. **GitHub/GitLab 사용:** 버전 관리 + 공유
3. **빌드 파일 제외:** 항상 build/, .gradle/ 제외
4. **문서화:** README.md에 설정 방법 명시

## 📝 체크리스트

압축 전 확인:
- [ ] build/ 폴더 제외
- [ ] .gradle/ 폴더 제외  
- [ ] local.properties 제외
- [ ] .idea/caches/ 제외
- [ ] gradle/wrapper/ 포함 확인
- [ ] app/src/ 포함 확인
- [ ] build.gradle.kts 포함 확인

