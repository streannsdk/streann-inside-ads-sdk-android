package com.streann.insidead.players.insidead

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import com.streann.insidead.InsideAdSdk
import com.streann.insidead.R
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.callbacks.InsideAdProgressCallback
import com.streann.insidead.models.InsideAd
import com.streann.insidead.utils.Helper

@SuppressLint("ViewConstructor")
class InsideAdPlayer(
    context: Context,
    callback: InsideAdProgressCallback
) : FrameLayout(context), SurfaceHolder.Callback {

    private var insideAd: InsideAd? = null

    private var gradientBgView: View? = null
    private var imageAdView: ImageView? = null
    private var surfaceView: SurfaceView? = null
    private var mediaPlayer: MediaPlayer? = null

    private var adCloseButton: ImageView? = null
    private var adVolumeButton: ImageView? = null
    private var videoProgressBar: ProgressBar? = null
    private var learnMoreLayout: LinearLayout? = null

    private var insideAdCallback: InsideAdCallback? = null
    private var insideAdProgressCallback: InsideAdProgressCallback? = callback

    private var showCloseButtonHandler: Handler? = null
    private var closeImageAdHandler: Handler? = null

    private var savedAdPosition = 0
    private var adSoundPlaying = true
    private var isSurfaceDestroyed: Boolean = false

    init {
        LayoutInflater.from(context).inflate(R.layout.inside_ad_player, this)
        imageAdView = findViewById(R.id.imageView)
    }

    fun playAd(bitmap: Bitmap?, ad: InsideAd, callback: InsideAdCallback) {
        insideAd = ad
        insideAdCallback = callback
        showCloseButtonHandler = Handler(Looper.getMainLooper())
        closeImageAdHandler = Handler(Looper.getMainLooper())

        if (bitmap != null) {
            showLocalImageAd(bitmap)
            setupGradientBackground()
            setupLearnMoreLayout(null, null)
            setupCloseButton()
        } else {
            setupLocalVideoAd()
            setupGradientBackground()
            setupCloseButton()
            setupVolumeButton()

            val insideAdUrl = ad.url
            Log.i(InsideAdSdk.LOG_TAG, "adUrl: $insideAdUrl")

            prepareMediaPlayer(Uri.parse(insideAdUrl))
        }
    }

    private fun showLocalImageAd(bitmap: Bitmap) {
        imageAdView?.visibility = VISIBLE
        surfaceView?.visibility = GONE
        setCloseButtonVisibility()

        imageAdView?.setImageBitmap(bitmap)
        Helper.setViewSize(imageAdView, resources)

        Log.i(InsideAdSdk.LOG_TAG, "playAd")
        insideAdCallback?.insideAdPlay()

        InsideAdSdk.durationInSeconds?.let {
            closeImageAdHandler?.postDelayed({
                stopAd()
            }, it)
        }
    }

    private fun setupLocalVideoAd() {
        surfaceView = SurfaceView(context)
        surfaceView?.holder?.addCallback(this)

        addView(surfaceView)
        Helper.setViewSize(surfaceView, resources)

        imageAdView?.visibility = GONE
        surfaceView?.visibility = VISIBLE

        setupProgressBar()
    }

    private fun prepareMediaPlayer(videoUrl: Uri) {
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(context, videoUrl)
                prepareAsync()

                setOnPreparedListener { mediaPlayer ->
                    Log.i(InsideAdSdk.LOG_TAG, "loadAd")
                    insideAdCallback?.insideAdLoaded()

                    if (savedAdPosition > 0) {
                        mediaPlayer.seekTo(savedAdPosition)
                    }

                    setCloseButtonVisibility()
                    setAdVolumeControl(mediaPlayer)
                }

                setOnErrorListener { _: MediaPlayer?, errorType: Int, _: Int ->
                    notifySdkAboutAdError(errorType)
                    true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                notifySdkAboutAdError(MediaPlayer.MEDIA_ERROR_UNKNOWN)
            }
        }
    }

    private fun notifySdkAboutAdError(errorType: Int): Boolean {
        Log.i(InsideAdSdk.LOG_TAG, "notifySdkAboutAdError")
        insideAdProgressCallback?.insideAdError()

        when (errorType) {
            MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> {
                Log.e(
                    InsideAdSdk.LOG_TAG,
                    "notifySdkAboutAdError: MEDIA_ERROR_UNSUPPORTED"
                )
                insideAdCallback?.insideAdError("Ad Error: MEDIA_ERROR_UNSUPPORTED")
            }

            MediaPlayer.MEDIA_ERROR_TIMED_OUT -> {
                Log.e(
                    InsideAdSdk.LOG_TAG,
                    "notifySdkAboutAdError: MEDIA_ERROR_TIMED_OUT"
                )
                insideAdCallback?.insideAdError("Ad Error: MEDIA_ERROR_TIMED_OUT")
            }

            MediaPlayer.MEDIA_ERROR_UNKNOWN -> {
                Log.e(
                    InsideAdSdk.LOG_TAG,
                    "notifySdkAboutAdError: MEDIA_ERROR_UNKNOWN"
                )
                insideAdCallback?.insideAdError("Ad Error: MEDIA_ERROR_UNKNOWN")
            }

            else -> {
                insideAdCallback?.insideAdError("Error while playing AD.")
            }
        }

        return true
    }

    fun startPlayingAd() {
        mediaPlayer?.start()

        Log.i(InsideAdSdk.LOG_TAG, "playAd")
        insideAdCallback?.insideAdPlay()
        videoProgressBar?.visibility = GONE

        mediaPlayer?.setOnCompletionListener {
            stopLocalVideoAd()
            removeHandlers()
        }
    }

    fun stopAd() {
        Log.i(InsideAdSdk.LOG_TAG, "stopAd")

        if (mediaPlayer?.isPlaying == true) {
            stopLocalVideoAd()
        } else if (imageAdView?.visibility == VISIBLE) {
            imageAdView?.setImageBitmap(null)
            removeCommonViews()
        }

        removeHandlers()
        insideAdCallback?.insideAdStop()
        insideAdProgressCallback?.insideAdStopped()
    }

    private fun stopLocalVideoAd() {
        stopMediaPlayer()
        savedAdPosition = 0
        removeCommonViews()
        removeView(adVolumeButton)
        removeView(videoProgressBar)
        removeView(surfaceView)
    }

    private fun stopMediaPlayer() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying)
                mp.stop()
            mp.release()
        }
        mediaPlayer = null
    }

    private fun setupProgressBar() {
        videoProgressBar = ProgressBar(context)

        val params = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        params.gravity = Gravity.CENTER

        addView(videoProgressBar, params)
        videoProgressBar?.visibility = VISIBLE
    }

    private fun setupCloseButton() {
        adCloseButton = ImageView(context)
        adCloseButton?.setImageResource(R.drawable.ic_close)
        adCloseButton?.setColorFilter(Color.WHITE)
        adCloseButton?.visibility = GONE

        val params = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        params.gravity = Gravity.TOP or Gravity.START
        params.marginStart = 10
        params.topMargin = 10

        addView(adCloseButton, params)

        adCloseButton?.setOnClickListener {
            stopAd()
        }
    }

    private fun setCloseButtonVisibility() {
        InsideAdSdk.showCloseButtonAfterSeconds?.let {
            showCloseButtonHandler?.postDelayed({
                adCloseButton?.visibility = VISIBLE
            }, it)
        }
    }

    private fun setupVolumeButton() {
        adVolumeButton = ImageView(context)

        val params = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        params.gravity = Gravity.TOP or Gravity.END
        params.marginEnd = 10
        params.topMargin = 10

        setupLearnMoreLayout(adVolumeButton, params)
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
        adVolumeButton?.setColorFilter(Color.WHITE)
        insideAdCallback?.insideAdVolumeChanged(sound)
    }

    private fun setupGradientBackground() {
        gradientBgView = View(context)

        val params = LayoutParams(MATCH_PARENT, 95)
        params.gravity = Gravity.TOP or Gravity.START

        val gradientDrawable = GradientDrawable()
        gradientDrawable.colors = intArrayOf(Color.parseColor("#70000000"), Color.TRANSPARENT)
        gradientDrawable.gradientType = GradientDrawable.LINEAR_GRADIENT
        gradientDrawable.orientation = GradientDrawable.Orientation.TOP_BOTTOM

        gradientBgView?.background = gradientDrawable
        addView(gradientBgView, params)
    }

    private fun setupLearnMoreLayout(
        adVolumeButton: View?,
        volumeButtonParams: LayoutParams?
    ) {
        val learnMoreButton = createLearnMoreButton()

        val learnMoreParams = RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        learnMoreParams.topMargin = 10
        learnMoreParams.marginEnd = 20

        learnMoreLayout = LinearLayout(context)
        learnMoreLayout?.gravity = Gravity.END
        learnMoreLayout?.orientation = LinearLayout.HORIZONTAL
        learnMoreLayout?.addView(learnMoreButton, learnMoreParams)

        adVolumeButton?.let { volumeButton ->
            volumeButtonParams?.let { params ->
                learnMoreLayout?.addView(volumeButton, params)
            }
        }

        addView(learnMoreLayout)
    }

    private fun createLearnMoreButton(): TextView {
        val learnMoreButton = TextView(context)

        learnMoreButton.text = context.getString(R.string.learn_more)
        learnMoreButton.textSize = 16f
        learnMoreButton.setTextColor(Color.WHITE)
        learnMoreButton.gravity = Gravity.END or Gravity.CENTER_VERTICAL

        val clickThroughUrl = insideAd?.properties?.clickThroughUrl
        if (clickThroughUrl?.isBlank() == true)
            learnMoreButton.visibility = GONE
        else {
            learnMoreButton.visibility = VISIBLE
            learnMoreButton.setOnClickListener {
                try {
                    val intent =
                        Intent(Intent.ACTION_VIEW, Uri.parse(clickThroughUrl))
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
        }

        return learnMoreButton
    }


    private fun removeCommonViews() {
        removeView(adCloseButton)
        removeView(learnMoreLayout)
        removeView(gradientBgView)
    }

    private fun removeHandlers() {
        closeImageAdHandler?.removeCallbacksAndMessages(null)
        closeImageAdHandler = null
        showCloseButtonHandler?.removeCallbacksAndMessages(null)
        showCloseButtonHandler = null
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        mediaPlayer?.setDisplay(holder)
        if (isSurfaceDestroyed) {
            if (mediaPlayer?.isPlaying == false) mediaPlayer?.start()
            isSurfaceDestroyed = false
        }
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            isSurfaceDestroyed = true
        }
    }

}