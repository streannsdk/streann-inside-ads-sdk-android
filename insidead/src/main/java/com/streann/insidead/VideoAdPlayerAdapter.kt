package com.streann.insidead

import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.VideoView
import com.google.ads.interactivemedia.v3.api.AdPodInfo
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import java.util.*

class VideoAdPlayerAdapter(private val videoPlayer: VideoView, audioManager: AudioManager) :
    VideoAdPlayer {
    private val audioManager: AudioManager
    private val videoAdPlayerCallbacks: ArrayList<VideoAdPlayer.VideoAdPlayerCallback> = ArrayList()
    private var timer: Timer? = null
    private var adDuration = 0

    private var savedAdPosition = 0
    private var loadedAdMediaInfo: AdMediaInfo? = null

    init {
        this.audioManager = audioManager
    }

    companion object {
        private const val LOGTAG = "InsideAdSdk"
        private const val POLLING_TIME_MS: Long = 250
        private const val INITIAL_DELAY_MS: Long = 250
    }

    private fun notifyImaSdkAboutAdLoaded() {
        Log.i(LOGTAG, "notifyImaSdkAboutAdLoaded")
        for (callback in videoAdPlayerCallbacks) {
            callback.onLoaded(loadedAdMediaInfo!!)
        }
    }

    private fun notifyImaSdkAboutAdStarted() {
        Log.i(LOGTAG, "notifyImaSdkAboutAdStarted")
        for (callback in videoAdPlayerCallbacks) {
            callback.onPlay(loadedAdMediaInfo!!)
        }
    }

    private fun notifyImaSdkAboutAdPaused() {
        Log.i(LOGTAG, "notifyImaSdkAboutAdPaused")
        for (callback in videoAdPlayerCallbacks) {
            callback.onPause(loadedAdMediaInfo!!)
        }
    }

    private fun notifyImaSdkAboutAdEnded() {
        Log.i(LOGTAG, "notifyImaSdkAboutAdEnded")
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
        Log.i(LOGTAG, "notifyImaSdkAboutAdError")

        when (errorType) {
            MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> Log.e(
                LOGTAG,
                "notifyImaSdkAboutAdError: MEDIA_ERROR_UNSUPPORTED"
            )
            MediaPlayer.MEDIA_ERROR_TIMED_OUT -> Log.e(
                LOGTAG,
                "notifyImaSdkAboutAdError: MEDIA_ERROR_TIMED_OUT"
            )
            else -> {}
        }

        for (callback in videoAdPlayerCallbacks) {
            callback.onError(loadedAdMediaInfo!!)
        }

        return true
    }

    private fun startAdTracking() {
        Log.i(LOGTAG, "startAdTracking")
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
        Log.i(LOGTAG, "stopAdTracking")
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
        return (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
    }

    override fun addCallback(videoAdPlayerCallback: VideoAdPlayer.VideoAdPlayerCallback) {
        videoAdPlayerCallbacks.add(videoAdPlayerCallback);
    }

    override fun loadAd(adMediaInfo: AdMediaInfo, adPodInfo: AdPodInfo) {
        Log.i(LOGTAG, "loadAd");
        loadedAdMediaInfo = adMediaInfo;
        notifyImaSdkAboutAdLoaded()
    }

    override fun pauseAd(adMediaInfo: AdMediaInfo) {
        Log.i(LOGTAG, "pauseAd");
        savedAdPosition = videoPlayer.currentPosition;
        stopAdTracking();
        notifyImaSdkAboutAdPaused()
    }

    override fun playAd(adMediaInfo: AdMediaInfo) {
        Log.i(LOGTAG, "playAd");
        videoPlayer.setVideoURI(Uri.parse(adMediaInfo.url))

        videoPlayer.setOnPreparedListener { mediaPlayer: MediaPlayer ->
            adDuration = mediaPlayer.duration
            if (savedAdPosition > 0) {
                mediaPlayer.seekTo(savedAdPosition)
            }
            mediaPlayer.start()
            startAdTracking()
            notifyImaSdkAboutAdStarted()
        }

        videoPlayer.setOnErrorListener { mediaPlayer: MediaPlayer?, errorType: Int, extra: Int ->
            notifyImaSdkAboutAdError(
                errorType
            )
        }

        videoPlayer.setOnCompletionListener { mediaPlayer: MediaPlayer? ->
            savedAdPosition = 0
            notifyImaSdkAboutAdEnded()
        }
    }

    override fun release() {
    }

    override fun removeCallback(videoAdPlayerCallback: VideoAdPlayer.VideoAdPlayerCallback) {
        videoAdPlayerCallbacks.remove(videoAdPlayerCallback);
    }

    override fun stopAd(adMediaInfo: AdMediaInfo) {
        Log.i(LOGTAG, "stopAd");
        stopAdTracking();
    }

}