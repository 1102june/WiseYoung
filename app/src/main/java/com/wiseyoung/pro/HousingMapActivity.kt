package com.wiseyoung.pro

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.wiseyoung.pro.ui.theme.AppColors
import com.wiseyoung.pro.ui.theme.Spacing
import com.wiseyoung.pro.ui.theme.ThemeWrapper
import com.wiseyoung.pro.ui.components.BottomNavigationBar
import com.wiseyoung.pro.service.CalendarService
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.platform.LocalView
import androidx.recyclerview.widget.RecyclerView
import com.kakao.vectormap.MapView
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.LatLng
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.platform.LocalDensity

data class ApartmentItem(
    val id: Int,
    val housingId: String? = null, // 실제 임대주택 ID (북마크용)
    val name: String,
    val distance: String,
    val deposit: Int, // 만원 단위
    val depositDisplay: String,
    val monthlyRent: Int, // 만원 단위
    val monthlyRentDisplay: String,
    val deadline: String,
    val address: String,
    val area: Int, // 제곱미터
    val completionDate: String,
    val organization: String,
    val count: Int,
    val region: String,
    val housingType: String,
    val heatingType: String,
    val hasElevator: Boolean,
    val parkingSpaces: Int,
    val convertibleDeposit: String,
    val totalUnits: Int,
    val link: String? = null, // 신청 링크
    val latitude: Double? = null, // 위도
    val longitude: Double? = null // 경도
)

data class HousingAnnouncementItem(
    val id: Int,
    val noticeId: String? = null, // 실제 공고 ID (북마크용)
    val title: String,
    val organization: String,
    val region: String,
    val housingType: String,
    val status: String, // "접수중", "예정", "마감"
    val deadline: String,
    val recruitmentPeriod: String,
    val address: String,
    val totalUnits: Int,
    val area: String, // "59㎡" 형식
    val deposit: Int, // 만원 단위
    val depositDisplay: String,
    val monthlyRent: Int, // 만원 단위
    val monthlyRentDisplay: String,
    val announcementDate: String,
    val link: String? = null // 신청 링크
)

data class HousingFilters(
    var region: String = "전체",
    var maxDeposit: Int = 20000,
    var maxMonthlyRent: Int = 100,
    var housingType: String = "전체",
    var status: String = "전체" // 공고 탭용
)

// 주소에서 지역 추출 함수
private fun extractRegionFromAddress(address: String): String {
    if (address.isEmpty()) return ""
    // 주소에서 첫 번째 공백 이전의 부분을 지역으로 추출
    // 예: "수원시 팔달구..." -> "수원시"
    val parts = address.split(" ")
    if (parts.isNotEmpty()) {
        val firstPart = parts[0]
        // "시", "도", "군", "구" 등이 포함된 경우 그대로 반환
        if (firstPart.contains("시") || firstPart.contains("도") || firstPart.contains("군")) {
            return firstPart
        }
    }
    return ""
}

class HousingMapActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 카카오맵 SDK는 YouthApplication에서 이미 초기화됨 (중복 초기화 방지)
        val userId = auth.currentUser?.uid ?: "test-user"
        
        setContent {
            ThemeWrapper {
                HousingMapScreen(
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
fun HousingMapScreen(
    userId: String,
    onNavigateHome: () -> Unit,
    onNavigateCalendar: () -> Unit,
    onNavigateBookmark: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateChatbot: () -> Unit
) {
    var activeTab by remember { mutableStateOf<String>("housing") }
    var showDetailDialog by remember { mutableStateOf(false) }
    var selectedApartment by remember { mutableStateOf<Any?>(null) } // ApartmentItem or HousingAnnouncementItem
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedHousing by remember { mutableStateOf<Any?>(null) }
    var showChatbotDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // 챗봇 버튼 위치 상태 (드래그 가능)
    val density = LocalDensity.current
    var chatbotOffsetX by remember { mutableStateOf(0f) }
    var chatbotOffsetY by remember { mutableStateOf(0f) }
    
    // 북마크 초기 상태 불러오기
    var bookmarkedHousings by remember {
        mutableStateOf(
            BookmarkPreferences.getBookmarks(context)
                .filter { it.type == BookmarkType.HOUSING }
                .map { it.title }
                .toSet()
        )
    }
    
    // API 데이터
    var apartmentsList by remember { mutableStateOf<List<ApartmentItem>>(emptyList()) }
    var announcementsList by remember { mutableStateOf<List<HousingAnnouncementItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    var filters by remember {
        mutableStateOf<HousingFilters>(
            HousingFilters()
        )
    }
    
    var notifications by remember {
        mutableStateOf<NotificationSettings>(
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
    
    // 주택 목록 로드 (단지 정보와 공고 정보를 분리된 API로 호출)
    LaunchedEffect(userId) {
        isLoading = true
        errorMessage = null
        
        try {
            // 단지 정보 조회 (housing 탭용)
            val complexesResponse = com.wiseyoung.pro.network.NetworkModule.apiService
                .getHousingComplexes(
                    userId = userId,
                    userIdParam = null,
                    lat = null,
                    lon = null,
                    radius = null,
                    limit = 50
                )
            
            if (complexesResponse.isSuccessful && complexesResponse.body()?.success == true) {
                val complexes = complexesResponse.body()?.data ?: emptyList()
                android.util.Log.d("HousingMapActivity", "단지 정보 조회 성공: ${complexes.size}개")
                
                // HousingComplexResponse를 ApartmentItem으로 변환
                apartmentsList = try {
                    complexes.mapIndexed { index, complex ->
                        ApartmentItem(
                            id = index + 1,
                            housingId = complex.complexId,
                            name = complex.hsmpNm ?: "",
                            distance = complex.distanceFromUser?.let { 
                                try {
                                    "${(it / 1000).toInt()}km"
                                } catch (e: Exception) {
                                    "거리 정보 없음"
                                }
                            } ?: "거리 정보 없음",
                            deposit = try { (complex.deposit ?: 0) / 10000 } catch (e: Exception) { 0 },
                            depositDisplay = try { "${(complex.deposit ?: 0) / 10000}만원" } catch (e: Exception) { "0만원" },
                            monthlyRent = try { (complex.monthlyRent ?: 0) / 10000 } catch (e: Exception) { 0 },
                            monthlyRentDisplay = try { "${(complex.monthlyRent ?: 0) / 10000}만원" } catch (e: Exception) { "0만원" },
                            deadline = "", // 단지 정보에는 마감일이 없음
                            address = complex.rnAdres ?: "",
                            area = try { (complex.supplyArea?.toInt() ?: 0) } catch (e: Exception) { 0 },
                            completionDate = complex.completeDate ?: "",
                            organization = complex.insttNm ?: "",
                            count = 0,
                            region = complex.signguNm ?: complex.brtcNm ?: "",
                            housingType = complex.houseTyNm ?: complex.suplyTyNm ?: "",
                            heatingType = complex.heatMthdDetailNm ?: "",
                            hasElevator = complex.elevator ?: false,
                            parkingSpaces = complex.parkingSpaces ?: 0,
                            convertibleDeposit = "",
                            totalUnits = complex.totalUnits ?: 0,
                            link = null, // 단지 정보에는 링크가 없음
                            latitude = complex.latitude,
                            longitude = complex.longitude
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HousingMapActivity", "ApartmentItem 변환 오류: ${e.message}", e)
                    emptyList()
                }
            } else {
                android.util.Log.e("HousingMapActivity", "단지 정보 조회 실패: ${complexesResponse.body()?.message}")
                apartmentsList = emptyList()
            }
            
            // 공고 정보 조회 (announcement 탭용)
            val noticesResponse = com.wiseyoung.pro.network.NetworkModule.apiService
                .getHousingNotices(
                    userId = userId,
                    userIdParam = null,
                    limit = 50
                )
            
            if (noticesResponse.isSuccessful && noticesResponse.body()?.success == true) {
                val notices = noticesResponse.body()?.data ?: emptyList()
                android.util.Log.d("HousingMapActivity", "공고 정보 조회 성공: ${notices.size}개")
                
                // HousingNoticeResponse를 HousingAnnouncementItem으로 변환
                announcementsList = try {
                    notices.mapIndexed { index, notice ->
                        val region = notice.cnpCdNm ?: ""
                        val applicationStart = notice.applicationStart?.take(10)?.replace("-", ".") ?: ""
                        val applicationEnd = notice.applicationEnd?.take(10)?.replace("-", ".") ?: ""
                        val now = Calendar.getInstance()
                        val deadlineDate = try {
                            if (applicationEnd.isNotEmpty()) {
                                val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                                dateFormat.parse(applicationEnd)
                            } else null
                        } catch (e: Exception) { null }
                        
                        val status = when {
                            deadlineDate == null -> "예정"
                            deadlineDate.before(now.time) -> "마감"
                            else -> "접수중"
                        }
                        
                        // 매칭된 단지 정보가 있으면 그 정보 사용, 없으면 공고 정보 사용
                        val matchedComplex = notice.matchedComplex
                        val supplyArea = matchedComplex?.supplyArea ?: 0.0
                        val deposit = matchedComplex?.deposit ?: 0
                        val monthlyRent = matchedComplex?.monthlyRent ?: 0
                        val totalUnits = matchedComplex?.totalUnits ?: 0
                        
                        HousingAnnouncementItem(
                            id = index + 1,
                            noticeId = notice.noticeId ?: notice.panId, // 실제 공고 ID 저장
                            title = notice.panNm ?: "${region} 입주자 모집",
                            organization = matchedComplex?.insttNm ?: "",
                            region = region,
                            housingType = matchedComplex?.houseTyNm ?: notice.aisTpCdNm ?: "",
                            status = status,
                            deadline = applicationEnd,
                            recruitmentPeriod = if (applicationStart.isNotEmpty() && applicationEnd.isNotEmpty()) {
                                "$applicationStart ~ $applicationEnd"
                            } else {
                                applicationEnd
                            },
                            address = matchedComplex?.rnAdres ?: "",
                            totalUnits = totalUnits,
                            area = try { "${supplyArea.toInt()}㎡" } catch (e: Exception) { "0㎡" },
                            deposit = try { deposit / 10000 } catch (e: Exception) { 0 },
                            depositDisplay = try { "${deposit / 10000}만원" } catch (e: Exception) { "0만원" },
                            monthlyRent = try { monthlyRent / 10000 } catch (e: Exception) { 0 },
                            monthlyRentDisplay = try { "${monthlyRent / 10000}만원" } catch (e: Exception) { "0만원" },
                            announcementDate = applicationStart,
                            link = notice.dtlUrl
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HousingMapActivity", "HousingAnnouncementItem 변환 오류: ${e.message}", e)
                    emptyList()
                }
            } else {
                android.util.Log.e("HousingMapActivity", "공고 정보 조회 실패: ${noticesResponse.body()?.message}")
                announcementsList = emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("HousingMapActivity", "주택 목록 로드 오류: ${e.message}", e)
            errorMessage = "네트워크 오류: ${e.message}"
            apartmentsList = emptyList()
            announcementsList = emptyList()
        } finally {
            isLoading = false
        }
    }
    
    val filteredApartments = apartmentsList.filter { apt ->
        if (filters.region != "전체" && apt.region != filters.region) return@filter false
        if (apt.deposit > filters.maxDeposit) return@filter false
        if (apt.monthlyRent > filters.maxMonthlyRent) return@filter false
        if (filters.housingType != "전체" && apt.housingType != filters.housingType) return@filter false
        true
    }
    
    val filteredAnnouncements = announcementsList.filter { announcement ->
        if (filters.region != "전체" && announcement.region != filters.region) return@filter false
        if (filters.housingType != "전체" && announcement.housingType != filters.housingType) return@filter false
        if (filters.status != "전체" && announcement.status != filters.status) return@filter false
        true
    }
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = "home",
                onNavigateHome = onNavigateHome,
                onNavigateCalendar = onNavigateCalendar,
                onNavigateBookmark = onNavigateBookmark,
                onNavigateProfile = onNavigateProfile
            )
        },
        floatingActionButton = {
            // 챗봇 버튼 (드래그 기능 포함)
            FloatingActionButton(
                onClick = { showChatbotDialog = true },
                containerColor = Color(0xFF59ABF7),
                contentColor = Color.White,
                modifier = Modifier
                    .offset(
                        x = with(density) { chatbotOffsetX.toDp() },
                        y = with(density) { chatbotOffsetY.toDp() }
                    )
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            chatbotOffsetX += dragAmount.x
                            chatbotOffsetY += dragAmount.y
                        }
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "챗봇",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            HousingMapHeader(onBack = onNavigateHome)
            
            // Tab Navigation
            TabNavigation(
                activeTab = activeTab,
                onTabChange = { activeTab = it },
                modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md)
            )
            
            when (activeTab) {
                "housing" -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Map Container를 LazyColumn 밖으로 분리하여 스크롤과 독립적으로 유지
                        // 이렇게 하면 지도가 스크롤과 독립적으로 유지되어 재생성되지 않음
                        MapContainer(
                            onFilterClick = null,
                            totalCount = filteredApartments.size,
                            regionLabel = filters.region.takeUnless { it == "전체" },
                            apartments = filteredApartments,
                            onMarkerClick = { apartment ->
                                selectedApartment = apartment
                                showDetailDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.screenHorizontal)
                                .padding(bottom = Spacing.md)
                        )
                        
                        // 아파트 리스트만 스크롤 가능
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = Spacing.screenHorizontal)
                        ) {
                            items(filteredApartments) { apartment ->
                                ApartmentCard(
                                    apartment = apartment,
                                    isBookmarked = bookmarkedHousings.contains(apartment.name),
                                    onHeartClick = {
                                        if (!bookmarkedHousings.contains(apartment.name)) {
                                            selectedHousing = apartment
                                            showNotificationDialog = true
                                        } else {
                                            // 북마크 제거
                                            bookmarkedHousings = bookmarkedHousings - apartment.name
                                            // 서버에 북마크 삭제 요청
                                            scope.launch {
                                                try {
                                                    // 서버에서 북마크 목록 조회하여 해당 북마크 찾기
                                                    val response = com.wiseyoung.pro.network.NetworkModule.apiService.getBookmarks(
                                                        userId = userId,
                                                        contentType = "housing"
                                                    )
                                                    if (response.isSuccessful && response.body()?.success == true) {
                                                        val bookmarks = response.body()?.data ?: emptyList()
                                                        // contentId로 북마크 찾기
                                                        val bookmark = bookmarks.find { it.contentId == apartment.id.toString() }
                                                        bookmark?.let {
                                                            com.wiseyoung.pro.network.NetworkModule.apiService.deleteBookmark(
                                                                userId = userId,
                                                                bookmarkId = it.bookmarkId
                                                            )
                                                            android.util.Log.d("HousingMapActivity", "서버 북마크 삭제 성공: ${it.bookmarkId}")
                                                            // 북마크 새로고침 플래그 업데이트
                                                            val prefs = context.getSharedPreferences("bookmark_prefs", android.content.Context.MODE_PRIVATE)
                                                            prefs.edit().putLong("last_bookmark_update", System.currentTimeMillis()).apply()
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    android.util.Log.e("HousingMapActivity", "서버 북마크 삭제 실패: ${e.message}", e)
                                                }
                                            }
                                        }
                                    },
                                    onDetailClick = {
                                        selectedApartment = apartment
                                        showDetailDialog = true
                                    },
                                    modifier = Modifier.padding(bottom = Spacing.sm)
                                )
                            }
                        }
                    }
                }
                "announcement"


                            -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = Spacing.screenHorizontal)
                    ) {
                        items(filteredAnnouncements) { announcement ->
                            AnnouncementCard(
                                announcement = announcement,
                                isBookmarked = bookmarkedHousings.contains(announcement.title),
                                onHeartClick = {
                                    if (!bookmarkedHousings.contains(announcement.title)) {
                                        selectedHousing = announcement
                                        showNotificationDialog = true
                                    } else {
                                        // 북마크 제거
                                        bookmarkedHousings = bookmarkedHousings - announcement.title
                                        // 서버에 북마크 삭제 요청
                                        scope.launch {
                                            try {
                                                // 서버에서 북마크 목록 조회하여 해당 북마크 찾기
                                                val response = com.wiseyoung.pro.network.NetworkModule.apiService.getBookmarks(
                                                    userId = userId,
                                                    contentType = "housing"
                                                )
                                                if (response.isSuccessful && response.body()?.success == true) {
                                                    val bookmarks = response.body()?.data ?: emptyList()
                                                    // contentId로 북마크 찾기
                                                    val bookmark = bookmarks.find { it.contentId == announcement.id.toString() }
                                                    bookmark?.let {
                                                        com.wiseyoung.pro.network.NetworkModule.apiService.deleteBookmark(
                                                            userId = userId,
                                                            bookmarkId = it.bookmarkId
                                                        )
                                                        android.util.Log.d("HousingMapActivity", "서버 북마크 삭제 성공: ${it.bookmarkId}")
                                                        // 북마크 새로고침 플래그 업데이트
                                                        val prefs = context.getSharedPreferences("bookmark_prefs", android.content.Context.MODE_PRIVATE)
                                                        prefs.edit().putLong("last_bookmark_update", System.currentTimeMillis()).apply()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("HousingMapActivity", "서버 북마크 삭제 실패: ${e.message}", e)
                                            }
                                        }
                                    }
                                },
                                onDetailClick = {
                                    selectedApartment = announcement
                                    showDetailDialog = true
                                },
                                onApplyClick = {
                                    // 신청하기 버튼 클릭 시 링크 열기
                                    val link = announcement.link
                                    if (!link.isNullOrBlank()) {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
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
                    }
                }
            }
        }
    }
    
    // Detail Dialog
    if (showDetailDialog && selectedApartment != null) {
        when (val item = selectedApartment) {
            is ApartmentItem -> {
                ApartmentDetailDialog(
                    apartment = item,
                    isBookmarked = bookmarkedHousings.contains(item.name),
                    onHeartClick = {
                        if (!bookmarkedHousings.contains(item.name)) {
                            selectedHousing = item
                            showDetailDialog = false
                            showNotificationDialog = true
                        } else {
                            // 북마크 제거
                            bookmarkedHousings = bookmarkedHousings - item.name
                            // 서버에 북마크 삭제 요청
                            scope.launch {
                                try {
                                    val response = com.wiseyoung.pro.network.NetworkModule.apiService.getBookmarks(
                                        userId = userId,
                                        contentType = "housing"
                                    )
                                    if (response.isSuccessful && response.body()?.success == true) {
                                        val bookmarks = response.body()?.data ?: emptyList()
                                        // title로 북마크 찾기
                                        val bookmark = bookmarks.find { it.title == item.name }
                                        bookmark?.let {
                                            com.wiseyoung.pro.network.NetworkModule.apiService.deleteBookmark(
                                                userId = userId,
                                                bookmarkId = it.bookmarkId
                                            )
                                            android.util.Log.d("HousingMapActivity", "서버 북마크 삭제 성공: ${it.bookmarkId}")
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("HousingMapActivity", "서버 북마크 삭제 실패: ${e.message}", e)
                                }
                            }
                        }
                    },
                    onClose = {
                        showDetailDialog = false
                        selectedApartment = null
                    }
                )
            }
            is HousingAnnouncementItem -> {
                AnnouncementDetailDialog(
                    announcement = item,
                    isBookmarked = bookmarkedHousings.contains(item.title),
                    onHeartClick = {
                        if (!bookmarkedHousings.contains(item.title)) {
                            selectedHousing = item
                            showDetailDialog = false
                            showNotificationDialog = true
                        } else {
                            // 북마크 제거
                            bookmarkedHousings = bookmarkedHousings - item.title
                            // 서버에 북마크 삭제 요청
                            scope.launch {
                                try {
                                    val response = com.wiseyoung.pro.network.NetworkModule.apiService.getBookmarks(
                                        userId = userId,
                                        contentType = "housing"
                                    )
                                    if (response.isSuccessful && response.body()?.success == true) {
                                        val bookmarks = response.body()?.data ?: emptyList()
                                        // title로 북마크 찾기
                                        val bookmark = bookmarks.find { it.title == item.title }
                                        bookmark?.let {
                                            com.wiseyoung.pro.network.NetworkModule.apiService.deleteBookmark(
                                                userId = userId,
                                                bookmarkId = it.bookmarkId
                                            )
                                            android.util.Log.d("HousingMapActivity", "서버 북마크 삭제 성공: ${it.bookmarkId}")
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("HousingMapActivity", "서버 북마크 삭제 실패: ${e.message}", e)
                                }
                            }
                        }
                    },
                    onClose = {
                        showDetailDialog = false
                        selectedApartment = null
                    },
                    onApply = {
                        // 신청하기 링크 열기
                        val link = item.link
                        if (link != null && link.isNotEmpty()) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.util.Log.e("HousingMapActivity", "링크 열기 실패: ${e.message}", e)
                                Toast.makeText(context, "링크를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "신청 링크가 제공되지 않았습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            else -> {
                // 타입이 맞지 않으면 다이얼로그를 닫음
                showDetailDialog = false
                selectedApartment = null
            }
        }
    }
    
    // Notification Dialog
    val calendarService = remember { CalendarService(context) }
    
    if (showNotificationDialog) {
        HousingNotificationDialog(
            notifications = notifications,
            onNotificationsChange = { newNotifications -> notifications = newNotifications },
            onSave = {
                selectedHousing?.let { housing ->
                    when (housing) {
                        is ApartmentItem -> {
                            bookmarkedHousings = bookmarkedHousings + housing.name
                            
                            // 서버에 북마크 및 캘린더 일정 저장 (로컬 저장 제거 - 서버에서만 관리)
                            scope.launch {
                                try {
                                    val contentId = housing.housingId ?: run {
                                        android.util.Log.w("HousingMapActivity", "⚠️ housingId가 null입니다. housing.name=${housing.name}, housing.id=${housing.id}")
                                        null
                                    }
                                    
                                    if (contentId == null) {
                                        android.util.Log.e("HousingMapActivity", "❌ housingId가 null이어서 북마크를 저장할 수 없습니다.")
                                        return@launch
                                    }
                                    
                                    android.util.Log.d("HousingMapActivity", "북마크 저장 시작: housing.name=${housing.name}, contentId=$contentId")
                                    val bookmarkResponse = com.wiseyoung.pro.network.NetworkModule.apiService.addBookmark(
                                        userId = userId,
                                        request = com.wiseyoung.pro.data.model.BookmarkRequest(
                                            userId = userId,
                                            contentType = "housing",
                                            contentId = contentId
                                        )
                                    )
                                    
                                    if (bookmarkResponse.isSuccessful && bookmarkResponse.body()?.success == true) {
                                        android.util.Log.d("HousingMapActivity", "✅ 서버 북마크 저장 성공: contentId=$contentId")
                                        
                                        // 북마크 새로고침 플래그 업데이트
                                        val prefs = context.getSharedPreferences("bookmark_prefs", android.content.Context.MODE_PRIVATE)
                                        prefs.edit().putLong("last_bookmark_update", System.currentTimeMillis()).apply()
                                        
                                        // deadline이 비어있지 않은 경우에만 캘린더에 추가
                                        if (housing.deadline.isNotEmpty()) {
                                            // 북마크 저장 성공 후 캘린더 일정 저장
                                            try {
                                                val endDate = housing.deadline.replace(".", "-")
                                                com.wiseyoung.pro.network.NetworkModule.apiService.addCalendarEvent(
                                                    userId = userId,
                                                    request = com.wiseyoung.pro.data.model.CalendarEventRequest(
                                                        userId = userId,
                                                        title = housing.name,
                                                        eventType = "housing",
                                                        endDate = endDate,
                                                        isSevenDaysAlert = notifications.sevenDays,
                                                        sevenDaysAlertTime = notifications.sevenDaysTime,
                                                        isOneDayAlert = notifications.oneDay,
                                                        oneDayAlertTime = notifications.oneDayTime,
                                                        isCustomAlert = notifications.custom,
                                                        customAlertDays = notifications.customDays,
                                                        customAlertTime = notifications.customTime
                                                    )
                                                )
                                                android.util.Log.d("HousingMapActivity", "✅ 서버 캘린더 일정 저장 성공")
                                            } catch (e: Exception) {
                                                android.util.Log.e("HousingMapActivity", "❌ 서버 캘린더 일정 저장 실패: ${e.message}", e)
                                            }
                                            
                                            // 로컬 캘린더에도 추가
                                            calendarService.addHousingToCalendar(
                                                title = housing.name,
                                                organization = housing.organization,
                                                deadline = housing.deadline,
                                                housingId = contentId,
                                                notificationSettings = notifications
                                            )
                                        } else {
                                            android.util.Log.w("HousingMapActivity", "⚠️ deadline이 비어있어서 캘린더에 추가하지 않습니다.")
                                        }
                                    } else {
                                        android.util.Log.e("HousingMapActivity", "❌ 서버 북마크 저장 실패: code=${bookmarkResponse.code()}, message=${bookmarkResponse.body()?.message}")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("HousingMapActivity", "서버 저장 실패: ${e.message}", e)
                                }
                            }
                        }
                        is HousingAnnouncementItem -> {
                            bookmarkedHousings = bookmarkedHousings + housing.title
                            
                            // 서버에 북마크 및 캘린더 일정 저장 (로컬 저장 제거 - 서버에서만 관리)
                            scope.launch {
                                try {
                                    val contentId = housing.noticeId ?: housing.id.toString()
                                    val bookmarkResponse = com.wiseyoung.pro.network.NetworkModule.apiService.addBookmark(
                                        userId = userId,
                                        request = com.wiseyoung.pro.data.model.BookmarkRequest(
                                            userId = userId,
                                            contentType = "housing",
                                            contentId = contentId
                                        )
                                    )
                                    android.util.Log.d("HousingMapActivity", "서버 북마크 저장 성공")
                                    
                                    // 북마크 새로고침 플래그 업데이트
                                    val prefs = context.getSharedPreferences("bookmark_prefs", android.content.Context.MODE_PRIVATE)
                                    prefs.edit().putLong("last_bookmark_update", System.currentTimeMillis()).apply()
                                    
                                    // deadline이 비어있지 않은 경우에만 캘린더에 추가
                                    if (housing.deadline.isNotEmpty()) {
                                        val endDate = housing.deadline.replace(".", "-")
                                        com.wiseyoung.pro.network.NetworkModule.apiService.addCalendarEvent(
                                            userId = userId,
                                            request = com.wiseyoung.pro.data.model.CalendarEventRequest(
                                                userId = userId,
                                                title = housing.title,
                                                eventType = "housing_announcement",
                                                endDate = endDate,
                                                isSevenDaysAlert = notifications.sevenDays,
                                                sevenDaysAlertTime = notifications.sevenDaysTime,
                                                isOneDayAlert = notifications.oneDay,
                                                oneDayAlertTime = notifications.oneDayTime,
                                                isCustomAlert = notifications.custom,
                                                customAlertDays = notifications.customDays,
                                                customAlertTime = notifications.customTime
                                            )
                                        )
                                        android.util.Log.d("HousingMapActivity", "서버 캘린더 일정 저장 성공")
                                        
                                        // 로컬 캘린더에도 추가 (임대주택 공고)
                                        calendarService.addHousingToCalendar(
                                            title = housing.title,
                                            organization = housing.organization,
                                            deadline = housing.deadline,
                                            housingId = housing.id.toString(),
                                            notificationSettings = notifications,
                                            isAnnouncement = true
                                        )
                                    } else {
                                        android.util.Log.w("HousingMapActivity", "⚠️ deadline이 비어있어서 캘린더에 추가하지 않습니다.")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("HousingMapActivity", "서버 저장 실패: ${e.message}", e)
                                }
                            }
                        }
                    }
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
    
    // Filter Dialog
    if (showFilterDialog) {
        FilterDialog(
            filters = filters,
            activeTab = activeTab,
            onFiltersChange = { filters = it },
            onApply = { showFilterDialog = false },
            onDismiss = { showFilterDialog = false }
        )
    }
    
    // 챗봇 다이얼로그
    ChatbotDialog(
        isOpen = showChatbotDialog,
        onClose = { showChatbotDialog = false },
        context = ChatbotContext.NONE
    )
}

@Composable
private fun HousingMapHeader(onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm), // 패딩 축소
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = "임대주택 추천",
                fontSize = 16.sp, // 폰트 크기 축소
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            
            Spacer(modifier = Modifier.size(24.dp)) // 균형 공간 축소
        }
    }
}

@Composable
private fun MapContainer(
    onFilterClick: (() -> Unit)?,
    totalCount: Int,
    regionLabel: String?,
    apartments: List<ApartmentItem>,
    onMarkerClick: ((ApartmentItem) -> Unit)? = null,
    modifier: Modifier = Modifier,
    onMapTouchStart: (() -> Unit)? = null,
    onMapTouchEnd: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Border),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // Map Area
            val rootView = LocalView.current
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .background(AppColors.Border)
                    // 지도 영역에서 발생하는 모든 터치 이벤트를 가로채서 부모 스크롤 비활성화
                    .pointerInteropFilter(
                        onTouchEvent = { event ->
                            when (event.action) {
                                android.view.MotionEvent.ACTION_DOWN -> {
                                    onMapTouchStart?.invoke()
                                    // 터치가 시작되면 부모 스크롤 비활성화
                                    rootView.parent?.let { parent ->
                                        var currentParent: android.view.ViewParent? = parent
                                        while (currentParent != null) {
                                            currentParent.requestDisallowInterceptTouchEvent(true)
                                            if (currentParent is android.view.ViewGroup) {
                                                currentParent.isNestedScrollingEnabled = false
                                                if (currentParent is RecyclerView) {
                                                    currentParent.stopScroll()
                                                }
                                            }
                                            currentParent = currentParent.parent
                                        }
                                    }
                                    false // 터치 이벤트를 MapView로 전달 (지도 드래그 가능)
                                }
                                android.view.MotionEvent.ACTION_MOVE -> {
                                    // MOVE 이벤트에서도 부모 스크롤 비활성화 유지
                                    rootView.parent?.let { parent ->
                                        var currentParent: android.view.ViewParent? = parent
                                        while (currentParent != null) {
                                            currentParent.requestDisallowInterceptTouchEvent(true)
                                            if (currentParent is android.view.ViewGroup) {
                                                currentParent.isNestedScrollingEnabled = false
                                                if (currentParent is RecyclerView) {
                                                    currentParent.stopScroll()
                                                }
                                            }
                                            currentParent = currentParent.parent
                                        }
                                    }
                                    false // 터치 이벤트를 MapView로 전달 (지도 드래그 가능)
                                }
                                android.view.MotionEvent.ACTION_UP,
                                android.view.MotionEvent.ACTION_CANCEL -> {
                                    onMapTouchEnd?.invoke()
                                    // 터치 종료 시에도 부모 스크롤 비활성화 유지 (지도 영역에서는 스크롤 비활성화)
                                    rootView.parent?.let { parent ->
                                        var currentParent: android.view.ViewParent? = parent
                                        while (currentParent != null) {
                                            currentParent.requestDisallowInterceptTouchEvent(true)
                                            if (currentParent is android.view.ViewGroup) {
                                                currentParent.isNestedScrollingEnabled = false
                                                if (currentParent is RecyclerView) {
                                                    currentParent.stopScroll()
                                                }
                                            }
                                            currentParent = currentParent.parent
                                        }
                                    }
                                    false // 터치 이벤트를 MapView로 전달
                                }
                                else -> false
                            }
                        }
                    )
            ) {
                // 카카오맵 SDK MapView 직접 사용
                // MapView를 remember로 유지하여 재생성 방지
                val mapView = remember { mutableStateOf<MapView?>(null) }
                var kakaoMapInstance by remember { mutableStateOf<com.kakao.vectormap.KakaoMap?>(null) }
                var mapError by remember { mutableStateOf<String?>(null) }
                var markersAdded by remember { mutableStateOf(false) }
                
                // 지도 영역에서 스크롤을 완전히 차단
                DisposableEffect(Unit) {
                    val view = rootView
                    // 지도 영역에 포커스가 있을 때 스크롤 비활성화
                    view.parent?.let { parent ->
                        var currentParent: android.view.ViewParent? = parent
                        while (currentParent != null) {
                            if (currentParent is android.view.ViewGroup) {
                                currentParent.isNestedScrollingEnabled = false
                                if (currentParent is RecyclerView) {
                                    currentParent.stopScroll()
                                }
                            }
                            currentParent = currentParent.parent
                        }
                    }
                    onDispose {
                        // 정리 작업
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        // 이미 MapView가 생성되어 있으면 재사용
                        if (mapView.value != null) {
                            return@AndroidView mapView.value!!
                        }
                        try {
                            val view = MapView(ctx)
                            // 지도 터치 이벤트가 부모 스크롤과 간섭하지 않도록 설정
                            view.isClickable = true
                            view.isFocusable = true
                            view.isFocusableInTouchMode = true
                            
                            // 터치 이벤트 리스너로 부모 스크롤 간섭 방지 (강화)
                            // MapView가 터치 이벤트를 처리하도록 하되, 부모 스크롤은 완전히 비활성화
                            var touchStartX = 0f
                            var touchStartY = 0f
                            var isMapDragging = false
                            
                            view.setOnTouchListener { v, event ->
                                when (event.action) {
                                    android.view.MotionEvent.ACTION_DOWN -> {
                                        touchStartX = event.x
                                        touchStartY = event.y
                                        isMapDragging = false
                                        
                                        // DOWN 이벤트에서 즉시 모든 부모 뷰의 스크롤 비활성화
                                        var parent = v.parent
                                        while (parent != null) {
                                            parent.requestDisallowInterceptTouchEvent(true)
                                            if (parent is android.view.ViewGroup) {
                                                parent.isNestedScrollingEnabled = false
                                                // RecyclerView인 경우 즉시 스크롤 중지
                                                if (parent is RecyclerView) {
                                                    parent.stopScroll()
                                                }
                                            }
                                            parent = parent.parent
                                        }
                                    }
                                    android.view.MotionEvent.ACTION_MOVE -> {
                                        // 이동 거리 계산
                                        val dx = kotlin.math.abs(event.x - touchStartX)
                                        val dy = kotlin.math.abs(event.y - touchStartY)
                                        
                                        // 수평 또는 수직으로 3px 이상 이동하면 지도 드래그로 판단
                                        if (dx > 3 || dy > 3) {
                                            isMapDragging = true
                                        }
                                        
                                        // MOVE 이벤트에서는 항상 부모 스크롤 비활성화 (위/아래 드래그 포함)
                                        // 모든 부모 뷰를 순회하며 스크롤 차단
                                        var parent = v.parent
                                        while (parent != null) {
                                            parent.requestDisallowInterceptTouchEvent(true)
                                            if (parent is android.view.ViewGroup) {
                                                parent.isNestedScrollingEnabled = false
                                                // LazyColumn의 RecyclerView인 경우 스크롤 완전히 차단
                                                if (parent.javaClass.simpleName.contains("RecyclerView") ||
                                                    parent.javaClass.simpleName.contains("LazyList")) {
                                                    parent.isNestedScrollingEnabled = false
                                                    // RecyclerView의 스크롤 중지
                                                    try {
                                                        val recyclerView = parent as? RecyclerView
                                                        recyclerView?.stopScroll()
                                                    } catch (e: Exception) {
                                                        // RecyclerView가 아니면 무시
                                                    }
                                                }
                                            }
                                            parent = parent.parent
                                        }
                                    }
                                    android.view.MotionEvent.ACTION_UP,
                                    android.view.MotionEvent.ACTION_CANCEL -> {
                                        // 터치 종료 시에도 부모 스크롤 비활성화 유지 (지도 영역에서는 스크롤 비활성화)
                                        // 지도 영역에서는 스크롤을 완전히 차단
                                        var parent = v.parent
                                        while (parent != null) {
                                            parent.requestDisallowInterceptTouchEvent(true)
                                            if (parent is android.view.ViewGroup) {
                                                parent.isNestedScrollingEnabled = false
                                                if (parent is RecyclerView) {
                                                    parent.stopScroll()
                                                }
                                            }
                                            parent = parent.parent
                                        }
                                        isMapDragging = false
                                    }
                                }
                                // false를 반환하여 MapView가 터치 이벤트를 처리하도록 함
                                false
                            }
                            
                            // 추가: MapView 자체의 스크롤 가능 여부 설정
                            view.isScrollContainer = false
                            view.isNestedScrollingEnabled = false
                            
                            view.start(object : com.kakao.vectormap.MapLifeCycleCallback() {
                                override fun onMapDestroy() {
                                    android.util.Log.d("HousingMapActivity", "Kakao Map Destroyed")
                                    kakaoMapInstance = null
                                }

                                override fun onMapError(error: Exception?) {
                                    android.util.Log.e("HousingMapActivity", "Kakao Map Error: ${error?.message}", error)
                                    mapError = error?.message ?: "지도 로드 오류"
                                }
                            }, object : com.kakao.vectormap.KakaoMapReadyCallback() {
                                override fun onMapReady(kakaoMap: com.kakao.vectormap.KakaoMap) {
                                    android.util.Log.d("HousingMapActivity", "Kakao Map Ready")
                                    mapError = null
                                    kakaoMapInstance = kakaoMap
                                    
                                    // 지도 클릭 이벤트는 LabelManager의 클릭 리스너로 처리됨
                                    
                                    // 마커는 LaunchedEffect에서 apartments 리스트가 준비되면 추가됨
                                }
                            })
                            mapView.value = view
                            view
                        } catch (e: Exception) {
                            android.util.Log.e("HousingMapActivity", "Map creation failed: ${e.message}", e)
                            mapError = "지도를 불러올 수 없습니다: ${e.message}"
                            android.widget.TextView(ctx).apply {
                                text = "지도를 불러올 수 없습니다.\n${e.message}"
                                textSize = 12f
                                gravity = android.view.Gravity.CENTER
                                setPadding(16, 16, 16, 16)
                                setBackgroundColor(android.graphics.Color.LTGRAY)
                                setTextColor(android.graphics.Color.DKGRAY)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        // MapView 업데이트 시 추가 작업 없음
                        // 마커는 LaunchedEffect에서 처리됨
                        // MapView가 이미 생성되어 있으면 재생성하지 않음
                        if (mapView.value == null && view is MapView) {
                            mapView.value = view
                        }
                    }
                )
                
                // 아파트 리스트와 지도가 준비되면 마커 추가 (한 번만 실행)
                val context = LocalContext.current
                LaunchedEffect(apartments.size, kakaoMapInstance) {
                    if (apartments.isNotEmpty() && kakaoMapInstance != null && !markersAdded) {
                        android.util.Log.d("HousingMapActivity", "마커 추가 시작: 아파트 ${apartments.size}개")
                        addApartmentMarkersWithGeocoding(
                            context = context,
                            kakaoMap = kakaoMapInstance!!,
                            apartments = apartments,
                            onMarkerClick = onMarkerClick
                        )
                        markersAdded = true
                    }
                }
                
                // 지도 로딩 중 또는 오류 표시
                if (mapError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = mapError ?: "지도를 불러오는 중입니다...",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }


                // MapView 생명주기 정리
                DisposableEffect(Unit) {
                    onDispose {
                        // v2에서는 start() 시 전달한 callback에서 destroy 처리되거나
                        // 뷰가 제거될 때 자동으로 처리됨 (일반적인 View)
                    }
                }
                
                // Map Controls - Right
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    MapControlButton(
                        icon = Icons.Default.Add,
                        onClick = { 
                            // 줌 인 기능
                            kakaoMapInstance?.let { map ->
                                try {
                                    // 방법 1: CameraUpdateFactory.zoomIn() 사용
                                    map.moveCamera(
                                        com.kakao.vectormap.camera.CameraUpdateFactory.zoomIn()
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("HousingMapActivity", "줌 인 실패: ${e.message}", e)
                                    // 방법 2: 현재 줌 레벨 가져와서 증가
                                    try {
                                        val currentPosition = map.cameraPosition
                                        if (currentPosition != null) {
                                            val currentZoom = currentPosition.zoomLevel
                                            val newZoom = (currentZoom + 1).coerceAtMost(20)
                                            map.moveCamera(
                                                com.kakao.vectormap.camera.CameraUpdateFactory.zoomTo(newZoom)
                                            )
                                        }
                                    } catch (e2: Exception) {
                                        android.util.Log.e("HousingMapActivity", "줌 인 대안 방법 실패: ${e2.message}", e2)
                                    }
                                }
                            }
                        }
                    )
                    MapControlButton(
                        icon = Icons.Default.Remove,
                        onClick = { 
                            // 줌 아웃 기능
                            kakaoMapInstance?.let { map ->
                                try {
                                    // 방법 1: CameraUpdateFactory.zoomOut() 사용
                                    map.moveCamera(
                                        com.kakao.vectormap.camera.CameraUpdateFactory.zoomOut()
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("HousingMapActivity", "줌 아웃 실패: ${e.message}", e)
                                    // 방법 2: 현재 줌 레벨 가져와서 감소
                                    try {
                                        val currentPosition = map.cameraPosition
                                        if (currentPosition != null) {
                                            val currentZoom = currentPosition.zoomLevel
                                            val newZoom = (currentZoom - 1).coerceAtLeast(1)
                                            map.moveCamera(
                                                com.kakao.vectormap.camera.CameraUpdateFactory.zoomTo(newZoom)
                                            )
                                        }
                                    } catch (e2: Exception) {
                                        android.util.Log.e("HousingMapActivity", "줌 아웃 대안 방법 실패: ${e2.message}", e2)
                                    }
                                }
                            }
                        }
                    )
                    MapControlButton(
                        icon = Icons.Default.LocationOn,
                        onClick = { 
                            // 현재 위치로 이동 기능 - 카카오맵 SDK v2 API 확인 필요
                            // TODO: 위치 권한 확인 및 현재 위치 가져오기 구현
                            android.util.Log.d("HousingMapActivity", "현재 위치 버튼 클릭")
                        }
                    )
                }
                
                // Filter Button - Left (onFilterClick이 null이 아닐 때만 표시)
                onFilterClick?.let { filterClick ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(Spacing.md)
                            .clickable { filterClick() },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filter",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "필터",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // 검은 동그라미 제거 - 실제 마커로 대체됨

                // Location Label (선택된 지역 기준 동적 텍스트)
                regionLabel?.let { label ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = (-40).dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = "$label 주변 임대주택",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .padding(Spacing.sm),
            tint = AppColors.TextSecondary
        )
    }
}

@Composable
private fun LocationMarker(count: Int) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(AppColors.TextPrimary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun TabNavigation(
    activeTab: String,
    onTabChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        TabButton(
            text = "임대주택",
            isSelected = activeTab == "housing",
            onClick = { onTabChange("housing") },
            modifier = Modifier.weight(1f)
        )
        TabButton(
            text = "임대주택 공고",
            isSelected = activeTab == "announcement",
            onClick = { onTabChange("announcement") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isHousingTab = text == "임대주택" || text == "임대주택 공고"
    val selectedColor = if (isHousingTab) {
        Color(0xFFFF9800) // 주황색
    } else {
        null // 기본 그라데이션 사용
    }
    
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color.Transparent else AppColors.Border.copy(alpha = 0.3f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isSelected) {
                        if (selectedColor != null) {
                            Modifier.background(selectedColor)
                        } else {
                            Modifier.background(
                                Brush.horizontalGradient(
                                    colors = listOf(AppColors.Purple, AppColors.BackgroundGradientStart)
                                )
                            )
                        }
                    } else {
                        Modifier.background(
                            Brush.horizontalGradient(colors = listOf(Color.Transparent, Color.Transparent))
                        )
                    }
                )
                .padding(vertical = Spacing.sm, horizontal = Spacing.sm)
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) Color.White else AppColors.TextSecondary,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun ApartmentCard(
    apartment: ApartmentItem,
    isBookmarked: Boolean,
    onHeartClick: () -> Unit,
    onDetailClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onDetailClick() },
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, AppColors.BackgroundGradientStart.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.BackgroundGradientStart.copy(alpha = 0.05f)
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
                    text = apartment.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
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
            
            // 정보 텍스트
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                apartment.address?.let { address ->
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
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "💰 보증금 ${apartment.depositDisplay} / 월세 ${apartment.monthlyRentDisplay}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ApartmentDetailDialog(
    apartment: ApartmentItem,
    isBookmarked: Boolean,
    onHeartClick: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
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
                        IconButton(onClick = onClose) {
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
                    text = apartment.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(Spacing.lg))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    ApartmentDetailRow("위치 / 주소", apartment.address)
                    ApartmentDetailRow("가격", "보증금 ${apartment.depositDisplay} / 월세 ${apartment.monthlyRentDisplay}")
                    ApartmentDetailRow("공급전용면적", "${apartment.area}㎡ (${sqmToPyeong(apartment.area)}평)")
                    ApartmentDetailRow("준공날짜", "${apartment.completionDate} (${getYearsSince(apartment.completionDate)}년차)")
                    ApartmentDetailRow("기관명", apartment.organization)
                    ApartmentDetailRow("지역", apartment.region)
                    ApartmentDetailRow("주택유형", apartment.housingType)
                    ApartmentDetailRow("난방종류", apartment.heatingType)
                    ApartmentDetailRow("엘리베이터", if (apartment.hasElevator) "있음" else "없음")
                    ApartmentDetailRow("주차공간", "${apartment.parkingSpaces}대")
                    ApartmentDetailRow("총세대수", "${apartment.totalUnits}세대")
                }
                
                Spacer(modifier = Modifier.height(Spacing.xl))
            }
        }
    }
}

@Composable
private fun ApartmentDetailRow(label: String, value: String) {
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
fun AnnouncementCard(
    announcement: HousingAnnouncementItem,
    isBookmarked: Boolean,
    onHeartClick: () -> Unit,
    onDetailClick: () -> Unit,
    onApplyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, AppColors.BackgroundGradientStart.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.BackgroundGradientStart.copy(alpha = 0.05f)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = announcement.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    )
                }
                
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
            
            // 고정 정보: 모집기간, 주택유형, 지역, 공고중
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.padding(bottom = Spacing.md)
            ) {
                Text(
                    text = "📅 모집기간: ${announcement.recruitmentPeriod}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "🏠 주택유형: ${announcement.housingType}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "📍 지역: ${announcement.region}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "📢 공고중",
                    fontSize = 14.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Medium
                )
            }
            
            // 신청하기 버튼
            Button(
                onClick = onApplyClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.BackgroundGradientStart
                )
            ) {
                Text(
                    text = "신청하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun AnnouncementDetailDialog(
    announcement: HousingAnnouncementItem,
    isBookmarked: Boolean,
    onHeartClick: () -> Unit,
    onClose: () -> Unit,
    onApply: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
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
                        IconButton(onClick = onClose) {
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
                    text = announcement.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(Spacing.lg))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    ApartmentDetailRow("위치 / 주소", announcement.address)
                    ApartmentDetailRow("가격", "보증금 ${announcement.depositDisplay} / 월세 ${announcement.monthlyRentDisplay}")
                    ApartmentDetailRow("공급전용면적", announcement.area)
                    ApartmentDetailRow("기관명", announcement.organization)
                    ApartmentDetailRow("모집기간", announcement.recruitmentPeriod)
                    ApartmentDetailRow("지역", announcement.region)
                    ApartmentDetailRow("주택유형", announcement.housingType)
                    ApartmentDetailRow("공고상태", announcement.status)
                }
            }
        }
    }
}

@Composable
private fun FilterDialog(
    filters: HousingFilters,
    activeTab: String,
    onFiltersChange: (HousingFilters) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    var localFilters by remember { mutableStateOf(filters) }
    
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
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "필터 설정",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = Spacing.md)
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // Region Filter
                    Column {
                        Text(
                            text = "지역",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = Spacing.xs)
                        )
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = localFilters.region,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                )
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                            
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                listOf("전체", "수원시", "서울시", "부산시").forEach { region ->
                                    DropdownMenuItem(
                                        text = { Text(region) },
                                        onClick = {
                                            localFilters = localFilters.copy(region = region)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Housing Type Filter
                    Column {
                        Text(
                            text = "주택유형",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = Spacing.xs)
                        )
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = localFilters.housingType,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                )
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                            
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                listOf("전체", "국민임대", "행복주택", "영구임대", "장기전세").forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            localFilters = localFilters.copy(housingType = type)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Max Deposit Slider (임대주택 탭에서만 표시)
                    if (activeTab == "housing") {
                        Column {
                            Text(
                                text = "최대 보증금",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = Spacing.xs)
                            )
                            Slider(
                                value = localFilters.maxDeposit.toFloat(),
                                onValueChange = { localFilters = localFilters.copy(maxDeposit = it.toInt()) },
                                valueRange = 0f..20000f,
                                steps = 19
                            )
                            Text(
                                text = "보증금: ${localFilters.maxDeposit}만원 이하",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Max Monthly Rent Slider (임대주택 탭에서만 표시)
                        Column {
                            Text(
                                text = "최대 월세",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = Spacing.xs)
                            )
                            Slider(
                                value = localFilters.maxMonthlyRent.toFloat(),
                                onValueChange = { localFilters = localFilters.copy(maxMonthlyRent = it.toInt()) },
                                valueRange = 0f..100f,
                                steps = 9
                            )
                            Text(
                                text = "월세: ${localFilters.maxMonthlyRent}만원 이하",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Status Filter (공고 탭에서만 표시)
                    if (activeTab == "announcement") {
                        Column {
                            Text(
                                text = "공고 상태",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = Spacing.xs)
                            )
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = localFilters.status,
                                        modifier = Modifier.weight(1f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                    )
                                    Icon(
                                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    listOf("전체", "접수중", "예정", "마감").forEach { status ->
                                        DropdownMenuItem(
                                            text = { Text(status) },
                                            onClick = {
                                                localFilters = localFilters.copy(status = status)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.md))
                
                Button(
                    onClick = {
                        onFiltersChange(localFilters)
                        onApply()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.TextPrimary
                    )
                ) {
                    Text("적용하기", color = Color.White)
                }
            }
        }
    }
}


private fun sqmToPyeong(sqm: Int): Int {
    return (sqm / 3.3058).toInt()
}

private fun getYearsSince(completionDate: String): Int {
    return try {
        val dateFormat = SimpleDateFormat("yyyy.MM", Locale.getDefault())
        val completion = dateFormat.parse(completionDate)
        val now = Calendar.getInstance()
        val completionCal = Calendar.getInstance()
        completion?.let { completionCal.time = it }
        
        val years = now.get(Calendar.YEAR) - completionCal.get(Calendar.YEAR)
        val months = now.get(Calendar.MONTH) - completionCal.get(Calendar.MONTH)
        
        if (months < 0 || (months == 0 && now.get(Calendar.DAY_OF_MONTH) < completionCal.get(Calendar.DAY_OF_MONTH))) {
            years - 1
        } else {
            years
        }
    } catch (e: Exception) {
        0
    }
}

/**
 * 도로명 주소를 지오코딩하여 위도/경도로 변환
 */
private suspend fun geocodeAddress(context: android.content.Context, address: String): Pair<Double?, Double?> {
    return withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) {
                android.util.Log.w("HousingMapActivity", "Geocoder가 사용 불가능합니다.")
                return@withContext Pair(null, null)
            }
            
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocationName(address, 1)
            
            if (addresses != null && addresses.isNotEmpty()) {
                val location = addresses[0]
                val lat = location.latitude
                val lon = location.longitude
                android.util.Log.d("HousingMapActivity", "지오코딩 성공: $address -> ($lat, $lon)")
                Pair(lat, lon)
            } else {
                android.util.Log.w("HousingMapActivity", "지오코딩 실패: 주소를 찾을 수 없습니다 - $address")
                Pair(null, null)
            }
        } catch (e: Exception) {
            android.util.Log.e("HousingMapActivity", "지오코딩 오류: $address, ${e.message}", e)
            Pair(null, null)
        }
    }
}

/**
 * 카카오맵에 임대주택 단지정보 마커 추가 (지오코딩 포함)
 * 위도/경도가 없는 경우 도로명 주소를 지오코딩하여 마커를 표시합니다.
 * 
 * 참고: https://apis.map.kakao.com/android_v2/docs/getting-started/mapdraw/
 * - LabelManager를 통해 마커(Label)를 추가합니다
 * - LabelManager -> LabelLayer -> Label 순서로 사용합니다
 */
private suspend fun addApartmentMarkersWithGeocoding(
    context: android.content.Context,
    kakaoMap: com.kakao.vectormap.KakaoMap,
    apartments: List<ApartmentItem>,
    onMarkerClick: ((ApartmentItem) -> Unit)? = null
) {
    try {
        // 위도/경도가 있는 아파트와 없는 아파트 분리
        val apartmentsWithCoordinates = mutableListOf<ApartmentItem>()
        val apartmentsWithoutCoordinates = mutableListOf<ApartmentItem>()
        
        apartments.forEach { apartment ->
            if (apartment.latitude != null && apartment.longitude != null) {
                apartmentsWithCoordinates.add(apartment)
            } else if (apartment.address.isNotEmpty()) {
                apartmentsWithoutCoordinates.add(apartment)
            }
        }
        
        android.util.Log.d("HousingMapActivity", "좌표 있는 아파트: ${apartmentsWithCoordinates.size}, 지오코딩 필요한 아파트: ${apartmentsWithoutCoordinates.size}")
        
        // 지오코딩이 필요한 아파트 처리
        val geocodedApartments = mutableListOf<ApartmentItem>()
        apartmentsWithoutCoordinates.forEach { apartment ->
            val (lat, lon) = geocodeAddress(context, apartment.address)
            if (lat != null && lon != null) {
                geocodedApartments.add(
                    apartment.copy(latitude = lat, longitude = lon)
                )
            }
        }
        
        // 모든 아파트 합치기
        val allApartments = apartmentsWithCoordinates + geocodedApartments
        android.util.Log.d("HousingMapActivity", "마커 추가 가능한 아파트 수: ${allApartments.size}")
        
        if (allApartments.isEmpty()) {
            android.util.Log.w("HousingMapActivity", "마커 추가할 아파트가 없습니다.")
            return
        }
        
        // 마커 추가
        addApartmentMarkers(context, kakaoMap, allApartments, onMarkerClick)
    } catch (e: Exception) {
        android.util.Log.e("HousingMapActivity", "마커 추가 중 오류: ${e.message}", e)
    }
}

/**
 * 카카오맵에 임대주택 단지정보 마커 추가
 * 카카오맵 SDK v2 API를 사용하여 아파트 위치에 마커를 표시합니다.
 * 
 * 참고: https://apis.map.kakao.com/android_v2/docs/getting-started/mapdraw/
 * - LabelManager를 통해 마커(Label)를 추가합니다
 * - LabelManager -> LabelLayer -> Label 순서로 사용합니다
 */
private fun addApartmentMarkers(
    context: android.content.Context,
    kakaoMap: com.kakao.vectormap.KakaoMap,
    apartments: List<ApartmentItem>,
    onMarkerClick: ((ApartmentItem) -> Unit)? = null
) {
    try {
        // 위도/경도가 있는 아파트만 마커 추가
        val validApartments = apartments.filter { it.latitude != null && it.longitude != null }
        android.util.Log.d("HousingMapActivity", "마커 추가 가능한 아파트 수: ${validApartments.size}")
        
        if (validApartments.isEmpty()) {
            android.util.Log.w("HousingMapActivity", "마커 추가할 아파트가 없습니다. (위도/경도 정보가 없음)")
            return
        }
        
        // LabelManager 가져오기 (카카오맵 SDK v2 API)
        // 참고: https://apis.map.kakao.com/android_v2/docs/getting-started/mapdraw/
        // https://apis.map.kakao.com/android_v2/docs/api-guide/label/
        val labelManager = kakaoMap.getLabelManager() ?: run {
            android.util.Log.e("HousingMapActivity", "LabelManager를 가져올 수 없습니다.")
            return
        }
        val labelLayer = labelManager.getLayer() ?: run {
            android.util.Log.e("HousingMapActivity", "LabelLayer를 가져올 수 없습니다.")
            return
        }
        
        // 마커 스타일 생성 (아파트 아이콘 사용)
        val drawable = context.resources.getDrawable(com.wiseyoung.pro.R.drawable.apartment, context.theme)
        
        // 아이콘 크기 조정 (작게 만들기 - 32dp 크기로)
        val iconSize = (32 * context.resources.displayMetrics.density).toInt()
        val bitmap = android.graphics.Bitmap.createBitmap(
            iconSize,
            iconSize,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, iconSize, iconSize)
        drawable.draw(canvas)
        
        val markerStyle = LabelStyle.from(bitmap)
        val markerStyles = LabelStyles.from("apartment_marker_style", markerStyle)
        labelManager.addLabelStyles(markerStyles)
        
        // 마커와 아파트 정보를 매핑하기 위한 맵
        val markerApartmentMap = mutableMapOf<com.kakao.vectormap.label.Label, ApartmentItem>()
        
        // 마커 추가
        validApartments.forEach { apartment ->
            val lat = apartment.latitude ?: return@forEach
            val lon = apartment.longitude ?: return@forEach
            
            try {
                // LatLng 생성 (위도, 경도)
                // 참고: https://apis.map.kakao.com/android_v2/docs/api-guide/coordinate/
                val latLng = LatLng.from(lat, lon)
                
                // LabelOptions 생성 (위치와 스타일 설정)
                // 참고: https://apis.map.kakao.com/android_v2/docs/api-guide/label/
                // setTexts()는 LabelTextBuilder를 받으므로 일단 제거 (아이콘만 표시)
                val labelOptions = LabelOptions.from(latLng)
                    .setStyles(markerStyles)
                
                // 마커 추가
                val label = labelLayer.addLabel(labelOptions)
                
                // 마커와 아파트 정보 매핑
                markerApartmentMap[label] = apartment
                
                android.util.Log.d("HousingMapActivity", "마커 추가 완료: ${apartment.name} - 주소: ${apartment.address}, 좌표: ($lat, $lon)")
            } catch (e: Exception) {
                android.util.Log.e("HousingMapActivity", "마커 추가 실패: ${apartment.name}, 오류: ${e.message}", e)
            }
        }
        
        // LabelManager에 클릭 리스너 등록 (마커 클릭 이벤트 처리)
        if (onMarkerClick != null) {
            // 카카오맵 SDK v2에서는 지도 클릭 이벤트로 처리하고, 클릭된 위치에 가장 가까운 마커를 찾음
            kakaoMap.setOnMapClickListener { kakaoMap, latLng, pointF, poi ->
                // 클릭된 위치에 가장 가까운 마커 찾기
                var closestApartment: ApartmentItem? = null
                var minDistance = Double.MAX_VALUE
                
                markerApartmentMap.forEach { (label, apartment) ->
                    val lat = apartment.latitude ?: return@forEach
                    val lon = apartment.longitude ?: return@forEach
                    
                    // 두 지점 간의 거리 계산 (간단한 유클리드 거리)
                    val latDiff = latLng.latitude - lat
                    val lonDiff = latLng.longitude - lon
                    val distance = kotlin.math.sqrt(latDiff * latDiff + lonDiff * lonDiff)
                    
                    if (distance < minDistance && distance < 0.01) { // 약 1km 이내
                        minDistance = distance
                        closestApartment = apartment
                    }
                }
                
                if (closestApartment != null) {
                    android.util.Log.d("HousingMapActivity", "마커 클릭: ${closestApartment.name}")
                    onMarkerClick(closestApartment)
                }
            }
        }
        
        // 지도 중심점 계산 및 이동 (모든 마커가 보이도록)
        val lats = validApartments.mapNotNull { it.latitude }
        val lons = validApartments.mapNotNull { it.longitude }
        
        if (lats.isNotEmpty() && lons.isNotEmpty()) {
            val minLat = lats.minOrNull() ?: 37.5
            val maxLat = lats.maxOrNull() ?: 37.6
            val minLon = lons.minOrNull() ?: 127.0
            val maxLon = lons.maxOrNull() ?: 127.1
            
            // 지도 중심점 계산
            val centerLat = (minLat + maxLat) / 2
            val centerLon = (minLon + maxLon) / 2
            
            // 카메라 이동 (모든 마커가 보이도록)
            // 참고: https://apis.map.kakao.com/android_v2/docs/api-guide/kakaomap/
            try {
                val centerLatLng = LatLng.from(centerLat, centerLon)
                // 카카오맵 SDK v2의 카메라 이동 API 확인 필요
                // kakaoMap.moveCamera() 또는 다른 메서드 사용
                android.util.Log.d("HousingMapActivity", "지도 중심점: (${centerLat}, ${centerLon}) - 카메라 이동 API 확인 필요")
            } catch (e: Exception) {
                android.util.Log.e("HousingMapActivity", "카메라 이동 실패: ${e.message}", e)
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("HousingMapActivity", "마커 추가 중 오류: ${e.message}", e)
    }
}

/**
 * 임대주택 알림 설정 다이얼로그 (WheelPicker 사용)
 */
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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
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

