package com.wiseyoung.app

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
<<<<<<< HEAD
=======
import androidx.compose.foundation.shape.CircleShape
>>>>>>> bdfba64cbbd8e8630ad8ed32b12ce54887bf96a3
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.Config
import com.example.app.ui.components.SquareButton
import com.example.app.ui.theme.ThemeWrapper
import kotlinx.coroutines.delay
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

class PasswordResetActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeWrapper {
                PasswordResetScreen(
                    onBack = { finish() },
                    onComplete = { finish() }
                )
            }
        }
    }
}

@Composable
fun PasswordResetScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var isOtpVerified by remember { mutableStateOf(false) }
    var isEmailChecked by remember { mutableStateOf(false) }
    var isEmailExists by remember { mutableStateOf(false) }
    
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
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        /* 뒤로가기 및 타이틀 */
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "비밀번호 찾기",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        /* 이메일 입력칸 */
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                otpSent = false
                isOtpVerified = false
                isEmailChecked = false
                isEmailExists = false
            },
            label = { Text("이메일 주소") },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, MaterialTheme.shapes.small),
            singleLine = true,
            isError = isEmailChecked && !isEmailExists,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color(0xFFF5F5F5),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )
        
        /* 이메일 존재 확인 결과 표시 */
        if (isEmailChecked) {
            if (!isEmailExists) {
                Text(
                    "✘ 등록되지 않은 이메일 주소입니다.",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    "✔ 등록된 이메일 주소입니다.",
                    color = Color(0xFF4CAF50),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        Spacer(Modifier.height(10.dp))
        
        /* 이메일 확인 및 인증번호 발송 버튼 */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 이메일 확인 버튼
            OutlinedButton(
                onClick = {
                    checkEmailExists(email, context) { exists ->
                        isEmailChecked = true
                        isEmailExists = exists
                    }
                },
                enabled = isEmailFormatValid && !isEmailChecked,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(),
                border = BorderStroke(1.dp, Color(0xFF59ABF7))
            ) {
                Text("이메일 확인", color = Color(0xFF59ABF7))
            }
            
            // 인증번호 발송 버튼
            Button(
                onClick = {
                    sendOtpToServer(email, context) { success ->
                        if (success) {
                            otpSent = true
                            isEmailChecked = true
                            isEmailExists = true
                        }
                    }
                },
                enabled = isEmailFormatValid && isEmailExists,
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
                
                // 재발송 버튼
                SquareButton(
                    text = "재발송",
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
                    enabled = !isTimerExpired,
                    backgroundColor = Color(0xFF59ABF7),
                    textColor = Color.White,
                    size = 64.dp
                )
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
                Text("✔ 인증번호 확인 완료", color = Color(0xFF4CAF50))
            } else if (isTimerExpired) {
                Text("✘ 인증번호가 만료되었습니다. 재발송 버튼을 눌러주세요.", color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }
        }
        
        /* 새 비밀번호 입력 (인증번호 확인 후에만 표시) */
        if (isOtpVerified) {
            Spacer(Modifier.height(20.dp))
            
            /* 비밀번호 */
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("새 비밀번호 (영어/숫자/특수문자 포함)") },
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
<<<<<<< HEAD
                // PwRule은 RegisterActivity.kt에 정의되어 있음 (같은 패키지이므로 직접 사용 가능)
                PwRule(hasMinLength, "8자리 이상")
                PwRule(hasEng, "영어 포함")
                PwRule(hasNum, "숫자 포함")
                PwRule(hasSpecial, "특수문자 포함")
=======
                PasswordResetPwRule(hasMinLength, "8자리 이상")
                PasswordResetPwRule(hasEng, "영어 포함")
                PasswordResetPwRule(hasNum, "숫자 포함")
                PasswordResetPwRule(hasSpecial, "특수문자 포함")
>>>>>>> bdfba64cbbd8e8630ad8ed32b12ce54887bf96a3
            }
            
            Spacer(Modifier.height(12.dp))
            
            /* 비밀번호 확인 */
            OutlinedTextField(
                value = passwordCheck,
                onValueChange = { passwordCheck = it },
                label = { Text("비밀번호 확인") },
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
                                if (isPasswordMatch) Color(0xFF10B981) else Color(0xFF9CA3AF)
                            )
                    )
                    Text(
                        text = if (isPasswordMatch) "비밀번호가 일치합니다" else "비밀번호가 일치하지 않습니다",
                        color = if (isPasswordMatch) Color(0xFF1A1A1A) else Color(0xFF666666),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                        )
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            /* 비밀번호 변경 버튼 */
            Button(
                onClick = {
                    resetPassword(email, password, context) { success ->
                        if (success) {
                            Toast.makeText(context, "비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                            onComplete()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isPasswordValid && isPasswordMatch,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF59ABF7))
            ) {
                Text("비밀번호 변경", color = Color.White)
            }
        }
    }
}

<<<<<<< HEAD
// PwRule 함수는 RegisterActivity.kt에 정의되어 있음 (중복 제거)
=======
@Composable
fun PasswordResetPwRule(valid: Boolean, text: String) {
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
                    if (valid) Color(0xFF10B981) else Color(0xFF9CA3AF)
                )
        )
        Text(
            text = text,
            color = if (valid) Color(0xFF1A1A1A) else Color(0xFF666666),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
            )
        )
    }
}
>>>>>>> bdfba64cbbd8e8630ad8ed32b12ce54887bf96a3

/**
 * 이메일 존재 확인 (비밀번호 찾기용 - 이메일이 존재해야 함)
 * @param email 확인할 이메일 주소
 * @param context Android Context
 * @param callback 존재 여부를 반환 (true: 존재, false: 존재하지 않음)
 */
fun checkEmailExists(email: String, context: Context, callback: (Boolean) -> Unit) {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    val url = "${Config.getUrl(Config.Api.EMAIL_CHECK)}?email=${java.net.URLEncoder.encode(email, "UTF-8")}"
    Log.d("PasswordResetActivity", "이메일 존재 확인 요청 URL: $url")
    
    val request = Request.Builder()
        .url(url)
        .get()
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("PasswordResetActivity", "이메일 존재 확인 네트워크 오류", e)
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
                    "네트워크 오류: ${e.message ?: "알 수 없는 오류"}"
                }
            }
            
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                callback(false)
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            try {
                if (response.isSuccessful) {
                    val responseBody = response.body
                    if (responseBody == null) {
                        Log.e("PasswordResetActivity", "응답 본문이 null입니다.")
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            Toast.makeText(context, "서버 응답이 없습니다.", Toast.LENGTH_SHORT).show()
                            callback(false)
                        }
                        return
                    }
                    
                    val jsonString = responseBody.string()
                    Log.d("PasswordResetActivity", "서버 응답: $jsonString")
                    
                    if (jsonString.isBlank()) {
                        Log.e("PasswordResetActivity", "응답 본문이 비어있습니다.")
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            Toast.makeText(context, "서버 응답이 비어있습니다.", Toast.LENGTH_SHORT).show()
                            callback(false)
                        }
                        return
                    }
                    
                    // JSON 파싱: {"success":true,"message":"성공","data":true/false}
                    // data가 true면 중복(존재), false면 사용 가능(존재하지 않음)
                    val jsonObject = JSONObject(jsonString)
                    val exists = jsonObject.optBoolean("data", false)
                    
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        callback(exists)
                        
                        if (exists) {
                            Toast.makeText(context, "등록된 이메일 주소입니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "등록되지 않은 이메일 주소입니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val errorBody = try {
                        response.body?.string() ?: "응답 없음"
                    } catch (e: Exception) {
                        "응답 읽기 실패: ${e.message}"
                    }
                    Log.e("PasswordResetActivity", "서버 오류: ${response.code}, 응답: $errorBody")
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        Toast.makeText(context, "이메일 확인 실패: ${response.code}", Toast.LENGTH_SHORT).show()
                        callback(false)
                    }
                }
            } catch (e: Exception) {
                Log.e("PasswordResetActivity", "이메일 존재 확인 오류", e)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(context, "이메일 확인 실패: ${e.message ?: "알 수 없는 오류"}", Toast.LENGTH_SHORT).show()
                    callback(false)
                }
            }
        }
    })
}

/**
 * 비밀번호 재설정
 * @param email 이메일 주소
 * @param newPassword 새 비밀번호
 * @param context Android Context
 * @param callback 성공 여부
 */
fun resetPassword(email: String, newPassword: String, context: Context, callback: (Boolean) -> Unit) {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    val json = """{"email":"$email","newPassword":"$newPassword"}"""
    val body = RequestBody.create("application/json".toMediaType(), json)
    
    val request = Request.Builder()
        .url(Config.getUrl(Config.Api.PASSWORD_RESET))
        .post(body)
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("PasswordResetActivity", "비밀번호 재설정 네트워크 오류", e)
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
                    "비밀번호 재설정 실패: ${e.message ?: "알 수 없는 오류"}"
                }
            }
            
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                callback(false)
            }
        }
        
        override fun onResponse(call: Call, response: Response) {
            try {
                if (response.isSuccessful) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        callback(true)
                    }
                } else {
                    val errorBody = try {
                        response.body?.string() ?: "응답 없음"
                    } catch (e: Exception) {
                        "응답 읽기 실패: ${e.message}"
                    }
                    Log.e("PasswordResetActivity", "비밀번호 재설정 실패: ${response.code}, 응답: $errorBody")
                    
                    val errorMessage = try {
                        val jsonObject = JSONObject(errorBody)
                        jsonObject.optString("message", "비밀번호 재설정 실패")
                    } catch (e: Exception) {
                        "비밀번호 재설정 실패: ${response.code}"
                    }
                    
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        callback(false)
                    }
                }
            } catch (e: Exception) {
                Log.e("PasswordResetActivity", "비밀번호 재설정 중 오류", e)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(context, "비밀번호 재설정 실패: ${e.message}", Toast.LENGTH_LONG).show()
                    callback(false)
                }
            }
        }
    })
}
