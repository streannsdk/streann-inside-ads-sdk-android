package com.streann.insidead.players.googleima

import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.VideoView
import com.google.ads.interactivemedia.v3.api.AdPodInfo
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import com.streann.insidead.InsideAdSdk
import com.streann.insidead.R
import java.util.*

class VideoAdPlayerAdapter(
    private val videoPlayer: VideoView,
    videoPlayerVolumeButton: FrameLayout
) : VideoAdPlayer {

    private val videoAdPlayerCallbacks: ArrayList<VideoAdPlayer.VideoAdPlayerCallback> = ArrayList()
    private var timer: Timer? = null
    private var adDuration = 0

    private var savedAdPosition = 0
    private var loadedAdMediaInfo: AdMediaInfo? = null

    private var adSoundPlaying = true
    private var videoPlayerVolumeButton: FrameLayout

    companion object {
        private const val POLLING_TIME_MS: Long = 250
        private const val INITIAL_DELAY_MS: Long = 250
    }

    init {
        this.videoPlayerVolumeButton = videoPlayerVolumeButton
    }

    private fun notifyImaSdkAboutAdLoaded() {
        Log.i(InsideAdSdk.LOG_TAG, "notifyImaSdkAboutAdLoaded")
        for (callback in videoAdPlayerCallbacks) {
            callback.onLoaded(loadedAdMediaInfo!!)
        }
    }

    private fun notifyImaSdkAboutAdStarted() {
        Log.i(InsideAdSdk.LOG_TAG, "notifyImaSdkAboutAdStarted")
        for (callback in videoAdPlayerCallbacks) {
            callback.onPlay(loadedAdMediaInfo!!)
        }
    }

    private fun notifyImaSdkAboutAdPaused() {
        Log.i(InsideAdSdk.LOG_TAG, "notifyImaSdkAboutAdPaused")
        for (callback in videoAdPlayerCallbacks) {
            callback.onPause(loadedAdMediaInfo!!)
        }
    }

    private fun notifyImaSdkAboutAdEnded() {
        Log.i(InsideAdSdk.LOG_TAG, "notifyImaSdkAboutAdEnded")
        savedAdPosition = 0
        for (callback in videoAdPlayerCallbacks) {
            callback.onEnded(loadedAdMediaInfo!!)
        }
    }

    private fun notifyImaSdkAboutAdProgress(adProgress: VideoProgressUpdate) {
        for (callback in videoAdPlayerCallbacks) {
            callback.onAdProgress(loadedAdMediaInfo!!, adProgress)
        }
    }

    private fun notifyImaSdkAboutAdError(errorType: Int): Boolean {
        Log.i(InsideAdSdk.LOG_TAG, "notifyImaSdkAboutAdError")

        when (errorType) {
            MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> Log.e(
                InsideAdSdk.LOG_TAG,
                "notifyImaSdkAboutAdError: MEDIA_ERROR_UNSUPPORTED"
            )

            MediaPlayer.MEDIA_ERROR_TIMED_OUT -> Log.e(
                InsideAdSdk.LOG_TAG,
                "notifyImaSdkAboutAdError: MEDIA_ERROR_TIMED_OUT"
            )

            else -> {}
        }

        for (callback in videoAdPlayerCallbacks) {
            callback.onError(loadedAdMediaInfo!!)
        }

        return true
    }

    private fun notifyImaSdkAboutAdVolumeChanged(level: Int) {
        Log.i(InsideAdSdk.LOG_TAG, "notifyImaSdkAboutAdVolumeChanged")
        for (callback in videoAdPlayerCallbacks) {
            callback.onVolumeChanged(loadedAdMediaInfo!!, level)
        }
    }

    private fun startAdTracking() {
        Log.i(InsideAdSdk.LOG_TAG, "startAdTracking")
        if (timer != null) {
            return
        }

        timer = Timer()

        val updateTimerTask: TimerTask = object : TimerTask() {
            override fun run() {
                val progressUpdate = adProgress
                notifyImaSdkAboutAdProgress(progressUpdate)
            }
        }

        timer!!.schedule(updateTimerTask, POLLING_TIME_MS, INITIAL_DELAY_MS)
    }

    private fun stopAdTracking() {
        Log.i(InsideAdSdk.LOG_TAG, "stopAdTracking")
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    override fun getAdProgress(): VideoProgressUpdate {
        val adPosition = videoPlayer.currentPosition.toLong()
        return VideoProgressUpdate(adPosition, adDuration.toLong())
    }

    override fun getVolume(): Int {
        return 0
    }

    override fun addCallback(videoAdPlayerCallback: VideoAdPlayer.VideoAdPlayerCallback) {
        videoAdPlayerCallbacks.add(videoAdPlayerCallback)
    }

    override fun loadAd(adMediaInfo: AdMediaInfo, adPodInfo: AdPodInfo) {
        Log.i(InsideAdSdk.LOG_TAG, "loadAd")
        loadedAdMediaInfo = adMediaInfo
        notifyImaSdkAboutAdLoaded()
    }

    override fun pauseAd(adMediaInfo: AdMediaInfo) {
        Log.i(InsideAdSdk.LOG_TAG, "pauseAd")
        savedAdPosition = videoPlayer.currentPosition
        stopAdTracking()
        notifyImaSdkAboutAdPaused()
    }

    override fun playAd(adMediaInfo: AdMediaInfo) {
        Log.i(InsideAdSdk.LOG_TAG, "playAd")

        videoPlayer.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
        videoPlayer.setVideoURI(Uri.parse(adMediaInfo.url))

        videoPlayer.setOnPreparedListener { mediaPlayer: MediaPlayer ->
            adDuration = mediaPlayer.duration

            if (savedAdPosition > 0) {
                mediaPlayer.seekTo(savedAdPosition)
            }

            setAdVolumeControl(mediaPlayer)
            videoPlayer.animate().alpha(1f)
            mediaPlayer.start()
            startAdTracking()
            notifyImaSdkAboutAdStarted()
        }

        videoPlayer.setOnErrorListener { _: MediaPlayer?, errorType: Int, _: Int ->
            notifyImaSdkAboutAdError(
                errorType
            )
        }

        videoPlayer.setOnCompletionListener {
            savedAdPosition = 0
            notifyImaSdkAboutAdEnded()
        }
    }

    fun stopAdPlaying() {
        Log.i(InsideAdSdk.LOG_TAG, "stopAdPlaying")
        if (videoPlayer.isPlaying) {
            stopAdTracking()
            videoPlayer.stopPlayback()
            notifyImaSdkAboutAdEnded()
        }
    }

    override fun release() {
    }

    override fun removeCallback(videoAdPlayerCallback: VideoAdPlayer.VideoAdPlayerCallback) {
        videoAdPlayerCallbacks.remove(videoAdPlayerCallback)
    }

    override fun stopAd(adMediaInfo: AdMediaInfo) {
        Log.i(InsideAdSdk.LOG_TAG, "stopAd")
        stopAdTracking()
    }

    private fun setAdVolumeControl(mediaPlayer: MediaPlayer) {
        adSoundPlaying = if (InsideAdSdk.isAdMuted == true) {
            setAdSound(mediaPlayer, 0, R.drawable.ic_volume_off)
            false
        } else {
            setAdSound(mediaPlayer, 1, R.drawable.ic_volume_up)
            true
        }

        videoPlayerVolumeButton.setOnClickListener {
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
        videoPlayerVolumeButton.findViewById<ImageView>(R.id.adVolumeIcon)
            .setImageResource(soundIcon)
        notifyImaSdkAboutAdVolumeChanged(sound)
    }

}