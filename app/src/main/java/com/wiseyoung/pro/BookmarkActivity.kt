package com.wiseyoung.pro

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.MaterialTheme
import com.wiseyoung.pro.ui.theme.AppColors
import com.wiseyoung.pro.ui.theme.Spacing
import com.wiseyoung.pro.ui.theme.ThemeWrapper
import com.wiseyoung.pro.data.model.displayApplicationPeriod
import com.wiseyoung.pro.ui.components.ElevatedCard
import com.wiseyoung.pro.network.NetworkModule
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import android.util.Log
import android.content.SharedPreferences
import androidx.compose.ui.tooling.preview.Preview

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
    onNavigateProfile: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var activeTab by remember { mutableStateOf("policy") }
    var isLoading by remember { mutableStateOf(true) }
    
    // 상세 다이얼로그 상태
    var showPolicyDetailDialog by remember { mutableStateOf(false) }
    var detailPolicy by remember { mutableStateOf<PolicyItem?>(null) }
    var showHousingDetailDialog by remember { mutableStateOf(false) }
    var detailHousing by remember { mutableStateOf<ApartmentItem?>(null) }
    
    // 정책과 임대주택 아이템으로 변환 (기존 카드 컴포넌트 재사용용)
    var policyItems by remember {
        mutableStateOf<List<PolicyItem>>(emptyList())
    }
    
    var apartmentItems by remember {
        mutableStateOf<List<ApartmentItem>>(emptyList())
    }
    
    // 임대주택 공고 아이템 (별도 처리)
    var announcementItems by remember {
        mutableStateOf<List<HousingAnnouncementItem>>(emptyList())
    }
    
    // 북마크 새로고침을 위한 키 (SharedPreferences에서 가져옴)
    val prefs = context.getSharedPreferences("bookmark_prefs", android.content.Context.MODE_PRIVATE)
    var refreshKey by remember { mutableStateOf(prefs.getLong("last_bookmark_update", 0L)) }
    
    // SharedPreferences 변경 감지를 위한 DisposableEffect
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "last_bookmark_update") {
                refreshKey = prefs.getLong("last_bookmark_update", 0L)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    // 서버에서 북마크 가져오기 (상세 정보 포함)
    LaunchedEffect(userId, refreshKey) {
        Log.d("BookmarkActivity", "북마크 로딩 시작: userId=$userId")
        isLoading = true
        try {
            // 정책 북마크 가져오기
            val policyResponse = NetworkModule.apiService.getBookmarks(
                userId = userId,
                contentType = "policy"
            )
            
            // 임대주택 북마크 가져오기
            val housingResponse = NetworkModule.apiService.getBookmarks(
                userId = userId,
                contentType = "housing"
            )
            
            val (policiesList, apartmentsList) = coroutineScope {
                val policiesList = mutableListOf<Pair<Int, PolicyItem>>() // bookmarkId to PolicyItem
                val apartmentsList = mutableListOf<Pair<Int, ApartmentItem>>() // bookmarkId to ApartmentItem
                
                // 정책 북마크 변환 (상세 정보 조회 및 PolicyItem 생성)
                if (policyResponse.isSuccessful && policyResponse.body()?.success == true) {
                    val policyBookmarks = policyResponse.body()?.data ?: emptyList()
                    
                    if (policyBookmarks.isNotEmpty()) {
                        val policyItems = policyBookmarks.map { bookmarkResponse ->
                            async {
                                try {
                                    val detailResponse = NetworkModule.apiService.getPolicyById(
                                        policyId = bookmarkResponse.contentId,
                                        userId = userId
                                    )
                                    
                                    if (detailResponse.isSuccessful && detailResponse.body()?.success == true) {
                                        val policy = detailResponse.body()?.data
                                        if (policy != null) {
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
                                                period = policy.displayApplicationPeriod(),
                                                content = policy.summary ?: "",
                                                applicationMethod = policy.eligibility ?: "",
                                                deadline = policy.applicationEnd?.take(10)?.replace("-", ".") ?: "",
                                                isUrgent = false,
                                                link1 = policy.link1,
                                                link2 = policy.link2
                                            )
                                            Pair(bookmarkResponse.bookmarkId, policyItem)
                                        } else {
                                            null
                                        }
                                    } else {
                                        null
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }
                        val fetchedPolicies = policyItems.awaitAll().filterNotNull()
                        policiesList.addAll(fetchedPolicies)
                    }
                }
                
                // 임대주택 북마크 변환 (상세 정보 조회 및 ApartmentItem 생성)
                if (housingResponse.isSuccessful && housingResponse.body()?.success == true) {
                    val housingBookmarks = housingResponse.body()?.data ?: emptyList()
                    
                    if (housingBookmarks.isNotEmpty()) {
                        // 공고 정보 먼저 조회
                        val noticesResponse = NetworkModule.apiService.getHousingNotices(
                            userId = userId,
                            userIdParam = null,
                            limit = 100
                        )
                        val allNotices = if (noticesResponse.isSuccessful && noticesResponse.body()?.success == true) {
                            noticesResponse.body()?.data ?: emptyList()
                        } else {
                            emptyList()
                        }
                        
                        val apartmentItems = housingBookmarks.map { bookmarkResponse ->
                            async {
                                try {
                                    // contentId가 공고 ID인지 확인
                                    val notice = allNotices.find { it.noticeId == bookmarkResponse.contentId }
                                    if (notice != null) {
                                        // 공고인 경우 HousingAnnouncementItem으로 변환
                                        val announcement = HousingAnnouncementItem(
                                            id = bookmarkResponse.bookmarkId,
                                            noticeId = notice.noticeId,
                                            title = notice.panNm ?: bookmarkResponse.title ?: "",
                                            organization = notice.uppAisTpNm ?: "",
                                            region = notice.cnpCdNm ?: "",
                                            housingType = notice.aisTpCdNm ?: "",
                                            status = notice.panSs ?: "공고중",
                                            deadline = notice.applicationEnd?.take(10)?.replace("-", ".") ?: "",
                                            recruitmentPeriod = notice.displayApplicationPeriod(),
                                            address = "",
                                            totalUnits = 0,
                                            area = "",
                                            deposit = 0,
                                            depositDisplay = "",
                                            monthlyRent = 0,
                                            monthlyRentDisplay = "",
                                            announcementDate = notice.panDt?.take(10)?.replace("-", ".") ?: "",
                                            link = notice.dtlUrl
                                        )
                                        // 공고는 별도 리스트에 추가하므로 null 반환
                                        null
                                    } else {
                                        // contentId로 상세 정보 조회 시도
                                        val detailResponse = NetworkModule.apiService.getHousingById(
                                            housingId = bookmarkResponse.contentId,
                                            userIdParam = userId
                                        )
                                        
                                        if (detailResponse.isSuccessful && detailResponse.body()?.success == true) {
                                            val housing = detailResponse.body()?.data
                                            if (housing != null && !housing.name.isNullOrBlank()) {
                                                
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
                                                null
                                            }
                                        } else {
                                            // getHousingById 실패 시 null 반환 (상세 정보 없이는 표시 불가)
                                            null
                                        }
                                    }
                                } catch (e: Exception) {
                                    // 예외 발생 시 null 반환
                                    null
                                }
                            }
                        }
                        val fetchedApartments = apartmentItems.awaitAll().filterNotNull()
                        apartmentsList.addAll(fetchedApartments)
                        
                        // 공고 아이템 별도 처리
                        val announcementItemsList = mutableListOf<HousingAnnouncementItem>()
                        housingBookmarks.forEach { bookmarkResponse ->
                            val notice = allNotices.find { it.noticeId == bookmarkResponse.contentId }
                            if (notice != null) {
                                val announcement = HousingAnnouncementItem(
                                    id = bookmarkResponse.bookmarkId,
                                    noticeId = notice.noticeId,
                                    title = notice.panNm ?: bookmarkResponse.title ?: "",
                                    organization = notice.uppAisTpNm ?: "",
                                    region = notice.cnpCdNm ?: "",
                                    housingType = notice.aisTpCdNm ?: "",
                                    status = notice.panSs ?: "공고중",
                                    deadline = notice.applicationEnd?.take(10)?.replace("-", ".") ?: "",
                                    recruitmentPeriod = notice.displayApplicationPeriod(),
                                    address = "",
                                    totalUnits = 0,
                                    area = "",
                                    deposit = 0,
                                    depositDisplay = "",
                                    monthlyRent = 0,
                                    monthlyRentDisplay = "",
                                    announcementDate = notice.panDt?.take(10)?.replace("-", ".") ?: "",
                                    link = notice.dtlUrl
                                )
                                announcementItemsList.add(announcement)
                            }
                        }
                        announcementItems = announcementItemsList
                    }
                }
                
                Pair(policiesList, apartmentsList)
            }
            
            // PolicyItem과 ApartmentItem 리스트 설정 (id에 이미 bookmarkId가 저장됨)
            policyItems = policiesList.map { it.second }
            apartmentItems = apartmentsList.mapNotNull { it.second }
            // announcementItems는 이미 위에서 설정됨
            
        } catch (e: Exception) {
            Log.e("BookmarkActivity", "서버에서 북마크 가져오기 실패: ${e.message}", e)
            policyItems = emptyList()
            apartmentItems = emptyList()
        } finally {
            isLoading = false
        }
    }
    
    // Scaffold 제거 -> MainActivity에서 처리
    Column(
            modifier = Modifier
                .fillMaxSize()
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
                                // PolicyCard 사용 (PolicyListActivity의 PolicyCard와 동일)
                                com.wiseyoung.pro.PolicyCard(
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
                        if (apartmentItems.isEmpty() && announcementItems.isEmpty()) {
                            EmptyBookmarkCard(
                                message = "북마크한 임대주택이 없습니다.",
                                modifier = Modifier.padding(top = Spacing.xxl)
                            )
                        } else {
                            // 임대주택 공고 먼저 표시
                            announcementItems.forEach { announcement ->
                                // AnnouncementCard 사용 (HousingMapActivity의 AnnouncementCard와 동일)
                                com.wiseyoung.pro.AnnouncementCard(
                                    announcement = announcement,
                                    isBookmarked = true,
                                    onHeartClick = {
                                        // 북마크 삭제
                                        scope.launch {
                                            try {
                                                NetworkModule.apiService.deleteBookmark(
                                                    userId = userId,
                                                    bookmarkId = announcement.id
                                                )
                                                Log.d("BookmarkActivity", "서버 북마크 삭제 성공: ${announcement.id}")
                                                // 목록에서 제거
                                                announcementItems = announcementItems.filter { it.id != announcement.id }
                                            } catch (e: Exception) {
                                                Log.e("BookmarkActivity", "서버 북마크 삭제 실패: ${e.message}", e)
                                            }
                                        }
                                    },
                                    onDetailClick = {
                                        // TODO: 상세 다이얼로그 표시
                                    },
                                    onApplyClick = {
                                        // 신청하기 링크 열기
                                        val link: String? = announcement.link
                                        if (!link.isNullOrBlank()) {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(link ?: ""))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "링크를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "신청 링크가 제공되지 않았습니다.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.padding(bottom = Spacing.sm)
                                )
                            }
                            
                            // 임대주택 표시
                            apartmentItems.forEach { apartment ->
                                // ApartmentCard 사용 (HousingMapActivity의 ApartmentCard와 동일)
                                com.wiseyoung.pro.ApartmentCard(
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
            
            // Policy Detail Dialog
            if (showPolicyDetailDialog && detailPolicy != null) {
                // PolicyItem을 BookmarkItem으로 변환하여 전달
                val bookmarkItem = BookmarkItem(
                    id = detailPolicy!!.id,
                    type = BookmarkType.POLICY,
                    title = detailPolicy!!.title,
                    organization = detailPolicy!!.organization,
                    age = detailPolicy!!.age,
                    period = detailPolicy!!.period,
                    content = detailPolicy!!.content,
                    applicationMethod = detailPolicy!!.applicationMethod,
                    deadline = detailPolicy!!.deadline,
                    link = detailPolicy!!.link1 ?: detailPolicy!!.link2,
                    contentId = detailPolicy!!.policyId
                )
                
                PolicyDetailDialog(
                    bookmark = bookmarkItem,
                    onDismiss = { showPolicyDetailDialog = false },
                    onApply = {
                        // 정책 신청 링크 처리
                        val link = bookmarkItem.link
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
            
            // Housing Detail Dialog
            if (showHousingDetailDialog && detailHousing != null) {
                // ApartmentItem을 BookmarkItem으로 변환하여 전달
                val bookmarkItem = BookmarkItem(
                    id = detailHousing!!.id,
                    type = BookmarkType.HOUSING,
                    title = detailHousing!!.name,
                    organization = detailHousing!!.organization,
                    address = detailHousing!!.address,
                    deposit = detailHousing!!.depositDisplay,
                    monthlyRent = detailHousing!!.monthlyRentDisplay,
                    area = "${detailHousing!!.area}㎡",
                    completionDate = detailHousing!!.completionDate,
                    distance = detailHousing!!.distance,
                    deadline = detailHousing!!.deadline,
                    link = detailHousing!!.link,
                    contentId = detailHousing!!.housingId
                )
                
                HousingDetailDialog(
                    bookmark = bookmarkItem,
                    isBookmarked = true, // 북마크 화면이므로 항상 북마크됨
                    onDismiss = { showHousingDetailDialog = false },
                    onHeartClick = {
                        // 북마크 삭제
                        scope.launch {
                            try {
                                NetworkModule.apiService.deleteBookmark(
                                    userId = userId,
                                    bookmarkId = detailHousing!!.id
                                )
                                Log.d("BookmarkActivity", "서버 북마크 삭제 성공: ${detailHousing!!.id}")
                                // 목록에서 제거
                                apartmentItems = apartmentItems.filter { it.id != detailHousing!!.id }
                                showHousingDetailDialog = false
                            } catch (e: Exception) {
                                Log.e("BookmarkActivity", "서버 북마크 삭제 실패: ${e.message}", e)
                            }
                        }
                    },
                    onApply = {
                        // 임대주택 신청 링크 처리
                        val link = bookmarkItem.link
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
            containerColor = AppColors.LightBlue.copy(alpha = 0.15f) // 더 진하게 조정 (0.05 -> 0.15)
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
                        BookmarkCategoryTag(it)
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
                        bookmark.address?.let {
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
                                    text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            }
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
                    BookmarkCategoryTag(it)
                }
                
                Spacer(modifier = Modifier.height(Spacing.lg))
                
                bookmark.organization?.let {
                    BookmarkPolicyDetailRow("주관기관명", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                bookmark.age?.let {
                    BookmarkPolicyDetailRow("연령", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                bookmark.period?.let {
                    BookmarkPolicyDetailRow("신청기간", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                bookmark.deadline.takeIf { it.isNotEmpty() }?.let {
                    BookmarkPolicyDetailRow("마감일", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                bookmark.content?.let {
                    BookmarkPolicyDetailRow("정책내용", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                bookmark.applicationMethod?.let {
                    BookmarkPolicyDetailRow("신청방법", it)
                }
                
                Spacer(modifier = Modifier.height(Spacing.xl))
                
                Button(
                    onClick = onApply,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF59ABF7) // 메인 컬러로 변경
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
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
private fun HousingDetailDialog(
    bookmark: BookmarkItem,
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
                    text = bookmark.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(Spacing.lg))
                
                bookmark.address?.let {
                    BookmarkPolicyDetailRow("위치 / 주소", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                if (bookmark.deposit != null && bookmark.monthlyRent != null) {
                    BookmarkPolicyDetailRow("가격", "보증금 ${bookmark.deposit} / 월세 ${bookmark.monthlyRent}")
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                bookmark.area?.let {
                    BookmarkPolicyDetailRow("공급전용면적", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                bookmark.completionDate?.let {
                    BookmarkPolicyDetailRow("준공날짜", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                bookmark.organization?.let {
                    BookmarkPolicyDetailRow("기관명", it)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                BookmarkPolicyDetailRow("마감날짜", bookmark.deadline)
                
                Spacer(modifier = Modifier.height(Spacing.xl))
                
                // 맨 밑에 신청하기 버튼만 (정책과 동일한 구조)
                Button(
                    onClick = onApply,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9A5C) // 주황색
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
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
private fun BookmarkCategoryTag(text: String) {
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
private fun BookmarkPolicyDetailRow(label: String, value: String) {
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
