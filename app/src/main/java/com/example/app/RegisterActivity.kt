package com.wiseyoung.app

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.app.ui.theme.WiseYoungTheme
import com.example.app.Config
import com.example.app.DeviceInfo
import com.example.app.data.FirestoreService
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.util.Date


class RegisterActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WiseYoungTheme {
                RegisterScreen(
                    onBack = { finish() },
                    onRegister = { email, password, nickname ->
                        registerUser(email, password, nickname)
                    }
                )
            }
        }
    }

    /** 🔥 Firebase 회원가입 → (이메일 인증 없음) → 서버 DB 저장 */
    private fun registerUser(email: String, password: String, nickname: String) {

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->

                val user = result.user ?: return@addOnSuccessListener

                // 🔥 Firebase ID Token 발급 → Spring 서버로 전달
                user.getIdToken(true)
                    .addOnSuccessListener { tokenResult ->
                        val idToken = tokenResult.token ?: return@addOnSuccessListener
                        sendSignupToServer(idToken, nickname)
                    }

                launchProfileSetup()
            }
            .addOnFailureListener {
                Toast.makeText(this, "회원가입 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun launchProfileSetup() {
        ProfilePreferences.setProfileCompleted(this, false)
        startActivity(Intent(this, ProfileSetupActivity::class.java))
        finish()
    }


    /** 🔥 서버로 idToken + nickname 전송 → MariaDB 저장 */
    private fun sendSignupToServer(idToken: String, nickname: String) {

        val client = OkHttpClient()

        val json = """
            {
                "idToken": "$idToken",
                "nickname": "$nickname"
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
                runOnUiThread {
                    Toast.makeText(
                        this@RegisterActivity,
                        "서버 연결 실패: ${e.message}",
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
    onRegister: (String, String, String) -> Unit,
    onLogin: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var isOtpVerified by remember { mutableStateOf(false) }

    var password by remember { mutableStateOf("") }
    var passwordCheck by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var nicknameChecked by remember { mutableStateOf(false) }

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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        /* 뒤로가기 */
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }

        Spacer(Modifier.height(16.dp))

        /* 이메일 입력칸 */
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                otpSent = false
                isOtpVerified = false
            },
            label = { Text("이메일 주소") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(10.dp))

        /* 인증번호 발송 버튼 */
        Button(
            onClick = {
                sendOtpToServer(email, context)
                otpSent = true
            },
            enabled = isEmailFormatValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("인증번호 발송")
        }

        /* 인증번호 입력 */
        if (otpSent) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = otp,
                onValueChange = { otp = it },
                label = { Text("인증번호 입력") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    verifyOtpWithServer(email, otp, context) { success ->
                        isOtpVerified = success
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("인증번호 확인")
            }

            if (isOtpVerified) {
                Text("✔ 이메일 인증 완료", color = Color(0xFF4CAF50))
            }
        }

        Spacer(Modifier.height(20.dp))

        /* 닉네임 */
        OutlinedTextField(
            value = nickname,
            onValueChange = {
                nickname = it
                nicknameChecked = false
            },
            label = { Text("닉네임") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        /* 비밀번호 */
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("비밀번호 (영어/숫자/특수문자 포함)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Column(Modifier.fillMaxWidth()) {
            PwRule(hasMinLength, "8자리 이상")
            PwRule(hasEng, "영어 포함")
            PwRule(hasNum, "숫자 포함")
            PwRule(hasSpecial, "특수문자 포함")
        }

        Spacer(Modifier.height(12.dp))

        /* 비밀번호 확인 */
        OutlinedTextField(
            value = passwordCheck,
            onValueChange = { passwordCheck = it },
            label = { Text("비밀번호 확인") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (passwordCheck.isNotEmpty()) {
            Text(
                if (isPasswordMatch) "비밀번호가 일치합니다 ✔"
                else "비밀번호가 일치하지 않습니다 ✘",
                color = if (isPasswordMatch) Color(0xFF4CAF50) else Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(24.dp))

        /* 회원가입 버튼 (OTP 인증 완료해야 활성화됨) */
        Button(
            onClick = { onRegister(email, password, nickname) },
            modifier = Modifier.fillMaxWidth(),
            enabled =
                isOtpVerified &&
                        nickname.isNotEmpty() &&
                        isPasswordValid &&
                        isPasswordMatch,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
        ) {
            Text("회원가입", color = Color.White)
        }
    }
}

@Composable
fun PwRule(valid: Boolean, text: String) {
    Text(
        text = if (valid) "✔ $text" else "✘ $text",
        color = if (valid) Color(0xFF4CAF50) else Color.Red,
        style = MaterialTheme.typography.bodySmall
    )
}


/* ---------------- OTP API ---------------- */

fun sendOtpToServer(email: String, context: Context) {
    val client = OkHttpClient()

    val json = """{"email":"$email"}"""
    val body = RequestBody.create("application/json".toMediaType(), json)

    val request = Request.Builder()
        .url("http://172.16.1.42:8080/auth/otp/send")
        .post(body)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Toast.makeText(context, "OTP 발송 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }

        override fun onResponse(call: Call, response: Response) {
            Toast.makeText(context, "인증번호가 이메일로 전송되었습니다.", Toast.LENGTH_SHORT).show()
        }
    })
}

fun verifyOtpWithServer(
    email: String,
    otp: String,
    context: Context,
    callback: (Boolean) -> Unit
) {
    val client = OkHttpClient()

    val json = """{"email":"$email","otp":"$otp"}"""
    val body = RequestBody.create("application/json".toMediaType(), json)

    val request = Request.Builder()
        .url("http://172.16.1.42:8080/auth/otp/verify")
        .post(body)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            callback(false)
            Toast.makeText(context, "인증 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        override fun onResponse(call: Call, response: Response) {
            val ok = response.isSuccessful
            callback(ok)
            Toast.makeText(
                context,
                if (ok) "인증 성공" else "인증 실패",
                Toast.LENGTH_SHORT
            ).show()
        }
    })
}
