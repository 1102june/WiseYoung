package com.example.app.ui.theme

/**
 * 앱 테마 모드
 */
enum class ThemeMode(val value: String) {
    LIGHT("light"),
    DARK("dark"),
    DARK_BLUE("dark_blue");

    companion object {
        fun fromString(value: String?): ThemeMode {
            return when (value) {
                "light" -> LIGHT
                "dark" -> DARK
                "dark_blue" -> DARK_BLUE
                else -> LIGHT  // 기본값
            }
        }
    }
}

