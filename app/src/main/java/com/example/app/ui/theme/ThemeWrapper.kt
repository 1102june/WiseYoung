package com.example.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * 저장된 테마 모드를 자동으로 불러와서 적용하는 래퍼
 * 모든 Activity에서 이 래퍼를 사용하면 저장된 테마 모드가 자동으로 적용됩니다.
 */
@Composable
fun ThemeWrapper(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // Activity가 재시작될 때마다 최신 테마를 불러오도록 remember로 관리
    var themeMode by remember { mutableStateOf(ThemePreferences.getThemeMode(context)) }
    
    // 테마 모드 변경을 감지하여 다시 불러오기
    androidx.compose.runtime.DisposableEffect(Unit) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val listener = object : android.content.SharedPreferences.OnSharedPreferenceChangeListener {
            override fun onSharedPreferenceChanged(
                sharedPreferences: android.content.SharedPreferences?,
                key: String?
            ) {
                if (key == ThemePreferences.KEY_THEME_MODE) {
                    // 메인 스레드에서 상태 업데이트 (SharedPreferences 리스너는 메인 스레드에서 실행되지만 안전을 위해 Handler 사용)
                    handler.post {
                        themeMode = ThemePreferences.getThemeMode(context)
                    }
                }
            }
        }
        val prefs = context.getSharedPreferences(ThemePreferences.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            handler.removeCallbacksAndMessages(null)
        }
    }
    
    // Activity가 재시작될 때 최신 테마를 다시 불러오기
    androidx.compose.runtime.LaunchedEffect(Unit) {
        themeMode = ThemePreferences.getThemeMode(context)
    }
    
    WiseYoungTheme(themeMode = themeMode) {
        content()
    }
}

