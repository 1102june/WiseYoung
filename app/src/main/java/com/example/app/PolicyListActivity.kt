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
import androidx.compose.ui.graphics.Brush
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
import com.example.app.service.CalendarService
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*

data class PolicyItem(
    val id: Int,
    val title: String,
    val date: String,
    val category: String,
    val support: String,
    val isFavorite: Boolean,
    val organization: String,
    val age: String,
    val period: String,
    val content: String,
    val applicationMethod: String,
    val deadline: String,
    val isUrgent: Boolean
)

val allPolicies = listOf(
    PolicyItem(
        id = 1,
        title = "청년 월세 한시 특별지원",
        date = "19-34세 2025.03.31",
        category = "주거",
        support = "월 20만원",
        isFavorite = true,
        organization = "경기도 수원시",
        age = "만 19세 ~ 34세",
        period = "2025.01.01 ~ 2025.03.31",
        content = "청년의 주거비 부담 완화를 위해 월세를 지원하는 정책입니다. 최대 12개월간 월 20만원까지 지원합니다.",
        applicationMethod = "온라인 신청 (복지로 홈페이지) 또는 주민센터 방문 신청",
        deadline = "2025-03-31",
        isUrgent = true
    ),
    PolicyItem(
        id = 2,
        title = "청년 자기계발 바우처",
        date = "18-34세 2025.4.30",
        category = "자기계발",
        support = "최대 50만원",
        isFavorite = false,
        organization = "고용노동부",
        age = "만 18세 ~ 34세",
        period = "2025.03.01 ~ 2025.04.30",
        content = "청년의 자기계발 활동을 지원하기 위해 온오프라인 교육, 자격증 취득 비용을 지원합니다.",
        applicationMethod = "내일배움카드 홈페이지에서 온라인 신청",
        deadline = "2025-04-30",
        isUrgent = true
    ),
    PolicyItem(
        id = 3,
        title = "청년 취업 성공패키지",
        date = "18-29세 2025.06.20",
        category = "취업",
        support = "참여수당",
        isFavorite = false,
        organization = "고용노동부",
        age = "만 18세 ~ 29세",
        period = "2025.04.01 ~ 2025.06.20",
        content = "취업을 원하는 청년에게 진로설정, 직업훈련, 취업알선 등을 종합적으로 지원하며, 참여수당을 지급합니다.",
        applicationMethod = "고용센터 방문 또는 워크넷 온라인 신청",
        deadline = "2025-06-20",
        isUrgent = false
    ),
    PolicyItem(
        id = 4,
        title = "청년 일자리 도약 장려금",
        date = "18-34세 2025.05.15",
        category = "취업",
        support = "최대 1,200만원",
        isFavorite = false,
        organization = "경기도",
        age = "만 18세 ~ 34세",
        period = "2025.02.01 ~ 2025.05.15",
        content = "중소·중견기업에 취업한 청년에게 3년간 최대 1,200만원의 장려금을 지원합니다.",
        applicationMethod = "경기일자리재단 홈페이지에서 온라인 신청",
        deadline = "2025-05-15",
        isUrgent = true
    ),
    PolicyItem(
        id = 5,
        title = "청년 전월세 보증금 대출",
        date = "19-34세 2025.12.31",
        category = "주거",
        support = "최대 1억원",
        isFavorite = true,
        organization = "주택도시기금",
        age = "만 19세 ~ 34세",
        period = "2025.01.01 ~ 2025.12.31",
        content = "전월세 보증금 마련이 어려운 청년에게 최대 1억원까지 저금리로 대출해주는 상품입니다.",
        applicationMethod = "금융기관 방문 또는 주택도시기금 홈페이지 신청",
        deadline = "2025-12-31",
        isUrgent = false
    ),
    PolicyItem(
        id = 6,
        title = "청년 평생교육 지원 사업",
        date = "20-39세 2025.07.30",
        category = "교육",
        support = "학비 지원",
        isFavorite = false,
        organization = "교육부",
        age = "만 20세 ~ 39세",
        period = "2025.05.01 ~ 2025.07.30",
        content = "평생교육 기회 확대를 위해 대학 진학 및 학위 취득 비용을 지원합니다.",
        applicationMethod = "평생교육진흥원 홈페이지에서 온라인 신청",
        deadline = "2025-07-30",
        isUrgent = false
    ),
    PolicyItem(
        id = 7,
        title = "청년 복지 지원금",
        date = "18-34세 2025.08.31",
        category = "복지",
        support = "월 30만원",
        isFavorite = false,
        organization = "보건복지부",
        age = "만 18세 ~ 34세",
        period = "2025.06.01 ~ 2025.08.31",
        content = "저소득 청년층의 생활 안정을 위한 복지 지원금을 지급합니다.",
        applicationMethod = "복지로 홈페이지에서 온라인 신청",
        deadline = "2025-08-31",
        isUrgent = false
    ),
    PolicyItem(
        id = 8,
        title = "청년 건강검진 지원",
        date = "19-39세 2025.09.30",
        category = "복지",
        support = "검진비 전액",
        isFavorite = false,
        organization = "국민건강보험공단",
        age = "만 19세 ~ 39세",
        period = "2025.07.01 ~ 2025.09.30",
        content = "청년층의 건강관리를 위해 종합 건강검진 비용을 전액 지원합니다.",
        applicationMethod = "국민건강보험 홈페이지에서 온라인 신청",
        deadline = "2025-09-30",
        isUrgent = false
    )
)

val userInterests = listOf("취업", "복지", "주거")
val categories = listOf("전체") + userInterests + listOf("자기계발", "교육")

class PolicyListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeWrapper {
                PolicyListScreen(
                    onNavigateHome = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onNavigateCalendar = {
                        startActivity(Intent(this, CalendarActivity::class.java))
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
                        // TODO: 챗봇 다이얼로그 표시
                    }
                )
            }
        }
    }
}

@Composable
fun PolicyListScreen(
    onNavigateHome: () -> Unit,
    onNavigateCalendar: () -> Unit,
    onNavigateBookmark: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateChatbot: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("전체") }
    var expandedCardId by remember { mutableStateOf<Int?>(null) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showUrgentDialog by remember { mutableStateOf(false) }
    var selectedPolicy by remember { mutableStateOf<PolicyItem?>(null) }
    var bookmarkedPolicies by remember { mutableStateOf(setOf<String>()) }
    var searchQuery by remember { mutableStateOf("") }
    
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
    
    val filteredPolicies = if (selectedCategory == "전체") {
        allPolicies
    } else {
        allPolicies.filter { it.category == selectedCategory }
    }
    
    val urgentPolicies = allPolicies.filter { it.isUrgent }
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = "home",
                onNavigateHome = onNavigateHome,
                onNavigateCalendar = onNavigateCalendar,
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
            // Header Section
            PolicyListHeader(
                onBack = onNavigateHome,
                onSearch = { /* TODO: 검색 로직 */ },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                urgentCount = urgentPolicies.size,
                onUrgentClick = { showUrgentDialog = true }
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md)
            ) {
                // Category Filter
                CategoryFilterRow(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    userInterests = userInterests,
                    modifier = Modifier.padding(bottom = Spacing.md)
                )
                
                // Policy Cards
                filteredPolicies.forEach { policy ->
                    PolicyCard(
                        policy = policy,
                        isExpanded = expandedCardId == policy.id,
                        isBookmarked = bookmarkedPolicies.contains(policy.title),
                        onToggleExpand = {
                            expandedCardId = if (expandedCardId == policy.id) null else policy.id
                        },
                        onHeartClick = {
                            if (!bookmarkedPolicies.contains(policy.title)) {
                                selectedPolicy = policy
                                showNotificationDialog = true
                            } else {
                                bookmarkedPolicies = bookmarkedPolicies - policy.title
                            }
                        },
                        onApply = {
                            // TODO: 신청하기 로직
                        },
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    )
                }
            }
        }
    }
    
    // Urgent Policies Dialog
    if (showUrgentDialog) {
        UrgentPoliciesDialog(
            policies = urgentPolicies,
            bookmarkedPolicies = bookmarkedPolicies,
            onHeartClick = { policy ->
                if (!bookmarkedPolicies.contains(policy.title)) {
                    selectedPolicy = policy
                    showNotificationDialog = true
                } else {
                    bookmarkedPolicies = bookmarkedPolicies - policy.title
                }
            },
            onDismiss = { showUrgentDialog = false }
        )
    }
    
    // Notification Dialog
    val context = LocalContext.current
    val calendarService = remember { CalendarService(context) }
    
    if (showNotificationDialog) {
        PolicyNotificationDialog(
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
}

// NotificationSettings는 com.example.app.NotificationSettings를 사용

@Composable
private fun PolicyNotificationDialog(
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

@Composable
private fun PolicyListHeader(
    onBack: () -> Unit,
    onSearch: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    urgentCount: Int,
    onUrgentClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md)
        ) {
            // Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    text = "청년정책 추천",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                
                Spacer(modifier = Modifier.size(48.dp))
            }
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            // User Info Card
            UserInfoCard()
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onSearch = onSearch
            )
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // Urgent Alert Button
            UrgentAlertButton(
                count = urgentCount,
                onClick = onUrgentClick
            )
        }
    }
}

@Composable
private fun UserInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, AppColors.Purple.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            AppColors.Purple.copy(alpha = 0.1f),
                            AppColors.BackgroundGradientStart.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(Spacing.md)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        AppColors.Purple,
                                        AppColors.BackgroundGradientStart
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Column {
                        Text(
                            text = "슬기로운 청년 님",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = "25세 경기도 수원시 거주 취업준비생",
                            fontSize = 12.sp,
                            color = AppColors.TextSecondary
                        )
                    }
                }
                
                Divider(
                    modifier = Modifier.padding(vertical = Spacing.sm),
                    color = AppColors.Purple.copy(alpha = 0.3f)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⭐",
                        fontSize = 16.sp
                    )
                    Text(
                        text = "설정된 관심분야",
                        fontSize = 12.sp,
                        color = AppColors.Purple,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(Spacing.xs))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    userInterests.forEach { interest ->
                        InterestTag(
                            text = interest,
                            backgroundColor = AppColors.Purple,
                            textColor = Color.White,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("정책 입대주택 통합검색", fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = AppColors.TextTertiary
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.TextPrimary,
                unfocusedBorderColor = AppColors.TextPrimary
            ),
            singleLine = true
        )
        
        Button(
            onClick = onSearch,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.TextPrimary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("검색", color = Color.White, fontSize = 14.sp)
        }
    }
}

@Composable
private fun UrgentAlertButton(
    count: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFFEE2E2),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = "마감 임박 정책 ${count}개",
                fontSize = 14.sp,
                color = Color(0xFFDC2626),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    userInterests: List<String>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        categories.forEach { category ->
            val isUserInterest = userInterests.contains(category)
            val isSelected = category == selectedCategory
            
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isUserInterest) {
                            Text("⭐", fontSize = 12.sp)
                        }
                        Text(category, fontSize = 14.sp)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = if (isUserInterest) {
                        AppColors.Purple
                    } else {
                        AppColors.BackgroundGradientStart
                    },
                    selectedLabelColor = Color.White,
                    containerColor = if (isUserInterest) {
                        AppColors.Purple.copy(alpha = 0.1f)
                    } else {
                        Color.White
                    },
                    labelColor = if (isUserInterest) {
                        AppColors.Purple
                    } else {
                        AppColors.TextSecondary
                    }
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) {
                        if (isUserInterest) AppColors.PurpleDark else AppColors.BackgroundGradientStart
                    } else {
                        if (isUserInterest) AppColors.Purple.copy(alpha = 0.5f) else AppColors.Border
                    },
                    selectedBorderColor = if (isUserInterest) AppColors.PurpleDark else AppColors.BackgroundGradientStart,
                    borderWidth = if (isSelected) 2.dp else 1.dp
                )
            )
        }
    }
}

@Composable
private fun PolicyCard(
    policy: PolicyItem,
    isExpanded: Boolean,
    isBookmarked: Boolean,
    onToggleExpand: () -> Unit,
    onHeartClick: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, AppColors.Purple.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.Purple.copy(alpha = 0.05f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
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
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 48.dp)
            ) {
                Text(
                    text = policy.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(bottom = Spacing.sm)
                )
                
                if (!isExpanded) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    ) {
                        CategoryTag(policy.category)
                        SupportTag(policy.support)
                    }
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Text(
                            text = "연령: ${policy.age}",
                            fontSize = 14.sp,
                            color = AppColors.TextSecondary
                        )
                        Text(
                            text = "신청기간: ${policy.period}",
                            fontSize = 14.sp,
                            color = AppColors.TextSecondary
                        )
                    }
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
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                if (!isExpanded) {
                    Button(
                        onClick = onToggleExpand,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.TextPrimary
                        )
                    ) {
                        Text("상세보기", color = Color.White)
                    }
                } else {
                    Button(
                        onClick = onToggleExpand,
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
                                            AppColors.Purple,
                                            AppColors.BackgroundGradientStart
                                        )
                                    )
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
}

@Composable
private fun CategoryTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.Purple.copy(alpha = 0.2f))
            .padding(horizontal = Spacing.sm, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = AppColors.Purple,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SupportTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.BackgroundGradientStart.copy(alpha = 0.2f))
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
private fun PolicyDetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = AppColors.TextTertiary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = AppColors.TextPrimary
        )
    }
}

@Composable
private fun InterestTag(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(horizontal = Spacing.md, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun UrgentPoliciesDialog(
    policies: List<PolicyItem>,
    bookmarkedPolicies: Set<String>,
    onHeartClick: (PolicyItem) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "마감 임박 정책 ${policies.size}개",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                }
                
                Divider(modifier = Modifier.padding(vertical = Spacing.md))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    policies.forEach { policy ->
                        val daysLeft = getDaysLeft(policy.deadline)
                        val isBookmarked = bookmarkedPolicies.contains(policy.title)
                        
                        UrgentPolicyCard(
                            policy = policy,
                            daysLeft = daysLeft,
                            isBookmarked = isBookmarked,
                            onHeartClick = { onHeartClick(policy) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UrgentPolicyCard(
    policy: PolicyItem,
    daysLeft: Int,
    isBookmarked: Boolean,
    onHeartClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFECACA)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            IconButton(
                onClick = onHeartClick,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Bookmark",
                    tint = if (isBookmarked) AppColors.TextPrimary else AppColors.TextTertiary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 48.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.padding(bottom = Spacing.xs)
                ) {
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
                    
                    CategoryTag(policy.category)
                }
                
                Text(
                    text = policy.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = policy.date,
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = "마감일: ${policy.deadline}",
                    fontSize = 12.sp,
                    color = AppColors.TextTertiary
                )
            }
        }
    }
}

private fun getDaysLeft(deadline: String): Int {
    return try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val deadlineDate = dateFormat.parse(deadline)
        val today = Date()
        val diff = deadlineDate?.time?.minus(today.time) ?: 0
        maxOf(0, (diff / (1000 * 60 * 60 * 24)).toInt())
    } catch (e: Exception) {
        0
    }
}

