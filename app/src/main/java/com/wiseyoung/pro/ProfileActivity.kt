package com.wiseyoung.pro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.wiseyoung.pro.ui.theme.AppColors
import com.wiseyoung.pro.ui.theme.Spacing
import com.wiseyoung.pro.ui.theme.ThemeWrapper
import com.wiseyoung.pro.ui.theme.ThemeMode
import com.wiseyoung.pro.ui.theme.ThemePreferences
import androidx.compose.ui.platform.LocalContext
import com.wiseyoung.pro.data.model.UserProfileResponse
import com.wiseyoung.pro.data.model.DeleteAccountRequest
import com.wiseyoung.pro.network.NetworkModule
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.wiseyoung.pro.data.model.OtpRequest

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
                    onNavigateIntro = {
                        startActivity(Intent(this, IntroActivity::class.java))
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
    onNavigateIntro: () -> Unit, // 추가
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    var themeMode by remember { mutableStateOf(ThemePreferences.getThemeMode(context)) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteVerification by remember { mutableStateOf(false) } // 이메일 인증 다이얼로그 상태
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var profile by remember { mutableStateOf<com.wiseyoung.pro.data.model.UserProfileResponse?>(null) }
    var isLoadingProfile by remember { mutableStateOf(true) }
    var isLoadingLogout by remember { mutableStateOf(false) }
    var isLoadingDelete by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    var showEditProfileDialog by remember { mutableStateOf(false) }

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
    
    // Scaffold 제거 -> MainActivity에서 처리함
    Column(
        modifier = Modifier
            .fillMaxSize()
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
                
                // Edit Profile Button
                Button(
                    onClick = { showEditProfileDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp), // 높이 약간 증가
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = "내 정보 수정하기", // 문구 수정
                        fontSize = 15.sp,
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
                
                // App Tour Button (앱 정보 보기)
                Button(
                    onClick = onNavigateIntro,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.md), // 고정 높이 제거하여 텍스트 길이에 대응
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.LightBlue
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp) // 내부 여백 충분히 확보
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "앱 정보 보기", // 요청하신 문구로 변경
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
                
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
                            // 비밀번호 입력 건너뛰고 바로 이메일 인증으로 이동
                            showDeleteVerification = true
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
    
    // Delete Account - Email Verification (비밀번호 입력 건너뛰고 바로 이메일 인증)
    if (showDeleteVerification) {
        val currentUser = auth.currentUser
        val email = currentUser?.email ?: ""
        
        DeleteAccountVerificationDialog(
            email = email,
            onVerified = { verifiedOtp ->
                showDeleteVerification = false
                // 인증된 OTP를 저장하고 최종 확인 다이얼로그로 이동
                deletePassword = verifiedOtp // 임시로 OTP를 저장 (변수명은 그대로 유지)
                showDeleteConfirm = true
            },
            onDismiss = {
                showDeleteVerification = false
                deletePassword = "" // 취소 시 OTP 초기화
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
                        val email = currentUser?.email
                        
                        if (currentUser == null || email.isNullOrEmpty()) {
                            Toast.makeText(context, "로그인된 사용자가 없습니다.", Toast.LENGTH_SHORT).show()
                            isLoadingDelete = false
                            showDeleteConfirm = false
                            deletePassword = ""
                            return@launch
                        }
                        
                        // 백엔드 회원탈퇴 API 호출 (이메일과 OTP 전달)
                        val response = NetworkModule.apiService.deleteAccount(
                            DeleteAccountRequest(email = email, otp = deletePassword)
                        )
                        
                        if (response.isSuccessful && response.body()?.success == true) {
                            // 백엔드에서 회원탈퇴 성공
                            android.util.Log.d("ProfileActivity", "백엔드 회원탈퇴 성공")
                            
                            // Firebase 계정 삭제
                            try {
                                currentUser.delete().await()
                            } catch (e: Exception) {
                                android.util.Log.w("ProfileActivity", "Firebase 계정 삭제 실패 (무시): ${e.message}")
                            }
                            
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
                        } else {
                            // 백엔드 회원탈퇴 실패
                            val errorMsg = response.body()?.message ?: "회원탈퇴에 실패했습니다."
                            android.util.Log.e("ProfileActivity", "백엔드 회원탈퇴 실패: $errorMsg")
                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                            isLoadingDelete = false
                            showDeleteConfirm = false
                            deletePassword = ""
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ProfileActivity", "회원탈퇴 실패: ${e.message}", e)
                        Toast.makeText(context, "회원탈퇴 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
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

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        EditProfileDialog(
            currentProfile = profile ?: com.wiseyoung.pro.data.model.UserProfileResponse(
                userId = auth.currentUser?.uid ?: "",
                nickname = "",
                age = null,
                region = "",
                education = null,
                jobStatus = "",
                interests = emptyList()
            ),
            onDismiss = { showEditProfileDialog = false },
            onSave = { nickname, region, jobStatus, interests ->
                scope.launch {
                    try {
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            // Firebase ID Token 가져오기
                            val idToken = try {
                                currentUser.getIdToken(true).await().token
                                    ?: run {
                                        Toast.makeText(context, "인증 토큰을 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                            } catch (e: Exception) {
                                android.util.Log.e("ProfileActivity", "ID Token 발급 실패: ${e.message}", e)
                                Toast.makeText(context, "인증 토큰 발급 실패", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            
                            // region을 province와 city로 분리 (간단한 처리)
                            val regionParts = region.split(" ")
                            val province = regionParts.getOrNull(0) ?: region
                            val city = regionParts.drop(1).joinToString(" ") ?: region
                            
                            // 프로필 업데이트 API 호출
                            val updateRequest = com.wiseyoung.pro.data.model.ProfileRequest(
                                idToken = idToken,
                                nickname = nickname,
                                province = province,
                                city = city,
                                employment = jobStatus,
                                interests = interests
                            )
                            val response = NetworkModule.apiService.saveProfile(updateRequest)
                            if (response.isSuccessful && response.body()?.success == true) {
                                // 성공 시 프로필 다시 로드
                                val profileResponse = NetworkModule.apiService.getUserProfile(currentUser.uid)
                                if (profileResponse.isSuccessful && profileResponse.body()?.success == true) {
                                    profile = profileResponse.body()?.data
                                }
                                Toast.makeText(context, "프로필이 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "프로필 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ProfileActivity", "프로필 업데이트 실패: ${e.message}", e)
                        Toast.makeText(context, "프로필 업데이트 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                showEditProfileDialog = false
            }
        )
    }
} // End of ProfileScreen

@Composable
private fun EditProfileDialog(
    currentProfile: UserProfileResponse,
    onDismiss: () -> Unit,
    onSave: (String, String, String, List<String>) -> Unit
) {
    var nickname by remember { mutableStateOf(currentProfile.nickname ?: "") }
    // region을 province와 city로 분리
    val regionParts = (currentProfile.region ?: "").split(" ")
    var province by remember { mutableStateOf(regionParts.getOrNull(0) ?: "") }
    var city by remember { mutableStateOf(regionParts.drop(1).joinToString(" ") ?: "") }
    var jobStatus by remember { mutableStateOf(currentProfile.jobStatus ?: "") }
    // 관심사는 Set으로 관리
    var interests by remember { mutableStateOf(currentProfile.interests.toSet()) }

    val cityMap = remember { provinceCities }
    val provinceDisplayMap = remember { provinceDisplayNames }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f) // 가로 크기 90%로 증가
                .fillMaxHeight(0.75f), // 세로 크기 축소 (0.85 -> 0.75)
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.md, top = Spacing.md, end = Spacing.md, bottom = 0.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "내 정보 수정",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(bottom = Spacing.md)
                )

                // 닉네임
                NicknameSection(
                    nickname = nickname,
                    onNicknameChange = { nickname = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 거주 지역 (도)
                DropdownSection(
                    label = "거주 지역 (도)",
                    value = province,
                    options = cityMap.keys.toList(),
                    displayMap = provinceDisplayMap,
                    placeholder = "도 선택",
                    onValueChange = {
                        province = it
                        city = ""
                    }
                )

                // 거주 지역 (시)
                if (province.isNotBlank()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    DropdownSection(
                        label = "거주 지역 (시)",
                        value = city,
                        options = cityMap[province].orEmpty(),
                        placeholder = "시 선택",
                        onValueChange = { city = it }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 직업/고용 상태
                DropdownSection(
                    label = "직업/고용 상태",
                    value = jobStatus,
                    options = listOf("학생", "직장인", "구직자", "자영업자"),
                    placeholder = "선택해주세요",
                    onValueChange = { jobStatus = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 관심분야
                InterestSection(
                    selected = interests,
                    onToggle = { interest ->
                        interests = if (interests.contains(interest)) {
                            interests - interest
                        } else {
                            interests + interest
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 버튼
                Button(
                    onClick = {
                        val regionText = if (province.isNotBlank() && city.isNotBlank()) {
                            "${provinceDisplayMap[province] ?: province} ${city}"
                        } else {
                            ""
                        }
                        onSave(nickname, regionText, jobStatus, interests.toList())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF59ABF7),
                        disabledContainerColor = Color(0xFF59ABF7).copy(alpha = 0.4f)
                    ),
                    enabled = nickname.isNotBlank() && province.isNotBlank() && city.isNotBlank() && jobStatus.isNotBlank() && interests.isNotEmpty()
                ) {
                    Text(
                        text = "저장",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Text(
                        text = "취소",
                        fontSize = 14.sp
                    )
                }
            }
        }
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
                            ProfileInterestTag(
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
private fun ProfileInterestTag(
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
private fun DeleteAccountVerificationDialog(
    email: String,
    onVerified: (String) -> Unit, // OTP를 반환하도록 수정
    onDismiss: () -> Unit
) {
    var otp by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var timer by remember { mutableStateOf(300) }
    var isTimerExpired by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 타이머 로직
    LaunchedEffect(otpSent) {
        if (otpSent) {
            timer = 300
            isTimerExpired = false
            while (timer > 0) {
                kotlinx.coroutines.delay(1000)
                timer--
            }
            isTimerExpired = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White, // 배경색 흰색으로 변경
        title = { Text("탈퇴 인증", color = AppColors.LightBlue) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(
                    text = "안전한 탈퇴를 위해 이메일 인증을 진행해주세요.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 이메일 표시
                OutlinedTextField(
                    value = email,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("이메일") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    enabled = false
                )

                if (!otpSent) {
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                try {
                                    // 회원탈퇴용 OTP 발송 API 사용
                                    val response = NetworkModule.apiService.sendOtpForDeleteAccount(OtpRequest(email = email))
                                    if (response.isSuccessful && response.body()?.success == true) {
                                        otpSent = true
                                        Toast.makeText(context, "인증번호가 발송되었습니다.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val errorMsg = response.body()?.message ?: "발송 실패"
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("ProfileActivity", "OTP 발송 실패: ${e.message}", e)
                                    Toast.makeText(context, "오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.LightBlue)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Text("인증번호 발송")
                        }
                    }
                } else {
                    // 인증번호 입력
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { otp = it },
                        label = { Text("인증번호") },
                        placeholder = { Text("인증번호 6자리") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            Text(
                                text = String.format("%02d:%02d", timer / 60, timer % 60),
                                color = if (timer < 60) Color.Red else AppColors.LightBlue,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    )
                    
                    if (isTimerExpired) {
                        Text("인증 시간이 만료되었습니다. 재발송해주세요.", color = Color.Red, fontSize = 12.sp)
                        TextButton(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    try {
                                        // 회원탈퇴용 OTP 재발송 API 사용
                                        val response = NetworkModule.apiService.sendOtpForDeleteAccount(OtpRequest(email = email))
                                        if (response.isSuccessful && response.body()?.success == true) {
                                            timer = 300
                                            isTimerExpired = false
                                            otpSent = true
                                            Toast.makeText(context, "인증번호가 재발송되었습니다.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val errorMsg = response.body()?.message ?: "재발송 실패"
                                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("ProfileActivity", "OTP 재발송 실패: ${e.message}", e)
                                        Toast.makeText(context, "재발송 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        ) {
                            Text("인증번호 재발송")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (otpSent) {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                // OTP 검증 (회원탈퇴용이지만 검증 API는 동일)
                                val response = NetworkModule.apiService.verifyOtp(OtpRequest(email = email, otp = otp))
                                if (response.isSuccessful && response.body()?.success == true) {
                                    Toast.makeText(context, "인증되었습니다.", Toast.LENGTH_SHORT).show()
                                    // 인증된 OTP를 전달
                                    onVerified(otp)
                                } else {
                                    val errorMsg = response.body()?.message ?: "인증 실패"
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ProfileActivity", "OTP 검증 실패: ${e.message}", e)
                                Toast.makeText(context, "오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = otp.isNotEmpty() && !isTimerExpired && !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.LightBlue)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("인증하기", color = Color.White)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = AppColors.LightBlue)
            }
        }
    )
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

// ProfileSetupActivity와 동일한 UI 컴포넌트들
@Composable
private fun NicknameSection(
    nickname: String,
    onNicknameChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "닉네임",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1A1A1A)
        )
        OutlinedTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            placeholder = { Text("", fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color.White, MaterialTheme.shapes.small)
                .padding(horizontal = 4.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color(0xFFF5F5F5),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSection(
    label: String,
    value: String,
    options: List<String>,
    placeholder: String,
    onValueChange: (String) -> Unit,
    displayMap: Map<String, String>? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1A1A1A)
        )
        Box {
            OutlinedButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (value.isBlank()) Color.Gray else Color.Black
                ),
                border = BorderStroke(2.dp, Color(0xFFE5E7EB))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (value.isBlank()) {
                            placeholder
                        } else {
                            displayMap?.get(value) ?: value
                        },
                        color = if (value.isBlank()) Color.Gray else Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = null
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(displayMap?.get(option) ?: option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InterestSection(selected: Set<String>, onToggle: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "관심분야",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1A1A1A)
        )
        val interests = listOf("일자리", "주거", "복지문화", "교육")
        
        // 한 줄로 배치 (가로 스크롤)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(interests) { interest ->
                InterestButton(
                    interest = interest,
                    isSelected = selected.contains(interest),
                    onToggle = { onToggle(interest) }
                )
            }
        }
    }
}

@Composable
private fun InterestButton(
    interest: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    if (isSelected) {
        Button(
            onClick = onToggle,
            modifier = Modifier
                .height(36.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF59ABF7),
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
        ) {
            Text(
                text = interest,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    } else {
        OutlinedButton(
            onClick = onToggle,
            modifier = Modifier
                .height(36.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF59ABF7)
            ),
            border = BorderStroke(1.dp, Color(0xFF59ABF7)),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
        ) {
            Text(
                text = interest,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private val provinceCities = mapOf(
    "서울" to listOf("서울특별시"),
    "부산" to listOf("부산광역시"),
    "경기" to listOf(
        "고양시", "과천시", "광명시", "광주시", "구리시", "군포시", "김포시", "남양주시", "동두천시", "부천시",
        "성남시", "수원시", "시흥시", "안산시", "안성시", "안양시", "양주시", "여주시", "오산시", "용인시",
        "의왕시", "의정부시", "이천시", "파주시", "평택시", "포천시", "하남시", "화성시"
    ),
    "인천" to listOf("인천광역시"),
    "대구" to listOf("대구광역시"),
    "광주" to listOf("광주광역시"),
    "대전" to listOf("대전광역시"),
    "울산" to listOf("울산광역시"),
    "강원" to listOf("강릉시", "동해시", "삼척시", "속초시", "원주시", "춘천시", "태백시"),
    "충북" to listOf("제천시", "청주시", "충주시"),
    "충남" to listOf("계룡시", "공주시", "논산시", "당진시", "보령시", "서산시", "아산시", "천안시"),
    "전북" to listOf("군산시", "김제시", "남원시", "익산시", "전주시", "정읍시"),
    "전남" to listOf("광양시", "나주시", "목포시", "순천시", "여수시"),
    "경북" to listOf("경산시", "경주시", "구미시", "김천시", "문경시", "상주시", "안동시", "영주시", "영천시", "포항시"),
    "경남" to listOf("거제시", "김해시", "밀양시", "사천시", "양산시", "진주시", "창원시", "통영시"),
    "제주" to listOf("제주시", "서귀포시")
)

private val provinceDisplayNames = mapOf(
    "서울" to "서울특별시",
    "부산" to "부산광역시",
    "경기" to "경기도",
    "인천" to "인천광역시",
    "대구" to "대구광역시",
    "광주" to "광주광역시",
    "대전" to "대전광역시",
    "울산" to "울산광역시",
    "강원" to "강원도",
    "충북" to "충청북도",
    "충남" to "충청남도",
    "전북" to "전라북도",
    "전남" to "전라남도",
    "경북" to "경상북도",
    "경남" to "경상남도",
    "제주" to "제주특별자치도"
)
