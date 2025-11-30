package com.wiseyoung.app

import androidx.compose.ui.tooling.preview.Preview
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.MaterialTheme
import com.example.app.ui.theme.AppColors
import com.example.app.ui.theme.Spacing
import com.example.app.ui.theme.ThemeWrapper
import com.example.app.ui.components.BottomNavigationBar
import com.example.app.ui.components.ElevatedCard
import com.example.app.ui.components.PrimaryButton
import com.example.app.ui.components.SecondaryButton
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import androidx.compose.runtime.LaunchedEffect
import com.example.app.network.NetworkModule
import com.example.app.data.model.BookmarkResponse
import android.util.Log
import com.wiseyoung.app.PolicyItem
import com.wiseyoung.app.ApartmentItem
import com.wiseyoung.app.PolicyCard
import com.wiseyoung.app.ApartmentCard
import com.wiseyoung.app.PolicyDetailDialog
import com.wiseyoung.app.ApartmentDetailDialog
import com.example.app.data.CalendarRepository
import com.example.app.data.CalendarEvent
import com.example.app.data.EventType

// 북마크 데이터 모델
data class BookmarkItem(
    val id: Int,
    val type: BookmarkType,
    val title: String,
    val organization: String? = null,
    val age: String? = null,
    val period: String? = null,
    val content: String? = null,
    val applicationMethod: String? = null,
    // 임대주택용 필드
    val address: String? = null,
    val deposit: String? = null,
    val monthlyRent: String? = null,
    val area: String? = null,
    val completionDate: String? = null,
    val distance: String? = null,
    val deadline: String,
    val link: String? = null, // 신청 링크
    val contentId: String? = null // 서버 contentId (링크 조회용)
)

enum class BookmarkType {
    POLICY, HOUSING
}

class BookmarkActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = auth.currentUser?.uid ?: "test-user"
        
        setContent {
            ThemeWrapper {
                BookmarkScreen(
                    userId = userId,
                    onNavigateHome = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onNavigateCalendar = {
                        startActivity(Intent(this, CalendarActivity::class.java))
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
fun BookmarkScreen(
    userId: String,
    onNavigateHome: () -> Unit,
    onNavigateCalendar: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateChatbot: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeTab by remember { mutableStateOf("policy") }
    var expandedCardId by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // 상세 다이얼로그 상태
    var showPolicyDetailDialog by remember { mutableStateOf(false) }
    var detailPolicy by remember { mutableStateOf<PolicyItem?>(null) }
    var showHousingDetailDialog by remember { mutableStateOf(false) }
    var detailHousing by remember { mutableStateOf<ApartmentItem?>(null) }
    
    // 북마크 상태 (서버 + 로컬 병합)
    var bookmarks by remember {
        mutableStateOf<List<BookmarkItem>>(emptyList())
    }
    
    // 정책과 임대주택 아이템으로 변환 (기존 카드 컴포넌트 재사용용)
    var policyItems by remember {
        mutableStateOf<List<PolicyItem>>(emptyList())
    }
    
    var apartmentItems by remember {
        mutableStateOf<List<ApartmentItem>>(emptyList())
    }
    
    // PolicyItem.id와 ApartmentItem.id에 이미 bookmarkId가 저장되어 있음
    
    // 서버에서 북마크 가져오기 (상세 정보 포함)
    LaunchedEffect(userId) {
        Log.d("BookmarkActivity", "북마크 로딩 시작: userId=$userId")
        isLoading = true
        try {
            // 정책 북마크 가져오기
            Log.d("BookmarkActivity", "정책 북마크 API 호출 시작")
            val policyResponse = NetworkModule.apiService.getBookmarks(
                userId = userId,
                contentType = "policy"
            )
            Log.d("BookmarkActivity", "정책 북마크 API 응답: isSuccessful=${policyResponse.isSuccessful}, code=${policyResponse.code()}")
            
            // 임대주택 북마크 가져오기
            Log.d("BookmarkActivity", "임대주택 북마크 API 호출 시작")
            val housingResponse = NetworkModule.apiService.getBookmarks(
                userId = userId,
                contentType = "housing"
            )
            Log.d("BookmarkActivity", "임대주택 북마크 API 응답: isSuccessful=${housingResponse.isSuccessful}, code=${housingResponse.code()}")
            
            val (bookmarksList, policiesList, apartmentsList) = coroutineScope {
                val bookmarksList = mutableListOf<BookmarkItem>()
                val policiesList = mutableListOf<Pair<Int, PolicyItem>>() // bookmarkId to PolicyItem
                val apartmentsList = mutableListOf<Pair<Int, ApartmentItem>>() // bookmarkId to ApartmentItem
                
                // 정책 북마크 변환 (상세 정보 조회 및 PolicyItem 생성)
                if (policyResponse.isSuccessful && policyResponse.body()?.success == true) {
                    val policyBookmarks = policyResponse.body()?.data ?: emptyList()
                    Log.d("BookmarkActivity", "서버에서 정책 북마크 ${policyBookmarks.size}개 가져옴")
                    
                    if (policyBookmarks.isNotEmpty()) {
                        val policyItems = policyBookmarks.map { bookmarkResponse ->
                            async {
                                try {
                                    Log.d("BookmarkActivity", "정책 상세 정보 조회 시작: contentId=${bookmarkResponse.contentId}")
                                    val detailResponse = NetworkModule.apiService.getPolicyById(
                                        policyId = bookmarkResponse.contentId,
                                        userId = userId
                                    )
                                    
                                    if (detailResponse.isSuccessful && detailResponse.body()?.success == true) {
                                        val policy = detailResponse.body()?.data
                                        if (policy != null) {
                                            Log.d("BookmarkActivity", "정책 상세 정보 조회 성공: ${policy.title}")
                                            // PolicyItem으로 변환
                                            val policyItem = PolicyItem(
                                                id = bookmarkResponse.bookmarkId,
                                                policyId = policy.policyId,
                                                title = policy.title,
                                                date = "${policy.ageStart ?: 0}-${policy.ageEnd ?: 0}세 ${policy.applicationEnd?.take(10)?.replace("-", ".") ?: ""}",
                                                category = policy.category ?: "기타",
                                                support = "지원금",
                                                isFavorite = true, // 북마크 화면이므로 항상 true
                                                organization = policy.region ?: "",
                                                age = "만 ${policy.ageStart ?: 0}세 ~ ${policy.ageEnd ?: 0}세",
                                                period = "${policy.applicationStart?.take(10)?.replace("-", ".") ?: ""} ~ ${policy.applicationEnd?.take(10)?.replace("-", ".") ?: ""}",
                                                content = policy.summary ?: "",
                                                applicationMethod = policy.eligibility ?: "",
                                                deadline = policy.applicationEnd?.take(10)?.replace("-", ".") ?: "",
                                                isUrgent = false,
                                                link1 = policy.link1,
                                                link2 = policy.link2
                                            )
                                            Pair(bookmarkResponse.bookmarkId, policyItem)
                                        } else {
                                            Log.w("BookmarkActivity", "정책 상세 정보가 null: contentId=${bookmarkResponse.contentId}")
                                            null
                                        }
                                    } else {
                                        Log.w("BookmarkActivity", "정책 상세 정보 조회 실패: contentId=${bookmarkResponse.contentId}, code=${detailResponse.code()}")
                                        null
                                    }
                                } catch (e: Exception) {
                                    Log.e("BookmarkActivity", "정책 상세 정보 조회 실패: contentId=${bookmarkResponse.contentId}, ${e.message}", e)
                                    null
                                }
                            }
                        }
                        val fetchedPolicies = policyItems.awaitAll().filterNotNull()
                        policiesList.addAll(fetchedPolicies)
                        Log.d("BookmarkActivity", "정책 북마크 변환 완료: ${fetchedPolicies.size}/${policyBookmarks.size}개 성공")
                    } else {
                        Log.d("BookmarkActivity", "정책 북마크가 없습니다.")
                    }
                } else {
                    Log.w("BookmarkActivity", "정책 북마크 조회 실패: isSuccessful=${policyResponse.isSuccessful}, success=${policyResponse.body()?.success}, code=${policyResponse.code()}")
                }
                
                // 임대주택 북마크 변환 (상세 정보 조회 및 ApartmentItem 생성)
                if (housingResponse.isSuccessful && housingResponse.body()?.success == true) {
                    val housingBookmarks = housingResponse.body()?.data ?: emptyList()
                    Log.d("BookmarkActivity", "✅ 서버에서 임대주택 북마크 ${housingBookmarks.size}개 가져옴")
                    
                    if (housingBookmarks.isNotEmpty()) {
                        Log.d("BookmarkActivity", "임대주택 북마크 상세 정보 조회 시작 (병렬 처리):")
                        housingBookmarks.forEach { bookmark ->
                            Log.d("BookmarkActivity", "  - bookmarkId=${bookmark.bookmarkId}, contentId=${bookmark.contentId}, title=${bookmark.title}")
                        }
                        
                        val apartmentItems = housingBookmarks.map { bookmarkResponse ->
                            async {
                                try {
                                    Log.d("BookmarkActivity", "🔍 임대주택 상세 정보 조회 시작: contentId=${bookmarkResponse.contentId}")
                                    val detailResponse = NetworkModule.apiService.getHousingById(
                                        housingId = bookmarkResponse.contentId,
                                        userIdParam = userId
                                    )
                                    
                                    if (detailResponse.isSuccessful && detailResponse.body()?.success == true) {
                                        val housing = detailResponse.body()?.data
                                        if (housing != null && !housing.name.isNullOrBlank()) {
                                            Log.d("BookmarkActivity", "✅ 임대주택 상세 정보 조회 성공: name=${housing.name}, housingId=${housing.housingId}")
                                            
                                            // housingId가 null이면 경고 로그 출력
                                            if (housing.housingId.isNullOrBlank()) {
                                                Log.w("BookmarkActivity", "⚠️ 임대주택 상세 정보에 housingId가 없습니다: name=${housing.name}")
                                            }
                                            
                                            // ApartmentItem으로 변환
                                            fun extractRegionFromAddress(address: String): String {
                                                val parts = address.split(" ")
                                                if (parts.isNotEmpty()) {
                                                    val firstPart = parts[0]
                                                    if (firstPart.contains("시") || firstPart.contains("도") || firstPart.contains("군")) {
                                                        return firstPart
                                                    }
                                                }
                                                return ""
                                            }
                                            
                                            val apartmentItem = ApartmentItem(
                                                id = bookmarkResponse.bookmarkId,
                                                housingId = housing.housingId, // 실제 임대주택 ID 저장
                                                name = housing.name,
                                                distance = housing.distanceFromUser?.let { "${(it / 1000).toInt()}km" } ?: "거리 정보 없음",
                                                deposit = try { (housing.deposit ?: 0) / 10000 } catch (e: Exception) { 0 },
                                                depositDisplay = try { "${(housing.deposit ?: 0) / 10000}만원" } catch (e: Exception) { "0만원" },
                                                monthlyRent = try { (housing.monthlyRent ?: 0) / 10000 } catch (e: Exception) { 0 },
                                                monthlyRentDisplay = try { "${(housing.monthlyRent ?: 0) / 10000}만원" } catch (e: Exception) { "0만원" },
                                                deadline = housing.applicationEnd?.take(10)?.replace("-", ".") ?: "",
                                                address = housing.address ?: "",
                                                area = try { (housing.supplyArea?.toInt() ?: 0) } catch (e: Exception) { 0 },
                                                completionDate = housing.completeDate?.take(10)?.replace("-", ".") ?: "",
                                                organization = housing.organization ?: "",
                                                count = 0,
                                                region = extractRegionFromAddress(housing.address ?: ""),
                                                housingType = housing.housingType ?: "",
                                                heatingType = housing.heatingType ?: "",
                                                hasElevator = housing.elevator ?: false,
                                                parkingSpaces = housing.parkingSpaces ?: 0,
                                                convertibleDeposit = "",
                                                totalUnits = housing.totalUnits ?: 0,
                                                link = housing.link,
                                                latitude = housing.latitude,
                                                longitude = housing.longitude
                                            )
                                            Pair(bookmarkResponse.bookmarkId, apartmentItem)
                                        } else {
                                            Log.w("BookmarkActivity", "임대주택 상세 정보가 null이거나 이름이 없음: contentId=${bookmarkResponse.contentId}")
                                            null
                                        }
                                    } else {
                                        val errorBody = try {
                                            detailResponse.errorBody()?.string()
                                        } catch (e: Exception) {
                                            null
                                        }
                                        Log.w("BookmarkActivity", "❌ 임대주택 상세 정보 조회 실패: contentId=${bookmarkResponse.contentId}, 응답 코드=${detailResponse.code()}, errorBody=$errorBody")
                                        Log.w("BookmarkActivity", "⚠️ contentId가 잘못되었을 수 있습니다. 이 북마크는 건너뜁니다.")
                                        // 잘못된 북마크는 건너뛰기
                                        null
                                    }
                                } catch (e: Exception) {
                                    Log.e("BookmarkActivity", "❌ 임대주택 상세 정보 조회 예외 발생: contentId=${bookmarkResponse.contentId}, ${e.message}", e)
                                    e.printStackTrace()
                                    Log.w("BookmarkActivity", "⚠️ 예외 발생으로 인해 이 북마크는 건너뜁니다.")
                                    // 예외 발생 시 북마크 건너뛰기
                                    null
                                }
                            }
                        }
                        val fetchedApartments = apartmentItems.awaitAll().filterNotNull()
                        apartmentsList.addAll(fetchedApartments)
                        Log.d("BookmarkActivity", "임대주택 북마크 변환 완료: ${fetchedApartments.size}/${housingBookmarks.size}개 성공")
                    } else {
                        Log.d("BookmarkActivity", "임대주택 북마크가 없습니다.")
                    }
                } else {
                    Log.w("BookmarkActivity", "임대주택 북마크 조회 실패: isSuccessful=${housingResponse.isSuccessful}, success=${housingResponse.body()?.success}, code=${housingResponse.code()}")
                }
                
                Triple(bookmarksList, policiesList, apartmentsList) // coroutineScope 블록의 반환값
            }
            
            // PolicyItem과 ApartmentItem 리스트 설정 (id에 이미 bookmarkId가 저장됨)
            policyItems = policiesList.map { it.second }
            apartmentItems = apartmentsList.mapNotNull { it.second }
            
            Log.d("BookmarkActivity", "✅ 북마크 로딩 완료: 정책 ${policyItems.size}개, 임대주택 ${apartmentItems.size}개")
            if (apartmentItems.isNotEmpty()) {
                Log.d("BookmarkActivity", "임대주택 카드 목록:")
                apartmentItems.forEach { apartment ->
                    Log.d("BookmarkActivity", "  - id=${apartment.id}, housingId=${apartment.housingId}, name=${apartment.name}")
                }
            } else {
                Log.w("BookmarkActivity", "⚠️ 임대주택 카드가 비어있습니다. apartmentsList.size=${apartmentsList.size}")
            }
        } catch (e: Exception) {
            Log.e("BookmarkActivity", "서버에서 북마크 가져오기 실패: ${e.message}", e)
            e.printStackTrace()
            // 실패 시 빈 리스트로 설정
            policyItems = emptyList()
            apartmentItems = emptyList()
        } finally {
            isLoading = false
            Log.d("BookmarkActivity", "북마크 로딩 종료: isLoading=false")
        }
    }
    
    // SharedPreferences 변경 감지는 제거 (서버 북마크만 사용)
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = "bookmark",
                onNavigateHome = onNavigateHome,
                onNavigateCalendar = onNavigateCalendar,
                onNavigateChatbot = onNavigateChatbot,
                onNavigateBookmark = {},
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
            BookmarkHeader()
            
            // Tabs
            BookmarkTabs(
                activeTab = activeTab,
                onTabChange = { activeTab = it }
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md)
            ) {
                when (activeTab) {
                    "policy" -> {
                        if (policyItems.isEmpty()) {
                            EmptyBookmarkCard(
                                message = "북마크한 정책이 없습니다.",
                                modifier = Modifier.padding(top = Spacing.xxl)
                            )
                        } else {
                            policyItems.forEach { policy ->
                                PolicyCard(
                                    policy = policy,
                                    isBookmarked = true,
                                    onShowDetail = {
                                        // PolicyItem을 직접 전달
                                        detailPolicy = policy
                                        showPolicyDetailDialog = true
                                    },
                                    onHeartClick = {
                                        // 북마크 삭제 (policy.id에 이미 bookmarkId가 저장됨)
                                        scope.launch {
                                            try {
                                                NetworkModule.apiService.deleteBookmark(
                                                    userId = userId,
                                                    bookmarkId = policy.id
                                                )
                                                Log.d("BookmarkActivity", "서버 북마크 삭제 성공: ${policy.id}")
                                                // 목록에서 제거
                                                policyItems = policyItems.filter { it.id != policy.id }
                                            } catch (e: Exception) {
                                                Log.e("BookmarkActivity", "서버 북마크 삭제 실패: ${e.message}", e)
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(bottom = Spacing.sm)
                                )
                            }
                        }
                    }
                    "housing" -> {
                        if (apartmentItems.isEmpty()) {
                            EmptyBookmarkCard(
                                message = "북마크한 임대주택이 없습니다.",
                                modifier = Modifier.padding(top = Spacing.xxl)
                            )
                        } else {
                            apartmentItems.forEach { apartment ->
                                ApartmentCard(
                                    apartment = apartment,
                                    isBookmarked = true,
                                    onHeartClick = {
                                        // 북마크 삭제 (apartment.id에 이미 bookmarkId가 저장됨)
                                        scope.launch {
                                            try {
                                                NetworkModule.apiService.deleteBookmark(
                                                    userId = userId,
                                                    bookmarkId = apartment.id
                                                )
                                                Log.d("BookmarkActivity", "서버 북마크 삭제 성공: ${apartment.id}")
                                                // 목록에서 제거
                                                apartmentItems = apartmentItems.filter { it.id != apartment.id }
                                            } catch (e: Exception) {
                                                Log.e("BookmarkActivity", "서버 북마크 삭제 실패: ${e.message}", e)
                                            }
                                        }
                                    },
                                    onDetailClick = {
                                        // ApartmentItem을 직접 전달
                                        detailHousing = apartment
                                        showHousingDetailDialog = true
                                    },
                                    modifier = Modifier.padding(bottom = Spacing.sm)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Policy Detail Dialog (PolicyListActivity의 다이얼로그 재사용)
    if (showPolicyDetailDialog && detailPolicy != null) {
        PolicyDetailDialog(
            policy = detailPolicy!!,
            onDismiss = { showPolicyDetailDialog = false },
            onApply = {
                // 정책 신청 링크 처리
                val policy = detailPolicy
                val link = policy?.link1 ?: policy?.link2
                if (!link.isNullOrEmpty()) {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.util.Log.e("BookmarkActivity", "링크 열기 실패: ${e.message}", e)
                        Toast.makeText(context, "링크를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "신청 링크가 제공되지 않았습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    
    // Housing Detail Dialog (HousingMapActivity의 다이얼로그 재사용)
    if (showHousingDetailDialog && detailHousing != null) {
        ApartmentDetailDialog(
            apartment = detailHousing!!,
            isBookmarked = true,
            onHeartClick = {
                // 북마크 삭제
                val apartment = detailHousing
                scope.launch {
                    try {
                        NetworkModule.apiService.deleteBookmark(
                            userId = userId,
                            bookmarkId = apartment?.id ?: 0
                        )
                        Log.d("BookmarkActivity", "서버 북마크 삭제 성공: ${apartment?.id}")
                        // 목록에서 제거
                        apartmentItems = apartmentItems.filter { it.id != apartment?.id }
                        showHousingDetailDialog = false
                    } catch (e: Exception) {
                        Log.e("BookmarkActivity", "서버 북마크 삭제 실패: ${e.message}", e)
                    }
                }
            },
            onClose = { showHousingDetailDialog = false },
            onApply = {
                // 임대주택 신청 링크 처리
                val apartment = detailHousing
                if (!apartment?.link.isNullOrEmpty()) {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(apartment.link))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.util.Log.e("BookmarkActivity", "링크 열기 실패: ${e.message}", e)
                        Toast.makeText(context, "링크를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "신청 링크가 제공되지 않았습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun BookmarkHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "좋아요",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BookmarkTabs(
    activeTab: String,
    onTabChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            TabButton(
                text = "정책",
                isSelected = activeTab == "policy",
                onClick = { onTabChange("policy") },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                text = "임대주택",
                isSelected = activeTab == "housing",
                onClick = { onTabChange("housing") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.md),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.onSurface)
            )
        }
    }
}

@Composable
private fun EmptyBookmarkCard(
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

@Preview(showBackground = true)
@Composable
fun BookmarkCardPreview() {
    val sampleBookmark = BookmarkItem(
        id = 1,
        type = BookmarkType.POLICY,
        title = "청년 월세 지원",
        organization = "서울시",
        age = "만 19세 ~ 34세",
        period = "2023년 연중",
        content = "월 20만원 지원",
        applicationMethod = "온라인 신청",
        deadline = "2023-12-31"
    )
    
    PolicyBookmarkCard(
        bookmark = sampleBookmark,
        onShowDetail = {},
        onRemoveBookmark = {}
    )
}

@Composable
private fun PolicyBookmarkCard(
    bookmark: BookmarkItem,
    onShowDetail: () -> Unit,
    onRemoveBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 정책 화면의 PolicyCard와 동일한 스타일 적용
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, AppColors.LightBlue.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.LightBlue.copy(alpha = 0.05f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            // 좋아요 버튼 (제거 버튼)
            IconButton(
                onClick = onRemoveBookmark,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Remove bookmark",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 48.dp)
            ) {
                Text(
                    text = bookmark.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = Spacing.sm)
                )
                
                // 카테고리와 지원금액 태그 (데이터가 있으면 표시)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.padding(bottom = Spacing.sm)
                ) {
                    // 북마크 데이터에 category가 없으므로 organization을 카테고리로 표시
                    bookmark.organization?.let {
                        CategoryTag(it)
                    }
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    bookmark.age?.let {
                        Text(
                            text = "연령: $it",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    bookmark.period?.let {
                        Text(
                            text = "신청기간: $it",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    bookmark.deadline.takeIf { it.isNotEmpty() }?.let {
                        Text(
                            text = "마감일: $it",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 상세보기 버튼 (오른쪽 하단, 작게)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onShowDetail,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface
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
private fun HousingBookmarkCard(
    bookmark: BookmarkItem,
    onShowDetail: () -> Unit,
    onRemoveBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 좋아요 버튼 (제거 버튼)
                IconButton(
                    onClick = onRemoveBookmark,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Remove bookmark",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 48.dp)
                ) {
                    Text(
                        text = bookmark.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        bookmark.distance?.let {
                            Text(
                                text = "📍 사용자로부터 $it",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (bookmark.deposit != null && bookmark.monthlyRent != null) {
                            Text(
                                text = "💰 보증금 ${bookmark.deposit} / 월세 ${bookmark.monthlyRent}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "📅 신청마감일: ${bookmark.deadline}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 상세보기 버튼 (오른쪽 하단, 작게)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onShowDetail,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface
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
    bookmark: BookmarkItem,
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
                    text = bookmark.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                bookmark.organization?.let {
                    CategoryTag(it)
                }
                
                Spacer(modifier = Modifier.height(Spacing.lg))
                
                bookmark.organization?.let {
                    PolicyDetailRow("주관기관명", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                bookmark.age?.let {
                    PolicyDetailRow("연령", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                bookmark.period?.let {
                    PolicyDetailRow("신청기간", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                bookmark.deadline.takeIf { it.isNotEmpty() }?.let {
                    PolicyDetailRow("마감일", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                bookmark.content?.let {
                    PolicyDetailRow("정책내용", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                bookmark.applicationMethod?.let {
                    PolicyDetailRow("신청방법", it)
                }
                
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
                            color = MaterialTheme.colorScheme.surface,
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
private fun HousingDetailDialog(
    bookmark: BookmarkItem,
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
                        text = "임대주택 상세 정보",
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
                    text = bookmark.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(Spacing.lg))
                
                bookmark.address?.let {
                    PolicyDetailRow("위치 / 주소", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                if (bookmark.deposit != null && bookmark.monthlyRent != null) {
                    PolicyDetailRow("가격", "보증금 ${bookmark.deposit} / 월세 ${bookmark.monthlyRent}")
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                bookmark.area?.let {
                    PolicyDetailRow("공급전용면적", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                bookmark.completionDate?.let {
                    PolicyDetailRow("준공날짜", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                bookmark.organization?.let {
                    PolicyDetailRow("기관명", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                PolicyDetailRow("마감날짜", bookmark.deadline)
                
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
                            color = MaterialTheme.colorScheme.surface,
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
            .background(AppColors.LightBlue.copy(alpha = 0.2f))
            .padding(horizontal = Spacing.sm, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = AppColors.LightBlue,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

