package com.wiseyoung.app

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.app.ui.theme.WiseYoungTheme
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException


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

    /** 🔥 Firebase 회원가입 + 이메일 인증 + 서버 DB 저장 */
    private fun registerUser(email: String, password: String, nickname: String) {

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->

                val user = result.user ?: return@addOnSuccessListener

                // 🔥 이메일 인증 보내기
                user.sendEmailVerification()
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "회원가입 성공! 이메일 인증을 완료해주세요.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                // 🔥 토큰 받아서 서버로 DB 저장 요청
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
            .url("http://172.16.1.42:8080/auth/signup")   // ⭐ 유정님 SpringBoot 주소
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
    var password by remember { mutableStateOf("") }
    var passwordCheck by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var nicknameChecked by remember { mutableStateOf(false) }
    var emailDuplicate by remember { mutableStateOf<Boolean?>(null) }

    val auth = FirebaseAuth.getInstance()

    /* 이메일 형식 검사 */
    val isEmailFormatValid = email.contains("@") && email.contains(".")

    /* 이메일 중복 검사 */
    LaunchedEffect(email) {
        if (isEmailFormatValid) {
            auth.fetchSignInMethodsForEmail(email)
                .addOnSuccessListener {
                    emailDuplicate = it.signInMethods?.isNotEmpty()
                }
        } else {
            emailDuplicate = null
        }
    }


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

        /* 이메일 입력 + 체크 */
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                },
                label = { Text("이메일 주소") },
                textStyle = LocalTextStyle.current.copy(color = Color.Black),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))

            if (email.isNotEmpty()) {
                val icon = when {
                    !isEmailFormatValid -> "✘"
                    emailDuplicate == true -> "✘"
                    else -> "✔"
                }

                val color = when {
                    !isEmailFormatValid -> Color.Red
                    emailDuplicate == true -> Color.Red
                    else -> Color(0xFF4CAF50)
                }

                Text(icon, color = color, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(Modifier.height(16.dp))

        /* 닉네임 */
        OutlinedTextField(
            value = nickname,
            onValueChange = {
                nickname = it
                nicknameChecked = false
            },
            label = { Text("닉네임") },
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { nicknameChecked = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("닉네임 중복확인")
        }

        if (nicknameChecked) {
            Text(
                "사용 가능한 닉네임 ✔",
                color = Color(0xFF4CAF50),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(20.dp))

        /* 비밀번호 */
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("비밀번호 (8자리 이상/영어/숫자/특수문자)") },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
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
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
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

        /* 회원가입 버튼 */
        Button(
            onClick = { onRegister(email, password, nickname) },
            modifier = Modifier.fillMaxWidth(),
            enabled =
                isEmailFormatValid &&
                        emailDuplicate == false &&
                        nicknameChecked &&
                        isPasswordValid &&
                        isPasswordMatch,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
        ) {
            Text("회원가입", color = Color.White)
        }

        Spacer(Modifier.height(24.dp))

        TextButton(onClick = onLogin) {
            Text("이미 회원이신가요? 로그인")
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
