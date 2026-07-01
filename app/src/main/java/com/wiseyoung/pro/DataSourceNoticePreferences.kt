package com.wiseyoung.pro

import android.content.Context

object DataSourceNoticePreferences {
    private const val PREFS_NAME = "data_source_notice_prefs"
    private const val KEY_POLICY_HIDE_UNTIL_PREFIX = "policy_notice_hide_until_"
    private const val KEY_HOUSING_HIDE_UNTIL_PREFIX = "housing_notice_hide_until_"
    private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L

    fun shouldShowPolicyNotice(context: Context, userId: String): Boolean =
        shouldShow(context, policyKey(userId))

    fun shouldShowHousingNotice(context: Context, userId: String): Boolean =
        shouldShow(context, housingKey(userId))

    fun hidePolicyNoticeForOneDay(context: Context, userId: String) =
        hideForOneDay(context, policyKey(userId))

    fun hideHousingNoticeForOneDay(context: Context, userId: String) =
        hideForOneDay(context, housingKey(userId))

    private fun policyKey(userId: String) = KEY_POLICY_HIDE_UNTIL_PREFIX + userId

    private fun housingKey(userId: String) = KEY_HOUSING_HIDE_UNTIL_PREFIX + userId

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
