package com.example.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material Design 3 Shape System
 * Material3의 둥근 모서리 스타일을 정의하여 일관된 UI 제공
 */
val Shapes = Shapes(
    // Extra Small - 작은 요소 (태그, 칩 등)
    extraSmall = RoundedCornerShape(4.dp),
    // Small - 작은 카드, 버튼
    small = RoundedCornerShape(8.dp),
    // Medium - 일반 카드, 다이얼로그
    medium = RoundedCornerShape(12.dp),
    // Large - 큰 카드, 시트
    large = RoundedCornerShape(16.dp),
    // Extra Large - 모달, 전체 화면 시트
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * 커스텀 Shape 정의
 */
object AppShapes {
    // 카드 모서리
    val card = RoundedCornerShape(16.dp)
    val cardSmall = RoundedCornerShape(12.dp)
    val cardLarge = RoundedCornerShape(24.dp)
    
    // 버튼 모서리
    val button = RoundedCornerShape(12.dp)
    val buttonSmall = RoundedCornerShape(8.dp)
    val buttonLarge = RoundedCornerShape(16.dp)
    
    // 입력 필드 모서리
    val textField = RoundedCornerShape(12.dp)
    
    // 다이얼로그 모서리
    val dialog = RoundedCornerShape(28.dp)
    
    // 칩/태그 모서리
    val chip = RoundedCornerShape(20.dp)
    
    // 완전히 둥근 모서리 (원형 버튼 등)
    val circular = RoundedCornerShape(50)
}

