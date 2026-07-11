package com.wiseyoung.pro

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.wiseyoung.pro.ui.theme.AppColors
import com.wiseyoung.pro.ui.theme.Spacing
import com.wiseyoung.pro.ui.theme.ThemeWrapper
import androidx.compose.ui.platform.LocalContext
import com.wiseyoung.pro.data.model.UserProfileResponse
import com.wiseyoung.pro.data.model.DeleteAccountRequest
import com.wiseyoung.pro.util.RegionConstants
import com.wiseyoung.pro.network.NetworkModule
import com.wiseyoung.pro.ads.BannerAd
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import retrofit2.Response
import com.wiseyoung.pro.data.model.ApiResponse

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
                    onNavigateIntro = {
                        startActivity(Intent(this, IntroActivity::class.java))
                    },
                    onProfileUpdated = {}
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
    onNavigateIntro: () -> Unit,
    onProfileUpdated: () -> Unit = {}
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    var showDeleteAccountWarning by remember { mutableStateOf(false) }
    var showDeleteGoogleReauth by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf<com.wiseyoung.pro.data.model.UserProfileResponse?>(null) }
    var isLoadingProfile by remember { mutableStateOf(true) }
    var isLoadingLogout by remember { mutableStateOf(false) }
    var isLoadingDelete by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    var showEditProfileDialog by remember { mutableStateOf(false) }

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleReauthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            Toast.makeText(context, "Google 계정 인증이 취소되었습니다.", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    Toast.makeText(context, "Google 인증 토큰을 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                val freshToken = auth.currentUser?.getIdToken(true)?.await()?.token
                if (freshToken.isNullOrBlank()) {
                    Toast.makeText(context, "인증 토큰 갱신에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                isLoadingDelete = true
                executeAccountDeletion(
                    context = context,
                    auth = auth,
                    googleSignInClient = googleSignInClient,
                    idToken = freshToken,
                    onFinished = { isLoadingDelete = false; showDeleteGoogleReauth = false }
                )
            } catch (e: Exception) {
                android.util.Log.e("ProfileActivity", "Google 재인증 실패: ${e.message}", e)
                Toast.makeText(context, "Google 계정 인증에 실패했습니다.", Toast.LENGTH_SHORT).show()
                isLoadingDelete = false
            }
        }
    }

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
                
                // App Tour Button (앱 정보 보기)
                Button(
                    onClick = onNavigateIntro,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.md),
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
                            showDeleteAccountWarning = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "회원탈퇴",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.md))
                    BannerAd()
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
    
    // Delete Account - initial warning
    if (showDeleteAccountWarning) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountWarning = false },
            containerColor = Color.White,
            title = { Text("회원탈퇴", color = AppColors.TextPrimary) },
            text = {
                Text(
                    text = "회원탈퇴 시 회원님의 정보가 영구 삭제됩니다.\n정말 탈퇴하시겠습니까?",
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountWarning = false
                        showDeleteGoogleReauth = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.BackgroundGradientStart
                    )
                ) {
                    Text("탈퇴하기", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountWarning = false }) {
                    Text("취소", color = AppColors.TextSecondary)
                }
            }
        )
    }

    // Delete Account - Google 재로그인 본인 확인
    if (showDeleteGoogleReauth) {
        DeleteAccountGoogleReauthDialog(
            isLoading = isLoadingDelete,
            onConfirm = {
                googleReauthLauncher.launch(googleSignInClient.signInIntent)
            },
            onDismiss = {
                if (!isLoadingDelete) {
                    showDeleteGoogleReauth = false
                }
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
            onSave = { nickname, provinceKey, cityKey, jobStatus, interests ->
                val success = saveProfileUpdate(
                    context = context,
                    auth = auth,
                    currentEducation = profile?.education,
                    nickname = nickname,
                    provinceKey = provinceKey,
                    cityKey = cityKey,
                    jobStatus = jobStatus,
                    interests = interests,
                    onProfileRefreshed = { updated -> profile = updated }
                )
                if (success) {
                    onProfileUpdated()
                }
                success
            }
        )
    }
}

private suspend fun saveProfileUpdate(
    context: android.content.Context,
    auth: FirebaseAuth,
    currentEducation: String?,
    nickname: String,
    provinceKey: String,
    cityKey: String,
    jobStatus: String,
    interests: List<String>,
    onProfileRefreshed: (UserProfileResponse?) -> Unit
): Boolean {
    val currentUser = auth.currentUser ?: return false

    val idToken = try {
        currentUser.getIdToken(true).await().token
    } catch (e: Exception) {
        android.util.Log.e("ProfileActivity", "ID Token 발급 실패: ${e.message}", e)
        Toast.makeText(context, "인증 토큰 발급 실패", Toast.LENGTH_SHORT).show()
        return false
    }

    if (idToken.isNullOrBlank()) {
        Toast.makeText(context, "인증 토큰을 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
        return false
    }

    val updateRequest = com.wiseyoung.pro.data.model.ProfileRequest(
        idToken = idToken,
        nickname = nickname,
        province = provinceKey,
        city = cityKey,
        education = currentEducation,
        employment = jobStatus,
        interests = interests,
        appVersion = DeviceInfo.getAppVersion(context),
        deviceId = DeviceInfo.getDeviceId(context)
    )

    android.util.Log.d("ProfileActivity", "프로필 업데이트 요청: province=$provinceKey, city=$cityKey")

    return try {
        val response = NetworkModule.apiService.saveProfile(updateRequest)
        android.util.Log.d(
            "ProfileActivity",
            "saveProfile 응답: code=${response.code()}, success=${response.body()?.success}, message=${response.body()?.message}"
        )

        val saved = isProfileSaveSuccessful(response)
        if (saved) {
            notifyProfileUpdated(context)
            refreshProfileAfterSave(currentUser.uid, onProfileRefreshed)
            Toast.makeText(context, "프로필이 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
            true
        } else {
            val errorMsg = response.body()?.message ?: "HTTP ${response.code()}"
            android.util.Log.e("ProfileActivity", "프로필 업데이트 실패: $errorMsg")
            Toast.makeText(context, "프로필 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show()
            false
        }
    } catch (e: Exception) {
        android.util.Log.e("ProfileActivity", "프로필 업데이트 실패: ${e.message}", e)
        val verified = verifyProfileSaved(currentUser.uid, nickname, jobStatus, onProfileRefreshed)
        if (verified) {
            notifyProfileUpdated(context)
            Toast.makeText(context, "프로필이 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
            true
        } else {
            Toast.makeText(context, "프로필 업데이트 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            false
        }
    }
}

private fun notifyProfileUpdated(context: android.content.Context) {
    context.getSharedPreferences("profile_prefs", android.content.Context.MODE_PRIVATE)
        .edit()
        .putLong("last_profile_update", System.currentTimeMillis())
        .apply()
}

private suspend fun refreshProfileAfterSave(
    userId: String,
    onProfileRefreshed: (UserProfileResponse?) -> Unit
) {
    try {
        val profileResponse = NetworkModule.apiService.getUserProfile(userId)
        if (profileResponse.isSuccessful && profileResponse.body()?.success == true) {
            onProfileRefreshed(profileResponse.body()?.data)
        }
    } catch (e: Exception) {
        android.util.Log.w("ProfileActivity", "저장 후 프로필 재조회 실패 (무시): ${e.message}")
    }
}

private suspend fun verifyProfileSaved(
    userId: String,
    nickname: String,
    jobStatus: String,
    onProfileRefreshed: (UserProfileResponse?) -> Unit
): Boolean {
    return try {
        kotlinx.coroutines.delay(400)
        val profileResponse = NetworkModule.apiService.getUserProfile(userId)
        val data = profileResponse.body()?.data
        val matches = profileResponse.isSuccessful &&
            profileResponse.body()?.success == true &&
            data?.nickname == nickname &&
            data.jobStatus == jobStatus
        if (matches) {
            onProfileRefreshed(data)
        }
        matches
    } catch (e: Exception) {
        false
    }
}

/** HTTP 200이면 저장 성공으로 간주. body.success가 명시적으로 false일 때만 실패 처리 */
private fun <T> isProfileSaveSuccessful(response: Response<ApiResponse<T>>): Boolean {
    if (!response.isSuccessful) return false
    return response.body()?.success != false
}

@Composable
private fun EditProfileDialog(
    currentProfile: UserProfileResponse,
    onDismiss: () -> Unit,
    onSave: suspend (String, String, String, String, List<String>) -> Boolean
) {
    var nickname by remember { mutableStateOf(currentProfile.nickname ?: "") }
    val (initialProvince, initialCity) = parseRegionToKeys(currentProfile.region)
    var province by remember { mutableStateOf(initialProvince) }
    var city by remember { mutableStateOf(initialCity) }
    var jobStatus by remember { mutableStateOf(currentProfile.jobStatus ?: "") }
    var interests by remember { mutableStateOf(currentProfile.interests.toSet()) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val cityMap = remember { RegionConstants.provinceCities }
    val provinceDisplayMap = remember { RegionConstants.provinceDisplayNames }
    val isFormValid = nickname.isNotBlank() &&
        province.isNotBlank() &&
        jobStatus.isNotBlank() &&
        interests.isNotEmpty()

    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.75f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
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
                        city = RegionConstants.defaultCityForProvince(it)
                    }
                )

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
                        scope.launch {
                            isSaving = true
                            try {
                                val resolvedCity = RegionConstants.defaultCityForProvince(province)
                                val success = onSave(
                                    nickname,
                                    province,
                                    resolvedCity,
                                    jobStatus,
                                    interests.toList()
                                )
                                if (success) {
                                    onDismiss()
                                }
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF59ABF7),
                        disabledContainerColor = Color(0xFF59ABF7).copy(alpha = 0.4f)
                    ),
                    enabled = isFormValid && !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "저장",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    enabled = !isSaving
                ) {
                    Text(
                        text = "취소",
                        fontSize = 14.sp
                    )
                }
            }

                if (isSaving) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.White.copy(alpha = 0.92f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            CircularProgressIndicator(color = AppColors.LightBlue)
                            Text(
                                text = "프로필을 저장하는 중이에요!\n잠시만 기다려주세요!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.TextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
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
private fun DeleteAccountGoogleReauthDialog(
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        containerColor = Color.White,
        title = { Text("본인 확인", color = AppColors.TextPrimary) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.LightBlue)
                    }
                } else {
                    Text(
                        text = "안전한 탈퇴를 위해 Google 계정으로 다시 로그인해 주세요.",
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.BackgroundGradientStart
                )
            ) {
                Text("Google 계정으로 확인", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("취소", color = AppColors.TextSecondary)
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
        containerColor = Color.White,
        title = { Text("로그아웃", color = AppColors.TextPrimary) },
        text = {
            Text(
                text = "정말 로그아웃하시겠습니까?",
                fontSize = 14.sp,
                color = AppColors.TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.LightBlue
                )
            ) {
                Text("로그아웃", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = AppColors.TextSecondary)
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
        containerColor = Color.White,
        title = { Text("정말 탈퇴하시겠습니까?", color = AppColors.TextPrimary) },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.LightBlue)
                }
            } else {
                Text(
                    text = "회원 탈퇴 시 모든 정보가 소실되며 복구할 수 없습니다.\n정말 탈퇴하시겠습니까?",
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.BackgroundGradientStart
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
                Text("뒤로가기", color = AppColors.TextSecondary)
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
                    .height(48.dp)
                    .background(Color.White, MaterialTheme.shapes.small),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = if (value.isBlank()) Color.Gray else Color.Black
                ),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
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
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White),
                containerColor = Color.White
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(displayMap?.get(option) ?: option, color = Color.Black) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = Color.Black,
                            leadingIconColor = Color.Black,
                            trailingIconColor = Color.Black
                        )
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

private suspend fun executeAccountDeletion(
    context: android.content.Context,
    auth: FirebaseAuth,
    googleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient,
    idToken: String,
    onFinished: () -> Unit
) {
    try {
        val response = NetworkModule.apiService.deleteAccount(DeleteAccountRequest(idToken = idToken))
        if (response.isSuccessful && response.body()?.success == true) {
            ProfilePreferences.clearAll(context)
            ProfilePreferences.setProfileCompleted(context, false)
            Toast.makeText(context, "회원탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()
            auth.signOut()
            try {
                googleSignInClient.signOut().await()
            } catch (e: Exception) {
                android.util.Log.w("ProfileActivity", "Google 로그아웃 실패 (무시): ${e.message}")
            }
            val intent = Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
            (context as? ComponentActivity)?.finishAffinity()
        } else {
            val errorMsg = response.body()?.message ?: "HTTP ${response.code()}"
            android.util.Log.e("ProfileActivity", "회원탈퇴 실패: $errorMsg")
            Toast.makeText(context, "회원탈퇴에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        android.util.Log.e("ProfileActivity", "회원탈퇴 오류: ${e.message}", e)
        Toast.makeText(context, "회원탈퇴 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
    } finally {
        onFinished()
    }
}

private fun parseRegionToKeys(region: String?): Pair<String, String> {
    if (region.isNullOrBlank()) return "" to ""

    val provinceCities = RegionConstants.provinceCities
    val provinceDisplayNames = RegionConstants.provinceDisplayNames

    if (provinceCities.containsKey(region)) return region to ""

    provinceDisplayNames.entries.firstOrNull { (key, display) ->
        region == display || region.startsWith(display)
    }?.let { (key, display) ->
        val cityPart = region.removePrefix(display).trim()
        if (cityPart.isBlank()) return key to ""
        val matchedCity = provinceCities[key]?.find { cityPart == it || cityPart.contains(it) || it.contains(cityPart) }
        return key to (matchedCity ?: cityPart)
    }

    provinceCities.entries.firstOrNull { (key, _) ->
        region == key || region.startsWith("$key ")
    }?.let { (key, cities) ->
        val cityPart = region.removePrefix(key).trim()
        if (cityPart.isBlank()) return key to ""
        val matchedCity = cities.find { cityPart == it || cityPart.contains(it) || it.contains(cityPart) }
        return key to (matchedCity ?: cityPart)
    }

    return "" to ""
}
