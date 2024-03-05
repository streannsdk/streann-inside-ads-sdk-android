package com.streann.insidead.players.nativeads

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.streann.insidead.InsideAdSdk
import com.streann.insidead.R
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.callbacks.InsideAdProgressCallback
import com.streann.insidead.models.InsideAd

@SuppressLint("ViewConstructor")
class NativeAdsPlayer(
    context: Context,
    callback: InsideAdProgressCallback
) : FrameLayout(context) {

    private var adView: NativeAdView
    private lateinit var nativeAd: NativeAd
    private var adCloseButton: ImageView? = null

    private var insideAdCallback: InsideAdCallback? = null
    private var insideAdProgressCallback: InsideAdProgressCallback? = callback

    private var showCloseButtonHandler: Handler? = null

    init {
        adView = LayoutInflater.from(context)
            .inflate(R.layout.native_ad_player, null) as NativeAdView
        adCloseButton = adView.findViewById(R.id.adCloseButton)
    }

    fun playAd(insideAd: InsideAd, callback: InsideAdCallback) {
        insideAdCallback = callback
        showCloseButtonHandler = Handler(Looper.getMainLooper())
        val adUrl = insideAd.url ?: ""

        val builder = AdLoader.Builder(context, adUrl)

        InsideAdSdk.isAdMuted?.let {
            val videoOptions = VideoOptions.Builder().setStartMuted(it).build()
            val adOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
            builder.withNativeAdOptions(adOptions)
        }

        builder.forNativeAd { nativeAd ->
            this.nativeAd = nativeAd
            populateNativeAdView(nativeAd, adView)
        }

        val adLoader = builder.withAdListener(object : AdListener() {
            override fun onAdClicked() {
                Log.i(InsideAdSdk.LOG_TAG, "onAdClicked")
                insideAdCallback?.insideAdClicked()
            }

            override fun onAdClosed() {
                Log.i(InsideAdSdk.LOG_TAG, "onAdClosed")
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(
                    InsideAdSdk.LOG_TAG,
                    "onAdFailedToLoad: ${adError.code}, ${adError.message}"
                )
                insideAdCallback?.insideAdError(adError.message)
                insideAdProgressCallback?.insideAdError()
            }

            override fun onAdImpression() {
                Log.i(InsideAdSdk.LOG_TAG, "onAdImpression")
            }

            override fun onAdLoaded() {
                Log.i(InsideAdSdk.LOG_TAG, "onAdLoaded")
                insideAdCallback?.insideAdLoaded()
                setCloseButtonVisibility()
            }

            override fun onAdOpened() {
                Log.i(InsideAdSdk.LOG_TAG, "onAdOpened")
            }
        }).build()

        adLoader.loadAd(AdManagerAdRequest.Builder().build())
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        val mediaView: MediaView = adView.findViewById(R.id.ad_media)
        mediaView.mediaContent = nativeAd.mediaContent
        adView.mediaView = mediaView

        adView.headlineView = adView.findViewById(R.id.ad_headline)
        (adView.headlineView as TextView).text = nativeAd.headline

        val mainImage: Drawable? = nativeAd.mediaContent?.mainImage
        mainImage?.let {
            Palette.from(mainImage.toBitmap()).generate { palette ->
                val vibrantColor = palette?.vibrantSwatch?.rgb ?: Color.TRANSPARENT

                val gradientDrawable = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(vibrantColor, Color.WHITE, vibrantColor)
                )

                val adViewLayout = findViewById<ConstraintLayout>(R.id.ad_view_layout)
                adViewLayout.background = gradientDrawable

                val dominantColor = palette?.dominantSwatch?.rgb ?: Color.TRANSPARENT
                dominantColor.let {
                    val red = (dominantColor shr 16) and 0xFF
                    val green = (dominantColor shr 8) and 0xFF
                    val blue = dominantColor and 0xFF

                    val backgroundColor = Color.rgb(red, green, blue)

                    adView.callToActionView?.backgroundTintList =
                        ColorStateList.valueOf(backgroundColor)
                }
            }
        }

        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        if (nativeAd.icon == null) {
            adView.iconView!!.visibility = View.INVISIBLE
        } else {
            (adView.iconView as ImageView).setImageDrawable(nativeAd.icon?.drawable)
            adView.iconView!!.visibility = View.VISIBLE
        }

        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
        if (nativeAd.advertiser.isNullOrBlank()) {
            adView.advertiserView!!.visibility = View.INVISIBLE
        } else {
            adView.advertiserView!!.visibility = View.VISIBLE
            (adView.advertiserView as TextView).text = nativeAd.advertiser
        }

        adView.starRatingView = adView.findViewById(R.id.ad_star_rating)
        if (nativeAd.starRating == null) {
            adView.starRatingView!!.visibility = View.INVISIBLE
        } else {
            val starRatingImage = getStartRatingImageView(nativeAd.starRating)
            starRatingImage?.let {
                (adView.starRatingView as ImageView).setImageDrawable(starRatingImage)
                adView.starRatingView!!.visibility = View.VISIBLE
            } ?: run {
                adView.starRatingView!!.visibility = View.INVISIBLE
            }
        }

        adView.bodyView = adView.findViewById(R.id.ad_body)
        if (nativeAd.body == null) {
            adView.bodyView!!.visibility = View.INVISIBLE
        } else {
            adView.bodyView!!.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }

        adView.priceView = adView.findViewById(R.id.ad_price)
        if (nativeAd.price.isNullOrBlank()) {
            adView.priceView!!.visibility = View.INVISIBLE
        } else {
            adView.priceView!!.visibility = View.VISIBLE
            (adView.priceView as TextView).text = nativeAd.price
        }

        adView.storeView = adView.findViewById(R.id.ad_store)
        if (nativeAd.store.isNullOrBlank()) {
            adView.storeView!!.visibility = View.INVISIBLE
        } else {
            adView.storeView!!.visibility = View.VISIBLE
            (adView.storeView as TextView).text = nativeAd.store
        }

        adView.callToActionView = adView.findViewById<Button>(R.id.ad_call_to_action)
        if (nativeAd.callToAction == null) {
            adView.callToActionView!!.visibility = View.INVISIBLE
        } else {
            adView.callToActionView!!.visibility = View.VISIBLE
            (adView.callToActionView as Button).text = nativeAd.callToAction
        }

        val videoController = nativeAd.mediaContent?.videoController
        if (videoController?.hasVideoContent() == true) {
            videoController.videoLifecycleCallbacks =
                object : VideoController.VideoLifecycleCallbacks() {
                    override fun onVideoStart() {
                        super.onVideoStart()
                        Log.d(InsideAdSdk.LOG_TAG, "onVideoStart")
                    }

                    override fun onVideoPlay() {
                        super.onVideoPlay()
                        Log.d(InsideAdSdk.LOG_TAG, "onVideoPlay")
                    }

                    override fun onVideoPause() {
                        super.onVideoPause()
                        Log.d(InsideAdSdk.LOG_TAG, "onVideoPause")
                    }

                    override fun onVideoMute(p0: Boolean) {
                        super.onVideoMute(p0)
                        Log.d(InsideAdSdk.LOG_TAG, "onVideoMute")
                    }

                    override fun onVideoEnd() {
                        super.onVideoEnd()
                        Log.d(InsideAdSdk.LOG_TAG, "onVideoEnd")
                    }
                }
        }

        adView.setNativeAd(nativeAd)
        addView(adView)
    }

    private fun getStartRatingImageView(starRating: Double?): Drawable? {
        return starRating?.let {
            when {
                starRating >= 5 -> ResourcesCompat.getDrawable(resources, R.drawable.stars_5, null)

                starRating >= 4.5 -> ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.stars_4_5,
                    null
                )

                starRating >= 4 -> ResourcesCompat.getDrawable(resources, R.drawable.stars_4, null)

                starRating >= 3.5 -> ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.stars_3_5,
                    null
                )

                else -> null
            }
        }
    }

    private fun setCloseButtonVisibility() {
        InsideAdSdk.showCloseButtonAfterSeconds?.let {
            showCloseButtonHandler?.postDelayed({
                adCloseButton?.visibility = VISIBLE
                adCloseButton?.setOnClickListener {
                    stopAd()
                }
            }, it)
        }
    }

    fun stopAd() {
        Log.i(InsideAdSdk.LOG_TAG, "stopAd")
        this.nativeAd.destroy()
        adCloseButton?.visibility = GONE
        removeView(adView)
        removeHandlers()
        insideAdCallback?.insideAdStop()
        insideAdProgressCallback?.insideAdStopped()
    }

    private fun removeHandlers() {
        showCloseButtonHandler?.removeCallbacksAndMessages(null)
        showCloseButtonHandler = null
    }

}
