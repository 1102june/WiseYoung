package com.wiseyoung.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.ui.theme.AppColors
import com.example.app.ui.theme.Spacing
import com.example.app.ui.theme.ThemeWrapper

data class NotificationItem(
    val id: Int,
    val icon: NotificationIcon,
    val title: String,
    val subtitle: String,
    val organization: String,
    val deadline: String,
    val hasAction: Boolean = true
)

enum class NotificationIcon {
    BELL, HOME
}

val notifications = listOf(
    NotificationItem(
        id = 1,
        icon = NotificationIcon.BELL,
        title = "[청년 월세 한시 특별지원]",
        subtitle = "마감이 3일 남았어요!",
        organization = "경기도 수원시",
        deadline = "2025.03.31"
    ),
    NotificationItem(
        id = 2,
        icon = NotificationIcon.BELL,
        title = "[청년 특별지원]",
        subtitle = "마감이 3일 남았어요!",
        organization = "고용노동부",
        deadline = "2025.04.15"
    ),
    NotificationItem(
        id = 3,
        icon = NotificationIcon.BELL,
        title = "[청년 자격증 지원금]",
        subtitle = "마감이 3일 남았어요!",
        organization = "교육부",
        deadline = "2025.04.20"
    ),
    NotificationItem(
        id = 4,
        icon = NotificationIcon.HOME,
        title = "[AA 아파트 101동]",
        subtitle = "접수 마감이 7일 남았어요!",
        organization = "한국토지주택공사(LH)",
        deadline = "2025.05.15"
    ),
    NotificationItem(
        id = 5,
        icon = NotificationIcon.HOME,
        title = "[BB 아파트 103동]",
        subtitle = "접수 마감이 10일 남았어요!",
        organization = "SH서울주택도시공사",
        deadline = "2025.05.20"
    ),
    NotificationItem(
        id = 6,
        icon = NotificationIcon.BELL,
        title = "[청년 창업 멘토링]",
        subtitle = "신규 정책이 등록되었어요!",
        organization = "서울시",
        deadline = "2025.07.30"
    ),
    NotificationItem(
        id = 7,
        icon = NotificationIcon.BELL,
        title = "[청년 취업 성공패키지]",
        subtitle = "관심 정책에 업데이트가 있어요!",
        organization = "고용노동부",
        deadline = "2025.06.20"
    ),
    NotificationItem(
        id = 8,
        icon = NotificationIcon.HOME,
        title = "[CC 아파트 205동]",
        subtitle = "새로운 임대주택이 등록되었어요!",
        organization = "경기주택도시공사",
        deadline = "2025.06.10"
    )
)

class NotificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeWrapper {
                NotificationScreen(
                    onBack = { finish() },
                    onNavigateCalendar = {
                        // TODO: Calendar 화면으로 이동
                    }
                )
            }
        }
    }
}

@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    onNavigateCalendar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        NotificationHeader(
            onBack = onBack,
            onCalendar = onNavigateCalendar
        )
        
        // Notification List
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md)
        ) {
            notifications.forEach { notification ->
                NotificationCard(
                    notification = notification,
                    onNavigate = onNavigateCalendar,
                    modifier = Modifier.padding(bottom = Spacing.sm)
                )
            }
        }
    }
}

@Composable
private fun NotificationHeader(
    onBack: () -> Unit,
    onCalendar: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    modifier = Modifier.size(32.dp),
                    tint = AppColors.TextPrimary
                )
            }
            
            Text(
                text = "마감일 알림",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            
            IconButton(onClick = onCalendar) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Calendar",
                    modifier = Modifier.size(28.dp),
                    tint = AppColors.TextPrimary
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationItem,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = notification.hasAction) { onNavigate() },
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, AppColors.Border),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (notification.icon == NotificationIcon.BELL) {
                            AppColors.Purple.copy(alpha = 0.1f)
                        } else {
                            AppColors.BackgroundGradientStart.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (notification.icon == NotificationIcon.BELL) {
                        Icons.Default.Notifications
                    } else {
                        Icons.Default.Home
                    },
                    contentDescription = null,
                    tint = if (notification.icon == NotificationIcon.BELL) {
                        AppColors.Purple
                    } else {
                        AppColors.BackgroundGradientStart
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text = notification.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                
                Text(
                    text = notification.subtitle,
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary
                )
                
                Spacer(modifier = Modifier.height(Spacing.xs))
                
                Text(
                    text = "기관: ${notification.organization}",
                    fontSize = 12.sp,
                    color = AppColors.TextTertiary
                )
                
                Text(
                    text = "마감일: ${notification.deadline}",
                    fontSize = 12.sp,
                    color = AppColors.TextTertiary
                )
                
                if (notification.hasAction) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { onNavigate() }
                    ) {
                        Text(
                            text = "바로가기",
                            fontSize = 14.sp,
                            color = AppColors.Purple,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = AppColors.Purple,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

