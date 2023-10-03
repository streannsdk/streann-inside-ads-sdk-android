package com.streann.insidead

import android.content.Context
import android.media.AudioManager
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventType
import com.google.ads.interactivemedia.v3.api.AdsLoader
import com.google.ads.interactivemedia.v3.api.AdsManager
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.models.InsideAd

class GoogleImaPlayer @JvmOverloads constructor(private val context: Context) :
    FrameLayout(context) {

    private val LOGTAG = "InsideAdStreann"
    private val VAST_TAG_URL =
        "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/single_ad_samples&sz=640x480&cust_params=sample_ct%3Dlinear&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator="
    private var sdkFactory: ImaSdkFactory? = null

    private var adsLoader: AdsLoader? = null
    private var adsManager: AdsManager? = null

    private var videoPlayer: VideoView? = null
    private var mediaController: MediaController? = null
    private var videoAdPlayerAdapter: VideoAdPlayerAdapter? = null

    private var insideAdListener: InsideAdCallback? = null

    init {
        init()
    }

    private fun init() {
        LayoutInflater.from(context).inflate(R.layout.google_ima_player, this)

        mediaController = MediaController(context);
        videoPlayer = findViewById(R.id.videoView);
        mediaController?.setAnchorView(videoPlayer);
        videoPlayer?.setMediaController(mediaController);

        val videoPlayerContainer = findViewById<ViewGroup>(R.id.videoPlayerContainer)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        videoPlayer?.let {
            videoAdPlayerAdapter = VideoAdPlayerAdapter(videoPlayer!!, audioManager)
        }

        sdkFactory = ImaSdkFactory.getInstance()

        val adDisplayContainer = ImaSdkFactory.createAdDisplayContainer(
            videoPlayerContainer,
            videoAdPlayerAdapter!!
        )

        val settings = sdkFactory!!.createImaSdkSettings()
        adsLoader = sdkFactory!!.createAdsLoader(context, settings, adDisplayContainer)

        adsLoader!!.addAdErrorListener { adErrorEvent ->
            Log.i(LOGTAG, "Ad Error: " + adErrorEvent.error.message)
        }

        adsLoader!!.addAdsLoadedListener { adsManagerLoadedEvent ->
            adsManager = adsManagerLoadedEvent.adsManager

            adsManager?.addAdErrorListener { adErrorEvent ->
                Log.e(LOGTAG, "Ad Error: " + adErrorEvent.error.message)
                val universalAdIds: String =
                    adsManager!!.currentAd.universalAdIds.contentToString()
                Log.i(
                    LOGTAG,
                    "Discarding the current ad break with universal "
                            + "ad Ids: "
                            + universalAdIds
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
                    AdEventType.CLICKED -> {}
                    else -> {}
                }
            }

            val adsRenderingSettings = ImaSdkFactory.getInstance().createAdsRenderingSettings()
            adsManager!!.init(adsRenderingSettings)
        }
    }

    private fun requestAds(adTagUrl: String) {
        val request = sdkFactory!!.createAdsRequest()
        request.adTagUrl = adTagUrl

        adsLoader!!.requestAds(request)
    }

    private fun setImaAdsCallback() {
        videoAdPlayerAdapter?.addCallback(object : VideoAdPlayer.VideoAdPlayerCallback {
            override fun onAdProgress(p0: AdMediaInfo, p1: VideoProgressUpdate) {
            }

            override fun onBuffering(p0: AdMediaInfo) {
                insideAdListener?.insideAdBuffering()
            }

            override fun onContentComplete() {
            }

            override fun onEnded(p0: AdMediaInfo) {
                insideAdListener?.insideAdStop()
            }

            override fun onError(p0: AdMediaInfo) {
                insideAdListener?.insideAdError()
            }

            override fun onLoaded(p0: AdMediaInfo) {
                insideAdListener?.insideAdLoaded()
            }

            override fun onPause(p0: AdMediaInfo) {
                insideAdListener?.insideAdPause()
            }

            override fun onPlay(p0: AdMediaInfo) {
                insideAdListener?.insideAdPlay()
            }

            override fun onResume(p0: AdMediaInfo) {
                insideAdListener?.insideAdResume()
            }

            override fun onVolumeChanged(p0: AdMediaInfo, p1: Int) {
                insideAdListener?.insideAdVolumeChanged(p1.toFloat())
            }
        })
    }

    fun playAd(insideAd: InsideAd, listener: InsideAdCallback) {
        insideAdListener = listener
        setImaAdsCallback()
        // testUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/single_preroll_skippable&sz=640x480&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator="
        insideAd.url?.let { requestAds(it) }
    }

}