package com.example.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 다크 테마 색상 스키마
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3A8AD8),  // 다크 버전 라이트 블루 (#3A8AD8) - 강력 추천
    secondary = Color(0xFF8D87FF),  // 서브 컬러 - 그레이/퍼플그레이 다크 버전 (#8D87FF)
    tertiary = Color(0xFFFF833A),  // 포인트 오렌지 톤 다운 버전 (#FF833A)
    background = Color(0xFF121212),  // 배경 (#121212)
    surface = Color(0xFF1A1C20),  // 카드/서브배경 (#1A1C20)
    surfaceVariant = Color(0xFF1F2126),  // 서브배경 변형 (#1F2126)
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFEDEDED),  // 메인 텍스트 (#EDEDED)
    onSurface = Color(0xFFEDEDED),  // 메인 텍스트 (#EDEDED)
    outline = Color(0xFF2A2A2A),
    outlineVariant = Color(0xFFA5A5A5)  // 서브 텍스트 (#A5A5A5)
)

// 다크블루 테마 색상 스키마 (프리미엄/고급 UX 모드)
private val DarkBlueColorScheme = darkColorScheme(
    primary = Color(0xFF59ABF7),  // 라이트 블루 (#59ABF7) - 그대로 사용 또는 #6EBBFF
    secondary = Color(0xFF8AA4FF),  // 서브 컬러 - 차분한 블루그레이 (#8AA4FF) 또는 #6F8BFF
    tertiary = Color(0xFFFF833A),  // 포인트 오렌지 (#FF833A)
    background = Color(0xFF0D1A2A),  // 배경 - 네이비 딥블루 (#0D1A2A)
    surface = Color(0xFF112033),  // 서브배경 (#112033)
    surfaceVariant = Color(0xFF12263D),  // 서브배경 변형 (#12263D)
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE4ECF6),  // 메인 텍스트 - 블루 화이트 (#E4ECF6)
    onSurface = Color(0xFFE4ECF6),  // 메인 텍스트 (#E4ECF6)
    outline = Color(0xFF1F2A3A),
    outlineVariant = Color(0xFFA9B8C9)  // 서브 텍스트 (#A9B8C9)
)

private val LightColorScheme = lightColorScheme(
    primary = LightBlue40,  // 라이트 블루 (메인) - #59abf7
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),  // Primary Container - 연한 블루 배경
    onPrimaryContainer = Color(0xFF0D47A1),  // Primary Container 텍스트
    
    secondary = GrayPurple40,  // 그레이 퍼플 (서브) - #6459f7
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E5FF),  // Secondary Container
    onSecondaryContainer = Color(0xFF1A0E5C),
    
    tertiary = Orange40,  // 오렌지 (포인트)
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0B2),  // Tertiary Container
    onTertiaryContainer = Color(0xFFE65100),
    
    error = Color(0xFFBA1A1A),  // Error 색상
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    background = Color(0xFFFFFFFF),  // 화이트 배경
    onBackground = Color(0xFF1A1A1A),  // 거의 검정 텍스트
    
    surface = Color(0xFFFFFFFF),  // 화이트 표면
    onSurface = Color(0xFF1A1A1A),  // 거의 검정 텍스트
    surfaceVariant = Color(0xFFF5F5F5),  // 약간의 그레이 톤
    onSurfaceVariant = Color(0xFF424242),  // Surface Variant 텍스트
    
    outline = Color(0xFFE5E7EB),  // 테두리 색상
    outlineVariant = Color(0xFFE0E0E0),  // Outline Variant
    
    inverseSurface = Color(0xFF1A1A1A),  // Inverse Surface
    inverseOnSurface = Color(0xFFF5F5F5),  // Inverse On Surface
    inversePrimary = Color(0xFF90CAF9)  // Inverse Primary
)

@Composable
fun WiseYoungTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    // Dynamic color는 사용자 지정 컬러 스키마를 사용하므로 비활성화
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.DARK_BLUE -> DarkBlueColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

// 하위 호환성을 위한 오버로드 (Boolean 기반)
// 파라미터 없이 호출 시 themeMode 기반 오버로드가 선택되도록 Boolean은 필수 파라미터로 설정
@Composable
fun WiseYoungTheme(
    darkTheme: Boolean,  // 기본값 제거 - 필수 파라미터로 변경하여 모호성 제거
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT
    WiseYoungTheme(themeMode = themeMode, dynamicColor = dynamicColor, content = content)
}