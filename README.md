# 📱 WiseYoung (슬기로운 청년생활)
> **"흩어진 청년 정책과 주거 정보를 한 눈에, 맞춤형 정책 큐레이션 플랫폼"**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android)](#)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin)](#)
[![Spring Boot](https://img.shields.io/badge/Backend-Spring%20Boot-6DB33F?logo=springboot)](#)

## 📌 Project Overview
**WiseYoung**은 부처별로 파편화된 청년 정책 정보와 LH 공공임대주택 공고를 통합하여, 청년들의 경제적 자립과 주거 안정을 돕는 모바일 서비스입니다. 가독성 낮은 공고문을 데이터화하여 **사용자 맞춤형 추천**과 **D-Day 관리**를 제공함으로써 청년들의 정책 접근성을 극대화합니다.

## 🎯 Key Problems & Solutions
| 문제점 (Pain Point) | 해결책 (Solution) |
| :--- | :--- |
| **정보의 파편화** | 온통청년, LH 등 공공 데이터 API 통합 및 단일 플랫폼 제공 |
| **낮은 가독성** | 핵심 정보 위주의 UI 구성 및 신청 페이지 직접 연결(One-Tap) |
| **일정 관리의 어려움** | 마감일 자동 계산(D-Day) 및 FCM 푸시 알림 서비스 |
| **개인화 부재** | 나이/소득/지역 기반 맞춤형 필터링 알고리즘 적용 |

## 🏗 System Architecture


### 📦 Android Architecture (MVVM + Clean Architecture)
단일 모듈 내에서 계층을 엄격히 분리하여 유지보수성과 테스트 용이성을 확보했습니다.
- **UI (Jetpack Compose):** 선언적 UI를 통한 직관적 인터페이스 및 State 중심 설계
- **Domain:** Pure Kotlin 기반의 비즈니스 로직(UseCase) 및 데이터 모델 정의
- **Data:** Retrofit(Remote) 및 Room(Local)을 활용한 데이터 소스 관리 및 Repository 구현
- **DI (Hilt):** 의존성 주입을 통한 객체 간 결합도 감소

### 🖥 Backend & Infrastructure
- **Framework:** Spring Boot 3.x (REST API)
- **Database:** MariaDB (AWS RDS)
- **Deployment:** AWS EC2 기반 서버 인스턴스 운영
- **Security:** BCrypt 단방향 암호화, Gmail SMTP 기반 이메일 인증 체계

## 🔒 Security & Auth
- **Google OAuth 2.0:** 안전하고 간편한 소셜 로그인 연동
- **Verification:** Gmail SMTP를 이용한 실명 및 소셜 이메일 인증
- **Data Privacy:** 사용자 비밀번호 BCrypt 해싱 및 개인정보 보호 강화

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
- **Framework:** Spring Boot 3.x
- **Database:** MariaDB
- **Server:** AWS EC2
- **Auth:** Google OAuth, Gmail SMTP

## 🛠 Troubleshooting (Updated)
- **Google OAuth 연동 이슈:** Firebase SHA-1 인증 지문 등록 및 클라이언트-서버 간 Web Client ID 불일치 문제 해결 중
- **환경 복구 및 최적화:** Gradle 캐시(`.gradle`) 및 IDE 설정(`.idea`) 충돌 해결을 통한 프로젝트 빌드 정상화

## 👥 Team
- **박선호 (Backend):** 서버 아키텍처 설계, API 개발 및 AWS 서버 운영
- **서예준 (Frontend):** Android UI/UX 구현, Clean Architecture 기반 클라이언트 개발

---

### 🎨 전체 구조도
<img width="1920" height="1080" alt="슬기로운청년생활 전체구조도" src="https://github.com/user-attachments/assets/6a837487-91aa-4ba2-91e4-ca5f7a66be51" />

### ▶️ 실행 방법
```bash
git clone [https://github.com/1102june/WiseYoung](https://github.com/1102june/WiseYoung)

## Open In AndroidStudio
```
