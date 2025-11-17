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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.app.ui.theme.WiseYoungTheme
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType

class LoginActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WiseYoungTheme {
                LoginScreen(
                    onBack = { finish() },
                    onRegister = {
                        startActivity(Intent(this, RegisterActivity::class.java))
                    },
                    onPasswordReset = { /* TODO */ },
                    onComplete = { email, password ->
                        loginUser(email, password)
                    },
                    onGoogleLogin = {
                        startActivity(Intent(this, AuthActivity::class.java))
                    },
                    onGoogleKeyLogin = {}
                )
            }
        }
    }

    private fun loginUser(email: String, password: String) {

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user ?: return@addOnSuccessListener

                if (!user.isEmailVerified) {
                    Toast.makeText(
                        this,
                        "이메일 인증 후 로그인 가능합니다.",
                        Toast.LENGTH_LONG
                    ).show()
                    auth.signOut()
                    return@addOnSuccessListener
                }

                user.getIdToken(true)
                    .addOnSuccessListener { token ->
                        sendIdTokenToServer(token.token ?: "")
                    }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "로그인 실패: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun sendIdTokenToServer(idToken: String) {
        val client = OkHttpClient()
        val json = """{"idToken": "$idToken"}"""
        val body = RequestBody.create("application/json".toMediaType(), json)

        val request = Request.Builder()
            .url("http://your_server_url/auth/login")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            runOnUiThread {
                if (response.isSuccessful) {
                    Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this,
                        "서버 오류: ${response.message}",
                        Toast.LENGTH_SHORT
                    ).show()
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
            label = { Text("이메일 주소") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = Color.Black)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("비밀번호") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = Color.Black)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = saveEmail, onCheckedChange = { saveEmail = it })
                Text("이메일 저장")
            }

            TextButton(onClick = onPasswordReset) {
                Text("비밀번호 찾기")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { onComplete(email, password) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
        ) {
            Text("로그인", color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onGoogleLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = "Google Logo",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("Google 로그인")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onGoogleKeyLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Google Passkey 로그인")
        }

        Spacer(modifier = Modifier.height(30.dp))

        TextButton(onClick = onRegister) {
            Text("아직 회원이 아니신가요? 회원가입")
        }
    }
}
