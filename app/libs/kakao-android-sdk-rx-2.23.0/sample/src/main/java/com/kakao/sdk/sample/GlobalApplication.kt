package com.kakao.sdk.sample

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        KakaoSdk.init(this, "e19a463823bdbefb87c2c66c3fb6ab59", loggingEnabled = true)
    }
}
