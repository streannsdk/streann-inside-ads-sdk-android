package com.streann.insidead

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import com.streann.insidead.callbacks.CampaignCallback
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.models.InsideAd
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

    fun requestAd(resellerId: String, insideAdCallback: InsideAdCallback?) {
        if (TextUtils.isEmpty(resellerId)) {
            insideAdCallback?.insideAdError("ID is required.")
            return
        }

        executor.execute {
            val geoIpJsonObject = HttpRequestsUtil.getGeoIp()
            if (geoIpJsonObject != null) {
                if (geoIpJsonObject.has("countryCode")) {
                    var geoCountryCode = geoIpJsonObject.get("countryCode").toString()
                    if (resellerId.isNotBlank() && geoCountryCode.isNotBlank()) {
                        HttpRequestsUtil.getCampaign(
                            resellerId,
                            geoCountryCode,
                            object : CampaignCallback {
                                override fun onSuccess(insideAd: InsideAd) {
                                    Log.d(LOGTAG, "onSuccess $insideAd")
                                    insideAdCallback?.let { showAd(insideAd, it) }
                                }

                                override fun onError(error: String?) {
                                    Log.d(LOGTAG, "onError $error")
                                }
                            })
                    }
                }
            }
        }

        executor.shutdown()
    }

    private fun showAd(insideAd: InsideAd, insideAdCallback: InsideAdCallback) {
        mGoogleImaPlayer?.visibility = VISIBLE
        mGoogleImaPlayer?.playAd(insideAd, insideAdCallback)
    }

}