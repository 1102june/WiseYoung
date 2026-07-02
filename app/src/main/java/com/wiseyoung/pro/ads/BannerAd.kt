package com.wiseyoung.pro.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.wiseyoung.pro.BuildConfig

@Composable
fun BannerAd(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val adUnitId = remember { AdIds.bannerAdUnitId(BuildConfig.DEBUG) }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            adView.adUnitId = adUnitId
        }
    )

    DisposableEffect(Unit) {
        onDispose { }
    }
}
