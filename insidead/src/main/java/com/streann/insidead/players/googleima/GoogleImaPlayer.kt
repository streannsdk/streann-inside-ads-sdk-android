package com.streann.insidead.players.googleima

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.VideoView
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventType
import com.google.ads.interactivemedia.v3.api.AdsLoader
import com.google.ads.interactivemedia.v3.api.AdsManager
import com.google.ads.interactivemedia.v3.api.FriendlyObstructionPurpose
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import com.streann.insidead.R
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.callbacks.InsideAdProgressCallback
import com.streann.insidead.models.InsideAd
import com.streann.insidead.utils.InsideAdHelper

@SuppressLint("ViewConstructor")
class GoogleImaPlayer constructor(
    context: Context,
    callback: InsideAdProgressCallback
) :
    FrameLayout(context) {

    private val LOGTAG = "InsideAdSdk"

    private var sdkFactory: ImaSdkFactory? = null
    private var adsLoader: AdsLoader? = null
    private var adsManager: AdsManager? = null

    private var videoPlayer: VideoView? = null
    private var videoAdPlayerAdapter: VideoAdPlayerAdapter? = null
    private var videoPlayerVolumeButton: FrameLayout? = null

    private var insideAdCallback: InsideAdCallback? = null
    private var insideAdProgressCallback: InsideAdProgressCallback? = callback

    init {
        init()
    }

    private fun init() {
        LayoutInflater.from(context).inflate(R.layout.google_ima_player, this)

        videoPlayer = findViewById(R.id.videoView)
        val videoPlayerContainer = findViewById<ViewGroup>(R.id.videoPlayerContainer)

        val isLandscape =
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        setupVideoViewSize(videoPlayerContainer, isLandscape)

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
        adsLoader = sdkFactory!!.createAdsLoader(context, settings, adDisplayContainer)

        adsLoader!!.addAdErrorListener { adErrorEvent ->
            Log.i(LOGTAG, "Ad Error: " + adErrorEvent.error.message)
            insideAdCallback?.insideAdError(adErrorEvent.error.message)
            insideAdProgressCallback?.insideAdError()
        }

        adsLoader!!.addAdsLoadedListener { adsManagerLoadedEvent ->
            adsManager = adsManagerLoadedEvent.adsManager

            adsManager?.addAdErrorListener { adErrorEvent ->
                Log.e(LOGTAG, "Ad Error: " + adErrorEvent.error.message)
                insideAdCallback?.insideAdError(adErrorEvent.error.message)
                insideAdProgressCallback?.insideAdError()

                val universalAdIds: String =
                    adsManager?.currentAd?.universalAdIds.contentToString()
                Log.i(
                    LOGTAG,
                    "Discarding the current ad break with universal ad Ids: $universalAdIds"
                )
                adsManager?.discardAdBreak()
            }

            adsManager?.addAdEventListener { adEvent ->
                if (adEvent.type != AdEventType.AD_PROGRESS) {
                    Log.i(LOGTAG, "Event: " + adEvent.type)
                }

                when (adEvent.type) {
                    AdEventType.LOADED ->
                        adsManager?.start()
                    AdEventType.ALL_ADS_COMPLETED -> {
                        adsManager?.destroy()
                        adsManager = null
                    }
                    AdEventType.SKIPPED -> {
                        insideAdCallback?.insideAdSkipped()
                    }
                    AdEventType.CLICKED -> {
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
        Log.i(LOGTAG, "adUrl: $adTagUrl")
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
                insideAdCallback?.insideAdStop()
                insideAdProgressCallback?.insideAdStopped()
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

    fun playAd(insideAd: InsideAd, listener: InsideAdCallback) {
        insideAdCallback = listener
        val url = InsideAdHelper.populateVASTURL(context, insideAd)
        url?.let { requestAds(it) }
    }

    fun stopAd() {
        videoAdPlayerAdapter?.stopAdPlaying()
    }

    private fun setupVideoViewSize(videoPlayerContainer: ViewGroup?, isLandscape: Boolean) {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val aspectRatio = 9.0 / 16.0

        val layoutParams = videoPlayerContainer?.layoutParams as LinearLayout.LayoutParams

        if (isLandscape) {
            val videoWidth = screenWidth / 2
            val videoHeight = (videoWidth * aspectRatio).toInt()

            layoutParams.width = videoWidth
            layoutParams.height = videoHeight
        } else {
            val videoHeight = (screenWidth * aspectRatio).toInt()

            layoutParams.width = screenWidth
            layoutParams.height = videoHeight
        }

        videoPlayerContainer.layoutParams = layoutParams
    }

}