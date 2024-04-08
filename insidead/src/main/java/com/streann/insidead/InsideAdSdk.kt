package com.streann.insidead

import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Log
import com.streann.insidead.callbacks.CampaignCallback
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.models.Campaign
import com.streann.insidead.models.GeoIp
import com.streann.insidead.models.TargetingFilters
import com.streann.insidead.utils.HttpRequestsUtil
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

object InsideAdSdk {

    internal const val LOG_TAG = "InsideAdSdk"

    internal var apiKey: String = ""
    internal var apiToken: String = ""
    internal var baseUrl: String = ""
    internal var bundleId: String? = ""
    internal var appName: String? = ""
    internal var appVersion: String? = ""
    internal var appDomain: String? = ""
    internal var siteUrl: String? = ""
    internal var storeUrl: String? = ""
    internal var descriptionUrl: String? = ""
    internal var userBirthYear: Int? = 0
    internal var userGender: String? = ""
    internal var adId: String? = ""
    internal var adLimitTracking: Int? = 0
    internal var playerWidth: Int = 0
    internal var playerHeight: Int = 0
    internal var isAdMuted: Boolean? = false
    internal var targetingFilters: TargetingFilters? = null

    internal var geoIp: GeoIp? = null
    internal var appPreferences: SharedPreferences? = null

    internal var intervalInMinutes: Long? = null
    internal var startAfterSeconds: Long? = null
    internal var showCloseButtonAfterSeconds: Long? = null
    internal var durationInSeconds: Long? = null

    internal var campaignsList: ArrayList<Campaign>? = null
    internal var campaignsErrorOrNull: Boolean? = false

    private var insideAdCallback: InsideAdCallback? = null
    private var requestCampaignExecutor: ScheduledExecutorService? = null

    var intervalForReels: Int? = null
    internal var showAdForReels: Boolean = false

    fun initializeSdk(
        apiKey: String, apiToken: String, baseUrl: String, appDomain: String? = "",
        siteUrl: String? = "", storeUrl: String? = "", descriptionUrl: String? = "",
        userBirthYear: Int? = 0, userGender: String? = ""
    ) {
        this.apiKey = apiKey
        this.apiToken = apiToken
        this.baseUrl = baseUrl
        this.appDomain = appDomain
        this.siteUrl = siteUrl
        this.storeUrl = storeUrl
        this.descriptionUrl = descriptionUrl
        this.userBirthYear = userBirthYear
        this.userGender = userGender

        requestCampaign()
    }

    private fun requestCampaign(
    ) {
        Log.i(LOG_TAG, "requestCampaign")

        if (TextUtils.isEmpty(apiKey) || TextUtils.isEmpty(baseUrl)) {
            val errorMsg =
                "Api Key and Base Url are required. Please implement the initializeSdk method."
            Log.e(LOG_TAG, errorMsg)
            insideAdCallback?.insideAdError(errorMsg)
            return
        }

        requestCampaignExecutor = Executors.newSingleThreadScheduledExecutor()
        requestCampaignExecutor!!.execute {
            val geoIpUrl = HttpRequestsUtil.getGeoIpUrl()
            if (!geoIpUrl.isNullOrBlank()) {
                val geoIp = HttpRequestsUtil.getGeoIp(geoIpUrl)
                if (geoIp != null) {
                    InsideAdSdk.geoIp = geoIp
                    val geoCountryCode = geoIp.countryCode
                    if (geoCountryCode?.isNotBlank() == true) {
                        getCampaigns(geoCountryCode)
                    }
                }
            }
        }
    }

    private fun getCampaigns(
        geoCountryCode: String,
    ) {
        Log.i(LOG_TAG, "getCampaigns")
        HttpRequestsUtil.getCampaign(
            geoCountryCode,
            object : CampaignCallback {
                override fun onSuccess(campaigns: ArrayList<Campaign>?) {
                    Log.i(LOG_TAG, "onSuccess: $campaigns")
                    campaignsList = campaigns
                    requestCampaignExecutor?.shutdown()
                }

                override fun onError(error: String?) {
                    var errorMsg = "Error while getting AD."
                    if (!error.isNullOrBlank()) errorMsg = error
                    Log.i(LOG_TAG, "onError: $errorMsg")
                    campaignsErrorOrNull = true
                    requestCampaignExecutor?.shutdown()
                }
            })
    }

    fun setInsideAdCallback(callback: InsideAdCallback) {
        insideAdCallback = callback
    }

    fun getInsideAdCallback(): InsideAdCallback? {
        return insideAdCallback
    }

    fun areTargetingFiltersEmpty(): Boolean {
        return targetingFilters?.let {
            it.vodId.isNullOrEmpty() &&
                    it.channelId.isNullOrEmpty() &&
                    it.radioId.isNullOrEmpty() &&
                    it.seriesId.isNullOrEmpty() &&
                    it.categoryIds.isNullOrEmpty() &&
                    it.contentProviderId.isNullOrEmpty()
        } ?: true
    }

}