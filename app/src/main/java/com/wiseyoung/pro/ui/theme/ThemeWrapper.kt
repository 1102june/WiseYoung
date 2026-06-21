package com.wiseyoung.pro.ui.theme

import androidx.compose.runtime.Composable

/**
 * 저장된 테마 모드를 자동으로 불러와서 적용하는 래퍼
 * 모든 Activity에서 이 래퍼를 사용하면 저장된 테마 모드가 자동으로 적용됩니다.
 */
@Composable
fun ThemeWrapper(
    content: @Composable () -> Unit
) {
    WiseYoungTheme(themeMode = ThemeMode.LIGHT) {
        content()
    }
}
