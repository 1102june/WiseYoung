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


