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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.app.NotificationSettings
import com.example.app.ui.theme.AppColors
import com.example.app.ui.theme.Spacing
import com.example.app.ui.theme.ThemeWrapper
import com.example.app.ui.components.BottomNavigationBar
import com.example.app.ui.components.CalendarView
import com.example.app.data.CalendarRepository
import com.example.app.data.CalendarEvent
import com.example.app.data.EventType
import com.example.app.service.CalendarService
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import com.example.app.network.NetworkModule
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import android.util.Log

data class CalendarBookmark(
    val id: Int,
    val type: BookmarkType,
    val title: String,
    val organization: String? = null,
    val deadline: String,
    val notifications: NotificationSettings = NotificationSettings()
)

class CalendarActivity : ComponentActivity() {
    private val repository by lazy { CalendarRepository(this) }
    private val auth = FirebaseAuth.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeWrapper {
                CalendarScreen(
                    repository = repository,
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
    repository: CalendarRepository,
    onNavigateHome: () -> Unit,
    onNavigateBookmark: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateChatbot: () -> Unit
) {
    val context = LocalContext.current
    val calendarService = remember { CalendarService(context) }
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val coroutineScope = rememberCoroutineScope()
    
    var selectedDate by remember { mutableStateOf(Date()) }
    var currentMonthDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedCategory by remember { mutableStateOf("전체") }
    var deleteDialogOpen by remember { mutableStateOf(false) }
    var notificationDialogOpen by remember { mutableStateOf(false) }
    var selectedEventId by remember { mutableStateOf<Long?>(null) }
    var editNotifications by remember {
        mutableStateOf(
            NotificationSettings()
        )
    }
    
    // Room DB에서 일정 가져오기
    val allEventsState: State<List<CalendarEvent>> = if (currentUser != null) {
        repository.getEventsByUserId(currentUser.uid).collectAsState(initial = emptyList<CalendarEvent>())
    } else {
        remember { mutableStateOf(emptyList<CalendarEvent>()) }
    }
    val allEvents = allEventsState.value
    
    // 선택된 날짜의 일정 필터링
    val selectedLocalDate = remember(selectedDate) {
        selectedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    }
    
    val eventsForSelectedDate by remember(selectedLocalDate, allEvents) {
        derivedStateOf {
            allEvents.filter { event -> event.endDate == selectedLocalDate }
        }
    }
    
    // 카테고리별 필터링
    val filteredEvents by remember(allEvents, selectedCategory) {
        derivedStateOf {
            when (selectedCategory) {
                "전체" -> allEvents
                "정책" -> allEvents.filter { event -> event.eventType == EventType.POLICY }
                "임대주택" -> allEvents.filter { event -> event.eventType == EventType.HOUSING }
                else -> allEvents
            }
        }
    }
    
    // 앱 시작 시 북마크를 캘린더에 동기화
    LaunchedEffect(currentUser?.uid) {
        currentUser?.let { user ->
            try {
                // 정책 북마크 가져오기
                val policyBookmarksResponse = com.example.app.network.NetworkModule.apiService.getBookmarks(
                    userId = user.uid,
                    contentType = "policy"
                )
                if (policyBookmarksResponse.isSuccessful && policyBookmarksResponse.body()?.success == true) {
                    val policyBookmarks = policyBookmarksResponse.body()?.data ?: emptyList()
                    policyBookmarks.forEach { bookmark ->
                        // 이미 캘린더에 있는지 확인
                        val existingEvent = allEvents.find { 
                            it.policyId == bookmark.contentId 
                        }
                        if (existingEvent == null && !bookmark.title.isNullOrBlank()) {
                            // 캘린더에 추가
                            calendarService.addPolicyToCalendar(
                                title = bookmark.title,
                                organization = bookmark.organization,
                                deadline = bookmark.deadline ?: "",
                                policyId = bookmark.contentId,
                                notificationSettings = NotificationSettings(
                                    sevenDays = true,
                                    sevenDaysTime = "09:00",
                                    oneDay = true,
                                    oneDayTime = "10:00",
                                    custom = false,
                                    customDays = 3,
                                    customTime = "09:00"
                                )
                            )
                        }
                    }
                }
                
                // 임대주택 북마크 가져오기
                val housingBookmarksResponse = com.example.app.network.NetworkModule.apiService.getBookmarks(
                    userId = user.uid,
                    contentType = "housing"
                )
                if (housingBookmarksResponse.isSuccessful && housingBookmarksResponse.body()?.success == true) {
                    val housingBookmarks = housingBookmarksResponse.body()?.data ?: emptyList()
                    housingBookmarks.forEach { bookmark ->
                        // 이미 캘린더에 있는지 확인
                        val existingEvent = allEvents.find { 
                            it.housingId == bookmark.contentId 
                        }
                        if (existingEvent == null && !bookmark.title.isNullOrBlank()) {
                            // 임대주택 상세 정보 조회하여 마감일 가져오기
                            coroutineScope.launch {
                                try {
                                    val housingResponse = com.example.app.network.NetworkModule.apiService.getHousingById(
                                        housingId = bookmark.contentId ?: "",
                                        userIdParam = user.uid
                                    )
                                    if (housingResponse.isSuccessful && housingResponse.body()?.success == true) {
                                        val housing = housingResponse.body()?.data
                                        val deadline = housing?.applicationEnd?.take(10)?.replace("-", ".") ?: bookmark.deadline ?: ""
                                        if (deadline.isNotEmpty()) {
                                            calendarService.addHousingToCalendar(
                                                title = bookmark.title,
                                                organization = bookmark.organization,
                                                deadline = deadline,
                                                housingId = bookmark.contentId,
                                                notificationSettings = NotificationSettings(
                                                    sevenDays = true,
                                                    sevenDaysTime = "09:00",
                                                    oneDay = true,
                                                    oneDayTime = "10:00",
                                                    custom = false,
                                                    customDays = 3,
                                                    customTime = "09:00"
                                                )
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("CalendarActivity", "임대주택 상세 정보 조회 실패: ${e.message}", e)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CalendarActivity", "북마크 동기화 실패: ${e.message}", e)
            }
        }
    }
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = "calendar",
                onNavigateHome = onNavigateHome,
                onNavigateCalendar = {},
                onNavigateBookmark = onNavigateBookmark,
                onNavigateProfile = onNavigateProfile
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
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
                    selectedDate = selectedLocalDate,
                    currentMonthDate = currentMonthDate,
                    onDateSelected = { date ->
                        selectedDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
                    },
                    onMonthChange = { date ->
                        currentMonthDate = date
                    },
                    events = allEvents,
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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = Spacing.md)
                )
                
                if (filteredEvents.isEmpty()) {
                    EmptyDeadlineCard(
                        message = "등록된 일정이 없습니다.",
                        modifier = Modifier.padding(top = Spacing.lg)
                    )
                } else {
                    filteredEvents.forEach { event ->
                        DeadlineCard(
                            event = event,
                            onDelete = {
                                selectedEventId = event.id
                                deleteDialogOpen = true
                            },
                            onEditNotification = {
                                selectedEventId = event.id
                                // 알림 설정 파싱
                                val gson = Gson()
                                val notifications = try {
                                    event.notificationSettings?.let {
                                        gson.fromJson(it, NotificationSettings::class.java)
                                    } ?: NotificationSettings()
                                } catch (e: Exception) {
                                    NotificationSettings()
                                }
                                editNotifications = notifications
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
                        selectedEventId?.let { id ->
                            // CalendarService를 통해서만 삭제 (서버 삭제 포함, 로컬 삭제도 서비스에서 처리)
                            coroutineScope.launch {
                                calendarService.removeEventFromCalendar(id)
                            }
                        }
                        deleteDialogOpen = false
                        selectedEventId = null
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
                selectedEventId?.let { id ->
                    // 알림 설정을 JSON으로 변환하여 업데이트
                    val gson = Gson()
                    val notificationJson = gson.toJson(editNotifications)
                    
                    coroutineScope.launch {
                        val event = allEvents.find { it.id == id }
                        event?.let {
                            val updatedEvent = it.copy(notificationSettings = notificationJson)
                            repository.updateEvent(updatedEvent)
                            
                            // 기존 알림 취소 후 재스케줄링
                            calendarService.removeEventFromCalendar(id)
                            calendarService.scheduleNotifications(
                                id,
                                it.title,
                                it.endDate,
                                editNotifications
                            )
                        }
                    }
                }
                notificationDialogOpen = false
                selectedEventId = null
            },
            onDismiss = {
                notificationDialogOpen = false
                selectedEventId = null
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
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CalendarCard(
    selectedDate: LocalDate,
    currentMonthDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (LocalDate) -> Unit,
    events: List<CalendarEvent>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, AppColors.Border),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            // 달력 UI
            CalendarView(
                selectedDate = selectedDate,
                events = events,
                onDateSelected = onDateSelected,
                onMonthChange = onMonthChange
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // 범례
            Column(
                modifier = Modifier.padding(top = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                LegendItem(
                    color = Color(0xFF59ABF7),
                    label = "청년정책 마감일"
                )
                LegendItem(
                    color = Color(0xFF10B981),
                    label = "임대주택 마감일"
                )
                LegendItem(
                    color = Color(0xFFFBBF24),
                    label = "오늘"
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
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
    event: CalendarEvent,
    onDelete: () -> Unit,
    onEditNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, event.endDate).toInt()
    val dateFormat = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")
    val deadlineStr = event.endDate.format(dateFormat)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, AppColors.Border),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                if (event.eventType == EventType.POLICY) {
                                    AppColors.Purple.copy(alpha = 0.1f)
                                } else {
                                    AppColors.BackgroundGradientStart.copy(alpha = 0.1f)
                                }
                            )
                            .padding(horizontal = Spacing.sm, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (event.eventType == EventType.POLICY) "정책" else "임대주택",
                            fontSize = 12.sp,
                            color = if (event.eventType == EventType.POLICY) {
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
                text = event.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.md)
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text = "${if (event.eventType == EventType.POLICY) "신청마감일" else "접수마감일"}: $deadlineStr",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                event.organization?.let {
                    Text(
                        text = "기관: $it",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 알림 태그들
                val gson = Gson()
                val notifications = remember(event.notificationSettings) {
                    try {
                        event.notificationSettings?.let {
                            gson.fromJson(it, NotificationSettings::class.java)
                        } ?: NotificationSettings()
                    } catch (e: Exception) {
                        NotificationSettings()
                    }
                }
                
                Row(
                    modifier = Modifier.padding(top = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (notifications.sevenDays) {
                        NotificationTag(
                            text = "7일전 ${notifications.sevenDaysTime}",
                            color = AppColors.Info.copy(alpha = 0.1f),
                            textColor = AppColors.Info
                        )
                    }
                    if (notifications.oneDay) {
                        NotificationTag(
                            text = "1일전 ${notifications.oneDayTime}",
                            color = AppColors.Success.copy(alpha = 0.1f),
                            textColor = AppColors.Success
                        )
                    }
                    if (notifications.custom) {
                        NotificationTag(
                            text = "${notifications.customDays}일전 ${notifications.customTime}",
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    containerColor = MaterialTheme.colorScheme.onSurface
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
            // 시간 선택기 (Wheel Picker 스타일)
            TimePickerSection(
                time = time,
                onTimeChange = onTimeChange
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
            Column(
                modifier = Modifier.padding(start = Spacing.md, top = Spacing.sm)
            ) {
                // 일 수 선택
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.padding(bottom = Spacing.sm)
                ) {
                    OutlinedTextField(
                        value = days.toString(),
                        onValueChange = { onDaysChange(it.toIntOrNull() ?: 0) },
                        modifier = Modifier.width(80.dp),
                        label = { Text("일") }
                    )
                    Text("일 전", fontSize = 14.sp)
                }
                
                // 시간 선택
                TimePickerSection(
                    time = time,
                    onTimeChange = onTimeChange
                )
            }
        }
    }
}

@Composable
private fun TimePickerSection(
    time: String,
    onTimeChange: (String) -> Unit
) {
    val parts = time.split(":").mapNotNull { it.toIntOrNull() }
    var selectedHour by remember(time) { mutableStateOf(if (parts.size == 2) parts[0] else 9) }
    var selectedMinute by remember(time) { mutableStateOf(if (parts.size == 2) parts[1] else 0) }
    
    // 값 변경 시 부모에게 알림
    LaunchedEffect(selectedHour, selectedMinute) {
        val newTime = String.format("%02d:%02d", selectedHour, selectedMinute)
        if (newTime != time) {
            onTimeChange(newTime)
        }
    }
    
    val hours = (0..23).toList()
    val minutes = (0..55 step 5).toList() // 5분 단위
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "알림 시간",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp))
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 시
                WheelPicker(
                    items = hours,
                    selectedValue = selectedHour,
                    onValueSelected = { selectedHour = it },
                    label = "시"
                )
                
                Text(
                    ":", 
                    modifier = Modifier.padding(horizontal = 8.dp),
                    fontWeight = FontWeight.Bold
                )
                
                // 분
                WheelPicker(
                    items = minutes,
                    selectedValue = selectedMinute,
                    onValueSelected = { selectedMinute = it },
                    label = "분"
                )
            }
        }
    }
}

@Composable
private fun WheelPicker(
    items: List<Int>,
    selectedValue: Int,
    onValueSelected: (Int) -> Unit,
    label: String
) {
    val itemHeight = 36.dp
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = items.indexOf(selectedValue).coerceAtLeast(0)
    )
    val density = LocalDensity.current
    
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            // 스크롤 멈춤 - 스냅
            val scrollOffset = with(density) { listState.firstVisibleItemScrollOffset.toDp() }
            val itemIndex = listState.firstVisibleItemIndex
            
            val targetIndex = if (scrollOffset < itemHeight / 2) {
                itemIndex
            } else {
                (itemIndex + 1).coerceAtMost(items.size - 1)
            }
            
            if (targetIndex >= 0 && targetIndex < items.size) {
                val value = items[targetIndex]
                if (value != selectedValue) {
                    onValueSelected(value)
                }
                listState.animateScrollToItem(targetIndex)
            }
        }
    }
    
    // 선택된 값이 외부에서 변경되면 스크롤 이동
    LaunchedEffect(selectedValue) {
        val index = items.indexOf(selectedValue)
        if (index >= 0 && !listState.isScrollInProgress && listState.firstVisibleItemIndex != index) {
            listState.scrollToItem(index)
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        Box(
            modifier = Modifier
                .height(108.dp) // 3개 항목 보이도록
                .fillMaxWidth()
        ) {
            // 선택 강조 표시
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .align(Alignment.Center)
                    .background(AppColors.LightBlue.copy(alpha = 0.1f))
            )
            
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = itemHeight), // 위아래 여백으로 중앙 정렬
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(items) { index, item ->
                    Box(
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%02d", item),
                            fontSize = if (item == selectedValue) 18.sp else 14.sp,
                            fontWeight = if (item == selectedValue) FontWeight.Bold else FontWeight.Normal,
                            color = if (item == selectedValue) AppColors.LightBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

