package com.wiseyoung.app

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.app.Config
import com.example.app.DeviceInfo
import com.example.app.data.FirestoreService
import com.example.app.ui.theme.ThemeWrapper
import com.wiseyoung.app.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import android.util.Log


class ProfileSetupActivity : ComponentActivity() {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)  // 연결 실패 시 자동 재시도
            .build()
    }
    private val auth = FirebaseAuth.getInstance()
    private var isFromGoogleLogin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Google 로그인에서 온 경우인지 확인
        isFromGoogleLogin = intent.getBooleanExtra("from_google_login", false)
        
        setContent {
            ThemeWrapper {
                var isSubmitting by remember { mutableStateOf(false) }
                var snackbarMessage by remember { mutableStateOf<String?>(null) }
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(snackbarMessage) {
                    snackbarMessage?.let {
                        snackbarHostState.showSnackbar(it)
                        snackbarMessage = null
                    }
                }

                Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
                    ProfileSetupScreen(
                        modifier = Modifier.padding(paddingValues),
                        isSubmitting = isSubmitting,
                        onLoadProfile = { callback ->
                            // 화면 로드 시 기존 프로필 정보 불러오기
                            loadExistingProfile(callback)
                        },
                        onBack = {
                            if (isFromGoogleLogin) {
                                // Google 로그인에서 온 경우 LoginActivity로 이동
                                startActivity(Intent(this@ProfileSetupActivity, LoginActivity::class.java))
                            }
                            finish()
                        },
                        onSubmit = { payload ->
                            if (isSubmitting) return@ProfileSetupScreen
                            isSubmitting = true
                            submitProfile(
                                payload = payload,
                                onResult = { success, message ->
                                    isSubmitting = false
                                    if (success) {
                                        ProfilePreferences.setProfileCompleted(this, true)
                                        Toast.makeText(
                                            this,
                                            "프로필 저장이 완료되었습니다.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        val intent = Intent(this, CompleteActivity::class.java)
                                        startActivity(intent)
                                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                                        finish()
                                    } else {
                                        snackbarMessage = message
                                    }
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    private fun submitProfile(
        payload: ProfilePayload,
        onResult: (Boolean, String) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Firebase 인증 토큰 가져오기
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "로그인이 필요합니다. 다시 로그인해주세요.")
                    }
                    return@launch
                }

                // Firebase 토큰 발급 (네트워크 오류 시 캐시된 토큰 사용)
                val idToken = try {
                    // 먼저 강제 새로고침 시도
                    currentUser.getIdToken(true).await()
                } catch (e: Exception) {
                    Log.w("ProfileSetup", "새 토큰 발급 실패, 캐시된 토큰 사용 시도: ${e.message}")
                    try {
                        // 네트워크 오류 시 캐시된 토큰 사용
                        currentUser.getIdToken(false).await()
                    } catch (e2: Exception) {
                        Log.e("ProfileSetup", "토큰 발급 실패: ${e2.message}")
                        withContext(Dispatchers.Main) {
                            val errorMsg = when {
                                e2.message?.contains("network", ignoreCase = true) == true -> 
                                    "네트워크 연결을 확인해주세요. Firebase 서버에 연결할 수 없습니다."
                                else -> 
                                    "인증 토큰 발급 실패: ${e2.message}"
                            }
                            onResult(false, errorMsg)
                        }
                        return@launch
                    }
                }

                val appVersion = DeviceInfo.getAppVersion(this@ProfileSetupActivity)
                val deviceId = DeviceInfo.getDeviceId(this@ProfileSetupActivity)

                val jsonObject = JSONObject().apply {
                    put("idToken", idToken.token)
                    put("birthDate", payload.birthDate)
                    put("nickname", payload.nickname)
                    put("gender", payload.gender)
                    put("province", payload.province)
                    put("city", payload.city)
                    put("education", payload.education)
                    put("employment", payload.employment)
                    // interests를 콤마로 구분된 문자열로 변환하여 category 필드로 전송
                    put("category", payload.interests.joinToString(","))
                    put("appVersion", appVersion)
                    put("deviceId", deviceId)
                }

                val requestBody = jsonObject.toString()
                    .toRequestBody("application/json".toMediaType())

                val url = Config.getUrl(Config.Api.PROFILE)
                Log.d("ProfileSetup", "프로필 저장 요청 URL: $url")
                Log.d("ProfileSetup", "요청 본문: ${jsonObject.toString()}")

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                Log.d("ProfileSetup", "서버 연결 시도 중...")
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    Log.d("ProfileSetup", "서버 응답 코드: ${response.code}")
                    Log.d("ProfileSetup", "서버 응답 본문: $responseBody")

                    val message = try {
                        val jsonResponse = JSONObject(responseBody)
                        if (response.isSuccessful) {
                            // 성공 응답 파싱
                            jsonResponse.optString("message", "프로필이 저장되었습니다.")
                        } else {
                            // 에러 응답 파싱
                            jsonResponse.optString("message", "서버 오류: ${response.code}")
                        }
                    } catch (e: Exception) {
                        // JSON 파싱 실패 시 기본 메시지
                        if (response.isSuccessful) {
                            "프로필이 저장되었습니다."
                        } else {
                            "서버 오류: ${response.code} - $responseBody"
                        }
                    }
                    
                    // ApiResponse의 success 필드도 확인
                    val isSuccess = try {
                        val jsonResponse = JSONObject(responseBody)
                        jsonResponse.optBoolean("success", response.isSuccessful)
                    } catch (e: Exception) {
                        response.isSuccessful
                    }
                    
                    // 서버 저장 성공 시 Firestore에도 저장
                    if (isSuccess && currentUser != null) {
                        val appVersion = DeviceInfo.getAppVersion(this@ProfileSetupActivity)
                        val deviceId = DeviceInfo.getDeviceId(this@ProfileSetupActivity)
                        
                        // User 정보 업데이트
                        val firestoreUser = FirestoreService.User(
                            userId = currentUser.uid,
                            email = currentUser.email ?: "",
                            emailVerified = currentUser.isEmailVerified,
                            passwordHash = "",
                            loginType = "GOOGLE", // 또는 "EMAIL"
                            osType = "ANDROID",
                            appVersion = appVersion,
                            deviceId = deviceId,
                            createdAt = Date()
                        )
                        
                        FirestoreService.saveUser(
                            user = firestoreUser,
                            onSuccess = {},
                            onFailure = { exception ->
                                Log.e("ProfileSetup", "Firestore User 저장 실패: ${exception.message}")
                            }
                        )
                        
                        // UserProfile 정보 저장
                        val firestoreProfile = FirestoreService.UserProfile(
                            userId = currentUser.uid,
                            birthYear = payload.birthDate, // "yyyy-MM-dd" 형식
                            nickname = payload.nickname,
                            gender = payload.gender,
                            region = payload.province, // province만 저장 (VARCHAR(10) 제약)
                            education = payload.education,
                            jobStatus = payload.employment
                        )
                        
                        FirestoreService.saveUserProfile(
                            profile = firestoreProfile,
                            onSuccess = {
                                Log.d("ProfileSetup", "Firestore 프로필 저장 성공")
                            },
                            onFailure = { exception ->
                                Log.e("ProfileSetup", "Firestore 프로필 저장 실패: ${exception.message}")
                            }
                        )
                    }
                    
                    withContext(Dispatchers.Main) {
                        onResult(isSuccess, message)
                    }
                }  // use 블록 닫기
            } catch (e: java.net.UnknownHostException) {
                    Log.e("ProfileSetup", "호스트를 찾을 수 없음: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        onResult(false, 
                            "서버를 찾을 수 없습니다.\n\n" +
                            "🔧 확인 사항:\n" +
                            "1. 컴퓨터 IP 주소 확인: ipconfig | findstr IPv4\n" +
                            "2. Config.kt의 IP 주소가 올바른지 확인\n" +
                            "3. USB 테더링 연결 확인\n" +
                            "4. Spring Boot 서버 실행 확인"
                        )
                    }
                } catch (e: java.net.ConnectException) {
                    Log.e("ProfileSetup", "연결 거부: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        onResult(false,
                            "서버 연결이 거부되었습니다.\n\n" +
                            "🔧 확인 사항:\n" +
                            "1. Spring Boot 서버가 실행 중인지 확인\n" +
                            "2. Windows 방화벽에서 8080 포트 허용 확인\n" +
                            "3. 서버가 모든 인터페이스에서 수신하는지 확인\n" +
                            "   (application.yml: server.address 확인)"
                        )
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    Log.e("ProfileSetup", "연결 타임아웃: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        onResult(false,
                            "서버 응답 시간이 초과되었습니다.\n\n" +
                            "🔧 확인 사항:\n" +
                            "1. 네트워크 연결 상태 확인\n" +
                            "2. 서버가 정상적으로 실행 중인지 확인\n" +
                            "3. 잠시 후 다시 시도"
                        )
                    }
                } catch (e: Exception) {
                Log.e("ProfileSetup", "프로필 저장 실패: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    // 네트워크 오류인 경우 상세한 안내 메시지 제공
                    val errorMessage = when {
                        e.message?.contains("Unable to resolve host") == true || 
                        e.message?.contains("Failed to connect") == true ||
                        e.message?.contains("Connection refused") == true -> 
                            "서버에 연결할 수 없습니다.\n\n" +
                            "🔧 확인 사항:\n" +
                            "1. Spring Boot 서버가 실행 중인지 확인\n" +
                            "   (http://172.16.2.178:8080 접속 테스트)\n" +
                            "2. USB 테더링 연결이 활성화되어 있는지 확인\n" +
                            "3. 컴퓨터 IP 주소 확인:\n" +
                            "   PowerShell: ipconfig | findstr IPv4\n" +
                            "4. Windows 방화벽에서 8080 포트 허용 확인\n" +
                            "5. 앱 재시작 후 다시 시도"
                        e.message?.contains("timeout") == true -> 
                            "서버 응답 시간이 초과되었습니다.\n\n" +
                            "🔧 확인 사항:\n" +
                            "1. 네트워크 연결 상태 확인\n" +
                            "2. 서버가 정상적으로 실행 중인지 확인\n" +
                            "3. 잠시 후 다시 시도"
                        e.message?.contains("Network is unreachable") == true ->
                            "네트워크에 연결할 수 없습니다.\n\n" +
                            "USB 테더링 연결을 확인해주세요."
                        else -> 
                            "프로필 저장 실패: ${e.message}\n\n" +
                            "네트워크 연결과 서버 상태를 확인해주세요."
                    }
                    onResult(false, errorMessage)
                }
            }
        }
    }

    /**
     * 기존 프로필 정보 불러오기 (Firestore 또는 서버에서)
     */
    private fun loadExistingProfile(callback: (ProfilePayload?) -> Unit) {
        val currentUser = auth.currentUser ?: run {
            callback(null)
            return
        }

        // Firestore에서 프로필 정보 불러오기
        FirestoreService.getUserProfile(
            userId = currentUser.uid,
            onSuccess = { profile ->
                if (profile != null && profile.birthYear != null) {
                    // 프로필 정보가 있는 경우 ProfilePayload 생성
                    val payload = ProfilePayload(
                        birthDate = profile.birthYear, // "yyyy-MM-dd" 형식
                        nickname = profile.nickname ?: "",
                        gender = profile.gender ?: "male",
                        province = profile.region ?: "",
                        city = "", // Firestore에 city 정보가 없음 (province만 저장)
                        education = profile.education ?: "",
                        employment = profile.jobStatus ?: "",
                        interests = emptyList() // Firestore에 interests 정보가 없음
                    )
                    callback(payload)
                } else {
                    // 프로필 정보가 없는 경우
                    callback(null)
                }
            },
            onFailure = { e ->
                Log.e("ProfileSetup", "프로필 정보 불러오기 실패: ${e.message}", e)
                callback(null)
            }
        )
    }
}

data class ProfilePayload(
    val birthDate: String,
    val nickname: String,
    val gender: String,
    val province: String,
    val city: String,
    val education: String,
    val employment: String,
    val interests: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    modifier: Modifier = Modifier,
    isSubmitting: Boolean,
    onLoadProfile: ((ProfilePayload?) -> Unit) -> Unit = {},
    onBack: () -> Unit,
    onSubmit: (ProfilePayload) -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var birthDate by remember { mutableStateOf<LocalDate?>(null) }
    var nickname by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("male") }
    var province by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var education by remember { mutableStateOf("") }
    var employment by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf(setOf<String>()) }
    var isProfileLoaded by remember { mutableStateOf(false) }

    // 화면 로드 시 기존 프로필 정보 불러오기
    LaunchedEffect(Unit) {
        if (!isProfileLoaded) {
            onLoadProfile { existingProfile ->
                if (existingProfile != null) {
                    // 기존 프로필 정보로 입력 필드 채우기
                    try {
                        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        birthDate = LocalDate.parse(existingProfile.birthDate, dateFormatter)
                    } catch (e: Exception) {
                        Log.e("ProfileSetup", "생년월일 파싱 실패: ${e.message}")
                    }
                    nickname = existingProfile.nickname
                    gender = existingProfile.gender
                    province = existingProfile.province
                    city = existingProfile.city
                    education = existingProfile.education
                    employment = existingProfile.employment
                    interests = existingProfile.interests.toSet()
                }
                isProfileLoaded = true
            }
        }
    }

    val cityMap = remember { provinceCities }
    val provinceDisplayMap = remember { provinceDisplayNames }

    val canSubmit = birthDate != null &&
            nickname.isNotBlank() &&
            province.isNotBlank() &&
            city.isNotBlank() &&
            education.isNotBlank() &&
            employment.isNotBlank() &&
            interests.isNotEmpty()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            HeaderSection(onBack = onBack)

            Spacer(modifier = Modifier.height(24.dp))

            // 생년월일
            DateSpinnerSection(
                birthDate = birthDate,
                onDateChange = { date ->
                    birthDate = date
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 닉네임
            NicknameSection(
                nickname = nickname,
                onNicknameChange = { nickname = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 성별
            GenderSection(
                selectedGender = gender,
                onSelect = { gender = it }
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

            // 학력/재학 상태
            DropdownSection(
                label = "학력/재학 상태",
                value = education,
                options = listOf("고등학교 졸업", "대학교 재학", "대학교 졸업", "대학원 이상"),
                placeholder = "선택해주세요",
                onValueChange = { education = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 직업/고용 상태
            DropdownSection(
                label = "직업/고용 상태",
                value = employment,
                options = listOf("학생", "직장인", "구직자", "자영업자"),
                placeholder = "선택해주세요",
                onValueChange = { employment = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 관심분야 (백엔드 policy.category와 동일한 4개 카테고리로 고정)
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

            // 완료 버튼
            Button(
                onClick = {
                    birthDate?.let { date ->
                        onSubmit(
                            ProfilePayload(
                                birthDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                nickname = nickname,
                                gender = gender,
                                province = province,
                                city = city,
                                education = education,
                                employment = employment,
                                interests = interests.toList()
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = canSubmit && !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF59ABF7),  // 라이트 블루 (메인 컬러)
                    disabledContainerColor = Color(0xFF59ABF7).copy(alpha = 0.4f)
                )
            ) {
                Text(
                    text = if (isSubmitting) "저장 중..." else "시작하기",
                    color = Color.White
                )
            }
        }

        if (isSubmitting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun HeaderSection(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "back",
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = "프로필 입력",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DateSpinnerSection(
    birthDate: LocalDate?,
    onDateChange: (LocalDate) -> Unit
) {
    val currentYear = LocalDate.now().year
    val currentMonth = LocalDate.now().monthValue
    
    var selectedYear by remember { mutableStateOf(birthDate?.year ?: (currentYear - 25)) }
    var selectedMonth by remember { mutableStateOf(birthDate?.monthValue ?: 1) }
    var selectedDay by remember { mutableStateOf(birthDate?.dayOfMonth ?: 1) }
    
    // 년도 범위: 현재년도 - 100년 ~ 현재년도
    val years = remember { (currentYear downTo currentYear - 100).toList() }
    val months = remember { (1..12).toList() }
    
    // 해당 월의 마지막 날짜 계산
    val getDaysInMonth = { year: Int, month: Int ->
        LocalDate.of(year, month, 1).lengthOfMonth()
    }
    
    val days = remember(selectedYear, selectedMonth) {
        (1..getDaysInMonth(selectedYear, selectedMonth)).toList()
    }
    
    // 날짜가 변경될 때마다 onDateChange 호출
    LaunchedEffect(selectedYear, selectedMonth, selectedDay) {
        val maxDay = getDaysInMonth(selectedYear, selectedMonth)
        val adjustedDay = if (selectedDay > maxDay) maxDay else selectedDay
        
        if (selectedDay != adjustedDay) {
            selectedDay = adjustedDay
        } else {
            val newDate = LocalDate.of(selectedYear, selectedMonth, adjustedDay)
            onDateChange(newDate)
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "생년월일",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1A1A1A)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(BorderStroke(2.dp, Color(0xFFE5E7EB)), RoundedCornerShape(8.dp))
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 년도 스피너
                DateSpinner(
                    items = years,
                    selectedValue = selectedYear,
                    onValueSelected = { selectedYear = it },
                    label = "년"
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // 월 스피너
                DateSpinner(
                    items = months,
                    selectedValue = selectedMonth,
                    onValueSelected = { selectedMonth = it },
                    label = "월"
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // 일 스피너
                DateSpinner(
                    items = days,
                    selectedValue = selectedDay,
                    onValueSelected = { selectedDay = it },
                    label = "일"
                )
            }
        }
    }
}

@Composable
private fun DateSpinner(
    items: List<Int>,
    selectedValue: Int,
    onValueSelected: (Int) -> Unit,
    label: String
) {
    val itemHeight = 40.dp
    val visibleItemCount = 5
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = items.indexOf(selectedValue).coerceAtLeast(0)
    )
    val density = LocalDensity.current
    
    // 스크롤 위치에 따라 선택된 값 업데이트 및 스냅
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            // 스크롤이 멈췄을 때 가장 가까운 항목으로 스냅
            val scrollOffset = with(density) { listState.firstVisibleItemScrollOffset.toDp() }
            val itemIndex = listState.firstVisibleItemIndex
            
            val targetIndex = if (scrollOffset < itemHeight / 2) {
                itemIndex
            } else {
                (itemIndex + 1).coerceAtMost(items.size - 1)
            }
            
            if (targetIndex >= 0 && targetIndex < items.size) {
                val targetValue = items[targetIndex]
                if (targetValue != selectedValue) {
                    onValueSelected(targetValue)
                }
                // 정확한 위치로 스냅
                listState.animateScrollToItem(targetIndex)
            }
        }
    }
    
    // 스크롤 중에도 선택된 값 업데이트
    LaunchedEffect(listState.firstVisibleItemScrollOffset, listState.firstVisibleItemIndex) {
        if (listState.isScrollInProgress) {
            val scrollOffset = with(density) { listState.firstVisibleItemScrollOffset.toDp() }
            val itemIndex = listState.firstVisibleItemIndex
            
            val selectedIndex = if (scrollOffset < itemHeight / 2) {
                itemIndex
            } else {
                (itemIndex + 1).coerceAtMost(items.size - 1)
            }.coerceIn(0, items.size - 1)
            
            if (selectedIndex >= 0 && selectedIndex < items.size) {
                val newValue = items[selectedIndex]
                if (newValue != selectedValue) {
                    onValueSelected(newValue)
                }
            }
        }
    }
    
    // 선택된 값이 변경되면 해당 위치로 스크롤
    LaunchedEffect(selectedValue) {
        val index = items.indexOf(selectedValue).coerceAtLeast(0)
        if (index < items.size && listState.firstVisibleItemIndex != index && !listState.isScrollInProgress) {
            listState.animateScrollToItem(index)
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        Box(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
        ) {
            // 선택 표시선
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .align(Alignment.Center)
                    .background(Color(0xFF59ABF7).copy(alpha = 0.1f))  // 라이트 블루 (메인 컬러)
            )
            
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 80.dp)
            ) {
                itemsIndexed(items) { index, item ->
                    val isSelected = item == selectedValue
                    Box(
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.toString(),
                            color = if (isSelected) Color(0xFF59ABF7) else Color(0xFF999999),  // 라이트 블루 (메인 컬러)
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
    }
}

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
            placeholder = { Text("닉네임을 입력하세요") },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color.White, MaterialTheme.shapes.small),
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

@Composable
private fun GenderSection(selectedGender: String, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "성별",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1A1A1A)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GenderButton(
                text = "남성",
                isSelected = selectedGender == "male",
                onClick = { onSelect("male") }
            )
            GenderButton(
                text = "여성",
                isSelected = selectedGender == "female",
                onClick = { onSelect("female") }
            )
        }
    }
}

@Composable
private fun RowScope.GenderButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    if (isSelected) {
    Button(
        onClick = onClick,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
        colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF59ABF7),  // 라이트 블루 (메인 컬러)
                contentColor = Color.White
        )
    ) {
        Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF59ABF7)  // 라이트 블루 (메인 컬러)
            ),
            border = BorderStroke(2.dp, Color(0xFF59ABF7))  // 라이트 블루 테두리
        ) {
            Text(text)
        }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
            interests.forEach { interest ->
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
private fun RowScope.InterestButton(
    interest: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    if (isSelected) {
                        Button(
            onClick = onToggle,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF59ABF7),  // 라이트 블루 (메인 컬러)
                contentColor = Color.White
                            )
                        ) {
                            Text(interest)
                        }
    } else {
        OutlinedButton(
            onClick = onToggle,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF59ABF7)  // 라이트 블루 (메인 컬러)
            ),
            border = BorderStroke(2.dp, Color(0xFF59ABF7))  // 라이트 블루 테두리
        ) {
            Text(interest)
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

private fun LocalDate.formatKoreanDate(): String {
    return String.format(Locale.KOREA, "%d년 %02d월 %02d일", year, monthValue, dayOfMonth)
}