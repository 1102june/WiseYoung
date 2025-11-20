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
import com.example.app.ui.theme.WiseYoungTheme
import com.example.app.ui.components.BottomNavigationBar
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar

data class ApartmentItem(
    val id: Int,
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
    val totalUnits: Int
)

val apartments = listOf(
    ApartmentItem(
        id = 1,
        name = "AA 아파트 101동",
        distance = "500m",
        deposit = 10000,
        depositDisplay = "1억",
        monthlyRent = 50,
        monthlyRentDisplay = "50만원",
        deadline = "2025.05.15",
        address = "수원시 팔달구 월 15동로 분당로",
        area = 59,
        completionDate = "2021.01",
        organization = "한국토지주택공사(LH)",
        count = 12,
        region = "수원시",
        housingType = "국민임대",
        heatingType = "개별난방",
        hasElevator = true,
        parkingSpaces = 150,
        convertibleDeposit = "5천만원",
        totalUnits = 200
    ),
    ApartmentItem(
        id = 2,
        name = "BB 아파트 103동",
        distance = "750m",
        deposit = 12000,
        depositDisplay = "1.2억",
        monthlyRent = 60,
        monthlyRentDisplay = "60만원",
        deadline = "2025.05.20",
        address = "수원시 팔달구 월 25동로 분당로",
        area = 46,
        completionDate = "2020.08",
        organization = "SH서울주택도시공사",
        count = 8,
        region = "수원시",
        housingType = "행복주택",
        heatingType = "지역난방",
        hasElevator = true,
        parkingSpaces = 100,
        convertibleDeposit = "4천만원",
        totalUnits = 150
    )
)

data class HousingFilters(
    var region: String = "전체",
    var maxDeposit: Int = 20000,
    var maxMonthlyRent: Int = 100,
    var housingType: String = "전체"
)

class HousingMapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WiseYoungTheme {
                HousingMapScreen(
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
    onNavigateHome: () -> Unit,
    onNavigateCalendar: () -> Unit,
    onNavigateBookmark: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateChatbot: () -> Unit
) {
    var showDetailDialog by remember { mutableStateOf(false) }
    var selectedApartment by remember { mutableStateOf<ApartmentItem?>(null) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedHousing by remember { mutableStateOf<ApartmentItem?>(null) }
    var bookmarkedHousings by remember { mutableStateOf(setOf<String>()) }
    
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
    
    val filteredApartments = apartments.filter { apt ->
        if (filters.region != "전체" && apt.region != filters.region) return@filter false
        if (apt.deposit > filters.maxDeposit) return@filter false
        if (apt.monthlyRent > filters.maxMonthlyRent) return@filter false
        if (filters.housingType != "전체" && apt.housingType != filters.housingType) return@filter false
        true
    }
    
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
            // Header
            HousingMapHeader(onBack = onNavigateHome)
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md)
            ) {
                // Map Container
                MapContainer(
                    onFilterClick = { showFilterDialog = true },
                    modifier = Modifier.padding(bottom = Spacing.md)
                )
                
                // Apartment List
                filteredApartments.forEach { apartment ->
                    ApartmentCard(
                        apartment = apartment,
                        isBookmarked = bookmarkedHousings.contains(apartment.name),
                        onHeartClick = {
                            if (!bookmarkedHousings.contains(apartment.name)) {
                                selectedHousing = apartment
                                showNotificationDialog = true
                            } else {
                                bookmarkedHousings = bookmarkedHousings - apartment.name
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
    
    // Detail Dialog
    if (showDetailDialog && selectedApartment != null) {
        ApartmentDetailDialog(
            apartment = selectedApartment!!,
            isBookmarked = bookmarkedHousings.contains(selectedApartment!!.name),
            onHeartClick = {
                if (!bookmarkedHousings.contains(selectedApartment!!.name)) {
                    selectedHousing = selectedApartment
                    showDetailDialog = false
                    showNotificationDialog = true
                } else {
                    bookmarkedHousings = bookmarkedHousings - selectedApartment!!.name
                }
            },
            onClose = {
                showDetailDialog = false
                selectedApartment = null
            },
            onApply = {
                // TODO: 신청하기 로직
            }
        )
    }
    
    // Notification Dialog
    if (showNotificationDialog) {
        HousingNotificationDialog(
            notifications = notifications,
            onNotificationsChange = { notifications = it },
            onSave = {
                selectedHousing?.let {
                    bookmarkedHousings = bookmarkedHousings + it.name
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
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp),
                    tint = AppColors.TextSecondary
                )
            }
            
            Text(
                text = "임대주택 추천",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun MapContainer(
    onFilterClick: () -> Unit,
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
                    .background(AppColors.Border),
                contentAlignment = Alignment.Center
            ) {
                // Mock Map Icon
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Map",
                    tint = AppColors.TextTertiary,
                    modifier = Modifier.size(48.dp)
                )
                
                // Map Controls - Right
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    MapControlButton(
                        icon = Icons.Default.Add,
                        onClick = { /* Zoom In */ }
                    )
                    MapControlButton(
                        icon = Icons.Default.Remove,
                        onClick = { /* Zoom Out */ }
                    )
                    MapControlButton(
                        icon = Icons.Default.LocationOn,
                        onClick = { /* Current Location */ }
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
                            tint = AppColors.TextSecondary
                        )
                        Text(
                            text = "필터",
                            fontSize = 12.sp,
                            color = AppColors.TextSecondary
                        )
                    }
                }
                
                // Location Markers
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = (-60).dp, y = (-40).dp)
                ) {
                    LocationMarker(count = 12)
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = 60.dp, y = 40.dp)
                ) {
                    LocationMarker(count = 8)
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = 0.dp, y = 60.dp)
                ) {
                    LocationMarker(count = 18)
                }
                
                // Location Label
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-40).dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "지도영역",
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 4.dp)
                    )
                }
            }
            
            // Location Info
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White
            ) {
                Text(
                    text = "경기도 수원시 경복궁과 2구역",
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(Spacing.md)
                )
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
private fun ApartmentCard(
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
                    text = apartment.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(bottom = Spacing.sm)
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = "📍 사용자로부터 ${apartment.distance}",
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = "💰 보증금 ${apartment.depositDisplay} / 월세 ${apartment.monthlyRentDisplay}",
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = "📅 신청마감일: ${apartment.deadline}",
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary
                    )
                }
            }
            
            Button(
                onClick = onDetailClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.TextPrimary
                )
            ) {
                Text("상세보기", color = Color.White)
            }
        }
    }
}

@Composable
private fun ApartmentDetailDialog(
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
                    color = AppColors.TextPrimary,
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
private fun FilterDialog(
    filters: HousingFilters,
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
                    color = AppColors.TextPrimary,
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
                            color = AppColors.TextPrimary,
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
                            color = AppColors.TextPrimary,
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
                    
                    // Max Deposit Slider
                    Column {
                        Text(
                            text = "최대 보증금",
                            fontSize = 14.sp,
                            color = AppColors.TextPrimary,
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
                            color = AppColors.TextSecondary
                        )
                    }
                    
                    // Max Monthly Rent Slider
                    Column {
                        Text(
                            text = "최대 월세",
                            fontSize = 14.sp,
                            color = AppColors.TextPrimary,
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
                            color = AppColors.TextSecondary
                        )
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

