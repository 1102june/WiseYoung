package com.wiseyoung.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavOptionsBuilder
import com.example.app.ui.theme.ThemeWrapper
import com.wiseyoung.app.R
import com.example.app.FcmTokenService
import com.google.firebase.auth.FirebaseAuth
import com.example.app.ui.components.BottomNavigationBar
import com.example.app.data.CalendarRepository
import com.wiseyoung.app.CalendarScreen
import com.wiseyoung.app.HomeScreen
import com.wiseyoung.app.BookmarkScreen
import com.wiseyoung.app.ProfileScreen

class MainActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    
    // CalendarRepository 초기화
    private val calendarRepository by lazy { CalendarRepository(this) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // FCM 토큰 가져오기 및 서버에 저장
        FcmTokenService.getAndSaveToken()
        
        val userId = auth.currentUser?.uid ?: "test-user"
        
        setContent {
            ThemeWrapper {
                MainScreen(userId = userId, calendarRepository = calendarRepository)
            }
        }
    }
}

@Composable
fun MainScreen(userId: String, calendarRepository: CalendarRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
    
    var showChatbotDialog by remember { mutableStateOf(false) }
    
    // 챗봇 버튼 위치 상태 (드래그 가능)
    val density = LocalDensity.current
    var chatbotOffsetX by remember { mutableStateOf(0f) }
    var chatbotOffsetY by remember { mutableStateOf(0f) }
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = currentRoute,
                onNavigateHome = { 
                    navController.navigate("home") {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateCalendar = { 
                    navController.navigate("calendar") {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateBookmark = { 
                    navController.navigate("bookmark") {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateProfile = { 
                    navController.navigate("profile") {
                        popUpTo("home") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        floatingActionButton = {
            // 챗봇 버튼 (드래그 기능 포함)
            FloatingActionButton(
                onClick = { showChatbotDialog = true },
                containerColor = Color(0xFF59ABF7),
                contentColor = Color.White,
                modifier = Modifier
                    .offset(
                        x = with(density) { chatbotOffsetX.toDp() },
                        y = with(density) { chatbotOffsetY.toDp() }
                    )
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            chatbotOffsetX += dragAmount.x
                            chatbotOffsetY += dragAmount.y
                        }
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "챗봇",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable("home") {
                val context = androidx.compose.ui.platform.LocalContext.current
                HomeScreen(
                    userId = userId,
                    onNavigateNotifications = {
                        val intent = Intent(context, NotificationActivity::class.java)
                        context.startActivity(intent)
                    },
                    onNavigatePolicy = {
                        val intent = Intent(context, PolicyListActivity::class.java)
                        context.startActivity(intent)
                    },
                    onNavigateHousing = {
                        val intent = Intent(context, HousingMapActivity::class.java)
                        context.startActivity(intent)
                    },
                    // 내부 내비게이션 사용
                    onNavigateCalendar = { navController.navigate("calendar") },
                    onNavigateBookmark = { navController.navigate("bookmark") },
                    onNavigateProfile = { navController.navigate("profile") },
                    onNavigateChatbot = { showChatbotDialog = true },
                    onBack = {
                        // 홈에서 뒤로가기 시 앱 종료 또는 로그아웃 화면으로?
                        // LoginActivity로 이동
                        val intent = Intent(context, LoginActivity::class.java)
                        context.startActivity(intent)
                        (context as? android.app.Activity)?.finish()
                    }
                )
            }
            
            composable("calendar") {
                CalendarScreen(
                    repository = calendarRepository,
                    onNavigateHome = { navController.navigate("home") },
                    onNavigateBookmark = { navController.navigate("bookmark") },
                    onNavigateProfile = { navController.navigate("profile") },
                    onNavigateChatbot = { showChatbotDialog = true }
                )
            }
            
            composable("bookmark") {
                BookmarkScreen(
                    userId = userId,
                    onNavigateHome = { navController.navigate("home") },
                    onNavigateCalendar = { navController.navigate("calendar") },
                    onNavigateProfile = { navController.navigate("profile") },
                    onNavigateChatbot = { showChatbotDialog = true }
                )
            }
            
            composable("profile") {
                val context = androidx.compose.ui.platform.LocalContext.current
                ProfileScreen(
                    onNavigateHome = { navController.navigate("home") },
                    onNavigateCalendar = { navController.navigate("calendar") },
                    onNavigateBookmark = { navController.navigate("bookmark") },
                    onNavigateEditProfile = { /* 다이얼로그로 처리됨 */ },
                    onNavigateChatbot = { showChatbotDialog = true },
                    onNavigateIntro = {
                        val intent = Intent(context, IntroActivity::class.java)
                        context.startActivity(intent)
                    },
                    onThemeModeChange = { mode ->
                        com.example.app.ui.theme.ThemePreferences.setThemeMode(context, mode)
                    }
                )
            }
        }
        
        // 챗봇 다이얼로그 (전역)
        ChatbotDialog(
            isOpen = showChatbotDialog,
            onClose = { showChatbotDialog = false },
            context = ChatbotContext.NONE // 필요시 컨텍스트 전달 가능
        )
    }
}
