package com.example.app.ui.theme

import android.content.Context

/**
 * 테마 모드 저장 및 불러오기
 */
object ThemePreferences {
    internal const val PREFS_NAME = "theme_prefs"  // ThemeWrapper에서 접근 가능하도록 internal로 변경
    internal const val KEY_THEME_MODE = "theme_mode"  // ThemeWrapper에서 접근 가능하도록 internal로 변경

    /**
     * 저장된 테마 모드 불러오기
     * 기본값: LIGHT
     */
    fun getThemeMode(context: Context): ThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getString(KEY_THEME_MODE, ThemeMode.LIGHT.value)
        return ThemeMode.fromString(value)
    }

    /**
     * 테마 모드 저장
     * commit()을 사용하여 동기적으로 저장 (즉시 반영)
     */
    fun setThemeMode(context: Context, mode: ThemeMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_THEME_MODE, mode.value)
            .commit()  // apply() 대신 commit() 사용하여 동기적으로 저장
    }
}

