package com.wiseyoung.pro.ads

/**
 * AdMob ID 정리
 *
 * - **앱 ID** (`~` 포함): AndroidManifest.xml `com.google.android.gms.ads.APPLICATION_ID` 에만 넣습니다.
 * - **광고 단위 ID** (`/` 포함): Kotlin 코드(배너 AdView, 전면 InterstitialAd)에서 사용합니다.
 *
 * 배너: ca-app-pub-7571119826535050/7618994934
 * 전면: AdMob 콘솔 > 앱 > 광고 단위 > 전면 에서 생성한 ID를 INTERSTITIAL_RELEASE 에 넣으세요.
 */
object AdIds {
    const val APP_ID = "ca-app-pub-7571119826535050~3444856335"

    const val BANNER_RELEASE = "ca-app-pub-7571119826535050/7618994934"

    const val INTERSTITIAL_RELEASE = "ca-app-pub-7571119826535050/2153091430"

    const val BANNER_TEST = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_TEST = "ca-app-pub-3940256099942544/1033173712"

    fun bannerAdUnitId(isDebug: Boolean): String =
        if (isDebug) BANNER_TEST else BANNER_RELEASE

    fun interstitialAdUnitId(isDebug: Boolean): String =
        if (isDebug) INTERSTITIAL_TEST else INTERSTITIAL_RELEASE
}
