package com.wiseyoung.app

import android.os.Build
import android.os.Bundle
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
import androidx.compose.ui.unit.dp
import com.example.app.ui.theme.WiseYoungTheme
import com.google.firebase.auth.FirebaseAuth
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

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

    // Firebase Auth로 회원가입
    private fun registerUser(email: String, password: String, nickname: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                val userData = hashMapOf(
                    "user_id" to uid,
                    "email" to email,
                    "password_hash" to password,  // 실제 배포 시: 해시 or Spring으로 넘겨 저장
                    "login_type" to "local",
                    "os_type" to "android",
                    "app_version" to "1.0.0",
                    "push_token" to "",
                    "device_id" to Build.ID,
                    "created_at" to System.currentTimeMillis(),
                    "nickname" to nickname
                )

                // TODO: Firestore 저장 로직이 있으면 여기서 추가

                // Spring 서버로 전송
                sendDataToSpring(uid, email, password, nickname)

            }
            .addOnFailureListener {
                Toast.makeText(this, "회원가입 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Spring 서버로 데이터 전송
    private fun sendDataToSpring(uid: String, email: String, password: String, nickname: String) {
        val client = OkHttpClient()

        val json = """
            {
                "user_id": "$uid",
                "email": "$email",
                "password_hash": "$password",
                "login_type": "local",
                "os_type": "android",
                "app_version": "1.0.0",
                "push_token": "",
                "device_id": "${Build.ID}",
                "created_at": "${System.currentTimeMillis()}",
                "nickname": "$nickname"
            }
        """.trimIndent()

        val body = RequestBody.create("application/json".toMediaType(), json)

        val request = Request.Builder()
            .url("http://your_server_url/auth/register")  // Spring Boot 서버 URL
            .post(body)
            .build()

        // ⚠️ 이 부분은 실제론 코루틴/백그라운드 스레드에서 돌리는 게 좋음
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                runOnUiThread {
                    Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "서버 오류: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegister: (String, String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("이메일 주소를 입력해주세요") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("비밀번호를 입력해주세요") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("닉네임을 입력해주세요") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onRegister(email, password, nickname) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
        ) {
            Text("회원가입", color = Color.White)
        }
    }
}
