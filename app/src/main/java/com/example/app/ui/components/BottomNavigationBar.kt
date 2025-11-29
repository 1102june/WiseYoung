package com.example.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.ui.theme.AppColors
import com.example.app.ui.theme.Spacing

import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun BottomNavigationBarPreview() {
    BottomNavigationBar(
        currentScreen = "home",
        onNavigateHome = {},
        onNavigateCalendar = {},
        onNavigateChatbot = {},
        onNavigateBookmark = {},
        onNavigateProfile = {}
    )
}

@Composable
fun BottomNavigationBar(
    currentScreen: String,
    onNavigateHome: () -> Unit,
    onNavigateCalendar: () -> Unit,
    onNavigateChatbot: () -> Unit,
    onNavigateBookmark: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    // 시스템 네비게이션 바 높이를 고려한 패딩
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = (navigationBarsPadding.calculateBottomPadding() + 8.dp)) // 하단 패딩 줄임 (+24dp -> +8dp)
        ) {
            // 상단 패딩 제거 (챗봇 튀어나옴 제거로 불필요)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
            // 홈 버튼
            BottomNavButton(
                icon = Icons.Default.Home,
                filledIcon = Icons.Filled.Home,
                label = "홈",
                isSelected = currentScreen == "home",
                onClick = onNavigateHome
            )
            
            // 캘린더 버튼
            BottomNavButton(
                icon = Icons.Default.CalendarToday,
                filledIcon = Icons.Filled.CalendarToday,
                label = "캘린더",
                isSelected = currentScreen == "calendar",
                onClick = onNavigateCalendar
            )
            
            // 챗봇 버튼 (일반 아이콘 스타일로 변경)
            BottomNavButton(
                icon = Icons.Default.Message,
                filledIcon = Icons.Filled.Message, // 채워진 아이콘이 있다면 사용, 없으면 기본값
                label = "챗봇",
                isSelected = false, // 챗봇은 다이얼로그라 선택 상태 없음
                onClick = onNavigateChatbot
            )
            
            // 좋아요 버튼 (하트 아이콘)
            BottomNavButton(
                icon = Icons.Default.Favorite,
                filledIcon = Icons.Filled.Favorite,
                label = "좋아요",
                isSelected = currentScreen == "bookmark",
                onClick = onNavigateBookmark
            )
            
            // 프로필 버튼
            BottomNavButton(
                icon = Icons.Default.Person,
                filledIcon = Icons.Filled.Person,
                label = "내정보",
                isSelected = currentScreen == "profile",
                onClick = onNavigateProfile
            )
            }
        }
    }
}

@Composable
private fun BottomNavButton(
    icon: ImageVector,
    filledIcon: ImageVector? = null,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 선택된 버튼에 배경 추가
        Box(
            modifier = Modifier
                .size(if (isSelected) 48.dp else 40.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) AppColors.LightBlue.copy(alpha = 0.1f) else Color.Transparent  // 라이트 블루 (메인 컬러)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected && filledIcon != null) filledIcon else icon,
                contentDescription = label,
                modifier = Modifier.size(if (isSelected) 28.dp else 24.dp),
                tint = if (isSelected) AppColors.LightBlue else AppColors.TextSecondary  // 라이트 블루 (메인 컬러)
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) AppColors.LightBlue else AppColors.TextSecondary,  // 라이트 블루 (메인 컬러)
            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
        )
    }
}

