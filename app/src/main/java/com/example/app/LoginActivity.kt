package com.wiseyoung.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color


import com.example.app.ui.theme.WiseYoungTheme
import com.google.firebase.auth.FirebaseAuth
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.Response

class LoginActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WiseYoungTheme {
                LoginScreen(
                    onBack = { finish() },
                    onRegister = {
                        val intent = Intent(this, RegisterActivity::class.java)
                        startActivity(intent)
                    },
                    onPasswordReset = {
                        // 비밀번호 찾기 처리
                    },
                    onComplete = { email, password ->
                        loginUser(email, password)
                    },
                    onGoogleLogin = {
                        // 구글 로그인 처리
                    },
                    onGoogleKeyLogin = {
                        // 구글 키 로그인 처리
                    }
                )
            }
        }
    }

    // FirebaseAuth로 로그인
    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                user?.getIdToken(true)?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val idToken = task.result?.token
                        // 서버에 ID Token 전송
                        idToken?.let {
                            sendIdTokenToServer(it)
                        }
                    } else {
                        Toast.makeText(this, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "로그인 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Spring Boot 서버로 ID Token 전송
    private fun sendIdTokenToServer(idToken: String) {
        val client = OkHttpClient()
        val json = """{"idToken": "$idToken"}"""
        val body = RequestBody.create("application/json".toMediaType(), json)

        val request = Request.Builder()
            .url("http://your_server_url/auth/login")  // Spring Boot 서버 URL
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                // 로그인 성공 후 처리
                Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
                // MainActivity로 이동하거나 다른 작업 수행
            } else {
                Toast.makeText(this, "서버 오류: ${response.message}", Toast.LENGTH_SHORT).show()
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
        Image(
            painter = painterResource(id = R.drawable.wy_logo),
            contentDescription = "WY Logo",
            modifier = Modifier.size(100.dp)
        )

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onPasswordReset) {
                Text("비밀번호 찾기", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onComplete(email, password) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
        ) {
            Text("로그인", color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onGoogleLogin,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Text("Google Login")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onGoogleKeyLogin,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Text("Google Key Login")
        }

        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = onRegister) {
            Text("아직 회원이 아니신가요? 회원가입", style = MaterialTheme.typography.bodyMedium)
        }
    }
}