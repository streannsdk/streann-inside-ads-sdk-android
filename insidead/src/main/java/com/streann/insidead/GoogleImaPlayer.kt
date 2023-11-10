package com.streann.insidead

import android.content.Context
import android.media.AudioManager
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import com.google.ads.interactivemedia.v3.api.*
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventType
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.models.GeoIp
import com.streann.insidead.models.InsideAd
import com.streann.insidead.utils.InsideAdHelper

class GoogleImaPlayer constructor(context: Context) :
    FrameLayout(context) {

    private val LOGTAG = "InsideAdSdk"

    private var sdkFactory: ImaSdkFactory? = null
    private var adsLoader: AdsLoader? = null
    private var adsManager: AdsManager? = null

    private var videoPlayer: VideoView? = null
    private var videoAdPlayerAdapter: VideoAdPlayerAdapter? = null
    private var videoPlayerVolumeButton: FrameLayout? = null

    private var insideAdListener: InsideAdCallback? = null

    init {
        init()
    }

    private fun init() {
        LayoutInflater.from(context).inflate(R.layout.google_ima_player, this)

        val videoPlayerContainer = findViewById<ViewGroup>(R.id.videoPlayerContainer)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        videoPlayer = findViewById(R.id.videoView)
        videoPlayerVolumeButton = findViewById(R.id.adVolumeLayout)
        videoAdPlayerAdapter =
            VideoAdPlayerAdapter(videoPlayer!!, videoPlayerVolumeButton!!, audioManager)

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

        adDisplayContainer.registerFriendlyObstruction(overlayObstruction)
        adDisplayContainer.registerFriendlyObstruction(volumeButtonObstruction)

        sdkFactory = ImaSdkFactory.getInstance()
        val settings = sdkFactory!!.createImaSdkSettings()
        adsLoader = sdkFactory!!.createAdsLoader(context, settings, adDisplayContainer)

        adsLoader!!.addAdErrorListener { adErrorEvent ->
            Log.i(LOGTAG, "Ad Error: " + adErrorEvent.error.message)
            insideAdListener?.insideAdError(adErrorEvent.error.message)
        }

        adsLoader!!.addAdsLoadedListener { adsManagerLoadedEvent ->
            adsManager = adsManagerLoadedEvent.adsManager

            adsManager?.addAdErrorListener { adErrorEvent ->
                Log.e(LOGTAG, "Ad Error: " + adErrorEvent.error.message)
                insideAdListener?.insideAdError(adErrorEvent.error.message)

                val universalAdIds: String =
                    adsManager!!.currentAd.universalAdIds.contentToString()
                Log.i(
                    LOGTAG,
                    "Discarding the current ad break with universal ad Ids: $universalAdIds"
                )
                adsManager!!.discardAdBreak()
            }

            adsManager!!.addAdEventListener { adEvent ->
                if (adEvent.type != AdEventType.AD_PROGRESS) {
                    Log.i(LOGTAG, "Event: " + adEvent.type)
                }

                when (adEvent.type) {
                    AdEventType.LOADED ->
                        adsManager!!.start()
                    AdEventType.ALL_ADS_COMPLETED -> {
                        adsManager!!.destroy()
                        adsManager = null
                    }
                    AdEventType.SKIPPED -> {
                        insideAdListener?.insideAdSkipped()
                    }
                    AdEventType.CLICKED -> {
                        insideAdListener?.insideAdClicked()
                    }
                    else -> {}
                }
            }

            val adsRenderingSettings = ImaSdkFactory.getInstance().createAdsRenderingSettings()
            adsManager!!.init(adsRenderingSettings)
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
                insideAdListener?.insideAdStop()
            }

            override fun onError(p0: AdMediaInfo) {
                insideAdListener?.insideAdError("Error while playing AD.")
            }

            override fun onLoaded(p0: AdMediaInfo) {
                insideAdListener?.insideAdLoaded()
            }

            override fun onPause(p0: AdMediaInfo) {
            }

            override fun onPlay(p0: AdMediaInfo) {
                insideAdListener?.insideAdPlay()
            }

            override fun onResume(p0: AdMediaInfo) {
            }

            override fun onVolumeChanged(p0: AdMediaInfo, p1: Int) {
                insideAdListener?.insideAdVolumeChanged(p1)
            }
        })
    }

    fun playAd(insideAd: InsideAd, geoIp: GeoIp, listener: InsideAdCallback) {
        insideAdListener = listener
        val url = InsideAdHelper.populateVASTURL(context, insideAd, geoIp)
        url?.let { requestAds(it) }
    }

}