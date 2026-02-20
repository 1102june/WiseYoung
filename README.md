📱 WiseYoung (슬기로운 청년생활)

청년 정책 및 공공임대주택 정보를 통합 제공하는 모바일 플랫폼

📌 프로젝트 소개

WiseYoung는 부처별로 파편화된 청년 정책 정보와 LH 공공임대주택 공고를
하나의 모바일 앱에서 통합 제공하는 서비스입니다.

사용자의 나이, 소득, 지역을 기반으로 맞춤형 정책을 추천하고,
마감일을 놓치지 않도록 D-Day 관리 및 푸시 알림 기능을 제공합니다.

🎯 문제 정의
1️⃣ 정보의 파편화
부처·기관별로 정책이 분산되어 있음
사용자는 여러 사이트를 직접 탐색해야 함

2️⃣ 낮은 가독성
정책 공고문이 텍스트 중심 구조
핵심 정보 파악에 시간 소요

3️⃣ 마감일 관리 어려움
정책별 신청 일정이 상이
개별 관리 필요

4️⃣ 기존 앱의 한계
단순 정책 나열
신청 페이지로의 연결성 부족
개인화 추천 기능 부재

💡 해결 전략
정책 + 공공임대주택 정보 통합
사용자 정보 기반 맞춤 추천
D-Day 자동 계산 및 캘린더 연동
FCM 푸시 알림으로 마감 방지
모바일 중심 UX 설계 (위젯/알림 활용)

🧱 시스템 아키텍처
📐 전체 구조
Android App (Kotlin, Compose)
        │
        ▼
Spring Boot Server (AWS EC2)
        │
        ▼
MariaDB
📦 Android 아키텍처
MVVM + Clean Architecture

ui
 ├─ screen
 ├─ navigation
 └─ viewmodel

domain
 ├─ usecase
 └─ repository interface

data
 ├─ repository impl
 ├─ remote datasource
 └─ local datasource

계층 분리 원칙 준수
ViewModel은 UseCase에만 의존
DI를 통한 의존성 관리
Navigation 중앙 관리

🔐 인증 및 사용자 관리

Google OAuth 로그인

Gmail SMTP 이메일 인증

회원가입 / 비밀번호 찾기

사용자 프로필 기반 추천 로직

🔔 주요 기능
1️⃣ 맞춤 정책 추천
나이 / 지역 / 소득 기반 필터링
정책 적합도 판단 후 추천

2️⃣ 공공임대주택 정보 통합
LH 공고 데이터 제공
지역 기반 조회 기능

3️⃣ D-Day 관리
자동 마감일 계산
캘린더 연동
북마크 기능

4️⃣ 푸시 알림 시스템
Firebase Cloud Messaging(FCM)
정책 마감 임박 알림
신규 공고 알림

5️⃣ AI 기능
AI 챗봇
AI 기반 정책 추천 시스템

🛠 기술 스택
📱 Android
Kotlin

Jetpack Compose

MVVM

Clean Architecture

Hilt

Retrofit

Room

Navigation Compose

Firebase (Auth, FCM)

🖥 Backend

Spring Boot

REST API

AWS EC2 배포

MariaDB

🚀 프로젝트 특징

모바일 환경 최적화 설계
정책 + 주거 정보 통합 플랫폼
실사용 문제 기반 기획
확장 가능한 구조 설계
클라이언트-서버 분리 구조 구현

👥 팀 구성

박선호 – Backend (Spring Boot 서버 운영)
서예준 – Frontend (Android UI)

▶️ 실행 방법
git clone https://github.com/1102june/WiseYoung

Android Studio에서 실행
Firebase 설정 필요
AWS 서버 연결
