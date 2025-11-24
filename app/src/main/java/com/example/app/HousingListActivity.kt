package com.wiseyoung.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.app.data.model.HousingResponse
import com.example.app.network.NetworkModule
import com.example.app.ui.theme.WiseYoungTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class HousingListActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val userId = auth.currentUser?.uid ?: "test-user"
        
        setContent {
            WiseYoungTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HousingListScreen(
                        userId = userId,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HousingListScreen(
    userId: String,
    modifier: Modifier = Modifier
) {
    var housingList by remember { mutableStateOf<List<HousingResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRecommended by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // 주택 목록 로드
    fun loadHousing() {
        isLoading = true
        errorMessage = null
        
        scope.launch {
            try {
                val response = if (showRecommended) {
                    NetworkModule.apiService.getRecommendedHousing(userId, null, null)
                } else {
                    NetworkModule.apiService.getActiveHousing(userId)
                }
                
                if (response.isSuccessful && response.body()?.success == true) {
                    housingList = response.body()?.data ?: emptyList()
                } else {
                    errorMessage = response.body()?.message ?: "주택 목록을 불러올 수 없습니다."
                }
            } catch (e: Exception) {
                errorMessage = "네트워크 오류: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // 초기 로드 및 필터 변경 시 재로드
    LaunchedEffect(showRecommended) {
        loadHousing()
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // 헤더
        TopAppBar(
            title = { Text("임대주택") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            actions = {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !showRecommended,
                        onClick = { showRecommended = false },
                        label = { Text("전체") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = showRecommended,
                        onClick = { showRecommended = true },
                        label = { Text("추천") }
                    )
                }
            }
        )
        
        // 로딩 상태
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }
        
        // 에러 상태
        if (errorMessage != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = errorMessage ?: "오류 발생",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { loadHousing() }) {
                    Text("다시 시도")
                }
            }
            return@Column
        }
        
        // 주택 목록
        if (housingList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "주택이 없습니다.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(housingList) { housing ->
                    HousingCard(housing = housing, userId = userId)
                }
            }
        }
    }
}

@Composable
fun HousingCard(
    housing: HousingResponse,
    userId: String
) {
    var isBookmarked by remember { mutableStateOf(housing.isBookmarked) }
    val scope = rememberCoroutineScope()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = {
            // TODO: 주택 상세 화면으로 이동
            // 사용자 활동 로그: VIEW
            scope.launch {
                try {
                    NetworkModule.apiService.logActivity(
                        userId,
                        com.example.app.data.model.UserActivityRequest(
                            activityType = "VIEW",
                            contentType = "housing",
                            contentId = housing.housingId
                        )
                    )
                } catch (e: Exception) {
                    // 로그 실패는 무시
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = housing.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    housing.address?.let { address ->
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
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                IconButton(
                    onClick = {
                        isBookmarked = !isBookmarked
                        // TODO: 북마크 API 호출
                        scope.launch {
                            try {
                                NetworkModule.apiService.logActivity(
                                    userId,
                                    com.example.app.data.model.UserActivityRequest(
                                        activityType = if (isBookmarked) "BOOKMARK" else "UNBOOKMARK",
                                        contentType = "housing",
                                        contentId = housing.housingId
                                    )
                                )
                            } catch (e: Exception) {
                                // 로그 실패는 무시
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isBookmarked) 
                            Icons.Default.Bookmark 
                        else 
                            Icons.Default.BookmarkBorder,
                        contentDescription = if (isBookmarked) "북마크 해제" else "북마크",
                        tint = if (isBookmarked) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 가격 정보
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                housing.deposit?.let { deposit ->
                    Column {
                        Text(
                            text = "보증금",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${deposit / 10000}만원",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                housing.monthlyRent?.let { rent ->
                    Column {
                        Text(
                            text = "월세",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${rent / 10000}만원",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                housing.supplyArea?.let { area ->
                    Column {
                        Text(
                            text = "면적",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${area}㎡",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // 거리 정보
            housing.distanceFromUser?.let { distance ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "거리: ${String.format("%.1f", distance / 1000)}km",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // 신청 기간
            housing.applicationEnd?.let { endDate ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "신청 마감: $endDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

