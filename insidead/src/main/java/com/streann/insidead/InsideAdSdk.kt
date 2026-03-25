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

    /**
     * Enable debug mode to get verbose logging for troubleshooting.
     * This includes detailed logs for:
     * - Skip button lifecycle (VAST ads only)
     * - Ad loading and playback
     * - Configuration changes
     * - Error handling
     *
     * WARNING: Only enable this in debug builds as it generates extensive logs.
     */
    var debugMode: Boolean = false

    /**
     * Internal helper for debug logging.
     * Only logs if debugMode is enabled.
     */
    internal fun debugLog(tag: String, message: String) {
        if (debugMode) {
            Log.d("$LOG_TAG-$tag", message)
        }
    }

    /**
     * Internal helper for detailed skip button state logging.
     * Only logs if debugMode is enabled.
     */
    internal fun logSkipButtonState(
        event: String,
        adName: String? = null,
        adType: String? = null,
        isSkippable: Boolean? = null,
        skipOffsetSeconds: Int? = null,
        adDuration: Float? = null,
        currentTime: Float? = null,
        additionalInfo: Map<String, Any?>? = null
    ) {
        if (!debugMode) return

        val sb = StringBuilder()
        sb.appendLine("╔═══════════════════════════════════════════════════════════╗")
        sb.appendLine("║          SKIP BUTTON DEBUG - $event")
        sb.appendLine("╠═══════════════════════════════════════════════════════════╣")
        adName?.let { sb.appendLine("║ Ad Name: $it") }
        adType?.let { sb.appendLine("║ Ad Type: $it") }
        isSkippable?.let { sb.appendLine("║ Is Skippable: $it") }
        skipOffsetSeconds?.let { sb.appendLine("║ Skip Offset: ${it}s") }
        adDuration?.let { sb.appendLine("║ Ad Duration: ${it}s") }
        currentTime?.let { sb.appendLine("║ Current Time: ${it}s") }

        additionalInfo?.forEach { (key, value) ->
            sb.appendLine("║ $key: $value")
        }

        sb.appendLine("╚═══════════════════════════════════════════════════════════╝")
        Log.d("$LOG_TAG-SkipButton", sb.toString())
    }

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
    internal var isAdMuted: Boolean? = true
    internal var targetingFilters: TargetingFilters? = null
    internal var resizeMode: InsideAdView.ResizeMode? = null

    internal var geoIp: GeoIp? = null

    private var prerollAdCallback: InsideAdCallback? = null
    internal var isPrerollMode: Boolean = false

    // Temporary storage for regular ad parameters when preroll is active
    internal var savedIsAdMuted: Boolean? = null
    internal var savedTargetingFilters: TargetingFilters? = null

    // Track active preroll ad request to enable cancellation
    internal var activePrerollAdView: InsideAdView? = null

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
                        return@execute
                    }
                }
            }
            // Shut down executor if we didn't reach getCampaigns (which handles its own shutdown)
            requestCampaignExecutor?.shutdown()
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
                    it.contentProviderId.isNullOrEmpty() &&
                    it.contentTitle.isNullOrEmpty()
        } ?: true
    }

    fun setPrerollAdCallback(callback: InsideAdCallback) {
        prerollAdCallback = callback
    }

    fun getPrerollAdCallback(): InsideAdCallback? {
        return prerollAdCallback
    }

    /**
     * Removes the preroll ad callback completely.
     * Call this when you no longer want to receive preroll ad events.
     * This should typically be called in Activity/Fragment onDestroy() or onStop().
     */
    fun removePrerollAdCallback() {
        Log.i(LOG_TAG, "removePrerollAdCallback")
        prerollAdCallback = null
    }

    /**
     * Cancels any ongoing preroll ad request.
     * Stops pending handlers, clears callbacks, and cleans up resources.
     * Safe to call even if no preroll ad is active.
     */
    fun cancelPrerollAdRequest() {
        Log.i(LOG_TAG, "cancelPrerollAdRequest")
        activePrerollAdView?.cancelAdRequest()
        activePrerollAdView = null
        prerollAdCallback = null

        // Restore regular ad parameters if preroll was in progress
        if (isPrerollMode) {
            isAdMuted = savedIsAdMuted
            targetingFilters = savedTargetingFilters
            isPrerollMode = false
            savedIsAdMuted = null
            savedTargetingFilters = null
        }
    }

    /**
     * Removes the regular inside ad callback completely.
     * Call this when you no longer want to receive ad events.
     * This should typically be called in Activity/Fragment onDestroy() or onStop().
     */
    fun removeInsideAdCallback() {
        Log.i(LOG_TAG, "removeInsideAdCallback")
        insideAdCallback = null
    }

    fun requestPrerollAd(
        context: android.content.Context,
        adContainer: InsideAdView,
        screen: String = "",
        isAdMuted: Boolean? = true,
        targetingFilters: TargetingFilters? = null
    ) {
        Log.i(LOG_TAG, "requestPrerollAd - screen: $screen")
        activePrerollAdView = adContainer
        adContainer.requestPrerollAd(screen, isAdMuted, targetingFilters)
    }

}