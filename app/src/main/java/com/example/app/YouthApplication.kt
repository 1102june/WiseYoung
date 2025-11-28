package com.wiseyoung.app

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk

class YouthApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Kakao Map SDK 초기화
        try {
            KakaoMapSdk.init(this, "a6d711e7786442c3aaf2b5596af9ae04")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

