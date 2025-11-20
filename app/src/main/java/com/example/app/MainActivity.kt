package com.wiseyoung.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.app.ui.theme.WiseYoungTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WiseYoungTheme {
                HomeScreen(
                    onNavigateNotifications = {
                        val intent = Intent(this, NotificationActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(android.R.anim.slide_in_right, android.R.anim.slide_out_left)
                    },
                    onNavigatePolicy = {
                        val intent = Intent(this, PolicyListActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(android.R.anim.slide_in_right, android.R.anim.slide_out_left)
                    },
                    onNavigateHousing = {
                        val intent = Intent(this, HousingMapActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(android.R.anim.slide_in_right, android.R.anim.slide_out_left)
                    },
                    onNavigateCalendar = {
                        val intent = Intent(this, CalendarActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(android.R.anim.slide_in_right, android.R.anim.slide_out_left)
                    },
                    onNavigateBookmark = {
                        val intent = Intent(this, BookmarkActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(android.R.anim.slide_in_right, android.R.anim.slide_out_left)
                    },
                    onNavigateProfile = {
                        val intent = Intent(this, ProfileActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(android.R.anim.slide_in_right, android.R.anim.slide_out_left)
                    },
                    onNavigateChatbot = {
                        // HomeScreen 내부에서 이미 챗봇 다이얼로그를 관리하고 있음
                        // BottomNavigationBar에서 호출 시 HomeScreen의 showChatbotDialog가 true로 설정됨
                    },
                    onBack = {}
                )
            }
        }
    }
}
