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

        requestAds(VAST_TAG_URL)
    }

    private fun requestAds(adTagUrl: String) {
        val request = sdkFactory!!.createAdsRequest()
        request.adTagUrl = adTagUrl

        adsLoader!!.requestAds(request)
    }

}