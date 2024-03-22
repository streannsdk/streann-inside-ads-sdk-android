package com.streann.insidead

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.callbacks.InsideAdProgressCallback
import com.streann.insidead.models.InsideAd
import com.streann.insidead.players.bannerads.BannerAdsPlayer
import com.streann.insidead.players.googleima.GoogleImaPlayer
import com.streann.insidead.players.insidead.InsideAdPlayer
import com.streann.insidead.players.nativeads.NativeAdsPlayer
import com.streann.insidead.utils.CampaignsFilterUtil.getActiveCampaigns
import com.streann.insidead.utils.Helper
import com.streann.insidead.utils.SharedPreferencesHelper
import com.streann.insidead.utils.constants.Constants
import com.streann.insidead.utils.constants.SharedPrefKeys
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class InsideAdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle), InsideAdProgressCallback {

    private var mInsideAdPlayer: InsideAdPlayer? = null
    private var mGoogleImaPlayer: GoogleImaPlayer? = null
    private var mBannerAdsPlayer: BannerAdsPlayer? = null
    private var mNativeAdsPlayer: NativeAdsPlayer? = null

    private var insideAd: InsideAd? = null
    private var fallbackAd: InsideAd? = null
    private var insideAdCallback: InsideAdCallback? = null

    private var populateSdkExecutor: ExecutorService? = null
    private var adIntervalHandler: Handler? = null
    private var showAdHandler: Handler? = null

    private var screen: String = ""
    private var apiKey: String = ""
    private var baseUrl: String = ""
    private var scale: Float = 0f

    private var retryCount = 0
    private val maxRetries = 3
    private val retryDelayMillis = 3000L
    private var retryRequestHandler: Handler? = null

    init {
        init()
    }

    private fun init() {
        initializePlayers()

        scale = resources.displayMetrics.density
        populateSdkInfo(context)

        MobileAds.initialize(context) { }
    }

    private fun initializePlayers() {
        createGoogleImaView()
        mInsideAdPlayer = InsideAdPlayer(context, this)
        addView(mInsideAdPlayer)
        mBannerAdsPlayer = BannerAdsPlayer(context, this)
        addView(mBannerAdsPlayer)
        mNativeAdsPlayer = NativeAdsPlayer(context, this)
        addView(mNativeAdsPlayer)
    }

    private fun populateSdkInfo(context: Context?) {
        apiKey = InsideAdSdk.apiKey
        baseUrl = InsideAdSdk.baseUrl

        context?.let {
            InsideAdSdk.bundleId = it.packageName
            InsideAdSdk.appName = it.applicationInfo.loadLabel(it.packageManager).toString()
            InsideAdSdk.appVersion =
                Helper.getPackageVersionCode(it.packageManager, it.packageName).toString()

            InsideAdSdk.appPreferences = it.getSharedPreferences(
                SharedPrefKeys.PREF_APP_PREFERENCES,
                Application.MODE_PRIVATE
            )

            populateSdkExecutor = Executors.newSingleThreadExecutor()
            populateSdkExecutor!!.execute {
                try {
                    val info = AdvertisingIdClient.getAdvertisingIdInfo(it)
                    SharedPreferencesHelper.putAdId(info.id)
                    SharedPreferencesHelper.putAdLimitTracking(info.isLimitAdTrackingEnabled)
                    InsideAdSdk.adId = SharedPreferencesHelper.getAdId()
                    InsideAdSdk.adLimitTracking = SharedPreferencesHelper.getAdLimitTracking()
                } catch (e: Exception) {
                    SharedPreferencesHelper.putAdId("")
                }

                InsideAdSdk.playerWidth = (width / scale).toInt()
                InsideAdSdk.playerHeight = (height / scale).toInt()
            }
            populateSdkExecutor!!.shutdown()
        }
    }

    fun requestAd(
        screen: String,
        isAdMuted: Boolean? = false,
    ) {
        Log.i("mano", "requestAd")

        retryRequestHandler = Handler(Looper.getMainLooper())

        InsideAdSdk.isAdMuted = isAdMuted
        this.insideAdCallback = InsideAdSdk.getInsideAdCallback()
        this.screen = screen

        if (TextUtils.isEmpty(apiKey) || TextUtils.isEmpty(baseUrl)) {
            val errorMsg =
                "Api Key and Base Url are required. Please implement the initializeSdk method."
            Log.e(InsideAdSdk.LOG_TAG, errorMsg)
            insideAdCallback?.insideAdError(errorMsg)
            return
        }

        getInsideAdRetry()
    }

    private fun getInsideAdRetry() {
        Log.i("mano", "getInsideAdRetry")
        if (retryCount < maxRetries) {
            if (InsideAdSdk.campaignsList != null) {
                Log.i("mano", "has campaigns, now filter and show inside ad")
                getInsideAd(screen, insideAdCallback)
            } else {
                Log.i("mano", "no campaigns, try showing ad again")
                retryCount++
                retryRequestHandler?.postDelayed({
                    getInsideAdRetry()
                }, retryDelayMillis)
            }
        }
    }

    private fun getInsideAd(
        screen: String,
        insideAdCallback: InsideAdCallback?
    ) {
        Log.i("mano", "getInsideAd")

        // get local list of campaigns and filter them
        val campaigns = getActiveCampaigns(InsideAdSdk.campaignsList)
        Log.i("mano", "campaigns: $campaigns")
        // if campaigns are null or empty close handlers
        // if campaigns are not null get an ad from them

//        insideAd = CampaignsFilterUtil.getInsideAd(campaigns, screen)
//        Log.i(InsideAdSdk.LOG_TAG, "insideAd: $insideAd")

        insideAdCallback?.let { callback ->
            insideAd?.let { ad ->
                callback.insideAdReceived(ad)
                fallbackAd = insideAd?.fallback
                showAd(ad, callback)
            }
        }
    }

    private fun showAd(
        insideAd: InsideAd,
        insideAdCallback: InsideAdCallback
    ) {
        Log.i("mano", "showAd")
        retryRequestHandler?.removeCallbacksAndMessages(null)
        retryRequestHandler = null
        showAdHandler = Handler(Looper.getMainLooper())

        val delayMillis = InsideAdSdk.startAfterSeconds ?: 0
        when (insideAd.adType) {
            Constants.AD_TYPE_VAST ->
                showAdHandler?.postDelayed({
                    showGoogleImaAd(
                        insideAd,
                        insideAdCallback
                    )
                }, delayMillis)

            Constants.AD_TYPE_LOCAL_VIDEO ->
                showAdHandler?.postDelayed({
                    showLocalVideoAd(insideAd, insideAdCallback)
                }, delayMillis)

            Constants.AD_TYPE_LOCAL_IMAGE -> {
                insideAd.url?.let { url ->
                    Helper.getBitmapFromURL(url, resources) { bitmap ->
                        showAdHandler?.postDelayed({
                            showLocalImageAd(bitmap, insideAd, insideAdCallback)
                        }, delayMillis)
                    }
                }
            }

            Constants.AD_TYPE_BANNER ->
                showAdHandler?.postDelayed({
                    showBannerAd(insideAd, insideAdCallback)
                }, delayMillis)

            Constants.AD_TYPE_FULLSCREEN_NATIVE ->
                // remove showAdHandler for Native Ads
                showNativeAd(insideAd, insideAdCallback)
        }
    }

    private fun setPlayerVisibility(
        imaPlayerVisibility: Int,
        insideAdPlayerVisibility: Int,
        bannerAdPlayerVisibility: Int,
        nativeAdPlayerVisibility: Int,
    ) {
        mGoogleImaPlayer?.visibility = imaPlayerVisibility
        mInsideAdPlayer?.visibility = insideAdPlayerVisibility
        mBannerAdsPlayer?.visibility = bannerAdPlayerVisibility
        mNativeAdsPlayer?.visibility = nativeAdPlayerVisibility
    }

    private fun showGoogleImaAd(insideAd: InsideAd, insideAdCallback: InsideAdCallback) {
        createGoogleImaView()
        setPlayerVisibility(VISIBLE, GONE, GONE, GONE)
        mGoogleImaPlayer?.playAd(insideAd, insideAdCallback)
    }

    private fun createGoogleImaView() {
        if (mGoogleImaPlayer == null) {
            mGoogleImaPlayer = GoogleImaPlayer(context, this)
            addView(mGoogleImaPlayer)
        }
    }

    private fun removeGoogleImaView() {
        if (mGoogleImaPlayer != null) {
            removeView(mGoogleImaPlayer)
            mGoogleImaPlayer = null
        }
    }

    private fun showLocalVideoAd(insideAd: InsideAd, insideAdCallback: InsideAdCallback) {
        setPlayerVisibility(GONE, VISIBLE, GONE, GONE)
        mInsideAdPlayer?.playAd(null, insideAd, insideAdCallback)
    }

    private fun showLocalImageAd(
        bitmap: Bitmap?,
        insideAd: InsideAd,
        insideAdCallback: InsideAdCallback
    ) {
        bitmap?.let {
            Log.i(InsideAdSdk.LOG_TAG, "loadAd")
            insideAdCallback.insideAdLoaded()
            setPlayerVisibility(GONE, VISIBLE, GONE, GONE)
            mInsideAdPlayer?.playAd(bitmap, insideAd, insideAdCallback)
        } ?: run {
            insideAdCallback.insideAdError("Error while getting AD.")
            insideAdError()
        }
    }

    private fun showBannerAd(insideAd: InsideAd, insideAdCallback: InsideAdCallback) {
        setPlayerVisibility(GONE, GONE, VISIBLE, GONE)
        mBannerAdsPlayer?.playAd(insideAd, insideAdCallback)
    }

    private fun showNativeAd(insideAd: InsideAd, insideAdCallback: InsideAdCallback) {
        setPlayerVisibility(GONE, GONE, GONE, VISIBLE)
        mNativeAdsPlayer?.playAd(insideAd, insideAdCallback)
    }

    fun stopAd() {
        insideAd?.let {
            when (it.adType) {
                Constants.AD_TYPE_VAST -> mGoogleImaPlayer?.stopAd()
                Constants.AD_TYPE_LOCAL_VIDEO, Constants.AD_TYPE_LOCAL_IMAGE -> mInsideAdPlayer?.stopAd()
                Constants.AD_TYPE_BANNER -> mBannerAdsPlayer?.stopAd()
                Constants.AD_TYPE_FULLSCREEN_NATIVE -> mNativeAdsPlayer?.stopAd()
                else -> {}
            }
        }

        showAdHandler?.removeCallbacksAndMessages(null)
        showAdHandler = null
    }

    fun playAd() {
        if (insideAd?.adType == Constants.AD_TYPE_LOCAL_VIDEO) {
            mInsideAdPlayer?.startPlayingAd()
        } else if (fallbackAd?.adType == Constants.AD_TYPE_LOCAL_VIDEO) {
            mInsideAdPlayer?.startPlayingAd()
        }
    }

    override fun insideAdStopped() {
        Log.i(InsideAdSdk.LOG_TAG, "insideAdStopped")
        removeGoogleImaView()
        // call this part of logic for all ads that are not native ads
        if (insideAd?.adType != Constants.AD_TYPE_FULLSCREEN_NATIVE) {
            if (InsideAdSdk.intervalInMinutes != null && InsideAdSdk.intervalInMinutes!! > 0) {
                adIntervalHandler = Handler(Looper.getMainLooper())
                adIntervalHandler?.postDelayed({
                    getInsideAd(screen, insideAdCallback)
                }, InsideAdSdk.intervalInMinutes!!)
            }
        }
    }

    override fun insideAdError() {
        Log.i(InsideAdSdk.LOG_TAG, "insideAdError, show fallbackAd")
        insideAd = null
        insideAdCallback?.let { callback ->
            fallbackAd?.let { fallbackAd ->
                Log.i(InsideAdSdk.LOG_TAG, "fallbackAd: $fallbackAd")
                fallbackAd.properties?.durationInSeconds?.let {
                    InsideAdSdk.durationInSeconds = Helper.getMillisFromSeconds(it.toLong())
                }
                showAd(fallbackAd, callback)
            }
        }
    }

}