package com.wiseyoung.pro

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk

import com.google.android.gms.ads.MobileAds
import com.wiseyoung.pro.ads.InterstitialAdManager

class YouthApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
        InterstitialAdManager.preload(this)
        // Kakao Map SDK 초기화
        try {
            KakaoMapSdk.init(this, "a6d711e7786442c3aaf2b5596af9ae04")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

