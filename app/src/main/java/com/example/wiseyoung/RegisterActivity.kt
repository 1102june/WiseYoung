package com.example.wiseyoung

import android.os.Bundle
import android.util.Log
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
import com.example.wiseyoung.ui.theme.WiseYoungTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseUser

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WiseYoungTheme {
                RegisterScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onBack: () -> Unit) {
    var nickname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Firebase references
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

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

        // 닉네임 입력
        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("닉네임") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 이메일 입력
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("이메일") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 비밀번호 입력
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("비밀번호 (8자리 이상 + 특수문자 + 숫자)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 비밀번호 확인 입력
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("비밀번호 확인") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 가입 버튼
        Button(
            onClick = { registerUser(email, password, nickname, confirmPassword, auth, firestore) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("가입", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBack) {
            Text(
                text = "이미 회원이 아니신가요? 로그인",
                color = Color(0xFF8B5CF6)
            )
        }
    }
}

// 회원가입 처리 함수
fun registerUser(email: String, password: String, nickname: String, confirmPassword: String, auth: FirebaseAuth, firestore: FirebaseFirestore) {
    // 비밀번호 확인
    if (password != confirmPassword) {
        showError("비밀번호가 일치하지 않습니다.")
        return
    }

    // 비밀번호 유효성 검사
    if (!isPasswordValid(password)) {
        showError("비밀번호는 8자 이상, 숫자 및 특수문자를 포함해야 합니다.")
        return
    }

    // 닉네임 중복 확인
    checkNicknameAvailability(nickname) { isAvailable ->
        if (isAvailable) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val userId = user?.uid
                        sendEmailVerification(user)  // 이메일 인증 요청
                        saveUserDataToFirestore(userId, email, nickname, firestore)
                    } else {
                        showError(task.exception?.message ?: "회원가입 실패")
                    }
                }
        } else {
            showError("닉네임이 이미 존재합니다.")
        }
    }
}

// 비밀번호 유효성 검사
fun isPasswordValid(password: String): Boolean {
    return password.length >= 8 &&
            password.contains(Regex("[!@#\$%^&*(),.?\":{}|<>]")) &&  // 특수문자 포함
            password.contains(Regex("[0-9]")) && // 숫자 포함
            password.contains(Regex("[A-Za-z]")) // 영어 포함
}

// 닉네임 중복 확인
fun checkNicknameAvailability(nickname: String, callback: (Boolean) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val usersCollection = firestore.collection("users")
    usersCollection.whereEqualTo("nickname", nickname).get()
        .addOnSuccessListener { documents ->
            callback(documents.isEmpty)  // 중복되지 않으면 true
        }
        .addOnFailureListener {
            callback(false)
        }
}

// 이메일 인증 요청
fun sendEmailVerification(user: FirebaseUser?) {
    user?.sendEmailVerification()
        ?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("Firebase", "Verification email sent.")
            } else {
                Log.e("Firebase", "Error sending verification email", task.exception)
            }
        }
}

// Firestore에 사용자 데이터 저장
fun saveUserDataToFirestore(userId: String?, email: String, nickname: String, firestore: FirebaseFirestore) {
    val userMap = hashMapOf(
        "email" to email,
        "nickname" to nickname,
        "signup_date" to System.currentTimeMillis(), // 가입 시간 (Unix timestamp)
        "is_verified" to false // 이메일 인증 여부 (기본값: false)
    )

    if (userId != null) {
        firestore.collection("users").document(userId)
            .set(userMap)
            .addOnSuccessListener {
                Log.d("Firestore", "User data saved successfully.")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error saving user data", e)
            }
    }
}

// 에러 처리
fun showError(message: String) {
    // 여기에 에러 메시지를 화면에 표시하는 로직을 작성
    Log.e("Error", message)
    // 예: Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
