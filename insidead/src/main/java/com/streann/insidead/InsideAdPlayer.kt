package com.streann.insidead

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.models.InsideAd

class InsideAdPlayer constructor(context: Context) :
    FrameLayout(context) {

    private val LOGTAG = "InsideAdSdk"

    private lateinit var imageAdView: ImageView
    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceViewLayout: RelativeLayout
    private var mediaPlayer: MediaPlayer? = null
    private var adCloseButton: ImageView? = null
    private var adVolumeButton: ImageView? = null
    private var videoProgressBar: ProgressBar? = null

    private var insideAdListener: InsideAdCallback? = null

    private var adSoundPlaying = true
    private var savedAdPosition = 0

    init {
        init()
    }

    private fun init() {
        LayoutInflater.from(context).inflate(R.layout.inside_ad_player, this)

        imageAdView = findViewById(R.id.imageView)
        surfaceViewLayout = findViewById(R.id.surfaceViewLayout)

        adVolumeButton = findViewById(R.id.adVolumeIcon)
        videoProgressBar = findViewById(R.id.videoProgressBar)

        adCloseButton = findViewById(R.id.adCloseIcon)
        adCloseButton?.setOnClickListener {
            stopAd()
        }
    }

    fun playAd(bitmap: Bitmap?, insideAd: InsideAd, listener: InsideAdCallback) {
        insideAdListener = listener
        surfaceView = SurfaceView(context)
        surfaceViewLayout.addView(surfaceView)
        setSurfaceViewSize()

        if (bitmap != null) {
            surfaceView.visibility = GONE
            imageAdView.visibility = VISIBLE
            adCloseButton?.visibility = VISIBLE

            imageAdView.setImageBitmap(bitmap)

            Log.i(LOGTAG, "playAd")
            insideAdListener?.insideAdPlay()
        } else {
            imageAdView.visibility = GONE
            surfaceView.visibility = VISIBLE
            videoProgressBar?.visibility = VISIBLE
            videoProgressBar?.bringToFront()

            val insideAdUrl = insideAd.url
            Log.i(LOGTAG, "adUrl: $insideAdUrl")

            prepareMediaPlayer(Uri.parse(insideAdUrl))
            setupSurfaceView()
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
        adVolumeButton?.visibility = VISIBLE
        adVolumeButton?.setImageResource(soundIcon)
        insideAdListener?.insideAdVolumeChanged(sound)
    }

    fun stopAd() {
        Log.i(LOGTAG, "stopAdPlaying")

        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
            savedAdPosition = 0
            adVolumeButton?.visibility = GONE
            insideAdListener?.insideAdStop()
            surfaceViewLayout.removeView(surfaceView)
        } else if (imageAdView.visibility == VISIBLE) {
            imageAdView.setImageBitmap(null)
            insideAdListener?.insideAdStop()
        }

        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun prepareMediaPlayer(videoUrl: Uri) {
        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, videoUrl)
            prepareAsync()

            setOnPreparedListener { mediaPlayer ->
                Log.i(LOGTAG, "loadAd")
                insideAdListener?.insideAdLoaded()

                if (savedAdPosition > 0) {
                    mediaPlayer.seekTo(savedAdPosition)
                }

                adCloseButton?.visibility = VISIBLE
                setAdVolumeControl(mediaPlayer)
            }
        }
    }

    private fun setupSurfaceView() {
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                mediaPlayer?.setDisplay(holder)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                mediaPlayer?.release()
                mediaPlayer = null
            }
        })
    }

    private fun setSurfaceViewSize() {
        val aspectRatioWidth = 16
        val aspectRatioHeight = 9

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val aspectRatio = aspectRatioWidth.toDouble() / aspectRatioHeight.toDouble()

        val calculatedWidth: Int
        val calculatedHeight: Int

        if (screenWidth < (screenHeight * aspectRatio).toInt()) {
            calculatedWidth = screenWidth
            calculatedHeight = (screenWidth / aspectRatio).toInt()
        } else {
            calculatedWidth = (screenHeight * aspectRatio).toInt()
            calculatedHeight = screenHeight
        }

        surfaceView.layoutParams.width = calculatedWidth
        surfaceView.layoutParams.height = calculatedHeight
    }

    fun startPlayingAd() {
        mediaPlayer?.start()

        Log.i(LOGTAG, "playAd")
        insideAdListener?.insideAdPlay()
        videoProgressBar?.visibility = GONE

        mediaPlayer?.setOnCompletionListener {
            savedAdPosition = 0
            insideAdListener?.insideAdStop()
        }

        mediaPlayer?.setOnErrorListener { mediaPlayer: MediaPlayer?, errorType: Int, extra: Int ->
            notifySdkAboutAdError(errorType)
            true
        }
    }

}