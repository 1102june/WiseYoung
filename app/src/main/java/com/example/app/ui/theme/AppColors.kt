package com.example.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * WiseYoung 앱의 색상 시스템
 * 모든 색상은 여기서 정의하고 사용합니다.
 * 
 * 컬러 스키마:
 * - 메인 컬러: 라이트 블루 (#59abf7) - 청년·정책·신뢰 느낌
 * - 서브 컬러: 중성 그레이 (#6459f7), 화이트
 * - 포인트 컬러: 오렌지 (로고, 강조 버튼, 이벤트 배너)
 */
object AppColors {
    // 메인 컬러 (Primary) - 라이트 블루
    val LightBlue = Color(0xFF59ABF7)  // 메인 블루
    val LightBlueDark = Color(0xFF4A8FD9)  // 진한 블루 (hover/active)
    val LightBlueLight = Color(0xFF7BB9F9)  // 연한 블루
    
    // 서브 컬러 (Secondary) - 중성 그레이
    val GrayPurple = Color(0xFF6459F7)  // 그레이 퍼플
    val GrayPurpleDark = Color(0xFF5248D9)  // 진한 그레이 퍼플
    val GrayPurpleLight = Color(0xFF7D75F9)  // 연한 그레이 퍼플
    
    // 포인트 컬러 (Accent) - 오렌지
    val Orange = Color(0xFFFF9800)  // 오렌지 (메인)
    val OrangeDark = Color(0xFFE68900)  // 진한 오렌지
    val OrangeLight = Color(0xFFFFB340)  // 연한 오렌지
    
    // 기본 색상 (하위 호환성)
    val Purple = LightBlue  // 기존 코드 호환성
    val PurpleDark = LightBlueDark
    val Secondary = GrayPurple
    
    // 그레이 톤
    val Gray = Color(0xFF9CA3AF)
    val GrayDark = Color(0xFF6B7280)
    val GrayLight = Color(0xFFD1D5DB)
    
    // 배경 색상 - 화이트 기반
    val Background = Color(0xFFFFFFFF)  // 화이트
    val Surface = Color(0xFFFFFFFF)  // 화면 배경
    val SurfaceVariant = Color(0xFFF5F5F5)  // 약간의 그레이 톤
    
    // 그라데이션 (오렌지 계열 - 포인트 컬러)
    val BackgroundGradientStart = Color(0xFFFF9A5C)
    val BackgroundGradientEnd = Color(0xFFFF6B2C)
    
    // 텍스트 색상
    val TextPrimary = Color(0xFF1A1A1A)  // 거의 검정
    val TextSecondary = Color(0xFF666666)  // 중간 그레이
    val TextTertiary = Color(0xFF999999)  // 연한 그레이
    val TextDisabled = Color(0xFFCCCCCC)  // 비활성화
    
    // 테두리 색상
    val Border = Color(0xFFE5E7EB)  // 연한 그레이
    val BorderDark = Color(0xFFD1D5DB)  // 중간 그레이
    val BorderLight = Color(0xFFF3F4F6)  // 아주 연한 그레이
    
    // 상태 색상
    val Error = Color(0xFFEF4444)  // 빨강
    val Success = Color(0xFF10B981)  // 초록
    val Warning = Color(0xFFF59E0B)  // 노랑
    val Info = LightBlue  // 정보 - 메인 블루 사용
}

