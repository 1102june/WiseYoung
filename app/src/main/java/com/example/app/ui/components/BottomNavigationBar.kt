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

@Composable
fun BottomNavigationBar(
    currentScreen: String,
    onNavigateHome: () -> Unit,
    onNavigateCalendar: () -> Unit,
    onNavigateChatbot: () -> Unit,
    onNavigateBookmark: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
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
            
            // 챗봇 버튼 (특별 스타일)
            Column(
                modifier = Modifier.padding(top = (-24).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    AppColors.Purple,
                                    AppColors.BackgroundGradientStart
                                )
                            )
                        )
                        .clickable { onNavigateChatbot() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = "Chatbot",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "챗봇",
                    fontSize = 12.sp,
                    color = AppColors.TextPrimary
                )
            }
            
            // 북마크 버튼
            BottomNavButton(
                icon = Icons.Default.Bookmark,
                filledIcon = Icons.Filled.Bookmark,
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
            .padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (isSelected && filledIcon != null) filledIcon else icon,
            contentDescription = label,
            modifier = Modifier.size(28.dp),
            tint = AppColors.TextPrimary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = AppColors.TextPrimary
        )
    }
}

