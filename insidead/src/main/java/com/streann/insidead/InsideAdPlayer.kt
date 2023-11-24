package com.streann.insidead

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.VideoView
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.models.InsideAd

class InsideAdPlayer constructor(context: Context) :
    FrameLayout(context) {

    private val LOGTAG = "InsideAdSdk"

    private lateinit var videoPlayer: VideoView
    private var adCloseButton: ImageView? = null
    private var adVolumeButton: ImageView? = null

    private var insideAdListener: InsideAdCallback? = null

    private var adSoundPlaying = true
    private var savedAdPosition = 0

    init {
        init()
    }

    private fun init() {
        LayoutInflater.from(context).inflate(R.layout.inside_ad_player, this)
        videoPlayer = findViewById(R.id.videoView)
        adVolumeButton = findViewById(R.id.adVolumeIcon)

        adCloseButton = findViewById(R.id.adCloseIcon)
        adCloseButton?.setOnClickListener {
            stopAd()
        }
    }

    fun playAd(insideAd: InsideAd, listener: InsideAdCallback?) {
        insideAdListener = listener

        val insideAdUrl =
            "https://streann-res.s3.us-west-004.backblazeb2.com/61290efae4b0304f3eb75567/uploads/videos/8a20dad4-6e4e-44c3-973f-a52ba71132be/SampleVideo_1280x720_5mb.mp4"
        //        insideAdUrl = insideAd.url
        Log.i(LOGTAG, "adUrl: $insideAdUrl")

        videoPlayer.setVideoURI(Uri.parse(insideAdUrl))

        videoPlayer.setOnPreparedListener { mediaPlayer: MediaPlayer ->
            Log.i(LOGTAG, "loadAd")
            insideAdListener?.insideAdLoaded()

            if (savedAdPosition > 0) {
                mediaPlayer.seekTo(savedAdPosition)
            }

            setAdVolumeControl(mediaPlayer)

            Log.i(LOGTAG, "playAd")
            mediaPlayer.start()
            insideAdListener?.insideAdPlay()
        }

        videoPlayer.setOnErrorListener { mediaPlayer: MediaPlayer?, errorType: Int, extra: Int ->
            notifySdkAboutAdError(errorType)
        }

        videoPlayer.setOnCompletionListener { mediaPlayer: MediaPlayer? ->
            savedAdPosition = 0
            insideAdListener?.insideAdStop()
        }
    }

    private fun notifySdkAboutAdError(errorType: Int): Boolean {
        Log.i(LOGTAG, "notifySdkAboutAdError")

        when (errorType) {
            MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> {
                Log.e(
                    LOGTAG,
                    "notifySdkAboutAdError: MEDIA_ERROR_UNSUPPORTED"
                )
                insideAdListener?.insideAdError("Ad Error: MEDIA_ERROR_UNSUPPORTED")
            }
            MediaPlayer.MEDIA_ERROR_TIMED_OUT -> {
                Log.e(
                    LOGTAG,
                    "notifySdkAboutAdError: MEDIA_ERROR_TIMED_OUT"
                )
                insideAdListener?.insideAdError("Ad Error: MEDIA_ERROR_TIMED_OUT")
            }
            else -> {
                insideAdListener?.insideAdError("Error while playing AD.")
            }
        }

        return true
    }

    private fun setAdVolumeControl(mediaPlayer: MediaPlayer) {
        adSoundPlaying = if (InsideAdSdk.isAdMuted == true) {
            setAdSound(mediaPlayer, 0, R.drawable.ic_volume_off)
            false
        } else {
            setAdSound(mediaPlayer, 1, R.drawable.ic_volume_up)
            true
        }

        adVolumeButton?.setOnClickListener {
            if (adSoundPlaying) {
                setAdSound(mediaPlayer, 0, R.drawable.ic_volume_off)
            } else {
                setAdSound(mediaPlayer, 1, R.drawable.ic_volume_up)
            }
            adSoundPlaying = !adSoundPlaying
        }
    }

    private fun setAdSound(mediaPlayer: MediaPlayer, sound: Int, soundIcon: Int) {
        mediaPlayer.setVolume(sound.toFloat(), sound.toFloat())
        adVolumeButton?.setImageResource(soundIcon)
        insideAdListener?.insideAdVolumeChanged(sound)
    }

    fun stopAd() {
        Log.i(LOGTAG, "stopAdPlaying")
        if (videoPlayer.isPlaying) {
            videoPlayer.stopPlayback()
            savedAdPosition = 0
            insideAdListener?.insideAdStop()
        }
    }

}