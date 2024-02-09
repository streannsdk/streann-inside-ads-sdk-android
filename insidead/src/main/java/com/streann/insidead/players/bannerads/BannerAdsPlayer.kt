package com.streann.insidead.players.bannerads

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.streann.insidead.InsideAdSdk
import com.streann.insidead.R
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.callbacks.InsideAdProgressCallback
import com.streann.insidead.models.InsideAd
import com.streann.insidead.utils.Helper

@SuppressLint("ViewConstructor")
class BannerAdsPlayer constructor(
    context: Context,
    callback: InsideAdProgressCallback
) : FrameLayout(context) {

    private val LOGTAG = "InsideAdSdk"

    private var adView: AdManagerAdView? = null
    private var closeBannerAdHandler: Handler? = null

    private var insideAdCallback: InsideAdCallback? = null
    private var insideAdProgressCallback: InsideAdProgressCallback? = callback

    init {
        init()
    }

    private fun init() {
        LayoutInflater.from(context).inflate(R.layout.banner_ad_player, this)
        MobileAds.initialize(context) { }
    }

    fun playAd(insideAd: InsideAd, callback: InsideAdCallback) {
        insideAdCallback = callback
        closeBannerAdHandler = Handler(Looper.getMainLooper())

        adView = AdManagerAdView(context)
        adView?.adUnitId = insideAd.url ?: ""
        addView(adView)

        insideAd.properties?.sizes?.let { sizesArray ->
            val adSizes = arrayListOf<AdSize>()
            for (sdkAdSize in sizesArray) {
                adSizes.add(AdSize(sdkAdSize.width!!, sdkAdSize.height!!))
            }
            if (adSizes.isNotEmpty())
                adView?.setAdSizes(*adSizes.toTypedArray())
            else adView?.setAdSizes(AdSize.BANNER)
        } ?: run {
            adView?.setAdSizes(AdSize.BANNER)
        }

        adView?.adSize?.let { Helper.setBannerAdHeight(adSize = it) }

        val adRequest = AdManagerAdRequest.Builder().build()
        adView?.loadAd(adRequest)

        adView?.adListener = object : AdListener() {
            override fun onAdClicked() {
                Log.i(LOGTAG, "onAdClicked")
                insideAdCallback?.insideAdClicked()
            }

            override fun onAdClosed() {
                Log.i(LOGTAG, "onAdClosed")
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(LOGTAG, "onAdFailedToLoad: ${adError.code}, ${adError.message}")
                Helper.setBannerAdHeight(null)
                insideAdCallback?.insideAdError(adError.message)
                insideAdProgressCallback?.insideAdError()
            }

            override fun onAdImpression() {
                Log.i(LOGTAG, "onAdImpression")
            }

            override fun onAdLoaded() {
                Log.i(LOGTAG, "onAdLoaded")
                insideAdCallback?.insideAdLoaded()

                InsideAdSdk.durationInSeconds?.let {
                    closeBannerAdHandler?.postDelayed({
                        stopAd()
                    }, it)
                }
            }

            override fun onAdOpened() {
                Log.i(LOGTAG, "onAdOpened")
            }
        }
    }

    fun stopAd() {
        Log.i(LOGTAG, "stopAd")
        removeView(adView)
        Helper.setBannerAdHeight(null)
        insideAdCallback?.insideAdStop()
        insideAdProgressCallback?.insideAdStopped()
        removeHandlers()
    }

    private fun removeHandlers() {
        closeBannerAdHandler?.removeCallbacksAndMessages(null)
        closeBannerAdHandler = null
    }

}