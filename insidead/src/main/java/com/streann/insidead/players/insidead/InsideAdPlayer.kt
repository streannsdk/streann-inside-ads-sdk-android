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
        removeHandlers()
        showCloseButtonHandler = Handler(Looper.getMainLooper())
        closeImageAdHandler = Handler(Looper.getMainLooper())

        // Important: Local video/image ads don't have skip buttons
        // They only have a close button that appears after showCloseButtonAfterSeconds
        InsideAdSdk.logSkipButtonState(
            event = "LOCAL AD STARTED",
            adName = ad.name ?: "Unknown",
            adType = if (bitmap != null) "LOCAL_IMAGE" else "LOCAL_VIDEO",
            isSkippable = false,
            additionalInfo = mapOf(
                "Skip Button Support" to "NOT SUPPORTED - Local ads only have close button",
                "Close Button Delay" to "${InsideAdSdk.showCloseButtonAfterSeconds?.div(1000) ?: 0}s",
                "Ad URL" to (ad.url ?: "N/A")
            )
        )

        // Size the parent container so overlay UI stays within bounds
        sizePlayerContainer()

        if (bitmap != null) {
            showLocalImageAd(bitmap)
            setupGradientBackground()
            setupLearnMoreLayout(null, null)
            setupCloseButton()
        } else {
            setupLocalVideoAd()
            setupProgressBar()
            setupGradientBackground()
            setupCloseButton()
            setupVolumeButton()

            val insideAdUrl = ad.url
            Log.i(InsideAdSdk.LOG_TAG, "adUrl: $insideAdUrl")

            prepareMediaPlayer(Uri.parse(insideAdUrl))
        }
    }

    private fun sizePlayerContainer() {
        // Size the parent container (this InsideAdPlayer) instead of child views
        // This ensures all overlay UI elements stay within the video bounds
        post {
            Helper.setViewSize(this, resources, InsideAdSdk.resizeMode)
            // Request layout to ensure centering is applied
            requestLayout()
        }
    }

    private fun showLocalImageAd(bitmap: Bitmap) {
        imageAdView?.visibility = VISIBLE
        surfaceView?.visibility = GONE
        setCloseButtonVisibility()

        imageAdView?.setImageBitmap(bitmap)
        // No need to size imageAdView - it will fill the parent which is already sized

        Log.i(InsideAdSdk.LOG_TAG, "playAd")
        insideAdCallback?.insideAdPlay()

        if (!InsideAdSdk.showAdForReels) {
            InsideAdSdk.durationInSeconds?.let {
                closeImageAdHandler?.postDelayed({
                    stopAd()
                }, it)
            }
        }
    }

    private fun setupLocalVideoAd() {
        surfaceView = SurfaceView(context)
        surfaceView?.holder?.addCallback(this)

        // Make video fill the parent container (which is already properly sized)
        val params = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        addView(surfaceView, params)

        imageAdView?.visibility = GONE
        surfaceView?.visibility = VISIBLE
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
            stopAd()
        }
    }

    fun stopAd() {
        Log.i(InsideAdSdk.LOG_TAG, "stopAd")

        if (mediaPlayer != null) {
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
            InsideAdSdk.debugLog("InsideAdPlayer", "Close button clicked (NOT skip button)")
            stopAd()
        }

        InsideAdSdk.debugLog(
            "InsideAdPlayer",
            "Close button created - Will appear after ${InsideAdSdk.showCloseButtonAfterSeconds?.div(1000) ?: 0}s"
        )
    }

    private fun setCloseButtonVisibility() {
        if (!InsideAdSdk.showAdForReels) {
            InsideAdSdk.showCloseButtonAfterSeconds?.let { delayMillis ->
                showCloseButtonHandler?.postDelayed({
                    adCloseButton?.visibility = VISIBLE
                    InsideAdSdk.debugLog(
                        "InsideAdPlayer",
                        "Close button NOW VISIBLE after ${delayMillis / 1000}s (this is NOT a skip button)"
                    )
                }, delayMillis)
            }
        } else {
            InsideAdSdk.debugLog(
                "InsideAdPlayer",
                "Close button disabled for Reels ads"
            )
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
        // Defensive: explicitly check for false, default to muted if null/true
        adSoundPlaying = if (InsideAdSdk.isAdMuted == false) {
            // Only unmute if explicitly set to false
            setAdSound(mediaPlayer, 1, R.drawable.ic_volume_up)
            true
        } else {
            // Default to muted (sound off) if isAdMuted is null or true
            setAdSound(mediaPlayer, 0, R.drawable.ic_volume_off)
            false
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
        if (clickThroughUrl?.isBlank() == true || clickThroughUrl.isNullOrEmpty()) {
            learnMoreButton.visibility = GONE
        } else {
            learnMoreButton.visibility = VISIBLE
            learnMoreButton.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(clickThroughUrl))
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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