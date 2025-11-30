package com.wiseyoung.app

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.tooling.preview.Preview
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.app.NotificationSettings
import com.example.app.ui.theme.AppColors
import com.example.app.ui.theme.Spacing
import com.example.app.ui.components.BottomNavigationBar
import com.example.app.ui.components.ElevatedCard
import com.example.app.service.CalendarService
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.app.network.NetworkModule
import com.wiseyoung.app.BookmarkItem
import com.wiseyoung.app.BookmarkType
import com.wiseyoung.app.BookmarkPreferences

data class PolicyRecommendation(
    val id: Int,
    val title: String,
    val date: String,
    val organization: String,
    val age: String,
    val period: String,
    val content: String,
    val applicationMethod: String,
    val deadline: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userId: String?,
    onNavigateNotifications: () -> Unit,
    onNavigatePolicy: () -> Unit = {},
    onNavigateHousing: () -> Unit = {},
    onNavigateCalendar: () -> Unit = {},
    onNavigateBookmark: () -> Unit = {},
    onNavigateProfile: () -> Unit = {},
    onNavigateChatbot: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var currentIndex by remember { mutableStateOf(0) }
    var isExpanded by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var showChatbotDialog by remember { mutableStateOf(false) }
    var selectedPolicy by remember { mutableStateOf<PolicyRecommendation?>(null) }
    var detailPolicy by remember { mutableStateOf<PolicyRecommendation?>(null) }
    var isTransitioning by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var bookmarkedPolicies by remember { mutableStateOf(setOf<String>()) }
    val coroutineScope = rememberCoroutineScope()
    
    // API 데이터
    var aiRecommendationsList by remember { mutableStateOf<List<PolicyRecommendation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var notifications by remember {
        mutableStateOf(
            NotificationSettings(
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
    
    // 메인 페이지 데이터 로드 (AI 추천 정책은 항상 서버 데이터만 사용)
    LaunchedEffect(userId) {
        if (userId != null) {
            isLoading = true
            errorMessage = null
            try {
                val response = com.example.app.network.NetworkModule.apiService.getMainPage(userId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val mainPageData = response.body()?.data
                    mainPageData?.aiRecommendedPolicies?.let { recommendations ->
                        // AIRecommendationResponse를 PolicyRecommendation으로 변환
                        aiRecommendationsList = recommendations.mapNotNull { rec ->
                            rec.policy?.let { policy ->
                                PolicyRecommendation(
                                    id = rec.recId?.toInt() ?: 0,
                                    title = policy.title,
                                    date = "${policy.ageStart ?: 0}-${policy.ageEnd ?: 0}세 ${policy.applicationEnd?.take(10)?.replace("-", ".") ?: ""}",
                                    organization = policy.region ?: "",
                                    age = "만 ${policy.ageStart ?: 0}세 ~ ${policy.ageEnd ?: 0}세",
                                    period = "${policy.applicationStart?.take(10)?.replace("-", ".") ?: ""} ~ ${policy.applicationEnd?.take(10)?.replace("-", ".") ?: ""}",
                                    content = policy.summary ?: "",
                                    applicationMethod = policy.link1 ?: "",
                                    deadline = policy.applicationEnd?.take(10) ?: ""
                                )
                            }
                        }
                    }
                } else {
                    errorMessage = response.body()?.message ?: "데이터를 불러올 수 없습니다."
                }
            } catch (e: Exception) {
                errorMessage = "네트워크 오류: ${e.message}"
            } finally {
                isLoading = false
            }
        } else {
            // userId가 없으면 추천 데이터를 불러올 수 없음
            isLoading = false
        }
    }
    
    val currentPolicy = aiRecommendationsList.getOrNull(
        if (aiRecommendationsList.isNotEmpty()) currentIndex % aiRecommendationsList.size else 0
    )
    val isBookmarked = currentPolicy?.let { bookmarkedPolicies.contains(it.title) } ?: false
    
    // 시스템 뒤로가기 버튼 처리
    BackHandler(onBack = onBack)
    
    // 자동 슬라이드 (3초마다)
    LaunchedEffect(isExpanded, aiRecommendationsList.size) {
        if (aiRecommendationsList.isNotEmpty()) {
            while (!isExpanded) {
                delay(3000)
                if (!isExpanded && aiRecommendationsList.isNotEmpty()) {
                    isTransitioning = true
                    delay(300)
                    currentIndex = (currentIndex + 1) % aiRecommendationsList.size
                    isTransitioning = false
                }
            }
        }
    }
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = "home",
                onNavigateHome = {},
                onNavigateCalendar = onNavigateCalendar,
                onNavigateChatbot = { showChatbotDialog = true },
                onNavigateBookmark = onNavigateBookmark,
                onNavigateProfile = onNavigateProfile
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            HomeHeader(
                onBack = onBack,
                onNotifications = onNavigateNotifications
            )
            
            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg)
            ) {
                // AI Recommendations Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Text(
                        text = "나와 비슷한 다른사람은 어떤한 정책을?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "AI 추천 정책 모음",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // 슬라이드 카드
                    if (currentPolicy != null) {
                        PolicyCard(
                            policy = currentPolicy,
                            isTransitioning = isTransitioning,
                            isBookmarked = isBookmarked,
                            currentIndex = currentIndex,
                            totalCount = aiRecommendationsList.size,
                            onShowDetail = {
                                detailPolicy = currentPolicy
                                showDetailDialog = true
                            },
                            onPrevious = {
                                if (aiRecommendationsList.isNotEmpty()) {
                                    isTransitioning = true
                                    coroutineScope.launch {
                                        delay(300)
                                        currentIndex = (currentIndex - 1 + aiRecommendationsList.size) % aiRecommendationsList.size
                                        isTransitioning = false
                                    }
                                }
                            },
                            onNext = {
                                if (aiRecommendationsList.isNotEmpty()) {
                                    isTransitioning = true
                                    coroutineScope.launch {
                                        delay(300)
                                        currentIndex = (currentIndex + 1) % aiRecommendationsList.size
                                        isTransitioning = false
                                    }
                                }
                            },
                            onIndicatorClick = { index ->
                                isTransitioning = true
                                coroutineScope.launch {
                                    delay(300)
                                    currentIndex = index
                                    isTransitioning = false
                                }
                            },
                            onHeartClick = {
                                if (!isBookmarked) {
                                    selectedPolicy = currentPolicy
                                    showNotificationDialog = true
                                } else {
                                    // 북마크 제거 (로컬 상태)
                                    bookmarkedPolicies = bookmarkedPolicies - currentPolicy.title
                                    // SharedPreferences에서도 제거
                                    BookmarkPreferences.removeBookmark(
                                        context,
                                        currentPolicy.title,
                                        BookmarkType.POLICY
                                    )
                                }
                            }
                        )
                    } else {
                        Text(
                            text = "현재 추천할 정책이 없습니다.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.lg))
                
                // 바로가기 카드들
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    QuickAccessCard(
                        title = "맞춤 청년정책",
                        subtitle = "나에게 딱 맞는 정책 찾기",
                        icon = Icons.Default.CalendarToday,
                        gradientColors = listOf(
                            Color(0xFF59ABF7),  // 라이트 블루 (메인 컬러)
                            Color(0xFF4A8FD9)  // 진한 블루
                        ),
                        onClick = onNavigatePolicy
                    )
                    
                    QuickAccessCard(
                        title = "맞춤 임대주택",
                        subtitle = "내 주변 임대주택 찾기",
                        icon = Icons.Default.Home,
                        gradientColors = listOf(
                            Color(0xFFFF9A5C),
                            Color(0xFFFF6B2C)
                        ),
                        onClick = onNavigateHousing
                    )
                }
            }
        }
    }
    
    // Policy Detail Dialog (팝업)
    if (showDetailDialog && detailPolicy != null) {
        PolicyDetailDialog(
            policy = detailPolicy!!,
            onDismiss = { showDetailDialog = false },
            onApply = {
                val url = detailPolicy!!.applicationMethod
                if (url.isNotEmpty()) {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }.onFailure {
                        // 링크가 유효하지 않으면 토스트 메시지 등 처리
                    }
                }
            }
        )
    }
    
    // 알림 설정 다이얼로그
    val calendarService = remember { CalendarService(context) }
    
    if (showNotificationDialog) {
        NotificationDialog(
            notifications = notifications,
            onNotificationsChange = { notifications = it },
            onSave = {
                selectedPolicy?.let { policy ->
                    // ... (기존 저장 로직)
                }
                showNotificationDialog = false
                selectedPolicy = null
            },
            onDismiss = {
                showNotificationDialog = false
                selectedPolicy = null
            }
        )
    }
    
    // 챗봇 다이얼로그
    ChatbotDialog(
        isOpen = showChatbotDialog,
        onClose = { showChatbotDialog = false },
        context = ChatbotContext.NONE
    )
}

// NotificationSettings는 com.example.app.NotificationSettings를 사용

@Composable
private fun HomeHeader(
    onBack: () -> Unit,
    onNotifications: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm), // 패딩 축소
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    modifier = Modifier.size(28.dp), // 아이콘 크기 약간 축소
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Image(
                painter = painterResource(id = R.drawable.wy_logo),
                contentDescription = "WY Logo",
                modifier = Modifier
                    .size(48.dp) // 로고 크기 축소 (64dp -> 48dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            IconButton(
                onClick = onNotifications,
                modifier = Modifier
                    .size(40.dp) // 버튼 크기 축소
                    .border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    modifier = Modifier.size(20.dp), // 내부 아이콘 크기 축소
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PolicyCardPreview() {
    val samplePolicy = PolicyRecommendation(
        id = 1,
        title = "청년 월세 지원",
        date = "2023.01.01 ~ 2023.12.31",
        organization = "서울시",
        age = "만 19세 ~ 34세",
        period = "2023년 연중",
        content = "월 20만원 지원",
        applicationMethod = "온라인 신청",
        deadline = "2023-12-31"
    )
    
    PolicyCard(
        policy = samplePolicy,
        isTransitioning = false,
        isBookmarked = false,
        currentIndex = 0,
        totalCount = 5,
        onShowDetail = {},
        onPrevious = {},
        onNext = {},
        onIndicatorClick = {},
        onHeartClick = {}
    )
}

@Composable
private fun PolicyCard(
    policy: PolicyRecommendation,
    isTransitioning: Boolean,
    isBookmarked: Boolean,
    currentIndex: Int,
    totalCount: Int,
    onShowDetail: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onIndicatorClick: (Int) -> Unit,
    onHeartClick: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (isTransitioning) 0f else 1f,
        animationSpec = tween(300),
        label = "card_alpha"
    )
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // 배경색 흰색으로 명시적 지정
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 상단: 좋아요 버튼과 제목
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 48.dp)
                        .alpha(alpha)
                ) {
                    Text(
                        text = policy.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = policy.date,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                // 좋아요 버튼
                IconButton(
                    onClick = onHeartClick,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.onSurface else AppColors.TextTertiary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            // 네비게이션 버튼
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = Spacing.md)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // 인디케이터
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = Spacing.md),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(totalCount) { index ->
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (index == currentIndex) 32.dp else 6.dp)
                                .padding(horizontal = 3.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (index == currentIndex) MaterialTheme.colorScheme.onSurface else AppColors.Border
                                )
                                .clickable { onIndicatorClick(index) }
                        )
                    }
                }
            }
            
            // 버튼 (오른쪽 하단 배치)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onShowDetail,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("상세보기", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun PolicyDetailDialog(
    policy: PolicyRecommendation,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        text = "정책 상세 정보",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.md))
                
                Text(
                    text = policy.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(Spacing.lg))
                
                PolicyDetailRow("주관기관명", policy.organization)
                Spacer(modifier = Modifier.height(Spacing.sm))
                PolicyDetailRow("연령", policy.age)
                Spacer(modifier = Modifier.height(Spacing.sm))
                PolicyDetailRow("신청기간", policy.period)
                Spacer(modifier = Modifier.height(Spacing.sm))
                PolicyDetailRow("정책내용", policy.content)
                Spacer(modifier = Modifier.height(Spacing.sm))
                PolicyDetailRow("신청방법", policy.applicationMethod)
                
                Spacer(modifier = Modifier.height(Spacing.xl))
                
                Button(
                    onClick = onApply,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        AppColors.LightBlue,
                                        AppColors.Orange
                                    )
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            "신청하기",
                            color = Color.White,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PolicyDetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = AppColors.TextTertiary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun QuickAccessCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(gradientColors),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(Spacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun NotificationDialog(
    notifications: NotificationSettings,
    onNotificationsChange: (NotificationSettings) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var localNotifications by remember { mutableStateOf(notifications) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("알림 설정") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // 7일전 알림
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
                
                // 1일전 알림
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
                
                // 사용자 지정 알림
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
        containerColor = Color.White, // 팝업창 배경 흰색으로 변경
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
                            color = if (item == selectedValue) AppColors.LightBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

