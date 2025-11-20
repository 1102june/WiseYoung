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
import com.example.app.ui.theme.WiseYoungTheme
import com.example.app.Config
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

        // Google Sign-In 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            WiseYoungTheme {
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
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            Toast.makeText(this, "Google 로그인 실패: ${e.message}", Toast.LENGTH_SHORT).show()
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
            .url(Config.getUrl(Config.Api.LOGIN))
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "서버 연결 실패: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        // 이메일 로그인 성공 후 프로필 완료 여부 확인하여 네비게이션
                        navigateAfterLogin(isGoogleLogin = false)
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "서버 오류: ${response.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    /**
     * 로그인 성공 후 네비게이션 처리
     * 이메일 로그인: 프로필 완료 여부에 따라 CompleteActivity 또는 ProfileSetupActivity로 이동
     * Google 로그인: 항상 ProfileSetupActivity로 이동 (최초 로그인 시 프로필 입력 필요)
     */
    private fun navigateAfterLogin(isGoogleLogin: Boolean = false) {
        val nextActivity = if (isGoogleLogin || !ProfilePreferences.hasCompletedProfile(this)) {
            // Google 로그인이거나 프로필이 미완료인 경우 프로필 입력 화면으로
            ProfileSetupActivity::class.java
        } else {
            // 프로필이 완료된 경우 CompleteActivity로
            CompleteActivity::class.java
        }
        val intent = Intent(this, nextActivity)
        if (isGoogleLogin) {
            // Google 로그인에서 온 경우 플래그 전달
            intent.putExtra("from_google_login", true)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_right, android.R.anim.slide_out_left)
        finish()
    }

    /**
     * Google 로그인 시작
     */
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    /**
     * Google 계정으로 Firebase 인증
     */
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user ?: return@addOnSuccessListener
                
                // 서버에 ID Token 전송
                user.getIdToken(true)
                    .addOnSuccessListener { token ->
                        // Google 로그인 성공 시 서버에 전송 후 프로필 입력 화면으로 이동
                        sendIdTokenToServerForGoogleLogin(token.token ?: "")
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "토큰 발급 실패: ${it.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Google 로그인 실패: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    /**
     * Google 로그인용 서버 전송 (프로필 입력 화면으로 이동)
     */
    private fun sendIdTokenToServerForGoogleLogin(idToken: String) {
        val client = OkHttpClient()
        val json = """{"idToken": "$idToken"}"""
        val body = RequestBody.create("application/json".toMediaType(), json)

        val request = Request.Builder()
            .url(Config.getUrl(Config.Api.LOGIN))
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    // 서버 연결 실패해도 Google 로그인은 성공했으므로 프로필 입력 화면으로 이동
                    navigateAfterLogin(isGoogleLogin = true)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    // Google 로그인 성공 시 항상 프로필 입력 화면으로 이동
                    navigateAfterLogin(isGoogleLogin = true)
                }
            }
        })
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.Center
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
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // 로그인 메시지
        Text(
            text = "이메일 주소로 로그인 하실 수 있습니다",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            textAlign = TextAlign.Center,
            color = Color(0xFF666666),
            fontSize = 14.sp
        )

        // 폼
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 이메일
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "이메일",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1A1A1A)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("이메일 주소를 입력해주세요", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
            }

            // 비밀번호
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "비밀번호",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1A1A1A)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("비밀번호를 입력해주세요", color = Color.Gray) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
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
                    containerColor = Color(0xFF8B5CF6)
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
                        color = Color(0xFF8B5CF6)
                    )
                }
            }
        }
    }
}
