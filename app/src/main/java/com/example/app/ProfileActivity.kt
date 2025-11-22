package com.wiseyoung.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.ui.theme.AppColors
import com.example.app.ui.theme.Spacing
import com.example.app.ui.theme.ThemeWrapper
import com.example.app.ui.theme.ThemeMode
import com.example.app.ui.theme.ThemePreferences
import com.example.app.ui.components.BottomNavigationBar
import androidx.compose.ui.platform.LocalContext

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeWrapper {
                ProfileScreen(
                    onNavigateHome = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onNavigateCalendar = {
                        startActivity(Intent(this, CalendarActivity::class.java))
                        finish()
                    },
                    onNavigateBookmark = {
                        startActivity(Intent(this, BookmarkActivity::class.java))
                        finish()
                    },
                    onNavigateEditProfile = {
                        // TODO: 프로필 편집 화면으로 이동
                    },
                    onNavigateChatbot = {
                        // TODO: 챗봇 다이얼로그 표시
                    },
                    onThemeModeChange = { mode ->
                        // ThemePreferences에 저장 (ThemeWrapper가 자동으로 감지하여 즉시 적용)
                        ThemePreferences.setThemeMode(this, mode)
                        // recreate() 제거 - ThemeWrapper의 DisposableEffect 리스너가 자동으로 테마 변경 감지
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileScreen(
    onNavigateHome: () -> Unit,
    onNavigateCalendar: () -> Unit,
    onNavigateBookmark: () -> Unit,
    onNavigateEditProfile: () -> Unit,
    onNavigateChatbot: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    var themeMode by remember { mutableStateOf(ThemePreferences.getThemeMode(context)) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    
    // ThemePreferences 변경 감지하여 themeMode 상태 업데이트 (ThemeWrapper와 동기화)
    androidx.compose.runtime.DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == ThemePreferences.KEY_THEME_MODE) {
                themeMode = ThemePreferences.getThemeMode(context)
            }
        }
        val prefs = context.getSharedPreferences(ThemePreferences.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = "profile",
                onNavigateHome = onNavigateHome,
                onNavigateCalendar = onNavigateCalendar,
                onNavigateChatbot = onNavigateChatbot,
                onNavigateBookmark = onNavigateBookmark,
                onNavigateProfile = {}
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            ProfileHeader()
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // User Info Card
                UserInfoCard(
                    modifier = Modifier.padding(bottom = Spacing.md)
                )
                
                // Edit Profile Button (크기 줄임)
                Button(
                    onClick = onNavigateEditProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),  // 높이 명시적으로 줄임
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),  // 모서리도 약간 줄임
                    contentPadding = PaddingValues(vertical = 12.dp)  // 패딩 줄임
                ) {
                    Text(
                        text = "내정보 변경하기",
                        fontSize = 15.sp,  // 폰트 크기 약간 줄임
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
                
                // Settings Section
                ThemeSettingCard(
                    themeMode = themeMode,
                    onThemeModeChange = { mode ->
                        themeMode = mode
                        ThemePreferences.setThemeMode(context, mode)
                        onThemeModeChange(mode)  // 부모에게 알림 (테마 즉시 적용)
                    },
                    modifier = Modifier.padding(top = Spacing.md)
                )
                
                // Logout and Delete Account
                Column(
                    modifier = Modifier.padding(top = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Button(
                        onClick = {
                            // TODO: 로그아웃 로직
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),  // 높이 명시적으로 줄임
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.BackgroundGradientStart
                        ),
                        shape = RoundedCornerShape(12.dp),  // 모서리도 약간 줄임
                        contentPadding = PaddingValues(vertical = 12.dp)  // 패딩 줄임
                    ) {
                        Text(
                            text = "로그아웃",
                            fontSize = 15.sp,  // 폰트 크기 약간 줄임
                            fontWeight = FontWeight.Medium,
                            color = Color.White  // 텍스트 색상 흰색으로 변경 (버튼 배경에 맞춤)
                        )
                    }
                    
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "회원탈퇴",
                            fontSize = 14.sp,
                            color = AppColors.TextTertiary
                        )
                    }
                }
            }
        }
    }
    
    // Delete Account Dialog - Password Input
    if (showDeleteDialog) {
        DeletePasswordDialog(
            password = deletePassword,
            onPasswordChange = { deletePassword = it },
            onConfirm = {
                showDeleteDialog = false
                showDeleteConfirm = true
            },
            onDismiss = {
                deletePassword = ""
                showDeleteDialog = false
            }
        )
    }
    
    // Delete Account Confirmation
    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            onConfirm = {
                // TODO: 실제 탈퇴 로직 실행
                showDeleteConfirm = false
                deletePassword = ""
            },
            onDismiss = {
                showDeleteConfirm = false
                deletePassword = ""
            }
        )
    }
}

@Composable
private fun ProfileHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "내정보",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
        }
    }
}

@Composable
private fun UserInfoCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, AppColors.Border),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Image
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(AppColors.Border),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                // User Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "슬기로운 청년 님",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "25세",
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = "경기도 수원시 거주",
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = "재학중",
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary
                    )
                }
            }
            
            // Interest Tags
            Divider(
                modifier = Modifier.padding(vertical = Spacing.md),
                color = AppColors.Border
            )
            
            Text(
                text = "관심선택 목록",
                fontSize = 14.sp,
                color = AppColors.TextTertiary,
                modifier = Modifier.padding(bottom = Spacing.sm)
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                InterestTag("취업", AppColors.Purple.copy(alpha = 0.1f), AppColors.Purple)
                InterestTag("복지", AppColors.BackgroundGradientStart.copy(alpha = 0.1f), AppColors.BackgroundGradientStart)
                InterestTag("주거", AppColors.Info.copy(alpha = 0.1f), AppColors.Info)
            }
        }
    }
}

@Composable
private fun InterestTag(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(horizontal = Spacing.sm, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ThemeSettingCard(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Border),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(AppColors.TextPrimary)
                    )
                }
                Text(
                    text = "테마",
                    fontSize = 16.sp,
                    color = AppColors.TextPrimary
                )
            }
            
            // Theme Selection (3개 버튼)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AppColors.Border
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ThemeToggleButton(
                        text = "라이트",
                        isSelected = themeMode == ThemeMode.LIGHT,
                        onClick = { onThemeModeChange(ThemeMode.LIGHT) }
                    )
                    ThemeToggleButton(
                        text = "다크",
                        isSelected = themeMode == ThemeMode.DARK,
                        onClick = { onThemeModeChange(ThemeMode.DARK) }
                    )
                    ThemeToggleButton(
                        text = "다크블루",
                        isSelected = themeMode == ThemeMode.DARK_BLUE,
                        onClick = { onThemeModeChange(ThemeMode.DARK_BLUE) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeToggleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color.White else Color.Transparent,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (isSelected) AppColors.TextPrimary else AppColors.TextSecondary,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = 4.dp),
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun DeletePasswordDialog(
    password: String,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("회원탈퇴") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(
                    text = "본인 확인을 위해 비밀번호를 입력해주세요.",
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary
                )
                
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("비밀번호") },
                    placeholder = { Text("비밀번호를 입력하세요") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = password.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626)
                )
            ) {
                Text("확인", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("정말 탈퇴하시겠습니까?") },
        text = {
            Text(
                text = "회원 탈퇴 시 모든 정보가 소실되며 복구할 수 없습니다.\n정말 탈퇴하시겠습니까?",
                fontSize = 14.sp,
                color = AppColors.TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626)
                )
            ) {
                Text("탈퇴하기", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("뒤로가기")
            }
        }
    )
}

