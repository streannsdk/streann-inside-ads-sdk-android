package com.streann.insidead

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.callbacks.InsideAdProgressCallback
import com.streann.insidead.models.TargetingFilters
import com.streann.insidead.models.InsideAd
import com.streann.insidead.players.bannerads.BannerAdsPlayer
import com.streann.insidead.players.googleima.GoogleImaPlayer
import com.streann.insidead.players.insidead.InsideAdPlayer
import com.streann.insidead.players.nativeads.NativeAdsPlayer
import com.streann.insidead.utils.CampaignsFilterUtil
import com.streann.insidead.utils.Helper
import com.streann.insidead.utils.SharedPreferencesHelper
import com.streann.insidead.utils.enums.ViewType
import com.streann.insidead.utils.constants.SharedPrefKeys
import com.streann.insidead.utils.enums.AdType
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class InsideAdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle), InsideAdProgressCallback {

    /**
     * Defines how the video player should resize to fill the available space.
     */
    enum class ResizeMode {
        /**
         * Maintain aspect ratio with letterboxing if needed (default behavior for split-screen).
         * In landscape, video width is half of screen width.
         */
        FIT,

        /**
         * Fill entire screen width while maintaining aspect ratio.
         * Recommended for fullscreen ads in landscape orientation.
         */
        FILL,

        /**
         * Use full screen width, adjust height to maintain aspect ratio.
         * Good for landscape fullscreen ads.
         */
        FIXED_WIDTH,

        /**
         * Use full screen height, adjust width to maintain aspect ratio.
         * Good for portrait fullscreen ads.
         */
        FIXED_HEIGHT
    }

    private var mInsideAdPlayer: InsideAdPlayer? = null
    private var mGoogleImaPlayer: GoogleImaPlayer? = null
    private var mBannerAdsPlayer: BannerAdsPlayer? = null
    private var mNativeAdsPlayer: NativeAdsPlayer? = null

    private var insideAd: InsideAd? = null
    private var fallbackAd: InsideAd? = null
    private var insideAdCallback: InsideAdCallback? = null

    private var populateSdkExecutor: ExecutorService? = null
    private var adIntervalHandler: Handler? = null
    private var showAdHandler: Handler? = null

    private var screen: String = ""
    private var apiKey: String = ""
    private var baseUrl: String = ""
    private var scale: Float = 0f

    private var retryCount = 0
    private val maxRetries = 3
    private val retryDelayMillis = 3000L
    private var retryRequestHandler: Handler? = null

    // Store the resize mode preference
    private var resizeMode: ResizeMode = ResizeMode.FIT

    // Track current orientation to detect changes
    private var currentOrientation: Int = 0

    // Store per-instance ad parameters to prevent race conditions with global state
    private var instanceIsAdMuted: Boolean? = true
    private var instanceTargetingFilters: TargetingFilters? = null

    // Track whether this instance is handling a preroll ad (instance-level, not global)
    private var isPrerollInstance: Boolean = false

    init {
        init()
    }

    private fun init() {
        // Initialize orientation tracking
        currentOrientation = resources.configuration.orientation

        initializePlayers()

        scale = resources.displayMetrics.density
        populateSdkInfo(context)

        MobileAds.initialize(context) { }
    }

    private fun initializePlayers() {
        createGoogleImaView()
        mInsideAdPlayer = InsideAdPlayer(context, this)
        // Add with centered layout params so player is centered in portrait/landscape
        val playerParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        playerParams.gravity = android.view.Gravity.CENTER
        addView(mInsideAdPlayer, playerParams)
        mBannerAdsPlayer = BannerAdsPlayer(context, this)
        addView(mBannerAdsPlayer)
        mNativeAdsPlayer = NativeAdsPlayer(context, this)
        addView(mNativeAdsPlayer)
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
        isAdMuted: Boolean? = true,
        targetingFilters: TargetingFilters? = null
    ) {
        Log.i(InsideAdSdk.LOG_TAG, "requestAd")
        retryRequestHandler = Handler(Looper.getMainLooper())

        // Store in instance variables to prevent race conditions
        this.instanceIsAdMuted = isAdMuted
        this.instanceTargetingFilters = targetingFilters
        this.insideAdCallback = InsideAdSdk.getInsideAdCallback()
        this.screen = screen

        InsideAdSdk.showAdForReels = screen == "Reels"

        if (TextUtils.isEmpty(apiKey) || TextUtils.isEmpty(baseUrl)) {
            val errorMsg =
                "Api Key and Base Url are required. Please implement the initializeSdk method."
            Log.e(InsideAdSdk.LOG_TAG, errorMsg)
            insideAdCallback?.insideAdError(errorMsg)
            return
        }

        getInsideAdRetry()
    }

    internal fun requestPrerollAd(
        screen: String,
        isAdMuted: Boolean? = true,
        targetingFilters: TargetingFilters? = null
    ) {
        Log.i(InsideAdSdk.LOG_TAG, "requestPrerollAd")
        retryRequestHandler = Handler(Looper.getMainLooper())

        // Store in instance variables to prevent race conditions
        this.instanceIsAdMuted = isAdMuted
        this.instanceTargetingFilters = targetingFilters

        // Save current regular ad parameters and temporarily replace with preroll values
        InsideAdSdk.savedIsAdMuted = InsideAdSdk.isAdMuted
        InsideAdSdk.savedTargetingFilters = InsideAdSdk.targetingFilters

        // Write preroll values to global state before filtering runs
        InsideAdSdk.isAdMuted = isAdMuted
        InsideAdSdk.targetingFilters = targetingFilters
        InsideAdSdk.isPrerollMode = true
        this.isPrerollInstance = true
        this.insideAdCallback = InsideAdSdk.getPrerollAdCallback()
        this.screen = screen

        if (TextUtils.isEmpty(apiKey) || TextUtils.isEmpty(baseUrl)) {
            val errorMsg = "Api Key and Base Url are required. Please implement the initializeSdk method."
            Log.e(InsideAdSdk.LOG_TAG, errorMsg)
            insideAdCallback?.insideAdError(errorMsg)
            restoreRegularAdParameters()
            return
        }

        getInsideAdRetry()
    }

    private fun restoreRegularAdParameters() {
        // Restore original regular ad parameters (default to muted/true if saved value was null)
        InsideAdSdk.isAdMuted = InsideAdSdk.savedIsAdMuted ?: true
        InsideAdSdk.targetingFilters = InsideAdSdk.savedTargetingFilters
        InsideAdSdk.isPrerollMode = false

        // Clear saved values
        InsideAdSdk.savedIsAdMuted = null
        InsideAdSdk.savedTargetingFilters = null
    }

    private fun getInsideAdRetry() {
        Log.i(InsideAdSdk.LOG_TAG, "getInsideAdRetry")
        if (retryCount < maxRetries) {
            if (InsideAdSdk.campaignsList == null && InsideAdSdk.campaignsErrorOrNull == false) {
                retryCount++
                retryRequestHandler?.postDelayed({
                    getInsideAdRetry()
                }, retryDelayMillis)
            } else if (InsideAdSdk.campaignsList != null) {
                getInsideAd(screen, insideAdCallback)
            } else {
                val errorMsg = "Failed to fetch campaigns from server"
                insideAdCallback?.insideAdError(errorMsg)
                if (isPrerollInstance) {
                    restoreRegularAdParameters()
                    // Clear active preroll ad view reference
                    if (InsideAdSdk.activePrerollAdView == this) {
                        InsideAdSdk.activePrerollAdView = null
                    }
                }
            }
        } else {
            val errorMsg = "Campaign list not available after $maxRetries retries"
            insideAdCallback?.insideAdError(errorMsg)
            if (isPrerollInstance) {
                restoreRegularAdParameters()
                // Clear active preroll ad view reference
                if (InsideAdSdk.activePrerollAdView == this) {
                    InsideAdSdk.activePrerollAdView = null
                }
            }
        }
    }

    private fun getInsideAd(
        screen: String,
        insideAdCallback: InsideAdCallback?
    ) {
        Log.i(InsideAdSdk.LOG_TAG, "getInsideAd")
        retryRequestHandler?.removeCallbacksAndMessages(null)
        retryRequestHandler = null

        val viewType = if (isPrerollInstance) ViewType.PREROLL.value else null
        insideAd = CampaignsFilterUtil.getInsideAd(
            InsideAdSdk.campaignsList,
            screen,
            viewType
        )

        insideAdCallback?.let { callback ->
            insideAd?.let { ad ->
                callback.insideAdReceived(ad)
                fallbackAd = insideAd?.fallback
                showAd(ad, callback)
            } ?: run {
                if (isPrerollInstance) {
                    val errorMsg = "No PREROLL ad available for the specified criteria"
                    Log.w(InsideAdSdk.LOG_TAG, errorMsg)
                    callback.insideAdError(errorMsg)
                    restoreRegularAdParameters()
                    // Clear active preroll ad view reference
                    if (InsideAdSdk.activePrerollAdView == this) {
                        InsideAdSdk.activePrerollAdView = null
                    }
                }
            }
        }
    }

    private fun showAd(
        insideAd: InsideAd,
        insideAdCallback: InsideAdCallback
    ) {
        Log.i(InsideAdSdk.LOG_TAG, "showAd")
        adIntervalHandler?.removeCallbacksAndMessages(null)
        adIntervalHandler = null

        // Write instance values to global state immediately before player reads them
        // This minimizes the race window to just milliseconds
        InsideAdSdk.isAdMuted = this.instanceIsAdMuted
        InsideAdSdk.targetingFilters = this.instanceTargetingFilters

        showAdHandler = Handler(Looper.getMainLooper())
        val delayMillis = if (isPrerollInstance || InsideAdSdk.showAdForReels) {
            0
        } else {
            InsideAdSdk.startAfterSeconds ?: 0
        }

        // Log which ad type is being shown and its skip button support
        val skipButtonSupport = when (insideAd.adType) {
            AdType.VAST.value -> "YES - Google IMA SDK controls skip button based on VAST XML skipoffset"
            AdType.LOCAL_VIDEO.value, AdType.LOCAL_IMAGE.value -> "NO - Only close button available"
            AdType.BANNER.value, AdType.FULLSCREEN_NATIVE.value -> "N/A - Not applicable for this ad type"
            else -> "UNKNOWN"
        }

        InsideAdSdk.debugLog(
            "InsideAdView",
            "Showing ${insideAd.adType} ad: ${insideAd.name} | Skip Button Support: $skipButtonSupport | Muted: ${this.instanceIsAdMuted}"
        )

        when (insideAd.adType) {
            AdType.VAST.value ->
                showAdHandler?.postDelayed({
                    showGoogleImaAd(
                        insideAd,
                        insideAdCallback
                    )
                }, delayMillis)

            AdType.LOCAL_VIDEO.value ->
                showAdHandler?.postDelayed({
                    showLocalVideoAd(insideAd, insideAdCallback)
                }, delayMillis)

            AdType.LOCAL_IMAGE.value -> {
                insideAd.url?.let { url ->
                    Helper.getBitmapFromURL(url, resources) { bitmap ->
                        showAdHandler?.postDelayed({
                            showLocalImageAd(bitmap, insideAd, insideAdCallback)
                        }, delayMillis)
                    }
                }
            }

            AdType.BANNER.value ->
                showAdHandler?.postDelayed({
                    showBannerAd(insideAd, insideAdCallback)
                }, delayMillis)

            AdType.FULLSCREEN_NATIVE.value ->
                showAdHandler?.postDelayed({
                    showNativeAd(insideAd, insideAdCallback)
                }, delayMillis)
        }
    }

    private fun setPlayerVisibility(
        imaPlayerVisibility: Int,
        insideAdPlayerVisibility: Int,
        bannerAdPlayerVisibility: Int,
        nativeAdPlayerVisibility: Int,
    ) {
        mGoogleImaPlayer?.visibility = imaPlayerVisibility
        mInsideAdPlayer?.visibility = insideAdPlayerVisibility
        mBannerAdsPlayer?.visibility = bannerAdPlayerVisibility
        mNativeAdsPlayer?.visibility = nativeAdPlayerVisibility
    }

    private fun showGoogleImaAd(insideAd: InsideAd, insideAdCallback: InsideAdCallback) {
        createGoogleImaView()
        setPlayerVisibility(VISIBLE, GONE, GONE, GONE)
        mGoogleImaPlayer?.playAd(insideAd, insideAdCallback)
    }

    private fun createGoogleImaView() {
        if (mGoogleImaPlayer == null) {
            mGoogleImaPlayer = GoogleImaPlayer(context, this)
            // Add with centered layout params so player is centered in portrait/landscape
            val playerParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            playerParams.gravity = android.view.Gravity.CENTER
            addView(mGoogleImaPlayer, playerParams)
            // Sizing is handled in GoogleImaPlayer.init()
        }
    }

    private fun removeGoogleImaView() {
        if (mGoogleImaPlayer != null) {
            mGoogleImaPlayer?.release()
            removeView(mGoogleImaPlayer)
            mGoogleImaPlayer = null
        }
    }

    private fun showLocalVideoAd(insideAd: InsideAd, insideAdCallback: InsideAdCallback) {
        setPlayerVisibility(GONE, VISIBLE, GONE, GONE)
        mInsideAdPlayer?.playAd(null, insideAd, insideAdCallback)
    }

    private fun showLocalImageAd(
        bitmap: Bitmap?,
        insideAd: InsideAd,
        insideAdCallback: InsideAdCallback
    ) {
        bitmap?.let {
            Log.i(InsideAdSdk.LOG_TAG, "loadAd")
            insideAdCallback.insideAdLoaded()
            setPlayerVisibility(GONE, VISIBLE, GONE, GONE)
            mInsideAdPlayer?.playAd(bitmap, insideAd, insideAdCallback)
        } ?: run {
            insideAdCallback.insideAdError("Error while getting AD.")
            insideAdError()
        }
    }

    private fun showBannerAd(insideAd: InsideAd, insideAdCallback: InsideAdCallback) {
        setPlayerVisibility(GONE, GONE, VISIBLE, GONE)
        mBannerAdsPlayer?.playAd(insideAd, insideAdCallback)
    }

    private fun showNativeAd(insideAd: InsideAd, insideAdCallback: InsideAdCallback) {
        setPlayerVisibility(GONE, GONE, GONE, VISIBLE)
        mNativeAdsPlayer?.playAd(insideAd, insideAdCallback)
    }

    fun stopAd() {
        insideAd?.let {
            when (it.adType) {
                AdType.VAST.value -> mGoogleImaPlayer?.stopAd()
                AdType.LOCAL_VIDEO.value, AdType.LOCAL_IMAGE.value -> mInsideAdPlayer?.stopAd()
                AdType.BANNER.value -> mBannerAdsPlayer?.stopAd()
                AdType.FULLSCREEN_NATIVE.value -> mNativeAdsPlayer?.stopAd()
                else -> {}
            }
        }

        showAdHandler?.removeCallbacksAndMessages(null)
        showAdHandler = null
    }

    fun playAd() {
        if (insideAd?.adType == AdType.LOCAL_VIDEO.value) {
            mInsideAdPlayer?.startPlayingAd()
        } else if (fallbackAd?.adType == AdType.LOCAL_VIDEO.value) {
            mInsideAdPlayer?.startPlayingAd()
        }
    }

    /**
     * Cancels any ongoing ad request and clears all pending handlers.
     * Safe to call even if no ad request is active.
     * This should be called when the containing Activity/Fragment is destroyed.
     */
    fun cancelAdRequest() {
        Log.i(InsideAdSdk.LOG_TAG, "cancelAdRequest")

        // Cancel all pending handlers
        retryRequestHandler?.removeCallbacksAndMessages(null)
        retryRequestHandler = null

        showAdHandler?.removeCallbacksAndMessages(null)
        showAdHandler = null

        adIntervalHandler?.removeCallbacksAndMessages(null)
        adIntervalHandler = null

        // Release Google IMA resources (destroy adsManager, clear listeners/callbacks)
        mGoogleImaPlayer?.release()

        // Stop any other playing ad types
        insideAd?.let {
            when (it.adType) {
                AdType.LOCAL_VIDEO.value, AdType.LOCAL_IMAGE.value -> mInsideAdPlayer?.stopAd()
                AdType.BANNER.value -> mBannerAdsPlayer?.stopAd()
                AdType.FULLSCREEN_NATIVE.value -> mNativeAdsPlayer?.stopAd()
                else -> {}
            }
        }

        // Clear callback reference to prevent firing on destroyed activity
        insideAdCallback = null

        // Reset retry count
        retryCount = 0

        // Restore regular ad parameters if this is a preroll instance
        if (isPrerollInstance) {
            restoreRegularAdParameters()
        }

        // Clear active preroll ad view reference
        if (InsideAdSdk.activePrerollAdView == this) {
            InsideAdSdk.activePrerollAdView = null
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelAdRequest()
    }

    /**
     * Sets how the video player should resize to fill the available space.
     * This affects how the internal video player calculates its dimensions.
     *
     * @param mode The resize mode to use. Defaults to FIT.
     *
     * Common usage:
     * - ResizeMode.FIT - Default, maintains aspect ratio with letterboxing
     * - ResizeMode.FILL - Fills screen width, recommended for fullscreen landscape ads
     * - ResizeMode.FIXED_WIDTH - Uses full width, adjusts height
     * - ResizeMode.FIXED_HEIGHT - Uses full height, adjusts width
     *
     * Note: Call this BEFORE requestAd() or requestPrerollAd() for best results.
     * If called after ad is loaded, it will apply to the next ad.
     */
    fun setResizeMode(mode: ResizeMode) {
        Log.i(InsideAdSdk.LOG_TAG, "setResizeMode: $mode")
        this.resizeMode = mode
        InsideAdSdk.resizeMode = mode
    }

    /**
     * Gets the current resize mode.
     * @return The current ResizeMode
     */
    fun getResizeMode(): ResizeMode {
        return resizeMode
    }

    /**
     * Handles configuration changes, particularly orientation changes.
     * Automatically resizes the currently visible player to prevent letterboxing/pillarboxing.
     */
    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        newConfig?.let { config ->
            val newOrientation = config.orientation

            // Only handle actual orientation changes (ignore other config changes)
            if (newOrientation != currentOrientation) {
                val orientationName = if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    "LANDSCAPE"
                } else {
                    "PORTRAIT"
                }

                InsideAdSdk.debugLog(
                    "InsideAdView",
                    "Orientation changed to $orientationName - Recalculating dimensions"
                )

                currentOrientation = newOrientation
                handleOrientationChange()
            }
        }
    }

    /**
     * Handles orientation change by recalculating dimensions and resizing visible player.
     */
    private fun handleOrientationChange() {
        // Step 1: Recalculate global dimensions for VAST macros
        updateGlobalDimensions()

        // Step 2: Identify which player is currently visible and resize it
        post {
            when {
                mGoogleImaPlayer?.visibility == VISIBLE -> {
                    resizeGoogleImaPlayer()
                }
                mInsideAdPlayer?.visibility == VISIBLE -> {
                    resizeInsideAdPlayer()
                }
                mBannerAdsPlayer?.visibility == VISIBLE -> {
                    resizeBannerAdsPlayer()
                }
                mNativeAdsPlayer?.visibility == VISIBLE -> {
                    resizeNativeAdsPlayer()
                }
                else -> {
                    InsideAdSdk.debugLog(
                        "InsideAdView",
                        "No player visible during orientation change - dimensions updated for next ad"
                    )
                }
            }
        }
    }

    /**
     * Updates global player dimensions (used for VAST URL macros).
     */
    private fun updateGlobalDimensions() {
        // Wait for layout to complete, then recalculate dimensions
        post {
            val newWidth = (width / scale).toInt()
            val newHeight = (height / scale).toInt()

            val oldWidth = InsideAdSdk.playerWidth
            val oldHeight = InsideAdSdk.playerHeight

            InsideAdSdk.playerWidth = newWidth
            InsideAdSdk.playerHeight = newHeight

            InsideAdSdk.debugLog(
                "InsideAdView",
                "Global dimensions updated: ${oldWidth}x${oldHeight} -> ${newWidth}x${newHeight}"
            )
        }
    }

    /**
     * Resizes GoogleImaPlayer's videoPlayerContainer for new orientation.
     */
    private fun resizeGoogleImaPlayer() {
        mGoogleImaPlayer?.let { player ->
            // Size the GoogleImaPlayer itself (FrameLayout) for proper centering
            Helper.setViewSize(player, resources, InsideAdSdk.resizeMode)
            player.requestLayout()

            InsideAdSdk.debugLog(
                "InsideAdView",
                "GoogleImaPlayer resized - dimensions updated"
            )
        }
    }

    /**
     * Resizes InsideAdPlayer for new orientation.
     */
    private fun resizeInsideAdPlayer() {
        mInsideAdPlayer?.let { player ->
            // InsideAdPlayer sizes itself (this), not a child view
            Helper.setViewSize(player, resources, InsideAdSdk.resizeMode)
            player.requestLayout()

            InsideAdSdk.debugLog(
                "InsideAdView",
                "InsideAdPlayer resized - Player container dimensions updated"
            )

            // SurfaceView/ImageView inside has MATCH_PARENT and will automatically adjust
        }
    }

    /**
     * Handles BannerAdsPlayer orientation change.
     * AdManagerAdView handles its own sizing automatically.
     */
    private fun resizeBannerAdsPlayer() {
        // Banner ads typically handle their own sizing via AdSize
        // No explicit resize needed - AdManagerAdView handles orientation internally

        InsideAdSdk.debugLog(
            "InsideAdView",
            "BannerAdsPlayer - No resize needed (AdManagerAdView handles sizing)"
        )
    }

    /**
     * Handles NativeAdsPlayer orientation change.
     * Native ad views use MATCH_PARENT and adapt automatically.
     */
    private fun resizeNativeAdsPlayer() {
        // Native ads typically fill the entire InsideAdView
        // The NativeAdView uses MATCH_PARENT and adapts automatically

        mNativeAdsPlayer?.requestLayout()

        InsideAdSdk.debugLog(
            "InsideAdView",
            "NativeAdsPlayer - Requested layout refresh"
        )
    }

    override fun insideAdStopped() {
        Log.i(InsideAdSdk.LOG_TAG, "insideAdStopped")
        removeGoogleImaView()

        if (isPrerollInstance) {
            restoreRegularAdParameters()
            // Clear active preroll ad view reference
            if (InsideAdSdk.activePrerollAdView == this) {
                InsideAdSdk.activePrerollAdView = null
            }
            return
        }

        if (!InsideAdSdk.showAdForReels) {
            if (InsideAdSdk.intervalInMinutes != null && InsideAdSdk.intervalInMinutes!! > 0) {
                adIntervalHandler = Handler(Looper.getMainLooper())
                adIntervalHandler?.postDelayed({
                    getInsideAd(screen, insideAdCallback)
                }, InsideAdSdk.intervalInMinutes!!)
            }
        }
    }

    override fun insideAdError() {
        Log.i(InsideAdSdk.LOG_TAG, "insideAdError")
        insideAd = null

        if (isPrerollInstance) {
            Log.i(InsideAdSdk.LOG_TAG, "Preroll instance: skipping fallback ad")
            restoreRegularAdParameters()
            // Clear active preroll ad view reference
            if (InsideAdSdk.activePrerollAdView == this) {
                InsideAdSdk.activePrerollAdView = null
            }
            return
        }

        insideAdCallback?.let { callback ->
            fallbackAd?.let { fallbackAd ->
                Log.i(InsideAdSdk.LOG_TAG, "fallbackAd: $fallbackAd")
                fallbackAd.properties?.durationInSeconds?.let {
                    InsideAdSdk.durationInSeconds = Helper.getMillisFromSeconds(it.toLong())
                }
                showAd(fallbackAd, callback)
            }
        }
    }

}