package com.wiseyoung.app

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.app.ui.theme.ThemeWrapper
import com.example.app.ui.components.SquareButton
import com.example.app.Config
import com.example.app.DeviceInfo
import com.example.app.data.FirestoreService
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException
import java.util.Date


class RegisterActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeWrapper {
                RegisterScreen(
                    onBack = { finish() },
                    onRegister = { email, password ->
                        registerUser(email, password)
                    }
                )
            }
        }
    }

    /** 🔥 Firebase 회원가입 → 서버 DB 저장 (Gmail SMTP 사용) */
    private fun registerUser(email: String, password: String, retryCount: Int = 0) {
        // 이메일과 비밀번호 앞뒤 공백 제거
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        
        // 이메일 형식 검증 (더 엄격한 검증)
        val emailPattern = android.util.Patterns.EMAIL_ADDRESS
        if (trimmedEmail.isEmpty() || !emailPattern.matcher(trimmedEmail).matches()) {
            Toast.makeText(
                this, 
                "올바른 이메일 주소를 입력해주세요.\n예: example@email.com", 
                Toast.LENGTH_LONG
            ).show()
            Log.e("RegisterActivity", "이메일 형식 오류: '$trimmedEmail'")
            return
        }
        
        // 이메일에 공백이 있는지 확인
        if (trimmedEmail.contains(" ")) {
            Toast.makeText(
                this, 
                "이메일 주소에 공백이 포함되어 있습니다.\n공백을 제거해주세요.", 
                Toast.LENGTH_LONG
            ).show()
            Log.e("RegisterActivity", "이메일 공백 포함: '$trimmedEmail'")
            return
        }
        
        Log.d("RegisterActivity", "회원가입 시작: $trimmedEmail (재시도 횟수: $retryCount)")

        auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPassword)
            .addOnSuccessListener { result ->
                Log.d("RegisterActivity", "Firebase 회원가입 성공")
                val user = result.user ?: return@addOnSuccessListener

                // 🔥 Firebase ID Token 발급 → Spring 서버로 전달 (비밀번호 포함)
                user.getIdToken(true)
                    .addOnSuccessListener { tokenResult ->
                        val idToken = tokenResult.token ?: return@addOnSuccessListener
                        Log.d("RegisterActivity", "ID Token 발급 성공, 서버로 전송 중...")
                        sendSignupToServer(idToken, trimmedPassword)
                    }
                    .addOnFailureListener { e ->
                        Log.e("RegisterActivity", "ID Token 발급 실패", e)
                        Toast.makeText(this, "토큰 발급 실패: ${e.message}", Toast.LENGTH_LONG).show()
                    }

                launchProfileSetup()
            }
            .addOnFailureListener { e ->
                Log.e("RegisterActivity", "Firebase 회원가입 실패 (재시도 횟수: $retryCount)", e)
                
                // reCAPTCHA 또는 Connection reset 오류 발생 시 최대 3번까지 재시도
                val isNetworkError = e.message?.contains("reCAPTCHA") == true || 
                                    e.message?.contains("Connection reset") == true ||
                                    e.message?.contains("network") == true ||
                                    e is com.google.firebase.FirebaseNetworkException
                
                if (isNetworkError && retryCount < 3) {
                    // 재시도 간격을 늘림: 1초 -> 2초 -> 3초 (지수 백오프)
                    val delayMs = (retryCount + 1) * 2000L // 2초, 4초, 6초
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        Log.d("RegisterActivity", "재시도 중... (${retryCount + 1}/3) - ${delayMs/1000}초 대기 후")
                        registerUser(trimmedEmail, trimmedPassword, retryCount + 1)
                    }, delayMs)
                } else {
                    val errorMessage = when {
                        isNetworkError -> {
                            "네트워크 연결 오류가 발생했습니다.\n\n" +
                            "🔧 해결 방법:\n" +
                            "1. Wi-Fi 또는 모바일 데이터 연결 확인\n" +
                            "2. 앱 완전 종료 후 재시작\n" +
                            "3. 기기 재부팅 후 재시도\n" +
                            "4. 다른 네트워크로 변경 후 재시도\n\n" +
                            "⚠️ Firebase reCAPTCHA 연결 문제일 수 있습니다.\n" +
                            "잠시 후 다시 시도해주세요."
                        }
                        e.message?.contains("email-already-in-use") == true -> {
                            "이미 사용 중인 이메일 주소입니다."
                        }
                        e.message?.contains("weak-password") == true -> {
                            "비밀번호가 너무 약합니다."
                        }
                        e.message?.contains("invalid-email") == true ||
                        e.message?.contains("badly formatted") == true ||
                        e.message?.contains("The email address is badly formatted") == true -> {
                            "이메일 주소 형식이 올바르지 않습니다.\n\n" +
                            "확인 사항:\n" +
                            "1. 이메일 주소에 공백이 없는지 확인\n" +
                            "2. @ 기호와 도메인이 포함되어 있는지 확인\n" +
                            "3. 예: example@email.com"
                        }
                        else -> {
                            "회원가입 실패: ${e.message ?: "알 수 없는 오류"}"
                        }
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun launchProfileSetup() {
        ProfilePreferences.setProfileCompleted(this, false)
        startActivity(Intent(this, ProfileSetupActivity::class.java))
        finish()
    }


    /** 🔥 서버로 idToken + password 전송 → MariaDB 저장 (서버에서 BCrypt로 해시화) */
    private fun sendSignupToServer(idToken: String, password: String) {

        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val json = """
            {
                "idToken": "$idToken",
                "password": "$password"
            }
        """.trimIndent()

        val requestBody = RequestBody.create(
            "application/json".toMediaType(),
            json
        )

        val request = Request.Builder()
            .url(Config.getUrl(Config.Api.SIGNUP))
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Log.e("RegisterActivity", "회원가입 서버 전송 네트워크 오류", e)
                val errorMessage = when {
                    e.message?.contains("Failed to connect") == true || 
                    e.message?.contains("Unable to resolve host") == true -> {
                        "서버에 연결할 수 없습니다.\n\n" +
                        "🔧 확인 사항:\n" +
                        "1. 서버가 실행 중인지 확인\n" +
                        "2. ADB 포트 포워딩 실행:\n" +
                        "   adb reverse tcp:8080 tcp:8080\n" +
                        "3. Wi-Fi 또는 모바일 데이터 연결 확인"
                    }
                    e.message?.contains("timeout") == true -> {
                        "연결 시간이 초과되었습니다.\n서버 응답을 기다리는 중입니다."
                    }
                    e.message?.contains("Connection refused") == true ||
                    e is java.net.ConnectException -> {
                        "서버 연결이 거부되었습니다.\n\n" +
                        "🔧 확인 사항:\n" +
                        "1. Spring Boot 서버가 실행 중인지 확인\n" +
                        "   (http://localhost:8080 접속 테스트)\n" +
                        "2. ADB 포트 포워딩 확인:\n" +
                        "   adb reverse --list\n" +
                        "   (없으면: adb reverse tcp:8080 tcp:8080)\n" +
                        "3. USB 연결이 끊기지 않았는지 확인"
                    }
                    else -> {
                        "서버 연결 실패: ${e.message ?: "알 수 없는 오류"}"
                    }
                }
                
                runOnUiThread {
                    Toast.makeText(
                        this@RegisterActivity,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        // 서버 저장 성공 시 Firestore에도 저장
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            val appVersion = DeviceInfo.getAppVersion(this@RegisterActivity)
                            val deviceId = DeviceInfo.getDeviceId(this@RegisterActivity)
                            
                            val firestoreUser = FirestoreService.User(
                                userId = currentUser.uid,
                                email = currentUser.email ?: "",
                                emailVerified = currentUser.isEmailVerified,
                                passwordHash = "", // Google 로그인 시 빈 문자열
                                loginType = "EMAIL",
                                osType = "ANDROID",
                                appVersion = appVersion,
                                deviceId = deviceId,
                                createdAt = Date()
                            )
                            
                            FirestoreService.saveUser(
                                user = firestoreUser,
                                onSuccess = {
                                    // Firestore 저장 성공
                                },
                                onFailure = { exception ->
                                    // Firestore 저장 실패 (로그만 남김, 서버 저장은 성공했으므로 계속 진행)
                                    android.util.Log.e("RegisterActivity", "Firestore 저장 실패: ${exception.message}")
                                }
                            )
                        }
                        
                        Toast.makeText(
                            this@RegisterActivity,
                            "회원정보(DB) 저장 완료!",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@RegisterActivity,
                            "서버 오류: ${response.code}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
    }
}


/* --------------------------- UI --------------------------- */

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegister: (String, String) -> Unit,
    onLogin: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var isOtpVerified by remember { mutableStateOf(false) }
    var isEmailChecked by remember { mutableStateOf(false) }
    var isEmailDuplicate by remember { mutableStateOf(false) }

    var password by remember { mutableStateOf("") }
    var passwordCheck by remember { mutableStateOf("") }

    val context = LocalContext.current

    /* 이메일 형식 검사 */
    val isEmailFormatValid = email.contains("@") && email.contains(".")

    /* 비밀번호 규칙 체크 */
    val hasMinLength = password.length >= 8
    val hasEng = password.any { it.isLetter() }
    val hasNum = password.any { it.isDigit() }
    val hasSpecial = password.any { "!@#$%^&*()_+-=[]{};:'\",.<>/?".contains(it) }

    val isPasswordValid = hasMinLength && hasEng && hasNum && hasSpecial
    val isPasswordMatch = password == passwordCheck

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()), // 스크롤 추가
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        /* 뒤로가기 및 타이틀 */
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "회원가입",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        // WY 로고 (원형)
        Box(
            modifier = Modifier
                .size(120.dp)  // 크기 120dp로 설정
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color.White)
                .border(1.dp, Color(0xFFE5E7EB), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.wy_logo),
                contentDescription = "WY Logo",
                modifier = Modifier.fillMaxSize(), // padding(16.dp) 제거하여 원본 크기 유지
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }

        Spacer(Modifier.height(24.dp))

            /* 이메일 입력칸 */
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                otpSent = false
                isOtpVerified = false
                isEmailChecked = false
                isEmailDuplicate = false
            },
            label = { Text("이메일 주소") },
            placeholder = { 
                Text(
                    "이메일을 입력하세요", 
                    color = Color.Gray, 
                    fontSize = 10.sp
                ) 
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, MaterialTheme.shapes.small),
            singleLine = true,
            isError = isEmailChecked && isEmailDuplicate,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color(0xFFF5F5F5),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )

        /* 이메일 중복 확인 결과 표시 */
        if (isEmailChecked) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(
                            if (!isEmailDuplicate) Color(0xFF10B981) else Color(0xFF9CA3AF)
                        )
                )
                Text(
                    text = if (!isEmailDuplicate) "사용 가능한 이메일 주소입니다" else "이미 사용 중인 이메일 주소입니다",
                    color = if (!isEmailDuplicate) Color(0xFF1A1A1A) else Color(0xFF666666),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                    )
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        /* 이메일 중복 확인 및 인증번호 발송 버튼 */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 중복 확인 버튼
            OutlinedButton(
                onClick = {
                    checkEmailDuplicate(email, context) { isDuplicate ->
                        isEmailChecked = true
                        isEmailDuplicate = isDuplicate
                    }
                },
                enabled = isEmailFormatValid && !isEmailChecked,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(),
                border = BorderStroke(1.dp, Color(0xFF59ABF7))
            ) {
                Text("중복 확인", color = Color(0xFF59ABF7))
            }

            // 인증번호 발송 버튼
        Button(
            onClick = {
                    sendOtpToServer(email, context) { success ->
                        if (success) {
                otpSent = true
                            isEmailChecked = true
                            isEmailDuplicate = false
                        }
                    }
            },
                enabled = isEmailFormatValid && !isEmailDuplicate,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF59ABF7))
        ) {
            Text("인증번호 발송")
            }
        }

        /* 인증번호 입력 */
        if (otpSent) {
            Spacer(Modifier.height(12.dp))
            
            // 5분 타이머 상태
            var remainingTime by remember { mutableStateOf(300) } // 5분 = 300초
            var isTimerExpired by remember { mutableStateOf(false) }
            
            // 타이머 시작
            LaunchedEffect(otpSent) {
                remainingTime = 300 // 발송 시마다 초기화
                isTimerExpired = false
                while (remainingTime > 0 && otpSent) {
                    kotlinx.coroutines.delay(1000)
                    remainingTime--
                }
                if (remainingTime == 0) {
                    isTimerExpired = true
                }
            }
            
            // 타이머 표시 포맷 (MM:SS)
            val minutes = remainingTime / 60
            val seconds = remainingTime % 60
            val timerText = String.format("%02d:%02d", minutes, seconds)
            
            // 인증번호 입력칸과 재발송 버튼을 옆에 배치
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = otp,
                    onValueChange = { otp = it },
                    label = { Text("인증번호 입력") },
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White, MaterialTheme.shapes.small),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color(0xFFF5F5F5),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    trailingIcon = {
                        if (!isTimerExpired) {
                            Text(
                                text = timerText,
                                color = if (remainingTime <= 60) Color.Red else Color(0xFF59ABF7),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        } else {
                            Text(
                                text = "만료",
                                color = Color.Red,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                )
                
                // 재발송 버튼 - 옆에 배치
                Button(
                    onClick = {
                        sendOtpToServer(email, context) { success ->
                            if (success) {
                                remainingTime = 300 // 타이머 초기화
                                isTimerExpired = false
                                otp = "" // 인증번호 입력칸 초기화
                                isOtpVerified = false
                                Toast.makeText(context, "인증번호가 재발송되었습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = true, // 항상 재발송 가능
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF59ABF7)  // 라이트 블루 (메인 컬러)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("재발송", color = Color.White)
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    verifyOtpWithServer(email, otp, context) { success ->
                        isOtpVerified = success
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTimerExpired && otp.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF59ABF7))
            ) {
                Text("인증번호 확인")
            }

            if (isOtpVerified) {
                Text("✔ 이메일 인증 완료", color = Color(0xFF4CAF50))
            } else if (isTimerExpired) {
                Text("✘ 인증번호가 만료되었습니다. 재발송 버튼을 눌러주세요.", color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(20.dp))

        /* 비밀번호 */
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("비밀번호") },
            placeholder = { 
                Text(
                    "비밀번호를 입력하세요 (8자 이상)", 
                    color = Color.Gray, 
                    fontSize = 10.sp
                ) 
            },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, MaterialTheme.shapes.small),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color(0xFFF5F5F5),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )

        Column(Modifier.fillMaxWidth()) {
            RegisterPwRule(hasMinLength, "8자리 이상")
            RegisterPwRule(hasEng, "영어 포함")
            RegisterPwRule(hasNum, "숫자 포함")
            RegisterPwRule(hasSpecial, "특수문자 포함")
        }

        Spacer(Modifier.height(12.dp))

        /* 비밀번호 확인 */
        OutlinedTextField(
            value = passwordCheck,
            onValueChange = { passwordCheck = it },
            label = { Text("비밀번호 확인") },
            placeholder = { 
                Text(
                    "비밀번호를 다시 입력하세요", 
                    color = Color.Gray, 
                    fontSize = 10.sp
                ) 
            },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, MaterialTheme.shapes.small),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color(0xFFF5F5F5),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )

        if (passwordCheck.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(
                            if (isPasswordMatch) Color(0xFF10B981) else Color(0xFF9CA3AF)  // 성공: 초록, 실패: 회색
                        )
                )
            Text(
                    text = if (isPasswordMatch) "비밀번호가 일치합니다" else "비밀번호가 일치하지 않습니다",
                    color = if (isPasswordMatch) Color(0xFF1A1A1A) else Color(0xFF666666),  // 성공: 진한 회색, 실패: 중간 회색
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                    )
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        /* 회원가입 버튼 (OTP 인증 완료해야 활성화됨) */
        Button(
            onClick = { onRegister(email.trim(), password.trim()) },
            modifier = Modifier.fillMaxWidth(),
            enabled =
                isOtpVerified &&
                        isPasswordValid &&
                        isPasswordMatch,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF59ABF7))
        ) {
            Text("회원가입", color = Color.White)
        }
    }
}

@Composable
fun RegisterPwRule(valid: Boolean, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 간단한 체크/엑스 아이콘 대신 색상 원 사용
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(
                    if (valid) Color(0xFF10B981) else Color(0xFF9CA3AF)  // 성공: 초록, 실패: 회색
                )
        )
    Text(
            text = text,
            color = if (valid) Color(0xFF1A1A1A) else Color(0xFF666666),  // 성공: 진한 회색, 실패: 중간 회색
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = if (valid) androidx.compose.ui.text.font.FontWeight.Normal else androidx.compose.ui.text.font.FontWeight.Normal
            )
        )
    }
}


/* ---------------- OTP API ---------------- */

/**
 * 이메일 중복 확인
 * @param email 확인할 이메일 주소
 * @param context Android Context
 * @param callback 중복 여부를 반환 (true: 중복, false: 사용 가능)
 */
fun checkEmailDuplicate(email: String, context: Context, callback: (Boolean) -> Unit) {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val url = "${Config.getUrl(Config.Api.EMAIL_CHECK)}?email=${java.net.URLEncoder.encode(email, "UTF-8")}"
    Log.d("RegisterActivity", "이메일 중복 확인 요청 URL: $url")
    
    val request = Request.Builder()
        .url(url)
        .get()
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("RegisterActivity", "이메일 중복 확인 네트워크 오류", e)
            val errorMessage = when {
                e.message?.contains("Failed to connect") == true || 
                e.message?.contains("Unable to resolve host") == true -> {
                    "서버에 연결할 수 없습니다.\n\n" +
                    "🔧 확인 사항:\n" +
                    "1. 서버가 실행 중인지 확인\n" +
                    "2. ADB 포트 포워딩 실행:\n" +
                    "   adb reverse tcp:8080 tcp:8080\n" +
                    "3. Wi-Fi 또는 모바일 데이터 연결 확인"
                }
                e.message?.contains("timeout") == true -> {
                    "연결 시간이 초과되었습니다.\n서버 응답을 기다리는 중입니다."
                }
                e.message?.contains("Connection refused") == true -> {
                    "서버 연결이 거부되었습니다.\n서버가 실행 중인지 확인해주세요."
                }
                else -> {
                    "네트워크 오류: ${e.message ?: "알 수 없는 오류"}"
                }
            }
            
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                callback(true) // 에러 시 안전하게 중복으로 처리
            }
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                // OkHttp의 enqueue는 백그라운드 스레드에서 실행되므로 여기서 응답 본문을 읽어도 됩니다
                if (response.isSuccessful) {
                    val responseBody = response.body
                    if (responseBody == null) {
                        Log.e("RegisterActivity", "응답 본문이 null입니다.")
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            Toast.makeText(context, "서버 응답이 없습니다.", Toast.LENGTH_SHORT).show()
                            callback(true)
                        }
                        return
                    }
                    
                    val jsonString = responseBody.string()
                    Log.d("RegisterActivity", "서버 응답: $jsonString")
                    
                    if (jsonString.isBlank()) {
                        Log.e("RegisterActivity", "응답 본문이 비어있습니다.")
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            Toast.makeText(context, "서버 응답이 비어있습니다.", Toast.LENGTH_SHORT).show()
                            callback(true)
                        }
                        return
                    }
                    
                    // JSON 파싱: {"success":true,"message":"성공","data":true/false}
                    // data가 true면 중복, false면 사용 가능
                    val jsonObject = JSONObject(jsonString)
                    val isDuplicate = jsonObject.optBoolean("data", false)
                    
                    // UI 업데이트는 메인 스레드에서 수행
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        callback(isDuplicate)
                        
                        if (isDuplicate) {
                            Toast.makeText(context, "이미 사용 중인 이메일 주소입니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "사용 가능한 이메일 주소입니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val errorBody = try {
                        response.body?.string() ?: "응답 없음"
                    } catch (e: Exception) {
                        "응답 읽기 실패: ${e.message}"
                    }
                    Log.e("RegisterActivity", "서버 오류: ${response.code}, 응답: $errorBody")
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        Toast.makeText(context, "중복 확인 실패: ${response.code}", Toast.LENGTH_SHORT).show()
                        callback(true)
                    }
                }
            } catch (e: Exception) {
                Log.e("RegisterActivity", "이메일 중복 확인 오류", e)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(context, "중복 확인 실패: ${e.message ?: "알 수 없는 오류"}", Toast.LENGTH_SHORT).show()
                    callback(true)
                }
            }
        }
    })
}

/**
 * 인증번호 발송 (자동 중복 확인 포함)
 * @param email 이메일 주소
 * @param context Android Context
 * @param callback 발송 성공 여부
 */
fun sendOtpToServer(email: String, context: Context, callback: (Boolean) -> Unit = {}) {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val json = """{"email":"$email"}"""
    val body = RequestBody.create("application/json".toMediaType(), json)

    val request = Request.Builder()
        .url(Config.getUrl(Config.Api.OTP_SEND))
        .post(body)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("RegisterActivity", "OTP 발송 네트워크 오류", e)
            val errorMessage = when {
                e.message?.contains("Failed to connect") == true || 
                e.message?.contains("Unable to resolve host") == true -> {
                    "서버에 연결할 수 없습니다.\n\n" +
                    "🔧 확인 사항:\n" +
                    "1. 서버가 실행 중인지 확인\n" +
                    "2. ADB 포트 포워딩 실행:\n" +
                    "   adb reverse tcp:8080 tcp:8080\n" +
                    "3. Wi-Fi 또는 모바일 데이터 연결 확인"
                }
                e.message?.contains("timeout") == true -> {
                    "연결 시간이 초과되었습니다.\n서버 응답을 기다리는 중입니다."
                }
                e.message?.contains("Connection refused") == true -> {
                    "서버 연결이 거부되었습니다.\n서버가 실행 중인지 확인해주세요."
                }
                else -> {
                    "OTP 발송 실패: ${e.message ?: "알 수 없는 오류"}"
                }
            }
            
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                callback(false)
            }
        }

        override fun onResponse(call: Call, response: Response) {
            // OkHttp의 enqueue는 백그라운드 스레드에서 실행되므로 여기서 응답 본문을 읽어도 됩니다
            when (response.code) {
                200 -> {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, "인증번호가 이메일로 전송되었습니다.", Toast.LENGTH_SHORT).show()
                        callback(true)
                    }
                }
                409 -> {
                    // 이메일 중복 (서버에서 자동 확인)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        Toast.makeText(context, "이미 등록된 이메일 주소입니다.", Toast.LENGTH_LONG).show()
                        callback(false)
                    }
                }
                else -> {
                    val errorMsg = try {
                        val jsonString = response.body?.string()
                        // 간단한 JSON 파싱 (실제로는 Gson 사용 권장)
                        jsonString?.substringAfter("\"message\":\"")?.substringBefore("\"") 
                            ?: "서버 오류: ${response.code}"
                    } catch (e: Exception) {
                        "서버 오류: ${response.code}"
                    }
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        Toast.makeText(context, "OTP 발송 실패: $errorMsg", Toast.LENGTH_LONG).show()
                        callback(false)
                    }
                }
            }
        }
    })
}

fun verifyOtpWithServer(
    email: String,
    otp: String,
    context: Context,
    callback: (Boolean) -> Unit
) {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val json = """{"email":"$email","otp":"$otp"}"""
    val body = RequestBody.create("application/json".toMediaType(), json)

    val request = Request.Builder()
        .url(Config.getUrl(Config.Api.OTP_VERIFY))
        .post(body)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("RegisterActivity", "OTP 인증 네트워크 오류", e)
            val errorMessage = when {
                e.message?.contains("Failed to connect") == true || 
                e.message?.contains("Unable to resolve host") == true -> {
                    "서버에 연결할 수 없습니다.\n\n" +
                    "🔧 확인 사항:\n" +
                    "1. 서버가 실행 중인지 확인\n" +
                    "2. ADB 포트 포워딩 실행:\n" +
                    "   adb reverse tcp:8080 tcp:8080\n" +
                    "3. Wi-Fi 또는 모바일 데이터 연결 확인"
                }
                e.message?.contains("timeout") == true -> {
                    "연결 시간이 초과되었습니다.\n서버 응답을 기다리는 중입니다."
                }
                e.message?.contains("Connection refused") == true -> {
                    "서버 연결이 거부되었습니다.\n서버가 실행 중인지 확인해주세요."
                }
                else -> {
                    "인증 실패: ${e.message ?: "알 수 없는 오류"}"
                }
            }
            
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                callback(false)
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        }

        override fun onResponse(call: Call, response: Response) {
            // OkHttp의 enqueue는 백그라운드 스레드에서 실행되므로 여기서 응답 본문을 읽어도 됩니다
            try {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d("RegisterActivity", "인증번호 검증 응답: $responseBody")
                    
                    if (responseBody != null && responseBody.isNotBlank()) {
                        try {
                            val jsonObject = JSONObject(responseBody)
                            val success = jsonObject.optBoolean("success", false)
                            
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                callback(success)
                                if (success) {
                                    Toast.makeText(context, "인증 성공", Toast.LENGTH_SHORT).show()
                                } else {
                                    val message = jsonObject.optString("message", "인증 실패")
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                            return
                        } catch (e: Exception) {
                            Log.e("RegisterActivity", "JSON 파싱 오류", e)
                        }
                    }
                    
                    // JSON 파싱 실패 시 기본 처리
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        callback(true) // 응답이 성공이면 인증 성공으로 처리
                        Toast.makeText(context, "인증 성공", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = try {
                        response.body?.string() ?: "응답 없음"
                    } catch (e: Exception) {
                        "응답 읽기 실패: ${e.message}"
                    }
                    Log.e("RegisterActivity", "인증번호 검증 실패: ${response.code}, 응답: $errorBody")
                    
                    val errorMessage = try {
                        val jsonObject = JSONObject(errorBody)
                        jsonObject.optString("message", "인증 실패")
                    } catch (e: Exception) {
                        "인증 실패: ${response.code}"
                    }
                    
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        callback(false)
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("RegisterActivity", "인증번호 검증 중 오류", e)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    callback(false)
                    Toast.makeText(context, "인증 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    })
}
