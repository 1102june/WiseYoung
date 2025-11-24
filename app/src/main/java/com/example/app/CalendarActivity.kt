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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.app.NotificationSettings
import com.example.app.ui.theme.AppColors
import com.example.app.ui.theme.Spacing
import com.example.app.ui.theme.ThemeWrapper
import com.example.app.ui.components.BottomNavigationBar
import java.text.SimpleDateFormat
import java.util.*

data class CalendarBookmark(
    val id: Int,
    val type: BookmarkType,
    val title: String,
    val organization: String? = null,
    val deadline: String,
    val notifications: NotificationSettings = NotificationSettings()
)

class CalendarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeWrapper {
                CalendarScreen(
                    onNavigateHome = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onNavigateBookmark = {
                        startActivity(Intent(this, BookmarkActivity::class.java))
                        finish()
                    },
                    onNavigateProfile = {
                        startActivity(Intent(this, ProfileActivity::class.java))
                        finish()
                    },
                    onNavigateChatbot = {
                        // TODO: Chatbot 화면으로 이동
                    }
                )
            }
        }
    }
}

@Composable
fun CalendarScreen(
    onNavigateHome: () -> Unit,
    onNavigateBookmark: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateChatbot: () -> Unit
) {
    var selectedDate by remember { mutableStateOf(Date()) }
    var selectedCategory by remember { mutableStateOf("전체") }
    var deleteDialogOpen by remember { mutableStateOf(false) }
    var notificationDialogOpen by remember { mutableStateOf(false) }
    var selectedBookmarkId by remember { mutableStateOf<Int?>(null) }
    var editNotifications by remember {
        mutableStateOf(
            NotificationSettings()
        )
    }
    
    // 임시 북마크 데이터
    var bookmarks by remember {
        mutableStateOf<List<CalendarBookmark>>(emptyList())
    }
    
    val filteredBookmarks = when (selectedCategory) {
        "전체" -> bookmarks
        "정책" -> bookmarks.filter { it.type == BookmarkType.POLICY }
        "임대주택" -> bookmarks.filter { it.type == BookmarkType.HOUSING }
        else -> bookmarks
    }
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = "calendar",
                onNavigateHome = onNavigateHome,
                onNavigateCalendar = {},
                onNavigateChatbot = onNavigateChatbot,
                onNavigateBookmark = onNavigateBookmark,
                onNavigateProfile = onNavigateProfile
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            // Header
            CalendarHeader()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md)
            ) {
                Spacer(modifier = Modifier.height(Spacing.md))
                
                // Calendar Card
                CalendarCard(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it },
                    bookmarks = bookmarks,
                    modifier = Modifier.padding(bottom = Spacing.lg)
                )
                
                // Category Filter
                CategoryFilter(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    modifier = Modifier.padding(bottom = Spacing.lg)
                )
                
                // Upcoming Deadlines
                Text(
                    text = "다가오는 마감일",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(bottom = Spacing.md)
                )
                
                if (filteredBookmarks.isEmpty()) {
                    EmptyDeadlineCard(
                        message = "등록된 일정이 없습니다.",
                        modifier = Modifier.padding(top = Spacing.lg)
                    )
                } else {
                    filteredBookmarks.forEach { bookmark ->
                        DeadlineCard(
                            bookmark = bookmark,
                            onDelete = {
                                selectedBookmarkId = bookmark.id
                                deleteDialogOpen = true
                            },
                            onEditNotification = {
                                selectedBookmarkId = bookmark.id
                                editNotifications = bookmark.notifications
                                notificationDialogOpen = true
                            },
                            modifier = Modifier.padding(bottom = Spacing.sm)
                        )
                    }
                }
            }
        }
    }
    
    // Delete Dialog
    if (deleteDialogOpen) {
        AlertDialog(
            onDismissRequest = { deleteDialogOpen = false },
            title = { Text("삭제하시겠습니까?") },
            text = { Text("이 일정을 삭제하면 다시 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedBookmarkId?.let { id ->
                            bookmarks = bookmarks.filter { it.id != id }
                        }
                        deleteDialogOpen = false
                        selectedBookmarkId = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialogOpen = false }) {
                    Text("취소")
                }
            }
        )
    }
    
    // Notification Dialog
    if (notificationDialogOpen) {
        NotificationEditDialog(
            notifications = editNotifications,
            onNotificationsChange = { editNotifications = it },
            onSave = {
                selectedBookmarkId?.let { id ->
                    bookmarks = bookmarks.map {
                        if (it.id == id) {
                            it.copy(notifications = editNotifications)
                        } else {
                            it
                        }
                    }
                }
                notificationDialogOpen = false
                selectedBookmarkId = null
            },
            onDismiss = {
                notificationDialogOpen = false
                selectedBookmarkId = null
            }
        )
    }
}

@Composable
private fun CalendarHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "D-day 캘린더",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
        }
    }
}

@Composable
private fun CalendarCard(
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    bookmarks: List<CalendarBookmark>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, AppColors.Border),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            // 간단한 날짜 선택기 (나중에 더 복잡한 캘린더로 교체 가능)
            Text(
                text = "날짜 선택 기능은 추후 구현 예정",
                fontSize = 14.sp,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(bottom = Spacing.md)
            )
            
            // 범례
            Column(
                modifier = Modifier.padding(top = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                LegendItem(
                    color = AppColors.Purple,
                    label = "청년정책 마감일"
                )
                LegendItem(
                    color = AppColors.BackgroundGradientStart,
                    label = "임대주택 마감일"
                )
                LegendItem(
                    color = Color(0xFFFBBF24),
                    label = "알림 설정일"
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = AppColors.TextSecondary
        )
    }
}

@Composable
private fun CategoryFilter(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        listOf("전체", "정책", "임대주택").forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AppColors.BackgroundGradientStart.copy(alpha = 0.2f),
                    selectedLabelColor = AppColors.TextPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedCategory == category,
                    borderColor = if (selectedCategory == category) {
                        AppColors.BackgroundGradientStart.copy(alpha = 0.5f)
                    } else {
                        AppColors.Border
                    },
                    selectedBorderColor = AppColors.BackgroundGradientStart.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun DeadlineCard(
    bookmark: CalendarBookmark,
    onDelete: () -> Unit,
    onEditNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysLeft = getDaysLeft(bookmark.deadline)
    val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, AppColors.Border),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    // 타입 태그
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (bookmark.type == BookmarkType.POLICY) {
                                    AppColors.Purple.copy(alpha = 0.1f)
                                } else {
                                    AppColors.BackgroundGradientStart.copy(alpha = 0.1f)
                                }
                            )
                            .padding(horizontal = Spacing.sm, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (bookmark.type == BookmarkType.POLICY) "정책" else "임대주택",
                            fontSize = 12.sp,
                            color = if (bookmark.type == BookmarkType.POLICY) {
                                AppColors.Purple
                            } else {
                                AppColors.BackgroundGradientStart
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // D-day 태그
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    daysLeft <= 3 -> Color(0xFFEF4444)
                                    daysLeft <= 7 -> AppColors.BackgroundGradientStart
                                    else -> AppColors.Info
                                }
                            )
                            .padding(horizontal = Spacing.sm, vertical = 4.dp)
                    ) {
                        Text(
                            text = "D-$daysLeft",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Text(
                text = bookmark.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.md)
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text = "${if (bookmark.type == BookmarkType.POLICY) "신청마감일" else "접수마감일"}: ${bookmark.deadline}",
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary
                )
                
                bookmark.organization?.let {
                    Text(
                        text = "기관: $it",
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary
                    )
                }
                
                // 알림 태그들
                Row(
                    modifier = Modifier.padding(top = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (bookmark.notifications.sevenDays) {
                        NotificationTag(
                            text = "7일전 ${bookmark.notifications.sevenDaysTime}",
                            color = AppColors.Info.copy(alpha = 0.1f),
                            textColor = AppColors.Info
                        )
                    }
                    if (bookmark.notifications.oneDay) {
                        NotificationTag(
                            text = "1일전 ${bookmark.notifications.oneDayTime}",
                            color = AppColors.Success.copy(alpha = 0.1f),
                            textColor = AppColors.Success
                        )
                    }
                    if (bookmark.notifications.custom) {
                        NotificationTag(
                            text = "${bookmark.notifications.customDays}일전 ${bookmark.notifications.customTime}",
                            color = AppColors.Purple.copy(alpha = 0.1f),
                            textColor = AppColors.Purple
                        )
                    }
                }
                
                Button(
                    onClick = onEditNotification,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.sm),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Purple.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "알림 수정",
                        color = AppColors.Purple,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationTag(
    text: String,
    color: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = textColor
        )
    }
}

@Composable
private fun EmptyDeadlineCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, AppColors.Border),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xxl),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                fontSize = 14.sp,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun NotificationEditDialog(
    notifications: NotificationSettings,
    onNotificationsChange: (NotificationSettings) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var localNotifications by remember { mutableStateOf(notifications) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("알림 수정") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                NotificationSettingRow(
                    label = "7일전 알림",
                    enabled = localNotifications.sevenDays,
                    time = localNotifications.sevenDaysTime,
                    onEnabledChange = {
                        localNotifications = localNotifications.copy(sevenDays = it)
                    },
                    onTimeChange = {
                        localNotifications = localNotifications.copy(sevenDaysTime = it)
                    }
                )
                
                NotificationSettingRow(
                    label = "1일전 알림",
                    enabled = localNotifications.oneDay,
                    time = localNotifications.oneDayTime,
                    onEnabledChange = {
                        localNotifications = localNotifications.copy(oneDay = it)
                    },
                    onTimeChange = {
                        localNotifications = localNotifications.copy(oneDayTime = it)
                    }
                )
                
                CustomNotificationRow(
                    enabled = localNotifications.custom,
                    days = localNotifications.customDays,
                    time = localNotifications.customTime,
                    onEnabledChange = {
                        localNotifications = localNotifications.copy(custom = it)
                    },
                    onDaysChange = {
                        localNotifications = localNotifications.copy(customDays = it)
                    },
                    onTimeChange = {
                        localNotifications = localNotifications.copy(customTime = it)
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onNotificationsChange(localNotifications)
                    onSave()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.TextPrimary
                )
            ) {
                Text("저장하기", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
private fun NotificationSettingRow(
    label: String,
    enabled: Boolean,
    time: String,
    onEnabledChange: (Boolean) -> Unit,
    onTimeChange: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 14.sp)
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        }
        
        if (enabled) {
            OutlinedTextField(
                value = time,
                onValueChange = onTimeChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.md, top = Spacing.sm),
                label = { Text("시간") },
                placeholder = { Text("09:00") }
            )
        }
    }
}

@Composable
private fun CustomNotificationRow(
    enabled: Boolean,
    days: Int,
    time: String,
    onEnabledChange: (Boolean) -> Unit,
    onDaysChange: (Int) -> Unit,
    onTimeChange: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("사용자 지정 알림", fontSize = 14.sp)
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        }
        
        if (enabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.md, top = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OutlinedTextField(
                    value = days.toString(),
                    onValueChange = { onDaysChange(it.toIntOrNull() ?: 0) },
                    modifier = Modifier.width(80.dp),
                    label = { Text("일") }
                )
                Text("일 전", fontSize = 14.sp)
                
                Spacer(modifier = Modifier.weight(1f))
                
                OutlinedTextField(
                    value = time,
                    onValueChange = onTimeChange,
                    modifier = Modifier.width(120.dp),
                    label = { Text("시간") },
                    placeholder = { Text("09:00") }
                )
            }
        }
    }
}

private fun getDaysLeft(deadline: String): Int {
    return try {
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        val deadlineDate = dateFormat.parse(deadline)
        val today = Date()
        val diff = deadlineDate?.time?.minus(today.time) ?: 0
        maxOf(0, (diff / (1000 * 60 * 60 * 24)).toInt())
    } catch (e: Exception) {
        0
    }
}

