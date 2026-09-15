package com.streann.insidead

import java.util.EnumMap
import com.streann.insidead.utils.enums.ViewType
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Log
import com.streann.insidead.callbacks.CampaignCallback
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.models.Campaign
import com.streann.insidead.models.GeoIp
import com.streann.insidead.models.TargetingFilters
import com.streann.insidead.models.isEmpty
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

    /**
     * Callbacks and in-flight views per dedicated slot.
     *
     * These were single fields when preroll was the only slot. The multiview canvas and right bar
     * can be on screen at the same time, so each slot needs its own entry or they clobber each
     * other.
     */
    private val slotCallbacks = EnumMap<ViewType, InsideAdCallback>(ViewType::class.java)
    private val activeSlotViews = EnumMap<ViewType, InsideAdView>(ViewType::class.java)

    internal var isPrerollMode: Boolean = false

    // Temporary storage for regular ad parameters when preroll is active
    internal var savedIsAdMuted: Boolean? = null
    internal var savedTargetingFilters: TargetingFilters? = null


    internal var appPreferences: SharedPreferences? = null

    internal var intervalInMinutes: Long? = null
    internal var startAfterSeconds: Long? = null
    internal var showCloseButtonAfterSeconds: Long? = null
    internal var durationInSeconds: Long? = null

    internal var campaignsList: ArrayList<Campaign>? = null
    internal var campaignsErrorOrNull: Boolean? = false

    private var insideAdCallback: InsideAdCallback? = null
    private var requestCampaignExecutor: ScheduledExecutorService? = null

    /**
     * How long a LOCAL_VIDEO ad may spend loading before it is abandoned, in milliseconds.
     *
     * MediaPlayer.prepareAsync() will wait forever on a creative that is too large or too high a
     * bitrate to stream, leaving the ad slot empty with no error raised. When this elapses the ad
     * reports an error, which lets the configured fallback ad run instead.
     *
     * Set to 0 to disable. Default 15s, matching the Google IMA video load timeout.
     */
    var localVideoLoadTimeoutMillis: Long = 15_000L

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

    fun areTargetingFiltersEmpty(): Boolean = targetingFilters.isEmpty()

    // ---- dedicated slot callbacks ------------------------------------------------------------

    /** Registers the callback that receives events for [viewType]. */
    fun setAdCallback(viewType: ViewType, callback: InsideAdCallback) {
        slotCallbacks[viewType] = callback
    }

    fun getAdCallback(viewType: ViewType): InsideAdCallback? = slotCallbacks[viewType]

    /**
     * Removes the callback for [viewType]. Call this from onDestroy()/onStop() - the callback
     * usually captures an Activity, so holding it leaks the screen.
     */
    fun removeAdCallback(viewType: ViewType) {
        Log.i(LOG_TAG, "removeAdCallback: ${viewType.value}")
        slotCallbacks.remove(viewType)
    }

    internal fun setActiveSlotView(viewType: ViewType, view: InsideAdView) {
        activeSlotViews[viewType] = view
    }

    /** Clears [view] from the registry only if it is still the view registered for that slot. */
    internal fun clearActiveSlotView(viewType: ViewType?, view: InsideAdView) {
        val slot = viewType ?: return
        if (activeSlotViews[slot] === view) {
            activeSlotViews.remove(slot)
        }
    }

    /**
     * Cancels any in-flight request for [viewType]. Safe to call when nothing is active.
     *
     * Only preroll restores the saved global parameters - the multiview slots never swap them,
     * so there is nothing for them to put back.
     */
    fun cancelAdRequest(viewType: ViewType) {
        Log.i(LOG_TAG, "cancelAdRequest: ${viewType.value}")
        activeSlotViews.remove(viewType)?.cancelAdRequest()
        slotCallbacks.remove(viewType)

        if (viewType == ViewType.PREROLL && isPrerollMode) {
            isAdMuted = savedIsAdMuted
            targetingFilters = savedTargetingFilters
            isPrerollMode = false
            savedIsAdMuted = null
            savedTargetingFilters = null
        }
    }

    /** Cancels every dedicated slot request. Convenient for onDestroy(). */
    fun cancelAllDedicatedAdRequests() {
        ViewType.values().forEach { cancelAdRequest(it) }
    }

    // ---- multiview convenience -----------------------------------------------------------------

    /** Requests an ad for the multiview canvas (the player grid). */
    @JvmOverloads
    fun requestMultiviewCanvasAd(
        adContainer: InsideAdView,
        screen: String = "",
        isAdMuted: Boolean? = true,
        targetingFilters: TargetingFilters? = null,
        callback: InsideAdCallback? = null
    ) = requestSlotAd(
        ViewType.MULTIVIEW_CANVAS, adContainer, screen, isAdMuted, targetingFilters, callback
    )

    /** Requests an ad for the multiview right bar (the streams selector). */
    @JvmOverloads
    fun requestMultiviewRightBarAd(
        adContainer: InsideAdView,
        screen: String = "",
        isAdMuted: Boolean? = true,
        targetingFilters: TargetingFilters? = null,
        callback: InsideAdCallback? = null
    ) = requestSlotAd(
        ViewType.MULTIVIEW_RIGHT_BAR, adContainer, screen, isAdMuted, targetingFilters, callback
    )

    @JvmOverloads
    fun requestSlotAd(
        viewType: ViewType,
        adContainer: InsideAdView,
        screen: String = "",
        isAdMuted: Boolean? = true,
        targetingFilters: TargetingFilters? = null,
        callback: InsideAdCallback? = null
    ) {
        Log.i(LOG_TAG, "requestSlotAd - viewType: ${viewType.value}, screen: $screen")
        callback?.let { slotCallbacks[viewType] = it }
        activeSlotViews[viewType] = adContainer
        adContainer.requestAdForSlot(viewType, screen, isAdMuted, targetingFilters, callback)
    }

    // ---- preroll: unchanged public surface, delegating to the slot registry --------------------

    fun setPrerollAdCallback(callback: InsideAdCallback) =
        setAdCallback(ViewType.PREROLL, callback)

    fun getPrerollAdCallback(): InsideAdCallback? = getAdCallback(ViewType.PREROLL)

    /**
     * Removes the preroll ad callback completely.
     * Call this when you no longer want to receive preroll ad events.
     * This should typically be called in Activity/Fragment onDestroy() or onStop().
     */
    fun removePrerollAdCallback() = removeAdCallback(ViewType.PREROLL)

    /**
     * Cancels any ongoing preroll ad request.
     * Stops pending handlers, clears callbacks, and cleans up resources.
     * Safe to call even if no preroll ad is active.
     */
    fun cancelPrerollAdRequest() = cancelAdRequest(ViewType.PREROLL)

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
        activeSlotViews[ViewType.PREROLL] = adContainer
        adContainer.requestPrerollAd(screen, isAdMuted, targetingFilters)
    }

}