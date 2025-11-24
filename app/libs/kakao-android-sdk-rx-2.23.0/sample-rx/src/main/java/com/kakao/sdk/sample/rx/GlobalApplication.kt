package com.kakao.sdk.sample.rx

import android.app.Application
import com.kakao.sdk.common.RxKakaoSdk

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        RxKakaoSdk.init(this, "e19a463823bdbefb87c2c66c3fb6ab59", loggingEnabled = true)
    }
}
