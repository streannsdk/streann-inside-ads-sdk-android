package com.streann.insidead

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import com.streann.insidead.application.AppController
import com.streann.insidead.callbacks.CampaignCallback
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.models.GeoIp
import com.streann.insidead.models.InsideAd
import com.streann.insidead.utils.HttpRequestsUtil
import java.util.concurrent.Executors

class InsideAdView @JvmOverloads constructor(
    private val context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {
    private val LOGTAG = "InsideAdStreann"
    private var mGoogleImaPlayer: GoogleImaPlayer? = null
    private val executor = Executors.newSingleThreadExecutor()

    init {
        init()
    }

    private fun init() {
        mGoogleImaPlayer = GoogleImaPlayer(context)
        addView(mGoogleImaPlayer)
    }

    fun requestAd(screen: String, insideAdCallback: InsideAdCallback?) {
        val resellerId = AppController.reseller.resellerId

        if (TextUtils.isEmpty(resellerId)) {
            insideAdCallback?.insideAdError("ID is required.")
            return
        }

        executor.execute {
            val geoIp = HttpRequestsUtil.getGeoIp()
            if (geoIp != null) {
                var geoCountryCode = geoIp.countryCode
                if (resellerId.isNotBlank() && geoCountryCode?.isNotBlank() == true) {
                    HttpRequestsUtil.getCampaign(
                        resellerId,
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