package com.wiseyoung.app

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.ui.text.style.TextAlign
import com.example.app.ui.theme.WiseYoungTheme

class LoginActivity : ComponentActivity() {
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
                    onLogin = { email, password -> /* Spring 서버로 로그인 요청 */ }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(onBack: () -> Unit, onRegister: () -> Unit, onLogin: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
        }

        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.wy_logo),
            contentDescription = "WY Logo",
            modifier = Modifier
                .size(100.dp)
                .padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("이메일 주소로 로그인 하실 수 있습니다", color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("이메일 주소를 입력해주세요") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("비밀번호를 입력해주세요") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onLogin(email, password) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("로그인", color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onRegister) {
            Text("아직 회원이 아니신가요? 회원가입", color = Color(0xFF8B5CF6))
        }
    }
}
