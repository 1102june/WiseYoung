package com.wiseyoung.app

import android.content.Context

object ProfilePreferences {
    private const val PREFS_NAME = "profile_prefs"
    private const val KEY_PROFILE_COMPLETE = "profile_complete"
    private const val KEY_IS_FIRST_LOGIN = "is_first_login"

    fun hasCompletedProfile(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PROFILE_COMPLETE, false)
    }

    fun setProfileCompleted(context: Context, completed: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PROFILE_COMPLETE, completed)
            .apply()
    }
    
    fun isFirstLogin(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_FIRST_LOGIN, true)
    }
    
    fun setFirstLogin(context: Context, isFirst: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_FIRST_LOGIN, isFirst)
            .apply()
    }
}