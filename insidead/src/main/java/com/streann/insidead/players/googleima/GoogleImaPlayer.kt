package com.streann.insidead.players.googleima

import com.streann.insidead.models.AdRequestContext
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventType
import com.google.ads.interactivemedia.v3.api.AdsLoader
import com.google.ads.interactivemedia.v3.api.AdsManager
import com.google.ads.interactivemedia.v3.api.FriendlyObstructionPurpose
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import com.streann.insidead.InsideAdSdk
import com.streann.insidead.R
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.callbacks.InsideAdProgressCallback
import com.streann.insidead.models.InsideAd
import com.streann.insidead.utils.Helper
import com.streann.insidead.utils.MacrosHelper

@SuppressLint("ViewConstructor")
class GoogleImaPlayer(
    context: Context,
    callback: InsideAdProgressCallback
) :
    FrameLayout(context) {

    private var sdkFactory: ImaSdkFactory? = null
    private var adsLoader: AdsLoader? = null
    private var adsManager: AdsManager? = null

    private var videoPlayer: VideoView? = null
    private var videoAdPlayerAdapter: VideoAdPlayerAdapter? = null

    /**
     * Guards against reporting the same ad as finished twice, now that both the natural end and a
     * skip route through [notifyAdFinished]. Reset when the next ad starts.
     */
    private var adFinishedNotified = false
    private var videoPlayerVolumeButton: FrameLayout? = null

    private var insideAdCallback: InsideAdCallback? = null
    private var insideAdProgressCallback: InsideAdProgressCallback? = callback

    /**
     * Per-request state for the ad this player is showing. Set by InsideAdView immediately before
     * playback starts. Reads fall back to the deprecated InsideAdSdk globals when it is null, so
     * any path that does not set a context behaves exactly as it did before.
     */
    internal var requestContext: AdRequestContext? = null

    private val ctxResizeMode get() = requestContext?.resizeMode ?: InsideAdSdk.resizeMode

    init {
        init()
    }

    private fun init() {
        LayoutInflater.from(context).inflate(R.layout.google_ima_player, this)

        videoPlayer = findViewById(R.id.videoView)
        val videoPlayerContainer = findViewById<ViewGroup>(R.id.videoPlayerContainer)

        // Size the parent container (this GoogleImaPlayer) instead of child videoPlayerContainer
        // This ensures proper centering in portrait/landscape
        post {
            Helper.setViewSize(this, resources, ctxResizeMode)
            requestLayout()
        }

        videoPlayerVolumeButton = findViewById(R.id.adVolumeLayout)
        videoAdPlayerAdapter = VideoAdPlayerAdapter(videoPlayer!!, videoPlayerVolumeButton!!)

        setImaAdsCallback()

        val adDisplayContainer = ImaSdkFactory.createAdDisplayContainer(
            videoPlayerContainer,
            videoAdPlayerAdapter!!
        )

        val myTransparentTapOverlay = findViewById<ViewGroup>(R.id.overlay)

        val overlayObstruction = ImaSdkFactory.getInstance().createFriendlyObstruction(
            myTransparentTapOverlay,
            FriendlyObstructionPurpose.NOT_VISIBLE,
            "This overlay is transparent"
        )

        val volumeButtonObstruction = ImaSdkFactory.getInstance().createFriendlyObstruction(
            videoPlayerVolumeButton!!,
            FriendlyObstructionPurpose.VIDEO_CONTROLS,
            "This is the video player volume button"
        )

        overlayObstruction.view.translationZ = 100f

        adDisplayContainer.registerFriendlyObstruction(overlayObstruction)
        adDisplayContainer.registerFriendlyObstruction(volumeButtonObstruction)

        sdkFactory = ImaSdkFactory.getInstance()
        val settings = sdkFactory!!.createImaSdkSettings()
        // Must be the real Activity context.
        //
        // Wrapping it in a ContextWrapper to catch ActivityNotFoundException - IMA opens
        // clickthroughs itself and does not catch that, so a creative with an unresolvable
        // intent:// deep link can crash the host - breaks clickthroughs outright: app-install ads
        // do nothing on tap and the skip countdown stalls, because IMA needs the Activity itself
        // to open one. Tried in 1.0.31 and reverted; do not reintroduce it.
        //
        // A creative whose clickthrough resolves to nothing is a trafficking problem and is
        // better fixed at the source.
        adsLoader = sdkFactory!!.createAdsLoader(context, settings, adDisplayContainer)

        adsLoader!!.addAdErrorListener { adErrorEvent ->
            Log.i(InsideAdSdk.LOG_TAG, "Ad Error: " + adErrorEvent.error.message)
            insideAdCallback?.insideAdError(adErrorEvent.error.message)
            insideAdProgressCallback?.insideAdError()
        }

        adsLoader!!.addAdsLoadedListener { adsManagerLoadedEvent ->
            adsManager = adsManagerLoadedEvent.adsManager

            InsideAdSdk.debugLog("GoogleIma", "Ads loaded - AdsManager created")

            adsManager?.addAdErrorListener { adErrorEvent ->
                Log.e(InsideAdSdk.LOG_TAG, "Ad Error: " + adErrorEvent.error.message)
                insideAdCallback?.insideAdError(adErrorEvent.error.message)
                insideAdProgressCallback?.insideAdError()

                val universalAdIds: String =
                    adsManager?.currentAd?.universalAdIds.contentToString()
                Log.i(
                    InsideAdSdk.LOG_TAG,
                    "Discarding the current ad break with universal ad Ids: $universalAdIds"
                )
                adsManager?.discardAdBreak()
            }

            adsManager?.addAdEventListener { adEvent ->
                if (adEvent.type != AdEventType.AD_PROGRESS) {
                    Log.i(InsideAdSdk.LOG_TAG, "Event: " + adEvent.type)
                }

                // Debug logging for skip button related events
                when (adEvent.type) {
                    AdEventType.LOADED -> {
                        adsManager?.start()
                        logVastAdDetails("LOADED")
                    }

                    AdEventType.STARTED -> {
                        logVastAdDetails("STARTED")
                    }

                    AdEventType.ALL_ADS_COMPLETED -> {
                        InsideAdSdk.debugLog("GoogleIma", "All ads completed")
                        adsManager?.destroy()
                        adsManager = null
                    }

                    AdEventType.SKIPPABLE_STATE_CHANGED -> {
                        val currentAd = adsManager?.currentAd
                        val isSkippable = currentAd?.isSkippable ?: false
                        val skipTimeOffset = currentAd?.skipTimeOffset ?: 0.0

                        InsideAdSdk.logSkipButtonState(
                            event = "SKIPPABLE_STATE_CHANGED",
                            adName = currentAd?.title ?: "Unknown",
                            adType = "VAST",
                            isSkippable = isSkippable,
                            skipOffsetSeconds = skipTimeOffset.toInt(),
                            adDuration = currentAd?.duration?.toFloat(),
                            additionalInfo = mapOf(
                                "Skip Button Should Show" to isSkippable,
                                "Ad ID" to (currentAd?.adId ?: "Unknown"),
                                "Ad System" to (currentAd?.adSystem ?: "Unknown")
                            )
                        )
                    }

                    AdEventType.SKIPPED -> {
                        val currentAd = adsManager?.currentAd
                        InsideAdSdk.logSkipButtonState(
                            event = "SKIPPED (User Clicked Skip)",
                            adName = currentAd?.title ?: "Unknown",
                            adType = "VAST",
                            additionalInfo = mapOf(
                                "Skip Action" to "User clicked skip button"
                            )
                        )
                        insideAdCallback?.insideAdSkipped()

                        // Skipping ends the ad, but IMA does not follow it with onEnded and
                        // stopAdPlaying() is a no-op by this point because the VideoView has
                        // already stopped. Without this nothing tells the host the ad is over, so
                        // the ad view stays on screen for good and the slot is never reused.
                        notifyAdFinished()
                    }

                    AdEventType.CLICKED -> {
                        InsideAdSdk.debugLog("GoogleIma", "Ad clicked")
                        insideAdCallback?.insideAdClicked()
                    }

                    else -> {}
                }
            }

            val adsRenderingSettings = ImaSdkFactory.getInstance().createAdsRenderingSettings()
            adsRenderingSettings.setLoadVideoTimeout(15000)
            adsManager?.init(adsRenderingSettings)
        }
    }

    private fun requestAds(adTagUrl: String) {
        Log.i(InsideAdSdk.LOG_TAG, "adUrl: $adTagUrl")
        val request = sdkFactory!!.createAdsRequest()
        request.adTagUrl = adTagUrl
        adsLoader!!.requestAds(request)
    }

    private fun setImaAdsCallback() {
        videoAdPlayerAdapter?.addCallback(object : VideoAdPlayer.VideoAdPlayerCallback {
            override fun onAdProgress(p0: AdMediaInfo, p1: VideoProgressUpdate) {
            }

            override fun onBuffering(p0: AdMediaInfo) {
            }

            override fun onContentComplete() {
            }

            override fun onEnded(p0: AdMediaInfo) {
                notifyAdFinished()
            }

            override fun onError(p0: AdMediaInfo) {
                insideAdCallback?.insideAdError("Error while playing AD.")
                insideAdProgressCallback?.insideAdError()
            }

            override fun onLoaded(p0: AdMediaInfo) {
                insideAdCallback?.insideAdLoaded()
            }

            override fun onPause(p0: AdMediaInfo) {
            }

            override fun onPlay(p0: AdMediaInfo) {
                insideAdCallback?.insideAdPlay()
            }

            override fun onResume(p0: AdMediaInfo) {
            }

            override fun onVolumeChanged(p0: AdMediaInfo, p1: Int) {
                insideAdCallback?.insideAdVolumeChanged(p1)
            }
        })
    }

    /**
     * Reports the ad as finished, exactly once, however it ended - played out or skipped.
     */
    private fun notifyAdFinished() {
        if (adFinishedNotified) return
        adFinishedNotified = true

        Log.i(InsideAdSdk.LOG_TAG, "VAST ad finished")
        insideAdCallback?.insideAdStop()
        insideAdProgressCallback?.insideAdStopped()
    }

    /** Mutes or unmutes the VAST ad while it is playing. */
    internal fun setMuted(muted: Boolean) {
        videoAdPlayerAdapter?.setMuted(muted)
    }

    fun playAd(insideAd: InsideAd, listener: InsideAdCallback) {
        insideAdCallback = listener
        adFinishedNotified = false

        // The adapter reads the mute state. Without this it falls back to the global
        // InsideAdSdk.isAdMuted, which whichever slot ran showAd() last has already overwritten -
        // so two concurrent slots would share one mute state.
        videoAdPlayerAdapter?.requestContext = requestContext

        // init() sized this view at construction time, before any request existed and before the
        // view was attached, so ctxResizeMode was still null and a MATCH_CONTAINER parent still
        // measured 0. Re-size now that both are known.
        post {
            Helper.setViewSize(this, resources, ctxResizeMode)
            requestLayout()
        }

        val url = MacrosHelper.populateVASTURL(context, insideAd, requestContext)
        url?.let { requestAds(it) }
    }

    fun stopAd() {
        videoAdPlayerAdapter?.stopAdPlaying()
    }

    fun release() {
        videoAdPlayerAdapter?.stopAdPlaying()
        videoAdPlayerAdapter?.release()
        adsManager?.destroy()
        adsManager = null
        adsLoader?.contentComplete()
        insideAdCallback = null
        insideAdProgressCallback = null
    }

    /**
     * Logs detailed VAST ad information for debugging skip button behavior.
     * This helps identify why skip button may not appear.
     */
    private fun logVastAdDetails(event: String) {
        if (!InsideAdSdk.debugMode) return

        val currentAd = adsManager?.currentAd
        if (currentAd == null) {
            InsideAdSdk.debugLog("GoogleIma", "$event - No current ad available")
            return
        }

        val isSkippable = currentAd.isSkippable
        val skipTimeOffset = currentAd.skipTimeOffset
        val duration = currentAd.duration
        val adId = currentAd.adId
        val title = currentAd.title
        val adSystem = currentAd.adSystem

        InsideAdSdk.logSkipButtonState(
            event = "$event (VAST Ad Details)",
            adName = title ?: "Unknown",
            adType = "VAST",
            isSkippable = isSkippable,
            skipOffsetSeconds = skipTimeOffset.toInt(),
            adDuration = duration.toFloat(),
            additionalInfo = mapOf(
                "Ad ID" to (adId ?: "Unknown"),
                "Ad System" to (adSystem ?: "Unknown"),
                "Is Linear" to currentAd.isLinear,
                "Ad Pod Info" to "Pod ${currentAd.adPodInfo.podIndex + 1}/${currentAd.adPodInfo.totalAds}",
                "Skip Button Note" to if (isSkippable) {
                    "Skip button will appear after $skipTimeOffset seconds"
                } else {
                    "This ad is NOT skippable (no skipoffset in VAST)"
                }
            )
        )
    }

}