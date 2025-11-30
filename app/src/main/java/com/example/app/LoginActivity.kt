package com.wiseyoung.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wiseyoung.app.ProfileSetupActivity
import com.example.app.ui.theme.ThemeWrapper
import com.wiseyoung.app.R
import com.example.app.Config
import com.example.app.FcmTokenService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import okhttp3.*
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException

class LoginActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Google Sign-In 설정 (네트워크 타임아웃 개선)
        try {
            val webClientId = getString(R.string.default_web_client_id)
            android.util.Log.d("LoginActivity", "Google Sign-In Web Client ID: $webClientId")
            
            if (webClientId.isBlank()) {
                android.util.Log.e("LoginActivity", "⚠️ default_web_client_id가 비어있습니다!")
                Toast.makeText(
                    this,
                    "Google 로그인 설정 오류: Web Client ID가 설정되지 않았습니다.",
                    Toast.LENGTH_LONG
                ).show()
            }
            
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .requestProfile()
                .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)
            android.util.Log.d("LoginActivity", "Google Sign-In 클라이언트 초기화 완료")
        } catch (e: Exception) {
            android.util.Log.e("LoginActivity", "Google Sign-In 초기화 실패: ${e.message}", e)
            Toast.makeText(
                this,
                "Google 로그인을 초기화할 수 없습니다: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }

        setContent {
            ThemeWrapper {
                LoginScreen(
                    onBack = { finish() },
                    onRegister = {
                        startActivity(Intent(this, RegisterActivity::class.java))
                    },
                    onPasswordReset = {
                        startActivity(Intent(this, PasswordResetActivity::class.java))
                    },
                    onComplete = { email, password ->
                        loginUser(email, password)
                    },
                    onGoogleLogin = {
                        signInWithGoogle()
                    },
                    onGoogleKeyLogin = {}
                )
            }
        }
    }

    // Google 로그인 결과 처리
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("LoginActivity", "Google 로그인 결과 수신 - result code: ${result.resultCode}")
        
        if (result.resultCode == RESULT_OK) {
            android.util.Log.d("LoginActivity", "Google 로그인 RESULT_OK 수신")
            
            val intent = result.data
            if (intent == null) {
                android.util.Log.e("LoginActivity", "Google 로그인 Intent가 null입니다")
                Toast.makeText(
                    this,
                    "Google 로그인 응답이 올바르지 않습니다. 다시 시도해주세요.",
                    Toast.LENGTH_LONG
                ).show()
                return@registerForActivityResult
            }
            
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
                android.util.Log.d("LoginActivity", "GoogleSignIn Task 생성 완료")
                
                if (task.isComplete) {
                    android.util.Log.d("LoginActivity", "Task 완료 상태: ${task.isComplete}, 성공: ${task.isSuccessful}")
                }
                
                val account = task.getResult(ApiException::class.java)
                android.util.Log.d("LoginActivity", "Google 계정 정보 가져오기 성공: ${account.email}")
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                android.util.Log.e("LoginActivity", "Google 로그인 ApiException: statusCode=${e.statusCode}, message=${e.message}", e)
                
                val errorMessage = when (e.statusCode) {
                    com.google.android.gms.common.api.CommonStatusCodes.SIGN_IN_REQUIRED -> {
                        "Google 로그인이 필요합니다. 다시 시도해주세요."
                    }
                    com.google.android.gms.common.api.CommonStatusCodes.NETWORK_ERROR -> {
                        "네트워크 연결을 확인해주세요. 인터넷 연결이 필요합니다.\n잠시 후 다시 시도해주세요."
                    }
                    com.google.android.gms.common.api.CommonStatusCodes.INTERNAL_ERROR -> {
                        "Google 로그인 중 내부 오류가 발생했습니다. 다시 시도해주세요."
                    }
                    com.google.android.gms.common.api.CommonStatusCodes.DEVELOPER_ERROR -> {
                        "Google 로그인 설정 오류입니다. 개발자에게 문의하세요. (오류 코드: ${e.statusCode})"
                    }
                    else -> "Google 로그인 실패: ${e.message} (오류 코드: ${e.statusCode})"
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                android.util.Log.e("LoginActivity", "Google 로그인 실패: ${e.statusCode} - ${e.message}", e)
            } catch (e: Exception) {
                android.util.Log.e("LoginActivity", "Google 로그인 처리 중 예외 발생: ${e.message}", e)
                Toast.makeText(
                    this,
                    "Google 로그인 처리 중 오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            // 로그인이 취소되거나 실패한 경우
            val intent = result.data
            android.util.Log.d("LoginActivity", "Google 로그인 취소 - Intent: ${if (intent != null) "존재" else "null"}")
            
            val cancelReason = when (result.resultCode) {
                RESULT_CANCELED -> "사용자가 취소함"
                RESULT_FIRST_USER -> "첫 사용자 취소"
                else -> "알 수 없는 이유 (코드: ${result.resultCode})"
            }
            android.util.Log.d("LoginActivity", "Google 로그인 취소됨: $cancelReason (result code: ${result.resultCode})")
            
            // Intent에서 추가 정보 확인
            if (intent != null) {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
                    if (task.isComplete && !task.isSuccessful) {
                        val exception = task.exception
                        if (exception is ApiException) {
                            android.util.Log.e("LoginActivity", "취소된 Google 로그인의 오류 코드: ${exception.statusCode}")
                            Toast.makeText(
                                this,
                                "Google 로그인 오류: ${exception.statusCode}",
                                Toast.LENGTH_LONG
                            ).show()
                            return@registerForActivityResult
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.d("LoginActivity", "취소된 Intent에서 정보 추출 실패: ${e.message}")
                }
            }
            
            // 사용자가 의도적으로 취소한 경우가 아니라면 안내
            if (result.resultCode == RESULT_CANCELED) {
                // 사용자가 뒤로가기를 눌러 취소한 경우이므로 Toast 표시 안 함
                android.util.Log.d("LoginActivity", "사용자가 Google 로그인을 취소했습니다.")
            } else {
                Toast.makeText(
                    this,
                    "Google 로그인이 취소되었습니다. (코드: ${result.resultCode})",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loginUser(email: String, password: String) {
        // 이메일과 비밀번호 앞뒤 공백 제거
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        
        // 이메일 형식 검증
        if (trimmedEmail.isEmpty()) {
            Toast.makeText(this, "이메일 주소를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (trimmedPassword.isEmpty()) {
            Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 이메일 형식 검증 (더 엄격한 검증)
        val emailPattern = android.util.Patterns.EMAIL_ADDRESS
        if (!emailPattern.matcher(trimmedEmail).matches()) {
            Toast.makeText(
                this,
                "올바른 이메일 주소를 입력해주세요.\n예: example@email.com",
                Toast.LENGTH_LONG
            ).show()
            android.util.Log.e("LoginActivity", "이메일 형식 오류: '$trimmedEmail'")
            return
        }
        
        // 이메일에 공백이 있는지 확인
        if (trimmedEmail.contains(" ")) {
            Toast.makeText(
                this,
                "이메일 주소에 공백이 포함되어 있습니다.\n공백을 제거해주세요.",
                Toast.LENGTH_LONG
            ).show()
            android.util.Log.e("LoginActivity", "이메일 공백 포함: '$trimmedEmail'")
            return
        }
        
        android.util.Log.d("LoginActivity", "이메일 로그인 시도: $trimmedEmail")
        
        auth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword)
            .addOnSuccessListener { result ->
                val user = result.user ?: run {
                    android.util.Log.e("LoginActivity", "로그인 성공했지만 user가 null")
                    Toast.makeText(
                        this,
                        "로그인 오류: 사용자 정보를 가져올 수 없습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }
                android.util.Log.d("LoginActivity", "Firebase 로그인 성공: ${user.uid}")

                // Firebase 이메일 인증 체크 제거 (Gmail SMTP 사용)
                user.getIdToken(true)
                    .addOnSuccessListener { token ->
                        android.util.Log.d("LoginActivity", "ID Token 발급 성공")
                        sendIdTokenToServer(token.token ?: "", trimmedPassword)
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("LoginActivity", "ID Token 발급 실패: ${e.message}", e)
                        Toast.makeText(
                            this,
                            "인증 토큰 발급 실패: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("LoginActivity", "Firebase 로그인 실패: ${e.message}", e)
                e.printStackTrace()
                
                val errorMessage = when {
                    e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                        when {
                            e.errorCode == "ERROR_WRONG_PASSWORD" -> {
                                "비밀번호가 올바르지 않습니다.\n\n" +
                                "확인 사항:\n" +
                                "1. 대소문자 구분 확인\n" +
                                "2. 특수문자 포함 여부 확인\n" +
                                "3. 비밀번호 재설정을 원하시면 '비밀번호 찾기'를 이용해주세요"
                            }
                            e.errorCode == "ERROR_INVALID_EMAIL" -> {
                                "이메일 주소 형식이 올바르지 않습니다.\n\n" +
                                "확인 사항:\n" +
                                "1. 이메일 주소에 공백이 없는지 확인\n" +
                                "2. @ 기호와 도메인이 포함되어 있는지 확인\n" +
                                "3. 예: example@email.com"
                            }
                            e.errorCode == "ERROR_USER_DISABLED" -> {
                                "사용자 계정이 비활성화되었습니다.\n관리자에게 문의하세요."
                            }
                            e.errorCode == "ERROR_USER_NOT_FOUND" -> {
                                "등록되지 않은 이메일 주소입니다.\n\n" +
                                "확인 사항:\n" +
                                "1. 이메일 주소가 정확한지 확인\n" +
                                "2. 회원가입을 먼저 진행해주세요"
                            }
                            e.errorCode == "ERROR_INVALID_CREDENTIAL" -> {
                                "이메일 주소 또는 비밀번호가 올바르지 않습니다.\n\n" +
                                "확인 사항:\n" +
                                "1. 이메일 주소와 비밀번호를 정확히 입력했는지 확인\n" +
                                "2. 회원가입을 먼저 진행했는지 확인\n" +
                                "3. 비밀번호 재설정이 필요하면 '비밀번호 찾기'를 이용해주세요"
                            }
                            else -> {
                                "인증 정보가 올바르지 않습니다.\n\n" +
                                "오류 코드: ${e.errorCode}\n" +
                                "오류 메시지: ${e.message}\n\n" +
                                "확인 사항:\n" +
                                "1. 이메일 주소와 비밀번호가 정확한지 확인\n" +
                                "2. 회원가입을 먼저 진행했는지 확인"
                            }
                        }
                    }
                    e.message?.contains("password", ignoreCase = true) == true -> 
                        "비밀번호가 올바르지 않습니다."
                    e.message?.contains("user", ignoreCase = true) == true && 
                    e.message?.contains("not found", ignoreCase = true) == true -> 
                        "등록되지 않은 이메일입니다."
                    e.message?.contains("network", ignoreCase = true) == true -> 
                        "네트워크 연결을 확인해주세요."
                    else -> {
                        "로그인 실패: ${e.message}\n\n" +
                        "오류 타입: ${e.javaClass.simpleName}\n\n" +
                        "확인 사항:\n" +
                        "1. 이메일 주소와 비밀번호가 정확한지 확인\n" +
                        "2. 회원가입을 먼저 진행했는지 확인\n" +
                        "3. 네트워크 연결 상태 확인"
                    }
                }
                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun sendIdTokenToServer(idToken: String, password: String) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val json = """{"idToken": "$idToken", "password": "$password"}"""
            val body = RequestBody.create("application/json; charset=utf-8".toMediaType(), json)

            val request = Request.Builder()
                .url(Config.getUrl(Config.Api.LOGIN))
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                runOnUiThread {
                    android.util.Log.e("LoginActivity", "서버 연결 실패: ${e.message}", e)
                    val errorMsg = when {
                        e.message?.contains("Unable to resolve host") == true -> 
                            "서버를 찾을 수 없습니다.\nConfig.kt의 BASE_URL을 확인해주세요."
                        e.message?.contains("Connection refused") == true -> 
                            "서버 연결이 거부되었습니다.\nSpring Boot 서버가 실행 중인지 확인해주세요."
                        e.message?.contains("timeout") == true -> 
                            "서버 응답 시간이 초과되었습니다.\n네트워크 연결을 확인해주세요."
                        else -> "서버 연결 실패: ${e.message}"
                    }
                    Toast.makeText(
                        this@LoginActivity,
                        errorMsg,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                android.util.Log.d("LoginActivity", "로그인 서버 응답 코드: ${response.code}")
                android.util.Log.d("LoginActivity", "로그인 서버 응답 본문: $responseBody")
                
                runOnUiThread {
                    if (response.isSuccessful) {
                        android.util.Log.d("LoginActivity", "✅ 로그인 서버 응답 성공 (코드: ${response.code})")
                        try {
                            // 응답 본문 파싱하여 확인
                            if (responseBody.isNotBlank()) {
                                try {
                                    val jsonResponse = org.json.JSONObject(responseBody)
                                    val success = jsonResponse.optBoolean("success", false)
                                    val message = jsonResponse.optString("message", "")
                                    android.util.Log.d("LoginActivity", "서버 응답 파싱 - success: $success, message: $message")
                                    
                                    if (!success) {
                                        android.util.Log.w("LoginActivity", "서버 응답에서 success가 false입니다: $message")
                                        Toast.makeText(
                                            this@LoginActivity,
                                            "로그인 실패: $message",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@runOnUiThread
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.w("LoginActivity", "응답 본문 파싱 실패 (계속 진행): ${e.message}")
                                }
                            }
                            
                            android.util.Log.d("LoginActivity", "프로필 확인 시작")
                            // FCM 토큰 저장
                            FcmTokenService.getAndSaveToken()
                            // 서버에서 프로필 확인 후 네비게이션
                            checkProfileAndNavigate(isGoogleLogin = false)
                        } catch (e: Exception) {
                            android.util.Log.e("LoginActivity", "프로필 확인 중 오류: ${e.message}", e)
                            // 오류 발생 시 기본 네비게이션
                            navigateAfterLogin(isGoogleLogin = false)
                        }
                    } else {
                        android.util.Log.e("LoginActivity", "❌ 로그인 서버 오류: ${response.code} - $responseBody")
                        val errorMsg = try {
                            val jsonResponse = org.json.JSONObject(responseBody)
                            jsonResponse.optString("message", "서버 오류: ${response.code}")
                        } catch (e: Exception) {
                            "서버 오류: ${response.code} - $responseBody"
                        }
                        Toast.makeText(
                            this@LoginActivity,
                            errorMsg,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
        } catch (e: Exception) {
            android.util.Log.e("LoginActivity", "서버 요청 생성 실패: ${e.message}", e)
            runOnUiThread {
                Toast.makeText(
                    this@LoginActivity,
                    "로그인 요청 생성 실패: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * 서버에서 프로필 확인 후 네비게이션 처리
     * 이메일/Google 로그인 모두: 서버에 프로필이 있으면 MainActivity, 없으면 ProfileSetupActivity로 이동
     */
    private fun checkProfileAndNavigate(isGoogleLogin: Boolean = false) {
        try {
            val currentUser = auth.currentUser ?: run {
                navigateAfterLogin(isGoogleLogin)
                return
            }

            currentUser.getIdToken(true).addOnSuccessListener { tokenResult ->
            val idToken = tokenResult.token ?: run {
                android.util.Log.e("LoginActivity", "ID Token이 null입니다")
                navigateAfterLogin(isGoogleLogin)
                return@addOnSuccessListener
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            // 백엔드의 /auth/profile 엔드포인트 사용 (Authorization 헤더 필요)
            val profileUrl = "${Config.BASE_URL}${Config.Api.PROFILE}"
            val request = Request.Builder()
                .url(profileUrl)
                .get()
                .addHeader("Authorization", "Bearer $idToken")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        // 서버 연결 실패 시 로컬 프로필 상태 확인
                        navigateAfterLogin(isGoogleLogin)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    // 백그라운드 스레드에서 응답 본문 읽기 (네트워크 I/O)
                    val responseBody = try {
                        response.body?.string() ?: ""
                    } catch (e: Exception) {
                        android.util.Log.e("LoginActivity", "응답 본문 읽기 실패: ${e.message}", e)
                        ""
                    }
                    
                    android.util.Log.d("LoginActivity", "프로필 확인 응답 코드: ${response.code}")
                    android.util.Log.d("LoginActivity", "프로필 확인 응답 본문: $responseBody")
                    
                    // 서버 응답 결과에 따라 로컬 플래그를 업데이트 (백그라운드에서 파싱)
                    val hasProfile = try {
                        if (response.isSuccessful && responseBody.isNotBlank()) {
                            // JSON 응답 파싱하여 프로필 데이터 확인
                            val jsonResponse = org.json.JSONObject(responseBody)
                            val success = jsonResponse.optBoolean("success", false)
                            val message = jsonResponse.optString("message", "")
                            val data = jsonResponse.optJSONObject("data")
                            
                            android.util.Log.d("LoginActivity", "파싱 결과 - success: $success, message: $message")
                            android.util.Log.d("LoginActivity", "파싱 결과 - data: $data")
                            
                            if (success && data != null) {
                                // data 객체가 있고, 필수 필드가 있는지 확인
                                val hasNickname = data.has("nickname") && data.optString("nickname").isNotBlank()
                                android.util.Log.d("LoginActivity", "프로필 데이터 확인 - nickname 존재: $hasNickname")
                                
                                // nickname이 있으면 프로필이 있다고 판단
                                if (hasNickname) {
                                    android.util.Log.d("LoginActivity", "✅ 서버에 프로필이 있음 - MainActivity로 이동")
                                    true
                                } else {
                                    android.util.Log.d("LoginActivity", "❌ 프로필 데이터에 nickname이 없음")
                                    false
                                }
                            } else {
                                android.util.Log.d("LoginActivity", "❌ success가 false이거나 data가 null")
                                false
                            }
                        } else {
                            android.util.Log.d("LoginActivity", "❌ 응답이 실패했거나 본문이 비어있음 - code: ${response.code}")
                            false
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("LoginActivity", "프로필 응답 파싱 실패: ${e.message}", e)
                        android.util.Log.e("LoginActivity", "응답 본문: $responseBody")
                        false
                    }

                    // UI 업데이트는 메인 스레드에서 수행
                    runOnUiThread {
                        if (hasProfile) {
                            // 서버에 프로필이 있으면 완료 표시하고 바로 MainActivity로 이동
                            ProfilePreferences.setProfileCompleted(this@LoginActivity, true)
                            android.util.Log.d("LoginActivity", "프로필 완료 플래그 설정 완료 - MainActivity로 이동")
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                            finish()
                        } else {
                            // 프로필이 없으면 ProfileSetupActivity로 이동
                            ProfilePreferences.setProfileCompleted(this@LoginActivity, false)
                            android.util.Log.d("LoginActivity", "프로필 없음 플래그 설정 - ProfileSetupActivity로 이동")
                            navigateAfterLogin(isGoogleLogin)
                        }
                    }
                }
            })
        }.addOnFailureListener { e ->
            android.util.Log.e("LoginActivity", "ID Token 발급 실패: ${e.message}", e)
            try {
                navigateAfterLogin(isGoogleLogin)
            } catch (ex: Exception) {
                android.util.Log.e("LoginActivity", "네비게이션 중 오류: ${ex.message}", ex)
            }
        }
        } catch (e: Exception) {
            android.util.Log.e("LoginActivity", "프로필 확인 함수 실행 중 오류: ${e.message}", e)
            try {
                navigateAfterLogin(isGoogleLogin)
            } catch (ex: Exception) {
                android.util.Log.e("LoginActivity", "네비게이션 중 오류: ${ex.message}", ex)
            }
        }
    }

    /**
     * 로그인 성공 후 네비게이션 처리
     * 이메일/Google 로그인 모두: 프로필 완료 여부에 따라 MainActivity 또는 ProfileSetupActivity로 이동
     */
    private fun navigateAfterLogin(isGoogleLogin: Boolean = false) {
        try {
            val hasCompletedProfile = ProfilePreferences.hasCompletedProfile(this)
            val nextActivity = if (!hasCompletedProfile) {
                // 프로필이 미완료인 경우 프로필 입력 화면으로
                ProfileSetupActivity::class.java
            } else {
                // 프로필이 완료된 경우 MainActivity로 바로 이동
                MainActivity::class.java
            }
            val intent = Intent(this, nextActivity)
            if (isGoogleLogin) {
                // Google 로그인에서 온 경우 플래그 전달
                intent.putExtra("from_google_login", true)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        } catch (e: Exception) {
            android.util.Log.e("LoginActivity", "네비게이션 중 오류: ${e.message}", e)
            // 오류 발생 시 기본적으로 ProfileSetupActivity로 이동
            try {
                val intent = Intent(this, ProfileSetupActivity::class.java)
                startActivity(intent)
                finish()
            } catch (ex: Exception) {
                android.util.Log.e("LoginActivity", "기본 네비게이션도 실패: ${ex.message}", ex)
            }
        }
    }

    /**
     * Google 로그인 시작
     */
    private fun signInWithGoogle() {
        try {
            android.util.Log.d("LoginActivity", "Google 로그인 시작...")
            val signInIntent = googleSignInClient.signInIntent
            android.util.Log.d("LoginActivity", "SignInIntent 생성 완료")
            googleSignInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            android.util.Log.e("LoginActivity", "Google 로그인 시작 실패: ${e.message}", e)
            Toast.makeText(
                this,
                "Google 로그인을 시작할 수 없습니다: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Google 계정으로 Firebase 인증
     */
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val idToken = acct.idToken
        if (idToken == null) {
            Toast.makeText(
                this,
                "Google 로그인 토큰을 가져올 수 없습니다. 다시 시도해주세요.",
                Toast.LENGTH_LONG
            ).show()
            android.util.Log.e("LoginActivity", "GoogleSignInAccount idToken is null")
            return
        }
        
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user ?: return@addOnSuccessListener
                
                // 서버에 ID Token 전송 (재시도 로직 포함)
                user.getIdToken(true)
                    .addOnSuccessListener { token ->
                        // Google 로그인 성공 시 서버에 전송 후 프로필 입력 화면으로 이동
                        sendIdTokenToServerForGoogleLogin(token.token ?: "")
                    }
                    .addOnFailureListener { e ->
                        val errorMsg = when {
                            e.message?.contains("network", ignoreCase = true) == true -> 
                                "네트워크 오류로 토큰을 가져올 수 없습니다. 인터넷 연결을 확인해주세요."
                            e.message?.contains("timeout", ignoreCase = true) == true -> 
                                "연결 시간이 초과되었습니다. 다시 시도해주세요."
                            else -> "토큰 발급 실패: ${e.message}"
                        }
                        Toast.makeText(
                            this,
                            errorMsg,
                            Toast.LENGTH_LONG
                        ).show()
                        android.util.Log.e("LoginActivity", "토큰 발급 실패", e)
                    }
            }
            .addOnFailureListener { e ->
                val errorMsg = when {
                    e.message?.contains("network", ignoreCase = true) == true -> 
                        "네트워크 오류로 Google 로그인에 실패했습니다. 인터넷 연결을 확인해주세요."
                    e.message?.contains("timeout", ignoreCase = true) == true -> 
                        "연결 시간이 초과되었습니다. 다시 시도해주세요."
                    else -> "Google 로그인 실패: ${e.message}"
                }
                Toast.makeText(
                    this,
                    errorMsg,
                    Toast.LENGTH_LONG
                ).show()
                android.util.Log.e("LoginActivity", "Google 로그인 실패", e)
            }
    }

    /**
     * Google 로그인용 서버 전송 (프로필 입력 화면으로 이동)
     * Google 로그인은 비밀번호가 없으므로 password 필드 없이 전송
     */
    private fun sendIdTokenToServerForGoogleLogin(idToken: String) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val json = """{"idToken": "$idToken"}"""
            val body = RequestBody.create("application/json; charset=utf-8".toMediaType(), json)

            val request = Request.Builder()
                .url(Config.getUrl(Config.Api.LOGIN))
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        android.util.Log.e("LoginActivity", "Google 로그인 서버 연결 실패: ${e.message}", e)
                        try {
                            // 서버 연결 실패해도 Google 로그인은 성공했으므로 프로필 확인 시도
                            checkProfileAndNavigate(isGoogleLogin = true)
                        } catch (ex: Exception) {
                            android.util.Log.e("LoginActivity", "프로필 확인 중 오류: ${ex.message}", ex)
                            navigateAfterLogin(isGoogleLogin = true)
                        }
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        try {
                            // Google 로그인 성공 시 서버에서 프로필 확인 후 네비게이션
                            checkProfileAndNavigate(isGoogleLogin = true)
                        } catch (e: Exception) {
                            android.util.Log.e("LoginActivity", "프로필 확인 중 오류: ${e.message}", e)
                            navigateAfterLogin(isGoogleLogin = true)
                        }
                    }
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("LoginActivity", "Google 로그인 서버 요청 생성 실패: ${e.message}", e)
            runOnUiThread {
                try {
                    checkProfileAndNavigate(isGoogleLogin = true)
                } catch (ex: Exception) {
                    android.util.Log.e("LoginActivity", "프로필 확인 중 오류: ${ex.message}", ex)
                    navigateAfterLogin(isGoogleLogin = true)
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onRegister: () -> Unit,
    onPasswordReset: () -> Unit,
    onComplete: (String, String) -> Unit,
    onGoogleLogin: () -> Unit,
    onGoogleKeyLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var saveEmail by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        // 헤더
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
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // WY 로고 (원형)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)  // 로고와 텍스트 사이 간격 줄임
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.wy_logo),
                    contentDescription = "WY Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp), // 패딩을 추가하여 로고가 잘리지 않도록
                    contentScale = ContentScale.Fit // Crop 대신 Fit 사용
                )
            }
            
            // 환영 메시지
            Text(
                text = "슬기로운 청년생활에 오신것을 환영합니다!",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                textAlign = TextAlign.Center,
                color = Color(0xFF1A1A1A),
                fontSize = 16.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
            )
        }

        // 폼 (간격도 줄임)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)  // 24.dp -> 16.dp
        ) {
            // 이메일 (입력 필드 크기 줄임)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "이메일",
                    style = MaterialTheme.typography.bodySmall,  // bodyMedium -> bodySmall
                    color = Color(0xFF1A1A1A),
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { 
                        Text(
                            "이메일을 입력하세요", 
                            color = Color.Gray, 
                            fontSize = 10.sp,
                            maxLines = 1
                        ) 
                    },
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

            // 비밀번호 (입력 필드 크기 줄임)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "비밀번호",
                    style = MaterialTheme.typography.bodySmall,  // bodyMedium -> bodySmall
                    color = Color(0xFF1A1A1A),
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { 
                        Text(
                            "비밀번호를 입력하세요", 
                            color = Color.Gray, 
                            fontSize = 10.sp,
                            maxLines = 1
                        ) 
                    },
                    visualTransformation = PasswordVisualTransformation(),
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

            // 이메일 저장 + 비밀번호 찾기
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = saveEmail,
                        onCheckedChange = { saveEmail = it }
                    )
                    Text(
                        text = "이메일 저장",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }

                TextButton(onClick = onPasswordReset) {
                    Text(
                        text = "비밀번호 찾기",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            // 로그인 버튼
            Button(
                onClick = { onComplete(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF59ABF7)  // 라이트 블루 (메인 컬러)
                )
            ) {
                Text("로그인", color = Color.White)
            }

            // 소셜 로그인
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // "또는" 구분선
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFE5E7EB)
                    )
                    Text(
                        text = "또는",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFE5E7EB)
                    )
                }

                // Google Login 로그인 버튼
                OutlinedButton(
                    onClick = onGoogleLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    border = BorderStroke(2.dp, Color(0xFFE5E7EB)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Black
                    )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google Login 로그인")
                }

                // Google Key Login 로그인 버튼
                OutlinedButton(
                    onClick = onGoogleKeyLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    border = BorderStroke(2.dp, Color(0xFFE5E7EB)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Black
                    )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google Key Login 로그인")
                }
            }

            // 하단 회원가입 링크
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "아직 회원이 아니신가요? ",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
                TextButton(onClick = onRegister) {
                    Text(
                        text = "회원가입",
                        fontSize = 14.sp,
                        color = Color(0xFF59ABF7)  // 라이트 블루 (메인 컬러)
                    )
                }
            }
        }
    }
}
