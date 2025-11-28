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
import androidx.compose.runtime.LaunchedEffect
import com.example.app.network.NetworkModule
import com.example.app.data.model.BookmarkResponse
import android.util.Log

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
    val deadline: String
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
    
    // 북마크 상태 (서버 + 로컬 병합)
    var bookmarks by remember {
        mutableStateOf<List<BookmarkItem>>(emptyList())
    }
    
    // 서버에서 북마크 가져오기
    LaunchedEffect(userId) {
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
            
            val serverBookmarks = mutableListOf<BookmarkItem>()
            
            // 정책 북마크 변환
            if (policyResponse.isSuccessful && policyResponse.body()?.success == true) {
                val policyBookmarks = policyResponse.body()?.data ?: emptyList()
                policyBookmarks.forEach { bookmarkResponse ->
                    // BookmarkResponse를 BookmarkItem으로 변환
                    // contentId를 사용하여 정책 상세 정보를 가져와야 하지만,
                    // 일단 기본 정보만 사용
                    serverBookmarks.add(
                        BookmarkItem(
                            id = bookmarkResponse.bookmarkId,
                            type = BookmarkType.POLICY,
                            title = bookmarkResponse.title ?: "정책 ${bookmarkResponse.contentId}",
                            organization = bookmarkResponse.organization,
                            deadline = bookmarkResponse.deadline ?: ""
                        )
                    )
                }
            }
            
            // 임대주택 북마크 변환
            if (housingResponse.isSuccessful && housingResponse.body()?.success == true) {
                val housingBookmarks = housingResponse.body()?.data ?: emptyList()
                housingBookmarks.forEach { bookmarkResponse ->
                    serverBookmarks.add(
                        BookmarkItem(
                            id = bookmarkResponse.bookmarkId,
                            type = BookmarkType.HOUSING,
                            title = bookmarkResponse.title ?: "임대주택 ${bookmarkResponse.contentId}",
                            organization = bookmarkResponse.organization,
                            deadline = bookmarkResponse.deadline ?: ""
                        )
                    )
                }
            }
            
            // 로컬 북마크와 병합 (서버 북마크 우선)
            val localBookmarks = BookmarkPreferences.getBookmarks(context)
            val localBookmarkTitles = serverBookmarks.map { it.title }.toSet()
            val mergedBookmarks = serverBookmarks + localBookmarks.filter { 
                !localBookmarkTitles.contains(it.title) 
            }
            
            bookmarks = mergedBookmarks
            Log.d("BookmarkActivity", "서버에서 북마크 가져오기 성공: ${bookmarks.size}개")
        } catch (e: Exception) {
            Log.e("BookmarkActivity", "서버에서 북마크 가져오기 실패: ${e.message}", e)
            // 실패 시 로컬 북마크만 사용
            bookmarks = BookmarkPreferences.getBookmarks(context)
        } finally {
            isLoading = false
        }
    }
    
    // SharedPreferences 변경 감지하여 북마크 새로고침
    androidx.compose.runtime.DisposableEffect(Unit) {
        val prefs = context.getSharedPreferences(BookmarkPreferences.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            // 로컬 북마크 변경 시 서버 북마크와 병합
            val localBookmarks = BookmarkPreferences.getBookmarks(context)
            val serverBookmarkTitles = bookmarks.filter { it.id > 0 }.map { it.title }.toSet()
            val mergedBookmarks = bookmarks.filter { it.id > 0 } + localBookmarks.filter { 
                !serverBookmarkTitles.contains(it.title) 
            }
            bookmarks = mergedBookmarks
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    val policyBookmarks = bookmarks.filter { it.type == BookmarkType.POLICY }
    val housingBookmarks = bookmarks.filter { it.type == BookmarkType.HOUSING }
    
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
                .background(Color.White)
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
                        if (policyBookmarks.isEmpty()) {
                            EmptyBookmarkCard(
                                message = "북마크한 정책이 없습니다.",
                                modifier = Modifier.padding(top = Spacing.xxl)
                            )
                        } else {
                            policyBookmarks.forEach { bookmark ->
                                PolicyBookmarkCard(
                                    bookmark = bookmark,
                                    isExpanded = expandedCardId == bookmark.id,
                                    onToggleExpand = {
                                        expandedCardId = if (expandedCardId == bookmark.id) null else bookmark.id
                                    },
                                    onRemoveBookmark = {
                                        // 서버 북마크인 경우 서버에 삭제 요청
                                        if (bookmark.id > 0) {
                                            scope.launch {
                                                try {
                                                    NetworkModule.apiService.deleteBookmark(
                                                        userId = userId,
                                                        bookmarkId = bookmark.id
                                                    )
                                                    Log.d("BookmarkActivity", "서버 북마크 삭제 성공: ${bookmark.id}")
                                                } catch (e: Exception) {
                                                    Log.e("BookmarkActivity", "서버 북마크 삭제 실패: ${e.message}", e)
                                                }
                                            }
                                        }
                                        // SharedPreferences에서 제거
                                        BookmarkPreferences.removeBookmark(context, bookmark.title, bookmark.type)
                                        // 로컬 상태 업데이트
                                        bookmarks = bookmarks.filter { it.id != bookmark.id }
                                    },
                                    onApply = {
                                        // TODO: 신청하기 로직
                                    },
                                    onNavigateCalendar = onNavigateCalendar,
                                    modifier = Modifier.padding(bottom = Spacing.sm)
                                )
                            }
                        }
                    }
                    "housing" -> {
                        if (housingBookmarks.isEmpty()) {
                            EmptyBookmarkCard(
                                message = "북마크한 임대주택이 없습니다.",
                                modifier = Modifier.padding(top = Spacing.xxl)
                            )
                        } else {
                            housingBookmarks.forEach { bookmark ->
                                HousingBookmarkCard(
                                    bookmark = bookmark,
                                    isExpanded = expandedCardId == bookmark.id,
                                    onToggleExpand = {
                                        expandedCardId = if (expandedCardId == bookmark.id) null else bookmark.id
                                    },
                                    onRemoveBookmark = {
                                        // 서버 북마크인 경우 서버에 삭제 요청
                                        if (bookmark.id > 0) {
                                            scope.launch {
                                                try {
                                                    NetworkModule.apiService.deleteBookmark(
                                                        userId = userId,
                                                        bookmarkId = bookmark.id
                                                    )
                                                    Log.d("BookmarkActivity", "서버 북마크 삭제 성공: ${bookmark.id}")
                                                } catch (e: Exception) {
                                                    Log.e("BookmarkActivity", "서버 북마크 삭제 실패: ${e.message}", e)
                                                }
                                            }
                                        }
                                        // SharedPreferences에서 제거
                                        BookmarkPreferences.removeBookmark(context, bookmark.title, bookmark.type)
                                        // 로컬 상태 업데이트
                                        bookmarks = bookmarks.filter { it.id != bookmark.id }
                                    },
                                    onApply = {
                                        // TODO: 신청하기 로직
                                    },
                                    onNavigateCalendar = onNavigateCalendar,
                                    modifier = Modifier.padding(bottom = Spacing.sm)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
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
                color = AppColors.TextPrimary
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
        color = Color.White,
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
                    .background(AppColors.TextPrimary)
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
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun PolicyBookmarkCard(
    bookmark: BookmarkItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onRemoveBookmark: () -> Unit,
    onApply: () -> Unit,
    onNavigateCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 좋아요 버튼
                IconButton(
                    onClick = onRemoveBookmark,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Remove bookmark",
                        tint = AppColors.TextPrimary,
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
                        color = AppColors.TextPrimary,
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    )
                    
                    if (!isExpanded) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                        bookmark.age?.let {
                            Text(
                                text = "연령: $it",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        bookmark.period?.let {
                            Text(
                                text = "신청기간: $it",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(top = Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            bookmark.organization?.let {
                                PolicyDetailRow("주관기관명", it)
                            }
                            PolicyDetailRow("정책명", bookmark.title)
                            bookmark.age?.let {
                                PolicyDetailRow("연령", it)
                            }
                            bookmark.period?.let {
                                PolicyDetailRow("신청기간", it)
                            }
                            bookmark.content?.let {
                                PolicyDetailRow("정책내용", it)
                            }
                            bookmark.applicationMethod?.let {
                                PolicyDetailRow("신청방법", it)
                            }
                        }
                    }
                }
            }
        }
        
        // 버튼들을 카드 밖에 배치
        Spacer(modifier = Modifier.height(Spacing.sm))
        
        if (!isExpanded) {
            PrimaryButton(
                text = "상세보기",
                onClick = onToggleExpand
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                SecondaryButton(
                    text = "닫기",
                    onClick = onToggleExpand,
                    modifier = Modifier.weight(1f)
                )
                
                PrimaryButton(
                    text = "신청하기",
                    onClick = onApply,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HousingBookmarkCard(
    bookmark: BookmarkItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onRemoveBookmark: () -> Unit,
    onApply: () -> Unit,
    onNavigateCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 좋아요 버튼
                IconButton(
                    onClick = onRemoveBookmark,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Remove bookmark",
                        tint = AppColors.TextPrimary,
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
                        color = AppColors.TextPrimary,
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    )
                    
                    if (!isExpanded) {
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
                    } else {
                        Column(
                            modifier = Modifier.padding(top = Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            bookmark.address?.let {
                                PolicyDetailRow("위치 / 주소", it)
                            }
                            if (bookmark.deposit != null && bookmark.monthlyRent != null) {
                                PolicyDetailRow("가격", "보증금 ${bookmark.deposit} / 월세 ${bookmark.monthlyRent}")
                            }
                            bookmark.area?.let {
                                PolicyDetailRow("공급전용면적", it)
                            }
                            bookmark.completionDate?.let {
                                PolicyDetailRow("준공날짜", it)
                            }
                            bookmark.organization?.let {
                                PolicyDetailRow("기관명", it)
                            }
                            PolicyDetailRow("마감날짜", bookmark.deadline)
                        }
                    }
                }
            }
        }
        
        // 버튼들을 카드 밖에 배치
        Spacer(modifier = Modifier.height(Spacing.sm))
        
        if (!isExpanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                SecondaryButton(
                    text = "캘린더",
                    onClick = onNavigateCalendar,
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = "상세보기",
                    onClick = onToggleExpand,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                SecondaryButton(
                    text = "닫기",
                    onClick = onToggleExpand,
                    modifier = Modifier.weight(1f)
                )
                
                SecondaryButton(
                    text = "캘린더",
                    onClick = onNavigateCalendar,
                    modifier = Modifier.weight(1f)
                )
                
                PrimaryButton(
                    text = "신청하기",
                    onClick = onApply,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PolicyDetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

