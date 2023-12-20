package com.streann.insidead

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.streann.insidead.callbacks.CampaignCallback
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.models.Campaign
import com.streann.insidead.models.InsideAd
import com.streann.insidead.utils.CampaignsFilterUtil
import com.streann.insidead.utils.Helper
import com.streann.insidead.utils.HttpRequestsUtil
import com.streann.insidead.utils.SharedPreferencesHelper
import com.streann.insidead.utils.constants.Constants
import com.streann.insidead.utils.constants.SharedPrefKeys
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class InsideAdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private val LOGTAG = "InsideAdSdk"

    private var mInsideAdPlayer: InsideAdPlayer? = null
    private var mGoogleImaPlayer: GoogleImaPlayer? = null

    private var insideAd: InsideAd? = null

    private var populateSdkExecutor: ExecutorService? = null
    private var requestAdExecutor: ExecutorService? = null
    private var showAdHandler: Handler? = null

    private var apiKey: String = ""
    private var baseUrl: String = ""
    private var scale: Float = 0f

    init {
        init()
    }

    private fun init() {
        mInsideAdPlayer = InsideAdPlayer(context)
        addView(mInsideAdPlayer)
        mGoogleImaPlayer = GoogleImaPlayer(context)
        addView(mGoogleImaPlayer)

        scale = resources.displayMetrics.density
        populateSdkInfo(context)
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
        InsideAdSdk.isAdMuted = isAdMuted

        if (TextUtils.isEmpty(apiKey)) {
            Log.e(LOGTAG, "Api Key is required. Please implement the initializeSdk method.")
            insideAdCallback?.insideAdError("Api Key is required. Please implement the initializeSdk method.")
            return
        }

        if (TextUtils.isEmpty(baseUrl)) {
            Log.e(LOGTAG, "Base Url is required. Please implement the initializeSdk method.")
            insideAdCallback?.insideAdError("Base Url is required. Please implement the initializeSdk method.")
            return
        }

        requestAdExecutor = Executors.newSingleThreadExecutor()
        requestAdExecutor!!.execute {
            val geoIpUrl = HttpRequestsUtil.getGeoIpUrl()
            if (!geoIpUrl.isNullOrBlank()) {
                val geoIp = HttpRequestsUtil.getGeoIp(geoIpUrl)
                if (geoIp != null) {
                    InsideAdSdk.geoIp = geoIp
                    val geoCountryCode = geoIp.countryCode
                    if (geoCountryCode?.isNotBlank() == true) {
                        HttpRequestsUtil.getCampaign(
                            geoCountryCode,
                            object : CampaignCallback {
                                override fun onSuccess(campaigns: ArrayList<Campaign>?) {
                                    Log.i(LOGTAG, "onSuccess: $campaigns")
                                    insideAd = CampaignsFilterUtil.getInsideAd(campaigns, screen)
                                    insideAdCallback?.let { callback ->
                                        insideAd?.let { ad ->
                                            callback.insideAdReceived(ad)
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
                }
            }
        }

        requestAdExecutor!!.shutdown()
    }

    private fun showAd(insideAd: InsideAd, insideAdCallback: InsideAdCallback) {
        showAdHandler = Handler(Looper.getMainLooper())

        when (insideAd.adType) {
            Constants.AD_TYPE_VAST -> {
                showAdHandler?.post {
                    mInsideAdPlayer?.visibility = GONE
                    mGoogleImaPlayer?.visibility = VISIBLE
                    mGoogleImaPlayer?.playAd(insideAd, insideAdCallback)
                }
            }
            Constants.AD_TYPE_LOCAL_VIDEO -> {
                showAdHandler?.post {
                    mGoogleImaPlayer?.visibility = GONE
                    mInsideAdPlayer?.visibility = VISIBLE
                    mInsideAdPlayer?.playAd(null, insideAd, insideAdCallback)
                }
            }
            Constants.AD_TYPE_LOCAL_IMAGE -> {
                val bitmap = insideAd.url?.let { Helper.getBitmapFromURL(it, resources) }
                showAdHandler?.postDelayed({
                    bitmap?.let {
                        Log.i(LOGTAG, "loadAd")
                        insideAdCallback.insideAdLoaded()

                        mGoogleImaPlayer?.visibility = GONE
                        mInsideAdPlayer?.visibility = VISIBLE

                        mInsideAdPlayer?.playAd(bitmap, insideAd, insideAdCallback)
                    } ?: run {
                        insideAdCallback.insideAdError("Error while getting AD.")
                    }
                }, 500)
            }
        }
    }

    fun stopAd() {
        insideAd?.let {
            when (insideAd!!.adType) {
                Constants.AD_TYPE_VAST -> {
                    mGoogleImaPlayer?.stopAd()
                }
                Constants.AD_TYPE_LOCAL_VIDEO, Constants.AD_TYPE_LOCAL_IMAGE -> {
                    mInsideAdPlayer?.stopAd()
                }
                else -> {}
            }
        }

        showAdHandler?.removeCallbacksAndMessages(null)
        showAdHandler = null
    }

}