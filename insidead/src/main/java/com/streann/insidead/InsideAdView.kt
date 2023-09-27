package com.streann.insidead

import android.content.Context
import android.os.Looper
import android.text.TextUtils
import android.util.AttributeSet
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
    private var mGoogleImaPlayer: GoogleImaPlayer? = null

    init {
        init()
    }

    private fun init() {
        mGoogleImaPlayer = GoogleImaPlayer(context)
        addView(mGoogleImaPlayer)
    }

    fun requestAd(resellerId: String, insideAdCallback: InsideAdCallback) {
        if (TextUtils.isEmpty(resellerId)) {
            insideAdCallback.insideAdError("ID is required.")
            return
        }

        val executor = Executors.newSingleThreadExecutor()
        val handler = android.os.Handler(Looper.getMainLooper())
        executor.execute {
            var geoIpJsonObject = HttpURLConnection.getGeoIp()
            var geoCountryCode = geoIpJsonObject?.get("countryCode").toString()

            handler.post {
                makeRequest(resellerId, insideAdCallback,
                    object : CampaignCallback {
                        override fun onSuccess(insideAd: InsideAd) {
                            showAd(insideAd, insideAdCallback)
                        }

                        override fun onError(error: String?) {
                        }
                    })
            }
        }
    }

    private fun makeRequest(
        id: String,
        insideAdCallback: InsideAdCallback,
        campaignCallback: CampaignCallback
    ) {

    }

    private fun showAd(insideAd: InsideAd, insideAdCallback: InsideAdCallback) {
        mGoogleImaPlayer?.visibility = VISIBLE
        mGoogleImaPlayer?.playAd(insideAd, insideAdCallback)
    }

}