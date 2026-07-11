package com.wiseyoung.pro.ads

/**
 * AdMob 광고 단위 ID.
 *
 * `USE_QA_TEST_ADS = true` → debug/release 모두 Google 공식 테스트 ID (가족 QA·내부 테스트용).
 * Play 스토어 출시 전 `USE_QA_TEST_ADS = false` 로 바꾸면 RELEASE에 운영 ID가 적용됩니다.
 */
object AdIds {

    /** true: 릴리즈 APK에도 테스트 광고 표시 (가족 QA·내부 테스트용) */
    const val USE_QA_TEST_ADS = false

    // Google AdMob 공식 테스트 ID (https://developers.google.com/admob/android/test-ads)
    const val APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
    const val BANNER = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
    const val NATIVE = "ca-app-pub-3940256099942544/2247696110"

    // 운영(실제) ID — USE_QA_TEST_ADS = false 일 때 release 빌드에 사용
    const val APP_ID_PROD = "ca-app-pub-7571119826535050~3444856335"
    const val BANNER_PROD = "ca-app-pub-7571119826535050/7618994934"
    const val INTERSTITIAL_PROD = "ca-app-pub-7571119826535050/2153091430"

    fun bannerAdUnitId(isDebug: Boolean): String =
        if (USE_QA_TEST_ADS || isDebug) BANNER else BANNER_PROD

    fun interstitialAdUnitId(isDebug: Boolean): String =
        if (USE_QA_TEST_ADS || isDebug) INTERSTITIAL else INTERSTITIAL_PROD

    fun appOpenAdUnitId(isDebug: Boolean): String = APP_OPEN

    fun rewardedAdUnitId(isDebug: Boolean): String = REWARDED

    fun nativeAdUnitId(isDebug: Boolean): String = NATIVE
}
