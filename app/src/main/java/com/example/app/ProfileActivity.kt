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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.ui.theme.AppColors
import com.example.app.ui.theme.Spacing
import com.example.app.ui.theme.ThemeWrapper
import com.example.app.ui.theme.ThemeMode
import com.example.app.ui.theme.ThemePreferences
import com.example.app.ui.components.BottomNavigationBar
import androidx.compose.ui.platform.LocalContext
import com.example.app.data.model.UserProfileResponse
import com.example.app.data.model.DeleteAccountRequest
import com.example.app.network.NetworkModule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.wiseyoung.app.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeWrapper {
                ProfileScreen(
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
                    onNavigateEditProfile = {
                        // TODO: 프로필 편집 화면으로 이동
                    },
                    onNavigateChatbot = {
                        // TODO: 챗봇 다이얼로그 표시
                    },
                    onThemeModeChange = { mode ->
                        // ThemePreferences에 저장
                        ThemePreferences.setThemeMode(this, mode)
                        // ThemeWrapper가 자동으로 SharedPreferences 변경을 감지하여 테마를 업데이트함
                        // recreate()는 필요 없음 - ThemeWrapper가 즉시 반영
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileScreen(
    onNavigateHome: () -> Unit,
    onNavigateCalendar: () -> Unit,
    onNavigateBookmark: () -> Unit,
    onNavigateEditProfile: () -> Unit,
    onNavigateChatbot: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    var themeMode by remember { mutableStateOf(ThemePreferences.getThemeMode(context)) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var profile by remember { mutableStateOf<com.example.app.data.model.UserProfileResponse?>(null) }
    var isLoadingProfile by remember { mutableStateOf(true) }
    var isLoadingLogout by remember { mutableStateOf(false) }
    var isLoadingDelete by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // 프로필 정보 불러오기 (PolicyListActivity와 동일한 API 사용)
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        val userId = currentUser?.uid
        if (userId != null) {
            scope.launch {
                try {
                    android.util.Log.d("ProfileActivity", "프로필 조회 시작: userId=$userId")
                    val response = NetworkModule.apiService.getUserProfile(userId)
                    android.util.Log.d("ProfileActivity", "프로필 응답: code=${response.code()}, success=${response.body()?.success}")
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        profile = response.body()?.data
                        if (profile != null) {
                            android.util.Log.d("ProfileActivity", "✅ 프로필 조회 성공: 닉네임=${profile?.nickname}, 나이=${profile?.age}, 지역=${profile?.region}, 관심사=${profile?.interests}")
                        } else {
                            android.util.Log.w("ProfileActivity", "⚠️ 프로필 데이터가 null입니다.")
                        }
                    } else {
                        val errorMsg = response.body()?.message ?: "알 수 없는 오류"
                        android.util.Log.w("ProfileActivity", "프로필 조회 실패: code=${response.code()}, message=$errorMsg")
                        profile = null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProfileActivity", "프로필 조회 오류: ${e.message}", e)
                    profile = null
                } finally {
                    isLoadingProfile = false
                }
            }
        } else {
            android.util.Log.w("ProfileActivity", "⚠️ userId가 null입니다.")
            isLoadingProfile = false
        }
    }
    
    // ThemePreferences 변경 감지하여 themeMode 상태 업데이트 (ThemeWrapper와 동기화)
    androidx.compose.runtime.DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == ThemePreferences.KEY_THEME_MODE) {
                themeMode = ThemePreferences.getThemeMode(context)
            }
        }
        val prefs = context.getSharedPreferences(ThemePreferences.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = "profile",
                onNavigateHome = onNavigateHome,
                onNavigateCalendar = onNavigateCalendar,
                onNavigateChatbot = onNavigateChatbot,
                onNavigateBookmark = onNavigateBookmark,
                onNavigateProfile = {}
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            ProfileHeader()
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // User Info Card
                UserInfoCard(
                    profile = profile,
                    isLoading = isLoadingProfile,
                    modifier = Modifier.padding(bottom = Spacing.md)
                )
                
                // Edit Profile Button (크기 줄임)
                Button(
                    onClick = onNavigateEditProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),  // 높이 명시적으로 줄임
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),  // 모서리도 약간 줄임
                    contentPadding = PaddingValues(vertical = 12.dp)  // 패딩 줄임
                ) {
                    Text(
                        text = "내정보 변경하기",
                        fontSize = 15.sp,  // 폰트 크기 약간 줄임
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
                
                // Settings Section
                ThemeSettingCard(
                    themeMode = themeMode,
                    onThemeModeChange = { mode ->
                        themeMode = mode
                        ThemePreferences.setThemeMode(context, mode)
                        // ThemeWrapper가 자동으로 감지하여 테마를 업데이트함
                        // recreate()를 호출하지 않고 즉시 반영되도록 함
                    },
                    modifier = Modifier.padding(top = Spacing.md)
                )
                
                // Logout and Delete Account
                Column(
                    modifier = Modifier.padding(top = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Button(
                        onClick = {
                            showLogoutDialog = true
                        },
                        enabled = !isLoadingLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),  // 높이 명시적으로 줄임
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.BackgroundGradientStart
                        ),
                        shape = RoundedCornerShape(12.dp),  // 모서리도 약간 줄임
                        contentPadding = PaddingValues(vertical = 12.dp)  // 패딩 줄임
                    ) {
                        Text(
                            text = "로그아웃",
                            fontSize = 15.sp,  // 폰트 크기 약간 줄임
                            fontWeight = FontWeight.Medium,
                            color = Color.White  // 텍스트 색상 흰색으로 변경 (버튼 배경에 맞춤)
                        )
                    }
                    
                    TextButton(
                        onClick = {
                            val currentUser = auth.currentUser
                            val providerId = currentUser?.providerData?.firstOrNull()?.providerId ?: "password"
                            val isGoogleSignIn = providerId == "google.com"
                            
                            // Google 로그인인 경우 비밀번호 입력 건너뛰기
                            if (isGoogleSignIn) {
                                showDeleteConfirm = true
                            } else {
                                showDeleteDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "회원탈퇴",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
    
    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onConfirm = {
                showLogoutDialog = false
                scope.launch {
                    isLoadingLogout = true
                    try {
                        val currentUser = auth.currentUser
                        val userId = currentUser?.uid
                        
                        if (userId != null) {
                            // 백엔드 로그아웃 API 호출 (실패해도 계속 진행)
                            try {
                                NetworkModule.apiService.logout(userId)
                            } catch (e: Exception) {
                                android.util.Log.w("ProfileActivity", "백엔드 로그아웃 실패 (무시): ${e.message}")
                            }
                        }
                        
                        // Firebase 로그아웃
                        auth.signOut()
                        
                        // Google 로그인도 로그아웃
                        try {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(context.getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build()
                            val googleSignInClient = GoogleSignIn.getClient(context, gso)
                            googleSignInClient.signOut().await()
                        } catch (e: Exception) {
                            android.util.Log.w("ProfileActivity", "Google 로그아웃 실패 (무시): ${e.message}")
                        }
                        
                        // 로그인 화면으로 이동
                        val intent = Intent(context, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(intent)
                        (context as? ComponentActivity)?.finishAffinity()
                    } catch (e: Exception) {
                        android.util.Log.e("ProfileActivity", "로그아웃 실패: ${e.message}", e)
                        Toast.makeText(context, "로그아웃 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    } finally {
                        isLoadingLogout = false
                    }
                }
            },
            onDismiss = {
                showLogoutDialog = false
            }
        )
    }
    
    // Delete Account Dialog - Password Input
    if (showDeleteDialog) {
        DeletePasswordDialog(
            password = deletePassword,
            onPasswordChange = { deletePassword = it },
            onConfirm = {
                showDeleteDialog = false
                showDeleteConfirm = true
            },
            onDismiss = {
                deletePassword = ""
                showDeleteDialog = false
            }
        )
    }
    
    // Delete Account Confirmation
    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            isLoading = isLoadingDelete,
            onConfirm = {
                scope.launch {
                    isLoadingDelete = true
                    try {
                        val currentUser = auth.currentUser
                        val userId = currentUser?.uid
                        
                        if (currentUser == null || userId == null) {
                            Toast.makeText(context, "로그인된 사용자가 없습니다.", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        
                        // 로그인 제공자 확인 (Google 로그인인지 이메일/비밀번호 로그인인지)
                        val providerId = currentUser.providerData.firstOrNull()?.providerId ?: "password"
                        val isGoogleSignIn = providerId == "google.com"
                        
                        // 이메일/비밀번호 로그인인 경우에만 비밀번호 확인
                        if (!isGoogleSignIn) {
                            val email = currentUser.email
                            if (email.isNullOrEmpty()) {
                                Toast.makeText(context, "이메일 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                                isLoadingDelete = false
                                showDeleteConfirm = false
                                deletePassword = ""
                                return@launch
                            }
                            
                            // Firebase 재인증 (비밀번호 확인)
                            try {
                                val credential = EmailAuthProvider.getCredential(email, deletePassword)
                                currentUser.reauthenticate(credential).await()
                            } catch (e: Exception) {
                                android.util.Log.e("ProfileActivity", "Firebase 재인증 실패: ${e.message}", e)
                                Toast.makeText(context, "비밀번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                                isLoadingDelete = false
                                showDeleteConfirm = false
                                deletePassword = ""
                                return@launch
                            }
                        }
                        
                        // 백엔드 회원탈퇴 API 호출
                        try {
                            NetworkModule.apiService.deleteAccount(
                                userId = userId,
                                request = DeleteAccountRequest(deletePassword)
                            )
                        } catch (e: Exception) {
                            android.util.Log.w("ProfileActivity", "백엔드 회원탈퇴 실패 (계속 진행): ${e.message}")
                        }
                        
                        // Firebase 계정 삭제
                        currentUser.delete().await()
                        
                        // Google 로그인도 로그아웃
                        try {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(context.getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build()
                            val googleSignInClient = GoogleSignIn.getClient(context, gso)
                            googleSignInClient.revokeAccess().await()
                        } catch (e: Exception) {
                            android.util.Log.w("ProfileActivity", "Google 계정 해제 실패 (무시): ${e.message}")
                        }
                        
                        Toast.makeText(context, "회원탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        
                        // 로그인 화면으로 이동
                        val intent = Intent(context, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(intent)
                        (context as? ComponentActivity)?.finishAffinity()
                    } catch (e: Exception) {
                        android.util.Log.e("ProfileActivity", "회원탈퇴 실패: ${e.message}", e)
                        Toast.makeText(context, "회원탈퇴 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isLoadingDelete = false
                        showDeleteConfirm = false
                        deletePassword = ""
                    }
                }
            },
            onDismiss = {
                showDeleteConfirm = false
                deletePassword = ""
            }
        )
    }
}

@Composable
private fun ProfileHeader() {
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
                text = "내정보",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun UserInfoCard(
    profile: UserProfileResponse?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp), // PolicyListActivity와 동일
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Border), // PolicyListActivity와 동일
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.sm),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.sm), // PolicyListActivity와 동일
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm), // PolicyListActivity와 동일
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 프로필 아이콘 (PolicyListActivity와 동일한 스타일)
                Box(
                    modifier = Modifier
                        .size(40.dp) // PolicyListActivity와 동일
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient( // PolicyListActivity와 동일한 그라데이션
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
                        tint = Color.White, // PolicyListActivity와 동일
                        modifier = Modifier.size(20.dp) // PolicyListActivity와 동일
                    )
                }
                
                // 닉네임과 정보 (PolicyListActivity와 동일한 형식)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    val nickname = profile?.nickname ?: "슬기로운 청년"
                    Text(
                        text = "$nickname 님",
                        fontSize = 16.sp, // PolicyListActivity와 동일
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 나이, 지역, 취업상태를 한 줄에 작게 표시 (PolicyListActivity와 동일)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        profile?.age?.let {
                            Text(
                                text = "${it}세",
                                fontSize = 11.sp, // PolicyListActivity와 동일
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } ?: Text(
                            text = "25세",
                            fontSize = 11.sp, // PolicyListActivity와 동일
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = "•",
                            fontSize = 11.sp, // PolicyListActivity와 동일
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        
                        if (profile?.region != null) {
                            Text(
                                text = profile.region,
                                fontSize = 11.sp, // PolicyListActivity와 동일
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "경기도 수원시",
                                fontSize = 11.sp, // PolicyListActivity와 동일
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Text(
                            text = "•",
                            fontSize = 11.sp, // PolicyListActivity와 동일
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        
                        if (profile?.jobStatus != null) {
                            Text(
                                text = profile.jobStatus,
                                fontSize = 11.sp, // PolicyListActivity와 동일
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "취업준비생",
                                fontSize = 11.sp, // PolicyListActivity와 동일
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // 관심분야 (작은 태그로 표시) - PolicyListActivity와 동일
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // PolicyListActivity와 동일: userInterests를 기본값으로 사용
                        val userInterests = listOf("일자리", "주거", "복지문화", "교육")
                        val interests = profile?.interests?.takeIf { it.isNotEmpty() } ?: userInterests.take(3)
                        interests.take(3).forEach { interest -> // PolicyListActivity와 동일하게 최대 3개
                            InterestTag(
                                text = interest,
                                backgroundColor = AppColors.LightBlue.copy(alpha = 0.2f), // PolicyListActivity와 동일
                                textColor = AppColors.LightBlue, // PolicyListActivity와 동일
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 관심사 태그 색상 매핑
@Composable
private fun getInterestTagColor(interest: String): Color {
    return when {
        interest.contains("일자리") || interest.contains("취업") || interest.contains("창업") -> 
            AppColors.Purple.copy(alpha = 0.1f)
        interest.contains("주거") || interest.contains("주택") || interest.contains("임대") -> 
            AppColors.BackgroundGradientStart.copy(alpha = 0.1f)
        interest.contains("복지") || interest.contains("문화") -> 
            AppColors.Info.copy(alpha = 0.1f)
        else -> AppColors.Border.copy(alpha = 0.3f)
    }
}

@Composable
private fun getInterestTagTextColor(interest: String): Color {
    return when {
        interest.contains("일자리") || interest.contains("취업") || interest.contains("창업") -> 
            AppColors.Purple
        interest.contains("주거") || interest.contains("주택") || interest.contains("임대") -> 
            AppColors.BackgroundGradientStart
        interest.contains("복지") || interest.contains("문화") -> 
            AppColors.Info
        else -> MaterialTheme.colorScheme.onSurface
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
            .clip(RoundedCornerShape(12.dp)) // PolicyListActivity와 동일
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 2.dp) // PolicyListActivity와 동일
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ThemeSettingCard(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Border),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                }
                Text(
                    text = "테마",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Theme Selection (2개 버튼 - 라이트, 다크만)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AppColors.Border
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ThemeToggleButton(
                        text = "라이트",
                        isSelected = themeMode == ThemeMode.LIGHT,
                        onClick = { onThemeModeChange(ThemeMode.LIGHT) }
                    )
                    ThemeToggleButton(
                        text = "다크",
                        isSelected = themeMode == ThemeMode.DARK,
                        onClick = { onThemeModeChange(ThemeMode.DARK) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeToggleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color.White else Color.Transparent,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = 4.dp),
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun DeletePasswordDialog(
    password: String,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("회원탈퇴") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(
                    text = "본인 확인을 위해 비밀번호를 입력해주세요.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("비밀번호") },
                    placeholder = { Text("비밀번호를 입력하세요") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = password.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626)
                )
            ) {
                Text("확인", color = Color.White)
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
private fun LogoutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("로그아웃") },
        text = {
            Text(
                text = "정말 로그아웃하시겠습니까?",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.BackgroundGradientStart
                )
            ) {
                Text("로그아웃", color = Color.White)
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
private fun DeleteConfirmDialog(
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isLoading) {
                onDismiss()
            }
        },
        title = { Text("정말 탈퇴하시겠습니까?") },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Text(
                    text = "회원 탈퇴 시 모든 정보가 소실되며 복구할 수 없습니다.\n정말 탈퇴하시겠습니까?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626)
                )
            ) {
                Text("탈퇴하기", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("뒤로가기")
            }
        }
    )
}

