package com.dhinova.milkmonthlyexpensecalendar.ui.components

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                // Test Ad Unit ID: ca-app-pub-3940256099942544/6300978111
                // Real Ad Unit ID: ca-app-pub-2795699587731133/9022074273
                adUnitId = "ca-app-pub-2795699587731133/9022074273" 
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

fun showInterstitial(context: Context, onAdClosed: () -> Unit) {
    val adRequest = AdRequest.Builder().build()
    // Test Ad Unit ID: ca-app-pub-3940256099942544/1033173712
    // Real Ad Unit ID: ca-app-pub-2795699587731133/7708992600
    InterstitialAd.load(
        context,
        "ca-app-pub-2795699587731133/7708992600",
        adRequest,
        object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                onAdClosed()
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        onAdClosed()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                        onAdClosed()
                    }
                }
                if (context is Activity) {
                    interstitialAd.show(context)
                } else {
                    onAdClosed()
                }
            }
        }
    )
}
