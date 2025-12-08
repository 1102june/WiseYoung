package com.wiseyoung.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.app.NotificationSettings
import com.example.app.data.model.HousingResponse
import com.example.app.network.NetworkModule
import com.example.app.service.CalendarService
import com.example.app.ui.theme.AppColors
import com.example.app.ui.theme.Spacing
import com.example.app.ui.theme.WiseYoungTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class HousingListActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val userId = auth.currentUser?.uid ?: "test-user"
        
        setContent {
            WiseYoungTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HousingListScreen(
                        userId = userId,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HousingListScreen(
    userId: String,
    modifier: Modifier = Modifier
) {
    var housingList by remember { mutableStateOf<List<HousingResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRecommended by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var selectedHousing by remember { mutableStateOf<HousingResponse?>(null) }
    var detailHousing by remember { mutableStateOf<HousingResponse?>(null) } // 상세 보기용
    var showDetailDialog by remember { mutableStateOf(false) } // 상세 다이얼로그 상태
    var notifications by remember {
        mutableStateOf(
            NotificationSettings(
                sevenDays = true,
                sevenDaysTime = "09:00",
                oneDay = true,
                oneDayTime = "09:00",
                custom = false,
                customDays = 3,
                customTime = "09:00"
            )
        )
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // 주택 목록 로드
    fun loadHousing() {
        isLoading = true
        errorMessage = null
        
        scope.launch {
            try {
                val response = if (showRecommended) {
                    NetworkModule.apiService.getRecommendedHousing(userId, null, null)
                } else {
                    NetworkModule.apiService.getActiveHousing(userId)
                }
                
                if (response.isSuccessful && response.body()?.success == true) {
                    housingList = response.body()?.data ?: emptyList()
                } else {
                    errorMessage = response.body()?.message ?: "주택 목록을 불러올 수 없습니다."
                }
            } catch (e: Exception) {
                errorMessage = "네트워크 오류: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // 초기 로드 및 필터 변경 시 재로드
    LaunchedEffect(showRecommended) {
        loadHousing()
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // 헤더
        TopAppBar(
            title = { Text("임대주택") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            actions = {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !showRecommended,
                        onClick = { showRecommended = false },
                        label = { Text("전체") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF9A5C), // 주황색
                            selectedLabelColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = showRecommended,
                        onClick = { showRecommended = true },
                        label = { Text("추천") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF9A5C), // 주황색
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        )
        
        // 로딩 상태
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }
        
        // 에러 상태
        if (errorMessage != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = errorMessage ?: "오류 발생",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { loadHousing() }) {
                    Text("다시 시도")
                }
            }
            return@Column
        }
        
        // 주택 목록
        if (housingList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "주택이 없습니다.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(housingList) { housing ->
                    HousingCard(
                        housing = housing,
                        userId = userId,
                        onCardClick = {
                            detailHousing = housing
                            showDetailDialog = true
                        },
                        onBookmarkClick = {
                            if (!housing.isBookmarked) {
                                selectedHousing = housing
                                showNotificationDialog = true
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Housing Detail Dialog
    if (showDetailDialog && detailHousing != null) {
        HousingDetailDialog(
            housing = detailHousing!!,
            isBookmarked = detailHousing!!.isBookmarked,
            onDismiss = { showDetailDialog = false },
            onHeartClick = {
                if (!detailHousing!!.isBookmarked) {
                    selectedHousing = detailHousing!!
                    showDetailDialog = false
                    showNotificationDialog = true
                } else {
                    // 북마크 제거
                    scope.launch {
                        try {
                            val response = NetworkModule.apiService.getBookmarks(
                                userId = userId,
                                contentType = "housing"
                            )
                            if (response.isSuccessful && response.body()?.success == true) {
                                val bookmarks = response.body()?.data ?: emptyList()
                                val contentId = detailHousing!!.housingId ?: detailHousing!!.name
                                val bookmark = bookmarks.find { it.contentId == contentId }
                                bookmark?.let {
                                    NetworkModule.apiService.deleteBookmark(
                                        userId = userId,
                                        bookmarkId = it.bookmarkId
                                    )
                                    // 주택 목록 새로고침
                                    loadHousing()
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("HousingListActivity", "서버 북마크 삭제 실패: ${e.message}", e)
                        }
                    }
                }
            },
            onApply = {
                val url = detailHousing!!.link
                if (!url.isNullOrEmpty()) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "링크를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "신청 링크가 제공되지 않았습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Notification Dialog
    val calendarService = remember { CalendarService(context) }
    
    if (showNotificationDialog && selectedHousing != null) {
        HousingNotificationDialog(
            notifications = notifications,
            onNotificationsChange = { notifications = it },
            onSave = {
                selectedHousing?.let { housing ->
                    // 북마크 추가
                    scope.launch {
                        try {
                            NetworkModule.apiService.logActivity(
                                userId,
                                com.example.app.data.model.UserActivityRequest(
                                    activityType = "BOOKMARK",
                                    contentType = "housing",
                                    contentId = housing.housingId
                                )
                            )
                        } catch (e: Exception) {
                            // 로그 실패는 무시
                        }
                    }
                    
                    // 캘린더에 일정 추가
                    val deadline = housing.applicationEnd ?: housing.applicationStart ?: ""
                    if (deadline.isNotEmpty()) {
                        calendarService.addHousingToCalendar(
                            title = housing.name,
                            organization = housing.organization,
                            deadline = deadline,
                            housingId = housing.housingId,
                            notificationSettings = notifications
                        )
                        
                        // 서버에 캘린더 일정 저장
                        scope.launch {
                            try {
                                NetworkModule.apiService.addCalendarEvent(
                                    userId = userId,
                                    request = com.example.app.data.model.CalendarEventRequest(
                                        userId = userId,
                                        title = housing.name,
                                        eventType = "housing",
                                        endDate = deadline.replace(".", "-"),
                                        isSevenDaysAlert = notifications.sevenDays,
                                        sevenDaysAlertTime = notifications.sevenDaysTime,
                                        isOneDayAlert = notifications.oneDay,
                                        oneDayAlertTime = notifications.oneDayTime,
                                        isCustomAlert = notifications.custom,
                                        customAlertDays = notifications.customDays,
                                        customAlertTime = notifications.customTime
                                    )
                                )
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    }
                    
                    // 서버에 북마크 저장
                    scope.launch {
                        try {
                            NetworkModule.apiService.addBookmark(
                                userId = userId,
                                request = com.example.app.data.model.BookmarkRequest(
                                    userId = userId,
                                    contentType = "housing",
                                    contentId = housing.housingId
                                )
                            )
                        } catch (e: Exception) {
                            // ignore
                        }
                    }

                    // 로컬 북마크 저장
                    com.wiseyoung.app.BookmarkPreferences.addBookmark(
                        context,
                        com.wiseyoung.app.BookmarkItem(
                            id = housing.housingId.hashCode(), // Int ID 생성
                            type = com.wiseyoung.app.BookmarkType.HOUSING,
                            title = housing.name,
                            organization = housing.organization,
                            address = housing.address,
                            deposit = housing.deposit?.let { "${it / 10000}만원" },
                            monthlyRent = housing.monthlyRent?.let { "${it / 10000}만원" },
                            area = housing.supplyArea?.let { "${it}㎡" },
                            completionDate = housing.completeDate,
                            distance = housing.distanceFromUser?.let { "${(it / 1000).toInt()}km" },
                            deadline = deadline
                        )
                    )
                }
                showNotificationDialog = false
                selectedHousing = null
            },
            onDismiss = {
                showNotificationDialog = false
                selectedHousing = null
            }
        )
    }
}

@Composable
fun HousingCard(
    housing: HousingResponse,
    userId: String,
    onCardClick: () -> Unit,
    onBookmarkClick: () -> Unit = {}
) {
    var isBookmarked by remember { mutableStateOf(housing.isBookmarked) }
    val scope = rememberCoroutineScope()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // 상세 화면 표시
                onCardClick()
                
                // 사용자 활동 로그: VIEW
                scope.launch {
                    try {
                        NetworkModule.apiService.logActivity(
                            userId,
                            com.example.app.data.model.UserActivityRequest(
                                activityType = "VIEW",
                                contentType = "housing",
                                contentId = housing.housingId
                            )
                        )
                    } catch (e: Exception) {
                        // 로그 실패는 무시
                    }
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = housing.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    housing.address?.let { address ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "위치",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = address,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                IconButton(
                    onClick = {
                        if (!isBookmarked) {
                            // 좋아요 추가 시 알림 설정 다이얼로그 표시
                            onBookmarkClick()
                        } else {
                            // 좋아요 해제
                            isBookmarked = false
                            scope.launch {
                                try {
                                    NetworkModule.apiService.logActivity(
                                        userId,
                                        com.example.app.data.model.UserActivityRequest(
                                            activityType = "UNBOOKMARK",
                                            contentType = "housing",
                                            contentId = housing.housingId
                                        )
                                    )
                                } catch (e: Exception) {
                                    // 로그 실패는 무시
                                }
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isBookmarked) 
                            Icons.Default.Bookmark 
                        else 
                            Icons.Default.BookmarkBorder,
                        contentDescription = if (isBookmarked) "북마크 해제" else "북마크",
                        tint = if (isBookmarked) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 가격 정보
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                housing.deposit?.let { deposit ->
                    Column {
                        Text(
                            text = "보증금",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${deposit / 10000}만원",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                housing.monthlyRent?.let { rent ->
                    Column {
                        Text(
                            text = "월세",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${rent / 10000}만원",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                housing.supplyArea?.let { area ->
                    Column {
                        Text(
                            text = "면적",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${area}㎡",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // 신청 기간
            housing.applicationEnd?.let { endDate ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "신청 마감: $endDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HousingDetailDialog(
    housing: HousingResponse,
    isBookmarked: Boolean,
    onDismiss: () -> Unit,
    onHeartClick: () -> Unit,
    onApply: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f), // 정책과 동일한 크기
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "임대주택 상세 정보",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // 오른쪽 상단 버튼들
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                        IconButton(onClick = onHeartClick) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isBookmarked) "북마크 해제" else "북마크",
                                tint = if (isBookmarked) Color(0xFFEF4444) else AppColors.TextTertiary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                Text(
                    text = housing.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                
                Spacer(modifier = Modifier.height(Spacing.lg))
                
                housing.address?.let {
                    DetailRow("위치 / 주소", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                if (housing.deposit != null && housing.monthlyRent != null) {
                    DetailRow("가격", "보증금 ${housing.deposit / 10000}만원 / 월세 ${housing.monthlyRent / 10000}만원")
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                housing.supplyArea?.let {
                    DetailRow("공급전용면적", "${it}㎡")
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                housing.completeDate?.let {
                    DetailRow("준공날짜", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                housing.organization?.let {
                    DetailRow("기관명", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                housing.applicationEnd?.let {
                    DetailRow("마감날짜", it)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = AppColors.TextTertiary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = AppColors.TextPrimary,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun HousingNotificationDialog(
    notifications: NotificationSettings,
    onNotificationsChange: (NotificationSettings) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var localNotifications by remember { mutableStateOf(notifications) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { 
            Text(
                "알림 설정",
                color = Color(0xFF59ABF7)
            ) 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                HousingNotificationSettingRow(
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
                
                HousingNotificationSettingRow(
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
                
                HousingCustomNotificationRow(
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
                    containerColor = Color(0xFF59ABF7)
                )
            ) {
                Text("저장하기", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = Color(0xFF59ABF7))
            }
        }
    )
}

@Composable
private fun HousingNotificationSettingRow(
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
            HousingTimePickerSection(
                time = time,
                onTimeChange = onTimeChange
            )
        }
    }
}

@Composable
private fun HousingCustomNotificationRow(
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
                
                HousingTimePickerSection(
                    time = time,
                    onTimeChange = onTimeChange
                )
            }
        }
    }
}

@Composable
private fun HousingTimePickerSection(
    time: String,
    onTimeChange: (String) -> Unit
) {
    val parts = time.split(":").mapNotNull { it.toIntOrNull() }
    var selectedHour by remember(time) { mutableStateOf(if (parts.size == 2) parts[0] else 9) }
    var selectedMinute by remember(time) { mutableStateOf(if (parts.size == 2) parts[1] else 0) }

    LaunchedEffect(selectedHour, selectedMinute) {
        val newTime = String.format("%02d:%02d", selectedHour, selectedMinute)
        if (newTime != time) {
            onTimeChange(newTime)
        }
    }

    val hours = (0..23).toList()
    val minutes = (0..55 step 5).toList()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "알림 시간",
            fontSize = 12.sp,
            color = AppColors.TextSecondary
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
                HousingWheelPicker(
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

                HousingWheelPicker(
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
private fun HousingWheelPicker(
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
                listState.scrollToItem(targetIndex)
            }
        }
    }

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
                .height(108.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .align(Alignment.Center)
                    .background(AppColors.LightBlue.copy(alpha = 0.1f))
            )

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = itemHeight),
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
                            color = if (item == selectedValue) AppColors.LightBlue else AppColors.TextSecondary
                        )
                    }
                }
            }
        }
        Text(label, fontSize = 12.sp, color = AppColors.TextSecondary)
    }
}
