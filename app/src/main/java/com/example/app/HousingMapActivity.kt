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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import java.util.Calendar
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.kakao.vectormap.MapView
import com.kakao.vectormap.KakaoMapSdk
import com.google.firebase.auth.FirebaseAuth

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
    val context = LocalContext.current
    
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
            val complexesResponse = com.example.app.network.NetworkModule.apiService
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
            val noticesResponse = com.example.app.network.NetworkModule.apiService
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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = Spacing.screenHorizontal)
                    ) {
                        item {
                            // Map Container
                            MapContainer(
                                onFilterClick = { showFilterDialog = true },
                                totalCount = filteredApartments.size,
                                regionLabel = filters.region.takeUnless { it == "전체" },
                                apartments = filteredApartments,
                                modifier = Modifier.padding(bottom = Spacing.md)
                            )
                        }

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
                                                val response = com.example.app.network.NetworkModule.apiService.getBookmarks(
                                                    userId = userId,
                                                    contentType = "housing"
                                                )
                                                if (response.isSuccessful && response.body()?.success == true) {
                                                    val bookmarks = response.body()?.data ?: emptyList()
                                                    // contentId로 북마크 찾기
                                                    val bookmark = bookmarks.find { it.contentId == apartment.id.toString() }
                                                    bookmark?.let {
                                                        com.example.app.network.NetworkModule.apiService.deleteBookmark(
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
                                onDetailClick = {
                                    selectedApartment = apartment
                                    showDetailDialog = true
                                },
                                modifier = Modifier.padding(bottom = Spacing.sm)
                            )
                        }
                    }
                }
                "announcement" -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = Spacing.screenHorizontal)
                    ) {
                        item {
                            // Filter Button for Announcements
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = Spacing.md),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { showFilterDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Border)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Filter",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    Text(
                                        text = "필터",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
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
                                                val response = com.example.app.network.NetworkModule.apiService.getBookmarks(
                                                    userId = userId,
                                                    contentType = "housing"
                                                )
                                                if (response.isSuccessful && response.body()?.success == true) {
                                                    val bookmarks = response.body()?.data ?: emptyList()
                                                    // contentId로 북마크 찾기
                                                    val bookmark = bookmarks.find { it.contentId == announcement.id.toString() }
                                                    bookmark?.let {
                                                        com.example.app.network.NetworkModule.apiService.deleteBookmark(
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
                                onDetailClick = {
                                    selectedApartment = announcement
                                    showDetailDialog = true
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
                                    val response = com.example.app.network.NetworkModule.apiService.getBookmarks(
                                        userId = userId,
                                        contentType = "housing"
                                    )
                                    if (response.isSuccessful && response.body()?.success == true) {
                                        val bookmarks = response.body()?.data ?: emptyList()
                                        // title로 북마크 찾기
                                        val bookmark = bookmarks.find { it.title == item.name }
                                        bookmark?.let {
                                            com.example.app.network.NetworkModule.apiService.deleteBookmark(
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
                                    val response = com.example.app.network.NetworkModule.apiService.getBookmarks(
                                        userId = userId,
                                        contentType = "housing"
                                    )
                                    if (response.isSuccessful && response.body()?.success == true) {
                                        val bookmarks = response.body()?.data ?: emptyList()
                                        // title로 북마크 찾기
                                        val bookmark = bookmarks.find { it.title == item.title }
                                        bookmark?.let {
                                            com.example.app.network.NetworkModule.apiService.deleteBookmark(
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
            onNotificationsChange = { notifications = it },
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
                                    val bookmarkResponse = com.example.app.network.NetworkModule.apiService.addBookmark(
                                        userId = userId,
                                        request = com.example.app.data.model.BookmarkRequest(
                                            userId = userId,
                                            contentType = "housing",
                                            contentId = contentId
                                        )
                                    )
                                    
                                    if (bookmarkResponse.isSuccessful && bookmarkResponse.body()?.success == true) {
                                        android.util.Log.d("HousingMapActivity", "✅ 서버 북마크 저장 성공: contentId=$contentId")
                                        
                                        // 북마크 저장 성공 후 캘린더 일정 저장
                                        try {
                                            com.example.app.network.NetworkModule.apiService.addCalendarEvent(
                                                userId = userId,
                                                request = com.example.app.data.model.CalendarEventRequest(
                                                    userId = userId,
                                                    title = housing.name,
                                                    eventType = "housing",
                                                    endDate = housing.deadline.replace(".", "-")
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
                                    val bookmarkResponse = com.example.app.network.NetworkModule.apiService.addBookmark(
                                        userId = userId,
                                        request = com.example.app.data.model.BookmarkRequest(
                                            userId = userId,
                                            contentType = "housing",
                                            contentId = housing.id.toString() // HousingAnnouncementItem에는 housingId 필드가 없음
                                        )
                                    )
                                    android.util.Log.d("HousingMapActivity", "서버 북마크 저장 성공")
                                    
                                    com.example.app.network.NetworkModule.apiService.addCalendarEvent(
                                        userId = userId,
                                        request = com.example.app.data.model.CalendarEventRequest(
                                            userId = userId,
                                            title = housing.title,
                                            eventType = "housing",
                                            endDate = housing.deadline.replace(".", "-")
                                        )
                                    )
                                    android.util.Log.d("HousingMapActivity", "서버 캘린더 일정 저장 성공")
                                } catch (e: Exception) {
                                    android.util.Log.e("HousingMapActivity", "서버 저장 실패: ${e.message}", e)
                                }
                            }
                            calendarService.addHousingToCalendar(
                                title = housing.title,
                                organization = housing.organization,
                                deadline = housing.deadline,
                                housingId = housing.id.toString(),
                                notificationSettings = notifications
                            )
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
    onFilterClick: () -> Unit,
    totalCount: Int,
    regionLabel: String?,
    apartments: List<ApartmentItem>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Border),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // Map Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(AppColors.Border)
            ) {
                // 카카오맵 SDK MapView 직접 사용
                var mapView by remember { mutableStateOf<MapView?>(null) }
                var kakaoMapInstance by remember { mutableStateOf<com.kakao.vectormap.KakaoMap?>(null) }
                var mapError by remember { mutableStateOf<String?>(null) }

                AndroidView(
                    factory = { ctx ->
                        try {
                            val view = MapView(ctx)
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
                                    
                                    // 마커 추가는 추후 SDK API 확인 후 구현
                                    // TODO: 카카오맵 SDK API로 마커 추가 구현
                                }
                            })
                            mapView = view
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
                        // MapView 업데이트 로직 (필요시)
                        // 마커 추가는 onMapReady 콜백에서 처리
                    }
                )
                
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
                
                // Filter Button - Left
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(Spacing.md)
                        .clickable { onFilterClick() },
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
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color.Transparent else AppColors.Border.copy(alpha = 0.3f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) {
                        Brush.horizontalGradient(
                            colors = listOf(AppColors.Purple, AppColors.BackgroundGradientStart)
                        )
                    } else {
                        Brush.horizontalGradient(colors = listOf(Color.Transparent, Color.Transparent))
                    }
                )
                .padding(vertical = Spacing.md, horizontal = Spacing.md)
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
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
                Text(
                    text = "📍 사용자로부터 ${apartment.distance}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "💰 보증금 ${apartment.depositDisplay} / 월세 ${apartment.monthlyRentDisplay}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "📅 신청마감일: ${apartment.deadline}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // 상세보기 버튼 (오른쪽 하단, 작게)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDetailClick,
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
fun ApartmentDetailDialog(
    apartment: ApartmentItem,
    isBookmarked: Boolean,
    onHeartClick: () -> Unit,
    onClose: () -> Unit,
    onApply: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
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
                    text = "상세 정보",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = Spacing.md)
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    ApartmentDetailRow("위치 / 주소", apartment.address)
                    ApartmentDetailRow("가격", "보증금 ${apartment.depositDisplay} / 월세 ${apartment.monthlyRentDisplay}")
                    ApartmentDetailRow("공급전용면적", "${apartment.area}㎡ (${sqmToPyeong(apartment.area)}평)")
                    ApartmentDetailRow("준공날짜", "${apartment.completionDate} (${getYearsSince(apartment.completionDate)}년차)")
                    ApartmentDetailRow("기관명", apartment.organization)
                    ApartmentDetailRow("마감날짜", apartment.deadline)
                    ApartmentDetailRow("지역", apartment.region)
                    ApartmentDetailRow("주택유형", apartment.housingType)
                    ApartmentDetailRow("난방종류", apartment.heatingType)
                    ApartmentDetailRow("엘리베이터", if (apartment.hasElevator) "있음" else "없음")
                    ApartmentDetailRow("주차공간", "${apartment.parkingSpaces}대")
                    ApartmentDetailRow("보증금환급금", apartment.convertibleDeposit)
                    ApartmentDetailRow("총세대수", "${apartment.totalUnits}세대")
                }
                
                Spacer(modifier = Modifier.height(Spacing.md))
                
                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onHeartClick) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) AppColors.TextPrimary else AppColors.TextTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Button(
                        onClick = onClose,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Border
                        )
                    ) {
                        Text("닫기", color = AppColors.TextPrimary)
                    }
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
private fun AnnouncementCard(
    announcement: HousingAnnouncementItem,
    isBookmarked: Boolean,
    onHeartClick: () -> Unit,
    onDetailClick: () -> Unit,
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
            // 상단: 상태 뱃지/제목 + 좋아요 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Status Badge
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = Spacing.xs)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (announcement.status) {
                                        "접수중" -> Color(0xFF10B981).copy(alpha = 0.1f)
                                        "예정" -> Color(0xFF3B82F6).copy(alpha = 0.1f)
                                        else -> AppColors.Border.copy(alpha = 0.3f)
                                    }
                                )
                                .padding(horizontal = Spacing.sm, vertical = 4.dp)
                        ) {
                            Text(
                                text = announcement.status,
                                fontSize = 12.sp,
                                color = when (announcement.status) {
                                    "접수중" -> Color(0xFF10B981)
                                    "예정" -> Color(0xFF3B82F6)
                                    else -> AppColors.TextSecondary
                                },
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = announcement.housingType,
                            fontSize = 12.sp,
                            color = AppColors.TextTertiary
                        )
                    }
                    
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
            
            // 정보 텍스트
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text = "🏢 ${announcement.organization}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "📍 ${announcement.address}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "💰 보증금 ${announcement.depositDisplay} / 월세 ${announcement.monthlyRentDisplay}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "📅 모집기간: ${announcement.recruitmentPeriod}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // 상세보기 버튼 (오른쪽 하단, 작게)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDetailClick,
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
                    text = "상세 정보",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = Spacing.md)
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
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
                
                Spacer(modifier = Modifier.height(Spacing.md))
                
                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onHeartClick) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) AppColors.TextPrimary else AppColors.TextTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Button(
                        onClick = onClose,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Border
                        )
                    ) {
                        Text("닫기", color = AppColors.TextPrimary)
                    }
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

