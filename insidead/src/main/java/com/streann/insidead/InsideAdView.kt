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
import com.streann.insidead.callbacks.CampaignCallback
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.callbacks.InsideAdProgressCallback
import com.streann.insidead.models.Campaign
import com.streann.insidead.models.InsideAd
import com.streann.insidead.players.bannerads.BannerAdsPlayer
import com.streann.insidead.players.googleima.GoogleImaPlayer
import com.streann.insidead.players.insidead.InsideAdPlayer
import com.streann.insidead.utils.CampaignsFilterUtil
import com.streann.insidead.utils.Helper
import com.streann.insidead.utils.HttpRequestsUtil
import com.streann.insidead.utils.SharedPreferencesHelper
import com.streann.insidead.utils.constants.Constants
import com.streann.insidead.utils.constants.SharedPrefKeys
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class InsideAdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle), InsideAdProgressCallback {

    private val LOGTAG = "InsideAdSdk"

    private var mInsideAdPlayer: InsideAdPlayer? = null
    private var mGoogleImaPlayer: GoogleImaPlayer? = null
    private var mBannerAdsPlayer: BannerAdsPlayer? = null

    private var insideAd: InsideAd? = null
    private var fallbackAd: InsideAd? = null
    private var insideAdCallback: InsideAdCallback? = null

    private var requestAdExecutor: ScheduledExecutorService? = null
    private var populateSdkExecutor: ExecutorService? = null
    private var showAdHandler: Handler? = null

    private var screen: String = ""
    private var apiKey: String = ""
    private var baseUrl: String = ""
    private var scale: Float = 0f

    init {
        init()
    }

    private fun init() {
        mInsideAdPlayer = InsideAdPlayer(context, this)
        addView(mInsideAdPlayer)
        mBannerAdsPlayer = BannerAdsPlayer(context, this)
        addView(mBannerAdsPlayer)
        createGoogleImaView()

        scale = resources.displayMetrics.density
        populateSdkInfo(context)

        MobileAds.initialize(context) { }
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
        insideAdCallback: InsideAdCallback?
    ) {
        Log.i(LOGTAG, "requestAd")
        InsideAdSdk.isAdMuted = isAdMuted
        this.insideAdCallback = insideAdCallback
        this.screen = screen

        if (TextUtils.isEmpty(apiKey) || TextUtils.isEmpty(baseUrl)) {
            val errorMsg =
                "Api Key and Base Url are required. Please implement the initializeSdk method."
            Log.e(LOGTAG, errorMsg)
            insideAdCallback?.insideAdError(errorMsg)
            return
        }

        requestAdExecutor = Executors.newSingleThreadScheduledExecutor()
        requestAdExecutor!!.execute {
            val geoIpUrl = HttpRequestsUtil.getGeoIpUrl()
            if (!geoIpUrl.isNullOrBlank()) {
                val geoIp = HttpRequestsUtil.getGeoIp(geoIpUrl)
                if (geoIp != null) {
                    InsideAdSdk.geoIp = geoIp
                    val geoCountryCode = geoIp.countryCode
                    if (geoCountryCode?.isNotBlank() == true) {
                        requestCampaign(geoCountryCode, screen, insideAdCallback)
                    }
                }
            }
        }
    }

    private fun requestCampaign(
        geoCountryCode: String,
        screen: String,
        insideAdCallback: InsideAdCallback?
    ) {
        Log.i(LOGTAG, "requestCampaign")
        HttpRequestsUtil.getCampaign(
            geoCountryCode,
            object : CampaignCallback {
                override fun onSuccess(campaigns: ArrayList<Campaign>?) {
                    Log.i(LOGTAG, "onSuccess: $campaigns")

                    insideAd = CampaignsFilterUtil.getInsideAd(campaigns, screen)
                    Log.i(LOGTAG, "insideAd: $insideAd")

                    insideAdCallback?.let { callback ->
                        insideAd?.let { ad ->
                            callback.insideAdReceived(ad)
                            fallbackAd = insideAd?.fallback
                            showAd(ad, callback)
                        }
                    }
                }

                override fun onError(error: String?) {
                    var errorMsg = "Error while getting AD."
                    if (!error.isNullOrBlank()) errorMsg = error
                    Log.i(LOGTAG, "onError: $errorMsg")
                    insideAdCallback?.insideAdError(errorMsg)
                }
            })
    }

    private fun showAd(
        insideAd: InsideAd,
        insideAdCallback: InsideAdCallback
    ) {
        Log.i(LOGTAG, "showAd")
        requestAdExecutor?.shutdown()
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
        }
    }

    private fun setPlayerVisibility(
        imaPlayerVisibility: Int,
        insideAdPlayerVisibility: Int,
        bannerAdPlayerVisibility: Int
    ) {
        mGoogleImaPlayer?.visibility = imaPlayerVisibility
        mInsideAdPlayer?.visibility = insideAdPlayerVisibility
        mBannerAdsPlayer?.visibility = bannerAdPlayerVisibility
    }

    private fun showGoogleImaAd(insideAd: InsideAd, insideAdCallback: InsideAdCallback) {
        createGoogleImaView()
        setPlayerVisibility(VISIBLE, GONE, GONE)
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
        setPlayerVisibility(GONE, VISIBLE, GONE)
        mInsideAdPlayer?.playAd(null, insideAd, insideAdCallback)
    }

    private fun showLocalImageAd(
        bitmap: Bitmap?,
        insideAd: InsideAd,
        insideAdCallback: InsideAdCallback
    ) {
        bitmap?.let {
            Log.i(LOGTAG, "loadAd")
            insideAdCallback.insideAdLoaded()
            setPlayerVisibility(GONE, VISIBLE, GONE)
            mInsideAdPlayer?.playAd(bitmap, insideAd, insideAdCallback)
        } ?: run {
            insideAdCallback.insideAdError("Error while getting AD.")
            insideAdError()
        }
    }

    private fun showBannerAd(insideAd: InsideAd, insideAdCallback: InsideAdCallback) {
        setPlayerVisibility(GONE, GONE, VISIBLE)
        mBannerAdsPlayer?.playAd(insideAd, insideAdCallback)
    }

    fun stopAd() {
        insideAd?.let {
            when (it.adType) {
                Constants.AD_TYPE_VAST -> mGoogleImaPlayer?.stopAd()
                Constants.AD_TYPE_LOCAL_VIDEO, Constants.AD_TYPE_LOCAL_IMAGE -> mInsideAdPlayer?.stopAd()
                Constants.AD_TYPE_BANNER -> mBannerAdsPlayer?.stopAd()
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
        Log.i(LOGTAG, "insideAdStopped")
        removeGoogleImaView()
        if (InsideAdSdk.intervalInMinutes != null && InsideAdSdk.intervalInMinutes!! > 0) {
            requestAdExecutor = Executors.newSingleThreadScheduledExecutor()
            val geoCountryCode = InsideAdSdk.geoIp?.countryCode
            if (geoCountryCode?.isNotBlank() == true) {
                requestAdExecutor!!.schedule({
                    requestCampaign(geoCountryCode, screen, insideAdCallback)
                }, InsideAdSdk.intervalInMinutes!!, TimeUnit.MILLISECONDS)
            }
        }
    }

    override fun insideAdError() {
        Log.i(LOGTAG, "insideAdError, show fallbackAd")
        insideAd = null
        insideAdCallback?.let { callback ->
            fallbackAd?.let { fallbackAd ->
                Log.i(LOGTAG, "fallbackAd: $fallbackAd")
                fallbackAd.properties?.durationInSeconds?.let {
                    InsideAdSdk.durationInSeconds = Helper.getMillisFromSeconds(it.toLong())
                }
                showAd(fallbackAd, callback)
            }
        }
    }

}