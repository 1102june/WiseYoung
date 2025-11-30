package com.wiseyoung.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalDensity
import com.example.app.NotificationSettings
import com.example.app.service.CalendarService
import com.example.app.ui.components.BottomNavigationBar
import com.example.app.ui.theme.AppColors
import com.example.app.ui.theme.Spacing
import com.example.app.ui.theme.ThemeWrapper
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed


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
    val isUrgent: Boolean,
    val link1: String?,
    val link2: String?
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
        isUrgent = true,
        link1 = null,
        link2 = null
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
        isUrgent = true,
        link1 = null,
        link2 = null
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
        isUrgent = false,
        link1 = null,
        link2 = null
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
        isUrgent = true,
        link1 = null,
        link2 = null
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
        isUrgent = false,
        link1 = null,
        link2 = null
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
        isUrgent = false,
        link1 = null,
        link2 = null
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
        isUrgent = false,
        link1 = null,
        link2 = null
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
        isUrgent = false,
        link1 = null,
        link2 = null
    )
)

// 백엔드 policy.category 필드와 동일한 카테고리 셋
val userInterests = listOf("일자리", "주거", "복지문화", "교육")
val categories = listOf("전체") + userInterests

class PolicyListActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentUser = auth.currentUser
        val userId = currentUser?.uid ?: "test-user"
        
        android.util.Log.d("PolicyListActivity", "onCreate - currentUser: ${currentUser?.email}, userId: $userId")
        if (currentUser == null) {
            android.util.Log.w("PolicyListActivity", "⚠️ 로그인되지 않은 상태입니다. test-user로 진행합니다.")
        }
        
        setContent {
            ThemeWrapper {
                PolicyListScreen(
                    userId = userId,
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
    userId: String,
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
    
    var showDetailDialog by remember { mutableStateOf(false) }
    var detailPolicy by remember { mutableStateOf<PolicyItem?>(null) }
    
    // API 데이터
    var policiesList by remember { mutableStateOf<List<PolicyItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
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
    
    val context = LocalContext.current

    // 프로필 정보
    var profile by remember { mutableStateOf<com.example.app.data.model.UserProfileResponse?>(null) }

    // 프로필 + 정책 목록 로드
    LaunchedEffect(userId, selectedCategory) {
        isLoading = true
        errorMessage = null
        try {
            // 1) 프로필 정보 조회 (userId 변경 시에만)
            if (profile == null) {
                try {
                    android.util.Log.d("PolicyListActivity", "프로필 조회 시작: userId=$userId")
                    val profileResponse = com.example.app.network.NetworkModule.apiService.getUserProfile(userId)
                    android.util.Log.d("PolicyListActivity", "프로필 응답: code=${profileResponse.code()}, success=${profileResponse.body()?.success}")
                    
                    if (profileResponse.isSuccessful && profileResponse.body()?.success == true) {
                        val profileData = profileResponse.body()?.data
                        profile = profileData
                        profileData?.let { p ->
                            android.util.Log.d("PolicyListActivity", "✅ 프로필 조회 성공: 닉네임=${p.nickname}, 나이=${p.age}, 지역=${p.region}, 관심사=${p.interests}")
                        } ?: run {
                            android.util.Log.w("PolicyListActivity", "⚠️ 프로필 데이터가 null입니다.")
                        }
                    } else {
                        val errorMsg = profileResponse.body()?.message ?: "알 수 없는 오류"
                        android.util.Log.w("PolicyListActivity", "프로필 조회 실패: code=${profileResponse.code()}, message=$errorMsg")
                        // 프로필이 없으면 null로 유지 (기본값 표시)
                        profile = null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PolicyListActivity", "프로필 조회 오류: ${e.message}", e)
                    // 프로필 조회 실패 시 null로 유지 (기본값 표시)
                    profile = null
                }
            } else {
                android.util.Log.d("PolicyListActivity", "프로필 이미 로드됨: ${profile?.nickname}")
            }

            // 2) 맞춤 정책 추천 목록 조회 (추천 로직 사용 - 점수 기반 추천 후 limit만큼만 반환)
            // "전체"일 때는 category 파라미터를 보내지 않고, 나머지 탭에서는 카테고리 이름을 함께 전송
            val categoryParam = if (selectedCategory == "전체") null else selectedCategory
            // limit을 명시적으로 설정하여 추천된 정책만 받아오기 (서버에서 점수 계산 후 상위 N개만 반환)
            val response = com.example.app.network.NetworkModule.apiService.getRecommendedPolicies(
                userId = userId,
                category = categoryParam,
                limit = 30  // 추천 정책만 받아오도록 limit 설정 (서버의 getPersonalizedPolicies에서 점수 계산 후 limit 적용)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val policies = response.body()?.data ?: emptyList()
                android.util.Log.d("PolicyListActivity", "추천 정책 받아옴: ${policies.size}개 (limit: 30, 프로필 있음: ${profile != null})")
                if (policies.size > 30) {
                    android.util.Log.w("PolicyListActivity", "⚠️ 서버에서 limit을 무시하고 전체 정책을 반환했습니다. 프로필이 없어서 전체 정책이 반환된 것으로 보입니다.")
                }
                // PolicyResponse를 PolicyItem으로 변환
                policiesList = policies.mapIndexed { index, policy ->
                    PolicyItem(
                        id = index + 1,
                        title = policy.title,
                        date = "${policy.ageStart ?: 0}-${policy.ageEnd ?: 0}세 ${policy.applicationEnd?.take(10)?.replace("-", ".") ?: ""}",
                        category = policy.category ?: "기타",
                        support = "지원금",
                        isFavorite = policy.isBookmarked,
                        organization = policy.region ?: "",
                        age = "만 ${policy.ageStart ?: 0}세 ~ ${policy.ageEnd ?: 0}세",
                        period = "${policy.applicationStart?.take(10)?.replace("-", ".") ?: ""} ~ ${policy.applicationEnd?.take(10)?.replace("-", ".") ?: ""}",
                        content = policy.summary ?: "",
                        // eligibility에는 지원내용/신청방법 등이 포함되어 있어 상세 정보로 사용
                        applicationMethod = policy.eligibility ?: "",
                        deadline = policy.applicationEnd?.take(10) ?: "",
                        isUrgent = false, // TODO: 마감일 계산
                        link1 = policy.link1,
                        link2 = policy.link2
                    )
                }
                // 데이터가 없으면 기본 데이터 사용
                if (policiesList.isEmpty()) {
                    policiesList = allPolicies
                }
            } else {
                errorMessage = response.body()?.message ?: "정책 목록을 불러올 수 없습니다."
                policiesList = allPolicies
            }
        } catch (e: Exception) {
            errorMessage = "네트워크 오류: ${e.message}"
            policiesList = allPolicies
        } finally {
            isLoading = false
        }
    }
    
    // 카테고리 + 통합검색(제목/내용/주관기관/카테고리) 필터링
    val filteredPolicies by remember(policiesList, selectedCategory, searchQuery) {
        mutableStateOf(
            policiesList
                // 카테고리 필터
                .filter { policy ->
                    selectedCategory == "전체" || policy.category == selectedCategory
                }
                // 검색어 필터
                .filter { policy ->
                    val query = searchQuery.trim()
                    if (query.isEmpty()) return@filter true
                    val q = query.lowercase()
                    listOf(
                        policy.title,
                        policy.content,
                        policy.organization,
                        policy.category
                    ).any { field ->
                        field.lowercase().contains(q)
                    }
                }
        )
    }
    
    val urgentPolicies = filteredPolicies.filter { it.isUrgent }
    
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
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .verticalScroll(scrollState)
        ) {
            // Header Section (스크롤 가능하도록 이동)
            PolicyListHeader(
                profile = profile,
                onBack = onNavigateHome,
                onSearch = { /* TODO: 검색 로직 */ },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it }
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                        isBookmarked = bookmarkedPolicies.contains(policy.title),
                        onShowDetail = {
                            detailPolicy = policy
                            showDetailDialog = true
                        },
                        onHeartClick = {
                            if (!bookmarkedPolicies.contains(policy.title)) {
                                selectedPolicy = policy
                                showNotificationDialog = true
                            } else {
                                bookmarkedPolicies = bookmarkedPolicies - policy.title
                            }
                        },
                        modifier = Modifier.padding(bottom = Spacing.sm)
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
                val url = when {
                    !detailPolicy!!.link1.isNullOrBlank() -> detailPolicy!!.link1
                    !detailPolicy!!.link2.isNullOrBlank() -> detailPolicy!!.link2
                    else -> null
                }

                if (url != null) {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }.onFailure {
                        Toast.makeText(context, "링크를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "신청 링크가 제공되지 않은 정책입니다.", Toast.LENGTH_SHORT).show()
                }
            }
        )
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
    val calendarService = remember { CalendarService(context) }
    
    if (showNotificationDialog) {
        PolicyNotificationDialog(
            notifications = notifications,
            onNotificationsChange = { notifications = it },
            onSave = {
                selectedPolicy?.let { policy ->
                    calendarService.addPolicyToCalendar(
                        title = policy.title,
                        organization = policy.organization,
                        deadline = policy.deadline,
                        policyId = policy.id.toString(),
                        notificationSettings = notifications
                    )
                    Toast.makeText(context, "캘린더에 일정이 추가되었습니다.", Toast.LENGTH_SHORT).show()
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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()), // 스크롤 추가
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
            // WheelPicker를 사용하는 TimePickerSection으로 교체
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
                
                // WheelPicker를 사용하는 TimePickerSection으로 교체
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
            color = AppColors.TextSecondary
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp) // 높이 축소 (140dp -> 100dp)
                .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp))
                .padding(vertical = 4.dp)
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
                            color = if (item == selectedValue) AppColors.LightBlue else AppColors.TextSecondary
                        )
                    }
                }
            }
        }
        Text(label, fontSize = 12.sp, color = AppColors.TextSecondary)
    }
}

@Composable
private fun PolicyListHeader(
    profile: com.example.app.data.model.UserProfileResponse?,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm) // vertical 패딩 축소
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
                    modifier = Modifier.size(28.dp), // 아이콘 크기 축소
                    tint = AppColors.TextPrimary
                )
            }
            
            Text(
                text = "청년정책 추천",
                fontSize = 16.sp, // 폰트 크기 축소
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            
            Spacer(modifier = Modifier.size(28.dp)) // 균형 맞추기 위한 공간 축소
        }
        
        Spacer(modifier = Modifier.height(Spacing.sm)) // 간격 축소
        
        // User Info Card (간추려서 표시)
        UserInfoCard(profile)
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        // Search Bar (작게)
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onSearch = onSearch
        )
    }
}

@Composable
private fun UserInfoCard(profile: com.example.app.data.model.UserProfileResponse?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Border),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로필 아이콘 (작게)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                AppColors.LightBlue,
                                Color(0xFF6EBBFF)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // 닉네임과 정보 (간추려서 표시)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                val nickname = profile?.nickname ?: "슬기로운 청년"
                Text(
                    text = "$nickname 님",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 나이, 지역, 취업상태를 한 줄에 작게 표시
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    profile?.age?.let {
                        Text(
                            text = "${it}세",
                            fontSize = 11.sp,
                            color = AppColors.TextSecondary
                        )
                    } ?: Text(
                        text = "25세",
                        fontSize = 11.sp,
                        color = AppColors.TextSecondary
                    )
                    
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = AppColors.TextTertiary
                    )
                    
                    if (profile?.region != null) {
                        Text(
                            text = profile.region,
                            fontSize = 11.sp,
                            color = AppColors.TextSecondary
                        )
                    } else {
                        Text(
                            text = "경기도 수원시",
                            fontSize = 11.sp,
                            color = AppColors.TextSecondary
                        )
                    }
                    
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = AppColors.TextTertiary
                    )
                    
                    if (profile?.jobStatus != null) {
                        Text(
                            text = profile.jobStatus,
                            fontSize = 11.sp,
                            color = AppColors.TextSecondary
                        )
                    } else {
                        Text(
                            text = "취업준비생",
                            fontSize = 11.sp,
                            color = AppColors.TextSecondary
                        )
                    }
                }
                
                // 관심분야 (작은 태그로 표시)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val interests = profile?.interests?.takeIf { it.isNotEmpty() } ?: userInterests.take(3)
                    interests.take(3).forEach { interest ->
                        InterestTag(
                            text = interest,
                            backgroundColor = AppColors.LightBlue.copy(alpha = 0.2f),
                            textColor = AppColors.LightBlue,
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
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("정책·임대주택 통합검색", fontSize = 12.sp) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = AppColors.TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        },
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.Border,
            unfocusedBorderColor = AppColors.Border
        ),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
    )
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
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(end = Spacing.md)
    ) {
        items(categories) { category ->
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
                        // "복지문화" 글자 크기 조정 (버튼 두 줄 방지)
                        val fontSize = if (category == "복지문화") 12.sp else 14.sp
                        Text(category, fontSize = fontSize)
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
    isBookmarked: Boolean,
    onShowDetail: () -> Unit,
    onHeartClick: () -> Unit,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            // 상단: 제목 + 좋아요 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = policy.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                
                IconButton(
                    onClick = onHeartClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) AppColors.TextPrimary else AppColors.TextTertiary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // 태그
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.padding(bottom = Spacing.sm)
            ) {
                CategoryTag(policy.category)
                SupportTag(policy.support)
            }
            
            // 정보 텍스트
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
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // 상세보기 버튼 (오른쪽 하단, 작게)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onShowDetail,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("상세보기", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PolicyDetailDialog(
    policy: PolicyItem,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f), // 팝업 크기 키움
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
                        text = "정책 상세 정보",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
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
                    color = AppColors.TextPrimary
                )
                
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    CategoryTag(policy.category)
                    SupportTag(policy.support)
                }
                
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
                                        Color(0xFF59ABF7), // 시작 색상
                                        Color(0xFF59ABF7)  // 끝 색상 (단색 효과)
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
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
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

