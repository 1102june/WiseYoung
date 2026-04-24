package com.wiseyoung.pro

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.wiseyoung.pro.ui.theme.AppColors
import com.wiseyoung.pro.ui.theme.Spacing
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var showDetailDialog by remember { mutableStateOf(false) }
    var showChatbotDialog by remember { mutableStateOf(false) }

    // onNavigateChatbot이 호출되면 챗봇 다이얼로그 열기
    val handleChatbotClick = { showChatbotDialog = true }
    var detailPolicy by remember { mutableStateOf<PolicyRecommendation?>(null) }
    var isTransitioning by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 챗봇 버튼 위치 상태 (드래그 가능하게 만들기 위해)
    val density = LocalDensity.current
    var chatbotOffsetX by remember { mutableStateOf(0f) }
    var chatbotOffsetY by remember { mutableStateOf(0f) }

    // API 데이터
    var aiRecommendationsList by remember { mutableStateOf<List<PolicyRecommendation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }


    // 메인 페이지 데이터 로드 (AI 추천 정책은 항상 서버 데이터만 사용)
    LaunchedEffect(userId) {
        if (userId != null) {
            isLoading = true
            errorMessage = null
            try {
                val response = com.wiseyoung.pro.network.NetworkModule.apiService.getMainPage(userId)
                android.util.Log.d("HomeScreen", "getMainPage 응답: isSuccessful=${response.isSuccessful}, success=${response.body()?.success}")
                if (response.isSuccessful && response.body()?.success == true) {
                    val mainPageData = response.body()?.data
                    android.util.Log.d("HomeScreen", "mainPageData: ${mainPageData != null}, aiRecommendedPolicies: ${mainPageData?.aiRecommendedPolicies?.size ?: 0}개")
                    mainPageData?.aiRecommendedPolicies?.let { recommendations ->
                        android.util.Log.d("HomeScreen", "AI 추천 정책 ${recommendations.size}개 수신")
                        // AIRecommendationResponse를 PolicyRecommendation으로 변환 (Top 5개만)
                        aiRecommendationsList = recommendations.take(5).mapNotNull { rec ->
                            rec.policy?.let { policy ->
                                PolicyRecommendation(
                                    id = rec.recId?.toInt() ?: 0,
                                    title = policy.title,
                                    date = "${policy.ageStart ?: 0}-${policy.ageEnd ?: 0}세 ${
                                        policy.applicationEnd?.take(
                                            10
                                        )?.replace("-", ".") ?: ""
                                    }",
                                    organization = policy.region ?: "",
                                    age = "만 ${policy.ageStart ?: 0}세 ~ ${policy.ageEnd ?: 0}세",
                                    period = "${
                                        policy.applicationStart?.take(10)?.replace("-", ".") ?: ""
                                    } ~ ${
                                        policy.applicationEnd?.take(10)?.replace("-", ".") ?: ""
                                    }",
                                    content = policy.summary ?: "",
                                    applicationMethod = policy.link1 ?: "",
                                    deadline = policy.applicationEnd?.take(10) ?: ""
                                )
                            }
                        }
                        android.util.Log.d("HomeScreen", "변환된 추천 정책 ${aiRecommendationsList.size}개")
                    } ?: run {
                        android.util.Log.w("HomeScreen", "aiRecommendedPolicies가 null이거나 비어있음")
                        aiRecommendationsList = emptyList()
                    }
                } else {
                    android.util.Log.e("HomeScreen", "getMainPage 실패: ${response.body()?.message}")
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

    // 시스템 뒤로가기 버튼 처리
    BackHandler(onBack = onBack)

    // 자동 슬라이드 (4.5초마다)
    LaunchedEffect(isExpanded, aiRecommendationsList.size) {
        if (aiRecommendationsList.isNotEmpty()) {
            while (!isExpanded) {
                delay(4500) // 4.5초로 변경
                if (!isExpanded && aiRecommendationsList.isNotEmpty()) {
                    isTransitioning = true
                    delay(300)
                    currentIndex = (currentIndex + 1) % aiRecommendationsList.size
                    isTransitioning = false
                }
            }
        }
    }

    // Scaffold 제거 -> MainActivity에서 처리함
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val boxWidth = constraints.maxWidth.toFloat()
            val boxHeight = constraints.maxHeight.toFloat()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                HomeHeader(
                    onBack = onBack,
                    onNotifications = onNavigateNotifications
                )

                // Main Content
                Column(
                modifier = Modifier.fillMaxWidth()
                ) {
                    // AI Recommendations Section
                    Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = Spacing.lg, horizontal = Spacing.screenHorizontal),
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
                            color = if (MaterialTheme.colorScheme.background == Color(0xFF121212) || MaterialTheme.colorScheme.background == Color(0xFF0D1A2A)) Color.White else AppColors.TextSecondary
                        )

                        // 슬라이드 카드
                        if (currentPolicy != null) {
                            PolicyCard(
                                policy = currentPolicy,
                                isTransitioning = isTransitioning,
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
                                            currentIndex =
                                                (currentIndex - 1 + aiRecommendationsList.size) % aiRecommendationsList.size
                                            isTransitioning = false
                                        }
                                    }
                                },
                                onNext = {
                                    if (aiRecommendationsList.isNotEmpty()) {
                                        isTransitioning = true
                                        coroutineScope.launch {
                                            delay(300)
                                            currentIndex =
                                                (currentIndex + 1) % aiRecommendationsList.size
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
                                }
                            )
                        } else {
                            Text(
                                text = "현재 추천할 정책이 없습니다.",
                                fontSize = 14.sp,
                                color = AppColors.TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.lg))

                // 바로가기 카드들 (패딩 추가)
                    Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
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

} // End of HomeScreen

// NotificationSettings는 com.example.app.NotificationSettings를 사용

@Composable
private fun HomeHeader(
    onBack: () -> Unit,
    onNotifications: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
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
                    tint = AppColors.TextPrimary
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
                    .border(2.dp, AppColors.TextPrimary, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    modifier = Modifier.size(20.dp), // 내부 아이콘 크기 축소
                    tint = AppColors.TextPrimary
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
        currentIndex = 0,
        totalCount = 5,
        onShowDetail = {},
        onPrevious = {},
        onNext = {},
        onIndicatorClick = {}
    )
}

@Composable
private fun PolicyCard(
        policy: PolicyRecommendation,
        isTransitioning: Boolean,
        currentIndex: Int,
        totalCount: Int,
        onShowDetail: () -> Unit,
        onPrevious: () -> Unit,
        onNext: () -> Unit,
        onIndicatorClick: (Int) -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (isTransitioning) 0f else 1f,
        animationSpec = tween(300),
        label = "card_alpha"
    )
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onShowDetail() }, // 카드 전체 클릭 시 상세보기 호출
        colors = CardDefaults.cardColors(containerColor = Color(0xFFDEDBB4)) // AI 추천 카드 색상 #dedbb4
    ) {
        // 카드 배경 이미지와 콘텐츠를 함께 배치
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            // 콘텐츠 (텍스트, 버튼 등)
        Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.md)
        ) {
            // 상단: 제목
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(alpha)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOP${currentIndex + 1}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.LightBlue
                    )
                    Text(
                        text = policy.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 연령, 신청기간, 정책내용 추가
                Text(
                    text = "연령: ${policy.age}",
                    fontSize = 12.sp,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "신청기간: ${policy.period}",
                    fontSize = 12.sp,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "정책내용: ${policy.content}",
                    fontSize = 12.sp,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

                Spacer(modifier = Modifier.weight(1f))

                // 네비게이션 버튼과 인디케이터
            Box(
                    modifier = Modifier.fillMaxWidth()
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
                                    AppColors.Border,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous",
                                tint = AppColors.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                    Color.White.copy(alpha = 0.7f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next",
                                tint = AppColors.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 인디케이터
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                            .padding(bottom = Spacing.sm),
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
                                        if (index == currentIndex) Color.White else Color.White.copy(alpha = 0.5f)
                                )
                                .clickable { onIndicatorClick(index) }
                        )
                    }
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
                    // 정책 제목 (왼쪽, 20.sp로 축소)
                    Text(
                        text = policy.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    
                    // 오른쪽 상단 닫기 버튼
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.lg))
                Spacer(modifier = Modifier.height(Spacing.sm))
                HomePolicyDetailRow("연령", policy.age)
                Spacer(modifier = Modifier.height(Spacing.sm))
                HomePolicyDetailRow("신청기간", policy.period)
                Spacer(modifier = Modifier.height(Spacing.sm))
                HomePolicyDetailRow("정책내용", policy.content)
                Spacer(modifier = Modifier.height(Spacing.sm))
                HomePolicyDetailRow("신청방법", policy.applicationMethod)

                Spacer(modifier = Modifier.height(Spacing.xl))

                Button(
                    onClick = onApply,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF59ABF7) // 메인 컬러로 변경
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp) // 패딩 직접 지정
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

@Composable
private fun HomeCategoryTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF59ABF7).copy(alpha = 0.1f))
            .padding(horizontal = Spacing.sm, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(0xFF59ABF7),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HomeSupportTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.BackgroundGradientStart.copy(alpha = 0.1f))
            .padding(horizontal = Spacing.sm, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = AppColors.BackgroundGradientStart,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HomePolicyDetailRow(label: String, value: String) {
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


