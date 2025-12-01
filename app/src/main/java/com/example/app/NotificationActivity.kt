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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.data.*
import com.example.app.ui.theme.AppColors
import com.example.app.ui.theme.Spacing
import com.example.app.ui.theme.ThemeWrapper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// UI 표시용 모델
data class NotificationItem(
    val id: Long,
    val icon: NotificationIcon,
    val title: String,
    val subtitle: String,
    val organization: String,
    val deadline: String?,
    val isRead: Boolean,
    val hasAction: Boolean = true
)

enum class NotificationIcon {
    BELL, HOME
}

class NotificationActivity : ComponentActivity() {
    // 알림 Repository (Room DB 연동)
    private val repository by lazy { NotificationRepository(this) }
    private val auth = FirebaseAuth.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeWrapper {
                NotificationScreen(
                    repository = repository,
                    userId = auth.currentUser?.uid ?: "",
                    onBack = { finish() },
                    onNavigateCalendar = {
                        val intent = Intent(this, CalendarActivity::class.java)
                        startActivity(intent)
                    },
                    onNotificationClick = { notification ->
                        // 알림 클릭 시 읽음 처리
                        CoroutineScope(Dispatchers.IO).launch {
                            repository.markAsRead(notification.id)
                        }
                        
                        // 해당 캘린더 이벤트가 있다면 캘린더로 이동 (선택사항)
                        if (notification.eventId != null) {
                            val intent = Intent(this, CalendarActivity::class.java)
                            // intent.putExtra("eventId", notification.eventId) // 필요 시 구현
                            startActivity(intent)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun NotificationScreen(
    repository: NotificationRepository,
    userId: String,
    onBack: () -> Unit,
    onNavigateCalendar: () -> Unit,
    onNotificationClick: (Notification) -> Unit
) {
    // DB에서 실시간으로 알림 목록 가져오기 (Flow)
    val notifications by repository.getAllNotifications(userId).collectAsState(initial = emptyList())
    
    // Notification 엔티티 -> NotificationItem 변환
    val notificationItems = notifications.map { notification ->
        NotificationItem(
            id = notification.id,
            icon = when (notification.eventType) {
                EventType.POLICY -> NotificationIcon.BELL
                EventType.HOUSING -> NotificationIcon.HOME
                else -> NotificationIcon.BELL
            },
            title = notification.title,
            subtitle = notification.body,
            organization = notification.organization ?: "알림",
            deadline = null, 
            isRead = notification.isRead,
            hasAction = true
        )
    }
    
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
        if (notificationItems.isEmpty()) {
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
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                notificationItems.forEach { item ->
                    val notification = notifications.find { it.id == item.id }!!
                    NotificationCard(
                        notification = item,
                        onNavigate = { onNotificationClick(notification) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
                text = "알림함",
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
        border = androidx.compose.foundation.BorderStroke(
            width = if (notification.isRead) 1.dp else 2.dp,
            color = if (notification.isRead) AppColors.Border.copy(alpha = 0.5f) else AppColors.Border
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color.White else Color(0xFFF8F9FA)
        )
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(AppColors.Purple, CircleShape)
                        )
                    }
                }
                
                Text(
                    text = notification.subtitle,
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary
                )
                
                if (notification.organization.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = notification.organization,
                        fontSize = 12.sp,
                        color = AppColors.TextTertiary
                    )
                }
            }
        }
    }
}
