package com.wiseyoung.app

import android.content.Context

object ProfilePreferences {
    private const val PREFS_NAME = "profile_prefs"
    private const val KEY_PROFILE_COMPLETE = "profile_complete"

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
}