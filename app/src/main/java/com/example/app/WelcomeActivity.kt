package com.wiseyoung.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.ui.theme.WiseYoungTheme
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WiseYoungTheme {
                WelcomeScreen(
                    onNext = {
                        // 첫 로그인 플래그를 false로 설정 (온보딩을 한 번만 보여줌)
                        ProfilePreferences.setFirstLogin(this, false)
                        
                        // 로그인 상태 확인
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        val hasCompletedProfile = ProfilePreferences.hasCompletedProfile(this)
                        
                        val nextActivity = when {
                            // 로그인되어 있고 프로필 완료 -> MainActivity
                            currentUser != null && hasCompletedProfile -> MainActivity::class.java
                            // 로그인되어 있고 프로필 미완료 -> ProfileSetupActivity
                            currentUser != null && !hasCompletedProfile -> ProfileSetupActivity::class.java
                            // 미로그인 -> AuthActivity (로그인 화면)
                            else -> AuthActivity::class.java
                        }
                        
                        val intent = Intent(this, nextActivity)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun WelcomeScreen(onNext: () -> Unit) {
    var dragOffset by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)  // 검은색 -> 흰색 배경
            .padding(horizontal = 32.dp, vertical = 64.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        // 오른쪽으로 스와이프 (왼쪽에서 오른쪽으로 드래그)
                        if (dragOffset < -100) {
                            onNext()
                        }
                        dragOffset = 0f
                    }
                ) { change, dragAmount ->
                    dragOffset += dragAmount
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 상단 섹션 (중앙 정렬)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // WY 로고 - 크게 표시 (원형 박스 제거)
            Image(
                painter = painterResource(id = R.drawable.wy_logo),
                contentDescription = "WY Logo",
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 메시지 - 검은색 텍스트
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "슬기로운 청년생활에\n오신것을 환영합니다!",
                    color = Color(0xFF1A1A1A),  // 검은색 텍스트
                    fontSize = 20.sp,  // 18.sp -> 20.sp로 조금 더 크게
                    fontWeight = FontWeight.Bold,  // Normal -> Bold
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )

                Text(
                    text = "청년의 내일을 슬기롭게",
                    color = Color(0xFF1A1A1A),  // 666666 -> 1A1A1A (더 진한 검은색)
                    fontSize = 16.sp,  // 14.sp -> 16.sp로 조금 더 크게
                    textAlign = TextAlign.Center
                )
            }
        }

        // 하단 버튼
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF59ABF7) // 라이트 블루 (메인 컬러)
            )
        ) {
            Text(
                text = "다음",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}
