package com.wiseyoung.pro.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.wiseyoung.pro.BuildConfig

/**
 * 전면 광고 — 자연스러운 이탈 시점(정책/임대주택 화면에서 홈으로 돌아갈 때)에만 표시.
 * 최소 3분 간격, 로딩 실패 시 바로 화면 전환.
 */
object InterstitialAdManager {

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var lastShownAtMs = 0L

    private const val MIN_INTERVAL_MS = 180_000L

    fun preload(context: Context) {
        val adUnitId = AdIds.interstitialAdUnitId(BuildConfig.DEBUG)
        if (adUnitId.isBlank()) return
        if (isLoading || interstitialAd != null) return

        isLoading = true
        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    android.util.Log.w("InterstitialAd", "로드 실패: ${error.message}")
                }
            }
        )
    }

    fun tryShow(activity: Activity, onFinished: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastShownAtMs < MIN_INTERVAL_MS) {
            onFinished()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            preload(activity.applicationContext)
            onFinished()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                lastShownAtMs = System.currentTimeMillis()
                preload(activity.applicationContext)
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                preload(activity.applicationContext)
                onFinished()
            }
        }

        interstitialAd = null
        ad.show(activity)
    }
}
