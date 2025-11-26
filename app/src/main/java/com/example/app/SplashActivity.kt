package com.wiseyoung.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.wiseyoung.app.ProfilePreferences

class SplashActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                SplashScreen(
                    onNext = {
                        navigateToNextScreen()
                    }
                )
            }
        }

        // 1초 후 자동 이동
        lifecycleScope.launch {
            delay(1000)
            navigateToNextScreen()
        }
    }

    /**
     * 다음 화면으로 네비게이션
     * 로그인 상태와 프로필 완료 여부를 모두 확인
     */
    private fun navigateToNextScreen() {
        val currentUser = auth.currentUser
        val hasCompletedProfile = ProfilePreferences.hasCompletedProfile(this)
        val isFirstLogin = ProfilePreferences.isFirstLogin(this)

        val nextActivity = when {
            // 로그인되어 있고 프로필도 완료된 경우 -> MainActivity
            currentUser != null && hasCompletedProfile -> {
                MainActivity::class.java
            }
            // 로그인되어 있지만 프로필이 미완료인 경우 -> ProfileSetupActivity
            currentUser != null && !hasCompletedProfile -> {
                ProfileSetupActivity::class.java
            }
            // 로그인되지 않은 경우 -> 첫 로그인이면 WelcomeActivity, 아니면 AuthActivity
            else -> {
                if (isFirstLogin) {
                    WelcomeActivity::class.java
                } else {
                    AuthActivity::class.java
                }
            }
        }

        val intent = Intent(this, nextActivity)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}

@Composable
fun SplashScreen(onNext: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF79659))  // #f79659 배경색
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // WY 로고
            Image(
                painter = painterResource(id = R.drawable.wy_logo),
                contentDescription = "WY Logo",
                modifier = Modifier.size(160.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 텍스트
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "슬기로운 청년생활",
                    color = Color(0xFF1A1A1A),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Find your way with Wise & Young",
                    color = Color(0xFF1A1A1A).copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
