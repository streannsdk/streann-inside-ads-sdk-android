package com.streann.insidead

import android.app.Application
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.streann.insidead.callbacks.CampaignCallback
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.models.Campaign
import com.streann.insidead.models.GeoIp
import com.streann.insidead.models.InsideAd
import com.streann.insidead.utils.Helper
import com.streann.insidead.utils.HttpRequestsUtil
import com.streann.insidead.utils.SharedPreferencesHelper
import com.streann.insidead.utils.constants.SharedPrefKeys
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class InsideAdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private val LOGTAG = "InsideAdSdk"
    private var mGoogleImaPlayer: GoogleImaPlayer? = null
    private var populateSdkExecutor: ExecutorService? = null
    private var requestAdExecutor: ExecutorService? = null
    private var apiKey: String = ""
    private var scale: Float = 0f

    init {
        init()
    }

    private fun init() {
        mGoogleImaPlayer = GoogleImaPlayer(context)
        addView(mGoogleImaPlayer)

        scale = resources.displayMetrics.density
        populateSdkInfo(context)
    }

    private fun populateSdkInfo(context: Context?) {
        apiKey = InsideAdSdk.apiKey

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

    fun requestAd(screen: String, insideAdCallback: InsideAdCallback?) {
        if (TextUtils.isEmpty(apiKey)) {
            Log.e(LOGTAG, "Api Key is required. Please implement the initializeSdk method.")
            insideAdCallback?.insideAdError("Api Key is required. Please implement the initializeSdk method.")
            return
        }

        requestAdExecutor = Executors.newSingleThreadExecutor()
        requestAdExecutor!!.execute {
            val geoIp = HttpRequestsUtil.getGeoIp()
            if (geoIp != null) {
                var geoCountryCode = geoIp.countryCode
                if (apiKey.isNotBlank() && geoCountryCode?.isNotBlank() == true) {
                    HttpRequestsUtil.getCampaign(
                        apiKey,
                        geoCountryCode,
                        screen,
                        object : CampaignCallback {
                            override fun onSuccess(campaign: Campaign) {
                                Log.i(LOGTAG, "onSuccess: $campaign")
                                insideAdCallback?.let {
                                    val insideAd = campaign.insideAd
                                    insideAd?.let { ad ->
                                        it.insideAdReceived(ad)
                                        showAd(ad, geoIp, it)
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
        requestAdExecutor!!.shutdown()
    }

    private fun showAd(
        insideAd: InsideAd,
        geoIp: GeoIp,
        insideAdCallback: InsideAdCallback
    ) {
        mGoogleImaPlayer?.playAd(insideAd, geoIp, insideAdCallback)
    }

}