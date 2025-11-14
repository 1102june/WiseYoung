package com.wiseyoung.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.app.ui.theme.WiseYoungTheme
import com.google.firebase.auth.FirebaseAuth
import android.os.Build
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.Response

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

                // POST로 Spring 서버에 데이터 보내기
                sendDataToSpring(uid, email, password, nickname)

            }
            .addOnFailureListener {
                Toast.makeText(this, "회원가입 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Spring 서버로 데이터 전송
    private fun sendDataToSpring(uid: String, email: String, password: String, nickname: String) {
        // POST 요청을 보내는 코드 (OkHttp 사용)
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
        """

        val body = RequestBody.create("application/json".toMediaType(), json)

        val request = Request.Builder()
            .url("http://your_server_url/auth/register")  // Spring Boot 서버 URL
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show()
                finish() // 회원가입 완료 후 화면 종료
            } else {
                Toast.makeText(this, "서버 오류: ${response.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun RegisterScreen(onBack: () -> Unit, onRegister: (String, String, String) -> Unit) {
    var nickname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text("회원가입", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("닉네임") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("이메일") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("비밀번호") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("비밀번호 확인") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (password == confirmPassword && email.isNotBlank()) {
                    onRegister(email, password, nickname)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("가입", color = Color.White)
        }
    }
}
