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

// 실제 알림 데이터는 서버/DB 또는 캘린더 알림에서 가져오도록 하고,
// 여기서는 더미 데이터를 정의하지 않습니다.

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
        
        // Notification List (현재는 서버 연동 전이므로 빈 상태 표시)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "도착한 알림이 없습니다.",
                fontSize = 14.sp,
                color = AppColors.TextSecondary
            )
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

