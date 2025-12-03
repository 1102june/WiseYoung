package com.wiseyoung.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.app.ui.theme.ThemeWrapper
import com.wiseyoung.app.R
import com.example.app.FcmTokenService
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // FCM 토큰 가져오기 및 서버에 저장
        FcmTokenService.getAndSaveToken()
        
        val userId = auth.currentUser?.uid
        
        setContent {
            ThemeWrapper {
                HomeScreen(
                    userId = userId,
                    onNavigateNotifications = {
                        val intent = Intent(this, NotificationActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    },
                    onNavigatePolicy = {
                        val intent = Intent(this, PolicyListActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    },
                    onNavigateHousing = {
                        val intent = Intent(this, HousingMapActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    },
                    onNavigateCalendar = {
                        val intent = Intent(this, CalendarActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    },
                    onNavigateBookmark = {
                        val intent = Intent(this, BookmarkActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    },
                    onNavigateProfile = {
                        val intent = Intent(this, ProfileActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    },
                    onNavigateChatbot = {
                        // FloatingActionButton을 통해 챗봇 다이얼로그가 열림
                        // HomeScreen 내부에서 handleChatbotClick으로 처리됨
                    },
                    onBack = {
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}
