package com.streann.insidead

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import com.streann.insidead.callbacks.CampaignCallback
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.models.GeoIp
import com.streann.insidead.models.InsideAd
import com.streann.insidead.utils.HttpRequestsUtil
import java.util.concurrent.Executors

class InsideAdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {
    private val LOGTAG = "InsideAdStreann"
    private var mGoogleImaPlayer: GoogleImaPlayer? = null
    private val executor = Executors.newSingleThreadExecutor()

    private var apiKey: String = ""
    private var siteUrl: String? = ""
    private var storeUrl: String? = ""
    private var descriptionUrl: String? = ""
    private var userBirthYear: Int? = 0
    private var userGender: String? = ""

    init {
        init()
    }

    private fun init() {
        mGoogleImaPlayer = GoogleImaPlayer(context)
        addView(mGoogleImaPlayer)
    }

    fun initializeSdk(
        apiKey: String,
        siteUrl: String? = "",
        storeUrl: String? = "",
        descriptionUrl: String? = "",
        userBirthYear: Int? = 0,
        userGender: String? = ""
    ) {
        this.apiKey = apiKey
        this.siteUrl = siteUrl
        this.storeUrl = storeUrl
        this.descriptionUrl = descriptionUrl
        this.userBirthYear = userBirthYear
        this.userGender = userGender
    }

    fun requestAd(screen: String, insideAdCallback: InsideAdCallback?) {

        if (TextUtils.isEmpty(apiKey)) {
            insideAdCallback?.insideAdError("Api Key is required.")
            return
        }

        executor.execute {
            val geoIp = HttpRequestsUtil.getGeoIp()
            if (geoIp != null) {
                var geoCountryCode = geoIp.countryCode
                if (apiKey.isNotBlank() && geoCountryCode?.isNotBlank() == true) {
                    HttpRequestsUtil.getCampaign(
                        apiKey,
                        geoCountryCode,
                        screen,
                        object : CampaignCallback {
                            override fun onSuccess(insideAd: InsideAd) {
                                Log.d(LOGTAG, "onSuccess $insideAd")
                                insideAdCallback?.let { showAd(insideAd, geoIp, it) }
                            }

                            override fun onError(error: String?) {
                                Log.d(LOGTAG, "onError $error")
                            }
                        })
                }

            }
        }
    }

    private fun showAd(
        insideAd: InsideAd,
        geoIp: GeoIp,
        insideAdCallback: InsideAdCallback
    ) {
        mGoogleImaPlayer?.visibility = VISIBLE
        mGoogleImaPlayer?.playAd(insideAd, geoIp, insideAdCallback)
    }

    fun shutdownInsideAdExecutor() {
        executor.shutdown()
    }

}