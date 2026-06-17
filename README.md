# 📱 WiseYoung (슬기로운 청년생활)
> **"흩어진 청년 정책과 주거 정보를 한 눈에, 맞춤형 정책 큐레이션 플랫폼"**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android)](#)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin)](#)
[![Spring Boot](https://img.shields.io/badge/Backend-Spring%20Boot-6DB33F?logo=springboot)](#)

### Backend: [https://github.com/1102june/1208spring]

## 📌 Project Overview
**WiseYoung**은 부처별로 파편화된 청년 정책 정보와 LH 공공임대주택 공고를 통합하여, 청년들의 경제적 자립과 주거 안정을 돕는 모바일 서비스입니다. 가독성 낮은 공고문을 데이터화하여 **사용자 맞춤형 추천**과 **D-Day 관리**를 제공함으로써 청년들의 정책 접근성을 극대화합니다.

## 🏗 System Architecture
<img width="4161" height="2402" alt="wiseyoung_출시구조도" src="https://github.com/user-attachments/assets/982e4347-1fa9-423b-bda2-6f33e0419576" />

## 🔔 Core Features
1. **맞춤 정책 추천:** 사용자 프로필(나이, 소득, 지역 등) 기반 적합도 판별 및 추천
2. **임대주택 통합 조회:** LH 공용 데이터 기반 조건별(보증금/월세) 주택 공고 검색
3. **AI 스마트 챗봇:** 정책 질의응답 및 비속어 필터링이 적용된 자동 응답 시스템
4. **D-Day 관리:** 관심 정책 북마크 및 마감 임박 푸시 알림(FCM)

## 🛠 Tech Stack
### Frontend
- **Language:** Kotlin
- **UI:** Jetpack Compose, Navigation Compose
- **Async/Stream:** Coroutines, Flow
- **DI:** Hilt
- **Network:** Retrofit2, OkHttp3
- **Local DB:** Room
- **Firebase:** Auth, FCM

### Backend
- **Framework:** Spring Boot 3
- **Database:** MariaDB
- **Server:** CLOUDTYPE
- **Auth:** Google OAuth

### 🔒 Login Flow
<img width="709" height="516" alt="google signin" src="https://github.com/user-attachments/assets/1b82b178-73fb-4239-b14c-cdb77a216aae" />

---
## 🛠 Troubleshooting & Optimization

### 1. 청년정책 추천 데이터 로딩 최적화 및 구조 개선
- **Tech:** `Jetpack Compose`, `Retrofit`, `Coroutines`
- **🔴 Issue:** UI와 Data 로직의 혼재로 API 명세 변경 시 유지보수 비용이 높았으며, 초기 데이터 로딩 시간이 3초 이상 지연됨.
- **🟢 Solution:** - `LaunchedEffect`와 `State`를 활용해 UI와 Data Layer를 명확히 분리.
  - 유저 프로필(지역 > 나이 > 관심분야) 기반으로 서버에서 가중치 점수 계산 후, 상위 50개만 우선 호출(`limit=50`)하도록 페이지네이션 적용.
- **🔵 Result:** API 변경 시 수정 범위가 Data Layer로 한정되어 코드 영향 범위 **70% 감소**. 전체 정책 호출 대기 시간 **3초 → 1.8초로 단축**.

### 2. 북마크 & 캘린더 동기화 및 로딩 속도 개선
- **Tech:** `Room DB Flow`, `Coroutines async`
- **🔴 Issue:** 정책/임대주택 북마크 순차 호출로 인한 대기 시간 증가 및 데이터 변경 시 UI 수동 새로고침 현상 발생.
- **🟢 Solution:** - `async {} + awaitAll()`을 활용해 북마크 조회를 병렬 처리.
  - `Room DB`의 Flow를 `collectAsState()`로 구독하여 데이터 변경 시 UI가 자동 갱신되도록 반응형 설계 구축.
  - 서버와 로컬 DB를 연동하여 오프라인 상태에서도 조회 가능한 Fallback 구성.
- **🔵 Result:** 북마크 로딩 시간 **50% 이상 단축**, 화면 전환 없이 즉시 동기화 달성. 네트워크 불안정 시에도 로컬 DB 기반의 앱 가용성 확보.

### 3. AI 챗봇 응답 지연 해결 및 Fallback 처리
- **Tech:** `Retrofit suspend`, `Kotlin Coroutines`
- **🔴 Issue:** 사용자 입력마다 전체 DB를 스캔하여 챗봇 응답이 지연되고, 검색 결과가 없을 경우 무한 로딩 및 에러를 반환함.
- **🟢 Solution:** - Quick Chip(AI 추천/정책 검색/임대주택)을 활용해 DB 탐색 범위를 사전 분기하여 쿼리 최적화.
  - 검색 결과 부재 시, 예외 처리를 통해 유저 프로필(지역/나이 등) 기반 추천 데이터를 반환하는 Fallback 로직 적용.
- **🔵 Result:** 챗봇 전체 응답 시간 **40% 단축** 및 검색 실패(Return Fail) 확률 **0% 달성**.

### 4. 공공데이터 좌표 누락 예외 처리 (마커 100% 렌더링)
- **Tech:** `Kakao Map SDK`, `Android Geocoder API`, `Coroutines`
- **🔴 Issue:** 공공데이터 특성상 위치 좌표가 누락된 임대주택 단지가 다수 존재해 지도에 마커가 표시되지 않음.
- **🟢 Solution:** - 좌표 누락 단지 발견 시, `Geocoder API`를 활용해 주소 텍스트를 좌표로 자동 변환하는 Fallback 프로세스 구축.
  - 주소 변환 과정에서 UI 스레드 블로킹(멈춤)을 방지하기 위해 `Dispatchers.IO` 환경에서 비동기 처리.
- **🔵 Result:** Main Thread 블로킹 없이 **전체 임대주택 마커 표시율 100% 달성** 및 부드러운 지도 조작성 확보.

### 5. 홈화면 핵심 정책 슬라이드 (TOP 5 큐레이션) 및 애니메이션 최적화
- **Tech:** `Jetpack Compose`, `animateFloatAsState`, `Retrofit`, `Material 3`
- **🔴 Issue:** 홈 화면에 전체 정책을 나열할 경우 정보 과부하로 인해 핵심 정책 인지율이 저하되며, 카드 화면 전환 시 끊김 현상으로 UX가 부자연스러움.
- **🟢 Solution:** - 지역 일치 → 나이 범위 → 관심분야 순으로 가중치를 누적 합산하는 **독립적인 홈화면 전용 큐레이션 알고리즘**을 적용하여 상위 5개 항목만 추출.
  - `animateFloatAsState(tween(300))`를 활용해 부드러운 페이드(Fade) 전환 애니메이션을 구현.
  - `LaunchedEffect`를 사용하여 4.5초 주기로 자동 순환(Auto-Scroll)되도록 스케줄링 적용.
- **🔵 Result:** 정보 과부하 방지 및 직관적인 핵심 정책 인지율 향상. 매끄러운 카드 전환 액션으로 사용자 체감 품질(UX) 대폭 개선.

### 6. Android 네이티브 특화 인증 시스템 구축 (소셜/생체 로그인 연동)
- **Tech:** `Firebase Auth`, `Google OAuth 2.0`, `FIDO2 Passkey`
- **🔴 Issue:** 파편화된 기존 로그인 방식으로 인해 사용자 진입 장벽이 존재. Android 환경에 최적화되고 빠르며 안전한 간편 로그인 시스템 도입 필요.
- **🟢 Solution:** - `이메일 OTP 인증` → `Firebase 계정 생성` → `서버 DB 연동`으로 이어지는 3단계 인증 파이프라인(Pipe-Line) 아키텍처 구축.
  - `Google Sign-In` 및 `FIDO2 Passkey` 생체 인증을 도입하여 비밀번호가 필요 없는(Passwordless) 로그인 지원.
  - 로그인 직후 `/auth/profile` 호출 결과를 통해 신규/기존 유저의 온보딩 플로우를 자동 분기 처리.
- **🔵 Result:** 복잡한 가입/로그인 절차 최소화로 신규 유저 진입 장벽 하락. 안드로이드 기기에 최적화된 생체/소셜 인증으로 보안성과 편의성 동시 확보.



