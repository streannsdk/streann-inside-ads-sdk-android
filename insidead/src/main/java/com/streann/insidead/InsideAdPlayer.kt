package com.streann.insidead

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.models.InsideAd

class InsideAdPlayer constructor(context: Context) : FrameLayout(context), SurfaceHolder.Callback {

    private val LOGTAG = "InsideAdSdk"

    private lateinit var imageAdView: ImageView
    private var surfaceView: SurfaceView? = null

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
    }

    fun playAd(bitmap: Bitmap?, insideAd: InsideAd, listener: InsideAdCallback) {
        insideAdListener = listener

        if (bitmap != null) {
            showLocalImageAd(bitmap)
        } else {
            setupLocalVideoAd()
            setupProgressBar()
            setupCloseButton()
            setupVolumeButton()

            val insideAdUrl = insideAd.url
            Log.i(LOGTAG, "adUrl: $insideAdUrl")

            prepareMediaPlayer(Uri.parse(insideAdUrl))
        }
    }

    private fun setupLocalVideoAd() {
        surfaceView = SurfaceView(context)
        surfaceView?.holder?.addCallback(this)

        addView(surfaceView)
        setSurfaceViewSize()

        imageAdView.visibility = GONE
        surfaceView?.visibility = VISIBLE
        videoProgressBar?.visibility = VISIBLE
    }

    private fun showLocalImageAd(bitmap: Bitmap) {
        imageAdView.visibility = VISIBLE
        adCloseButton?.visibility = VISIBLE
        surfaceView?.visibility = GONE

        imageAdView.setImageBitmap(bitmap)

        Log.i(LOGTAG, "playAd")
        insideAdListener?.insideAdPlay()
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

    fun startPlayingAd() {
        mediaPlayer?.start()

        Log.i(LOGTAG, "playAd")
        insideAdListener?.insideAdPlay()
        videoProgressBar?.visibility = GONE

        mediaPlayer?.setOnCompletionListener {
            stopLocalVideoAd()
        }

        mediaPlayer?.setOnErrorListener { mediaPlayer: MediaPlayer?, errorType: Int, extra: Int ->
            notifySdkAboutAdError(errorType)
            true
        }
    }

    fun stopAd() {
        Log.i(LOGTAG, "stopAd")

        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
            stopLocalVideoAd()
        } else if (imageAdView.visibility == VISIBLE) {
            imageAdView.setImageBitmap(null)
            insideAdListener?.insideAdStop()
        }

        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun stopLocalVideoAd() {
        savedAdPosition = 0
        insideAdListener?.insideAdStop()
        adVolumeButton?.visibility = GONE
        removeView(surfaceView)
        removeView(adCloseButton)
        removeView(adVolumeButton)
        removeView(videoProgressBar)
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

    private fun setupProgressBar() {
        videoProgressBar = ProgressBar(context)

        val params = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        params.gravity = Gravity.CENTER

        addView(videoProgressBar, params)
    }

    private fun setupCloseButton() {
        adCloseButton = ImageView(context)
        adCloseButton?.setImageResource(R.drawable.ic_close)

        val params = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        params.gravity = Gravity.TOP or Gravity.START
        params.marginStart = 20
        params.topMargin = 20

        addView(adCloseButton, params)

        adCloseButton?.setOnClickListener {
            stopAd()
        }
    }

    private fun setupVolumeButton() {
        adVolumeButton = ImageView(context)

        val params = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        params.gravity = Gravity.TOP or Gravity.END
        params.marginEnd = 20
        params.topMargin = 20

        addView(adVolumeButton, params)
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

        surfaceView?.layoutParams?.width = calculatedWidth
        surfaceView?.layoutParams?.height = calculatedHeight
    }

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

}