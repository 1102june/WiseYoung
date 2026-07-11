package com.wiseyoung.pro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.wiseyoung.pro.ui.theme.ThemeWrapper
import com.google.firebase.auth.FirebaseAuth
import com.wiseyoung.pro.ui.components.BottomNavigationBar
import com.wiseyoung.pro.ads.BannerAd
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import com.wiseyoung.pro.util.NotificationPermissionHelper
import com.wiseyoung.pro.data.CalendarRepository

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
    var homeRefreshTrigger by remember { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* 허용/거부 모두 앱 사용 가능 — FCM은 권한 없으면 시스템 알림만 제한 */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationPermissionHelper.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    Scaffold(
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                BannerAd()
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
                    refreshTrigger = homeRefreshTrigger,
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
                    onNavigateCalendar = { navController.navigate("calendar") },
                    onNavigateBookmark = { navController.navigate("bookmark") },
                    onNavigateProfile = { navController.navigate("profile") },
                    onBack = {
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
                    showScaffold = false
                )
            }
            
            composable("bookmark") {
                BookmarkScreen(
                    userId = userId,
                    onNavigateHome = { navController.navigate("home") },
                    onNavigateCalendar = { navController.navigate("calendar") },
                    onNavigateProfile = { navController.navigate("profile") }
                )
            }
            
            composable("profile") {
                val context = androidx.compose.ui.platform.LocalContext.current
                ProfileScreen(
                    onNavigateHome = { navController.navigate("home") },
                    onNavigateCalendar = { navController.navigate("calendar") },
                    onNavigateBookmark = { navController.navigate("bookmark") },
                    onNavigateEditProfile = { /* 다이얼로그로 처리됨 */ },
                    onNavigateIntro = {
                        val intent = Intent(context, IntroActivity::class.java)
                        context.startActivity(intent)
                    },
                    onProfileUpdated = { homeRefreshTrigger++ }
                )
            }
        }
    }
}
