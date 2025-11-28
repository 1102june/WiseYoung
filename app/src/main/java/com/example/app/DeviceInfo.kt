package com.example.app

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * 디바이스 정보를 가져오는 유틸리티 클래스
 */
object DeviceInfo {
    /**
     * 앱 버전 정보 가져오기 (예: "1.0")
     */
    fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0"
        }
    }

    /**
     * 디바이스 ID 가져오기 (ANDROID_ID)
     */
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"
    }
}












