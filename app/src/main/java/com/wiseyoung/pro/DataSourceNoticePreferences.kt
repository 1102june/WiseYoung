package com.wiseyoung.pro

import android.content.Context

object DataSourceNoticePreferences {
    private const val PREFS_NAME = "data_source_notice_prefs"
    private const val KEY_POLICY_HIDE_UNTIL = "policy_notice_hide_until"
    private const val KEY_HOUSING_HIDE_UNTIL = "housing_notice_hide_until"
    private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L

    fun shouldShowPolicyNotice(context: Context): Boolean =
        shouldShow(context, KEY_POLICY_HIDE_UNTIL)

    fun shouldShowHousingNotice(context: Context): Boolean =
        shouldShow(context, KEY_HOUSING_HIDE_UNTIL)

    fun hidePolicyNoticeForOneDay(context: Context) =
        hideForOneDay(context, KEY_POLICY_HIDE_UNTIL)

    fun hideHousingNoticeForOneDay(context: Context) =
        hideForOneDay(context, KEY_HOUSING_HIDE_UNTIL)

    private fun shouldShow(context: Context, key: String): Boolean {
        val hideUntil = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(key, 0L)
        return System.currentTimeMillis() >= hideUntil
    }

    private fun hideForOneDay(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(key, System.currentTimeMillis() + ONE_DAY_MS)
            .apply()
    }
}
