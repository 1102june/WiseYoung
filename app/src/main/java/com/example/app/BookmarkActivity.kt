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
    
    // 상세 다이얼로그 상태
    var showPolicyDetailDialog by remember { mutableStateOf(false) }
    var detailPolicy by remember { mutableStateOf<BookmarkItem?>(null) }
    var showHousingDetailDialog by remember { mutableStateOf(false) }
    var detailHousing by remember { mutableStateOf<BookmarkItem?>(null) }
    
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
                Log.d("BookmarkActivity", "서버에서 정책 북마크 ${policyBookmarks.size}개 가져옴")
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
            } else {
                Log.w("BookmarkActivity", "서버에서 정책 북마크 가져오기 실패: ${policyResponse.code()}, ${policyResponse.message()}")
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
            Log.d("BookmarkActivity", "로컬 북마크 ${localBookmarks.size}개 발견")
            val localBookmarkTitles = serverBookmarks.map { it.title }.toSet()
            val mergedBookmarks = serverBookmarks + localBookmarks.filter { 
                !localBookmarkTitles.contains(it.title) 
            }
            
            bookmarks = mergedBookmarks
            Log.d("BookmarkActivity", "병합된 북마크 총 ${bookmarks.size}개 (서버: ${serverBookmarks.size}, 로컬: ${localBookmarks.size})")
            Log.d("BookmarkActivity", "정책 북마크: ${bookmarks.filter { it.type == BookmarkType.POLICY }.size}개")
            Log.d("BookmarkActivity", "임대주택 북마크: ${bookmarks.filter { it.type == BookmarkType.HOUSING }.size}개")
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
                                    onShowDetail = {
                                        detailPolicy = bookmark
                                        showPolicyDetailDialog = true
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
                                    onShowDetail = {
                                        detailHousing = bookmark
                                        showHousingDetailDialog = true
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
                                    modifier = Modifier.padding(bottom = Spacing.sm)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Policy Detail Dialog
    if (showPolicyDetailDialog && detailPolicy != null) {
        PolicyDetailDialog(
            bookmark = detailPolicy!!,
            onDismiss = { showPolicyDetailDialog = false },
            onApply = {
                // BookmarkItem에는 링크 정보가 없을 수 있음 (현재 데이터 모델 기준)
                // 실제 구현 시에는 BookmarkItem에 link 필드를 추가하거나 API에서 가져와야 함
                Toast.makeText(context, "신청 링크가 제공되지 않았습니다.", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    // Housing Detail Dialog
    if (showHousingDetailDialog && detailHousing != null) {
        HousingDetailDialog(
            bookmark = detailHousing!!,
            onDismiss = { showHousingDetailDialog = false },
            onApply = {
                // 임대주택 신청 링크 처리
                Toast.makeText(context, "신청 링크가 제공되지 않았습니다.", Toast.LENGTH_SHORT).show()
            }
        )
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
                            color = AppColors.TextSecondary
                        )
                    }
                    bookmark.period?.let {
                        Text(
                            text = "신청기간: $it",
                            fontSize = 14.sp,
                            color = AppColors.TextSecondary
                        )
                    }
                    bookmark.deadline.takeIf { it.isNotEmpty() }?.let {
                        Text(
                            text = "마감일: $it",
                            fontSize = 14.sp,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }
            
            // 상세보기 버튼 (오른쪽 하단 배치)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onShowDetail,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("상세보기", color = Color.White)
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
            
            // 상세보기 버튼 (오른쪽 하단 배치)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onShowDetail,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("상세보기", color = Color.White)
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
                    text = bookmark.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
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
                                        AppColors.LightBlue,
                                        Color(0xFF6EBBFF)
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
                        color = AppColors.TextPrimary
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
                    color = AppColors.TextPrimary
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
                                        AppColors.LightBlue,
                                        Color(0xFF6EBBFF)
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

