package com.wiseyoung.app

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
import com.example.app.service.CalendarService
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.app.network.NetworkModule

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

val aiRecommendations = listOf(
    PolicyRecommendation(
        id = 1,
        title = "청년지원금",
        date = "18-25세 2025.4.30",
        organization = "고용노동부",
        age = "만 18세 ~ 25세",
        period = "2025.03.01 ~ 2025.04.30",
        content = "취업 준비 중인 청년에게 월 50만원의 지원금을 최대 6개월간 지원하는 정책입니다.",
        applicationMethod = "고용센터 방문 또는 온라인 신청",
        deadline = "2025.04.30"
    ),
    PolicyRecommendation(
        id = 2,
        title = "청년 창업 지원금",
        date = "20-39세 2025.5.15",
        organization = "중소벤처기업부",
        age = "만 20세 ~ 39세",
        period = "2025.04.01 ~ 2025.05.15",
        content = "창업 초기 기업에게 사업화 자금 및 멘토링을 지원하는 프로그램입니다.",
        applicationMethod = "K-Startup 홈페이지에서 온라인 신청",
        deadline = "2025.05.15"
    ),
    PolicyRecommendation(
        id = 3,
        title = "청년 주거 안정 지원",
        date = "19-34세 2025.6.30",
        organization = "국토교통부",
        age = "만 19세 ~ 34세",
        period = "2025.05.01 ~ 2025.06.30",
        content = "청년 전월세 보증금 및 월세를 지원하여 주거 안정을 도모합니다.",
        applicationMethod = "복지로 홈페이지 또는 주민센터 방문 신청",
        deadline = "2025.06.30"
    ),
    PolicyRecommendation(
        id = 4,
        title = "청년 일자리 도약 장려금",
        date = "18-34세 2025.05.15",
        organization = "경기도",
        age = "만 18세 ~ 34세",
        period = "2025.02.01 ~ 2025.05.15",
        content = "중소·중견기업에 취업한 청년에게 3년간 최대 1,200만원의 장려금을 지원합니다.",
        applicationMethod = "경기일자리재단 홈페이지에서 온라인 신청",
        deadline = "2025.05.15"
    ),
    PolicyRecommendation(
        id = 5,
        title = "청년 전월세 보증금 대출",
        date = "19-34세 2025.12.31",
        organization = "주택도시기금",
        age = "만 19세 ~ 34세",
        period = "2025.01.01 ~ 2025.12.31",
        content = "전월세 보증금 마련이 어려운 청년에게 최대 1억원까지 저금리로 대출해주는 상품입니다.",
        applicationMethod = "금융기관 방문 또는 주택도시기금 홈페이지 신청",
        deadline = "2025.12.31"
    )
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
    var showChatbotDialog by remember { mutableStateOf(false) }
    var selectedPolicy by remember { mutableStateOf<PolicyRecommendation?>(null) }
    var isTransitioning by remember { mutableStateOf(false) }
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
    
    // 메인 페이지 데이터 로드
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
                                    id = rec.recId.toInt(),
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
                    // 데이터가 없으면 기본 데이터 사용
                    if (aiRecommendationsList.isEmpty()) {
                        aiRecommendationsList = aiRecommendations
                    }
                } else {
                    errorMessage = response.body()?.message ?: "데이터를 불러올 수 없습니다."
                    // 에러 시 기본 데이터 사용
                    aiRecommendationsList = aiRecommendations
                }
            } catch (e: Exception) {
                errorMessage = "네트워크 오류: ${e.message}"
                // 에러 시 기본 데이터 사용
                aiRecommendationsList = aiRecommendations
            } finally {
                isLoading = false
            }
        } else {
            // userId가 없으면 기본 데이터 사용
            aiRecommendationsList = aiRecommendations
            isLoading = false
        }
    }
    
    if (aiRecommendationsList.isEmpty() && !isLoading) {
        aiRecommendationsList = aiRecommendations
    }
    
    val currentPolicy = if (aiRecommendationsList.isNotEmpty()) {
        aiRecommendationsList[currentIndex % aiRecommendationsList.size]
    } else {
        aiRecommendations[0]
    }
    val isBookmarked = bookmarkedPolicies.contains(currentPolicy.title)
    
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
                .background(Color.White)
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
                        color = AppColors.TextPrimary
                    )
                    
                    Text(
                        text = "AI 추천 정책 모음",
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary
                    )
                    
                    // 슬라이드 카드
                    PolicyCard(
                        policy = currentPolicy,
                        isExpanded = isExpanded,
                        isTransitioning = isTransitioning,
                        isBookmarked = isBookmarked,
                        currentIndex = currentIndex,
                        totalCount = aiRecommendationsList.size,
                        onToggleExpanded = { isExpanded = !isExpanded },
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
                                bookmarkedPolicies = bookmarkedPolicies - currentPolicy.title
                            }
                        },
                        onApply = {}
                    )
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
    
    // 알림 설정 다이얼로그
    val context = LocalContext.current
    val calendarService = remember { CalendarService(context) }
    
    if (showNotificationDialog) {
        NotificationDialog(
            notifications = notifications,
            onNotificationsChange = { notifications = it },
            onSave = {
                selectedPolicy?.let { policy ->
                    // 북마크 추가
                    bookmarkedPolicies = bookmarkedPolicies + policy.title
                    
                    // 캘린더에 일정 추가
                    calendarService.addPolicyToCalendar(
                        title = policy.title,
                        organization = policy.organization,
                        deadline = policy.deadline,
                        policyId = policy.id.toString(),
                        notificationSettings = notifications
                    )
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
            
            Image(
                painter = painterResource(id = R.drawable.wy_logo),
                contentDescription = "WY Logo",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            IconButton(
                onClick = onNotifications,
                modifier = Modifier
                    .size(48.dp)
                    .border(2.dp, AppColors.TextPrimary, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    modifier = Modifier.size(24.dp),
                    tint = AppColors.TextPrimary
                )
            }
        }
    }
}

@Composable
private fun PolicyCard(
    policy: PolicyRecommendation,
    isExpanded: Boolean,
    isTransitioning: Boolean,
    isBookmarked: Boolean,
    currentIndex: Int,
    totalCount: Int,
    onToggleExpanded: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onIndicatorClick: (Int) -> Unit,
    onHeartClick: () -> Unit,
    onApply: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (isTransitioning) 0f else 1f,
        animationSpec = tween(300),
        label = "card_alpha"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, AppColors.TextPrimary),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
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
                        color = AppColors.TextPrimary
                    )
                    
                    if (!isExpanded) {
                        Text(
                            text = policy.date,
                            fontSize = 14.sp,
                            color = AppColors.TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier.padding(top = Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            PolicyDetailRow("주관기관명", policy.organization)
                            PolicyDetailRow("정책명", policy.title)
                            PolicyDetailRow("연령", policy.age)
                            PolicyDetailRow("신청기간", policy.period)
                            PolicyDetailRow("정책내용", policy.content)
                            PolicyDetailRow("신청방법", policy.applicationMethod)
                        }
                    }
                }
                
                // 좋아요 버튼
                IconButton(
                    onClick = onHeartClick,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) AppColors.TextPrimary else AppColors.TextTertiary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            // 네비게이션 버튼 (펼쳐지지 않은 상태에서만)
            if (!isExpanded) {
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
                                    AppColors.TextPrimary.copy(alpha = 0.7f),
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
                                    AppColors.TextPrimary.copy(alpha = 0.7f),
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
                                        if (index == currentIndex) AppColors.TextPrimary else AppColors.Border
                                    )
                                    .clickable { onIndicatorClick(index) }
                            )
                        }
                    }
                }
            }
            
            // 버튼
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                if (!isExpanded) {
                    Button(
                        onClick = onToggleExpanded,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.TextPrimary
                        )
                    ) {
                        Text("상세보기", color = Color.White)
                    }
                } else {
                    Button(
                        onClick = onToggleExpanded,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Border
                        )
                    ) {
                        Text("닫아두기", color = AppColors.TextPrimary)
                    }
                    
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
                                            AppColors.LightBlue,  // 라이트 블루 (메인 컬러)
                                            AppColors.Orange  // 오렌지 (포인트 컬러)
                                        )
                                    )
                                )
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                "신청하기",
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
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
            color = AppColors.TextPrimary,
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
            // TimePicker는 간단하게 TextField로 대체
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

