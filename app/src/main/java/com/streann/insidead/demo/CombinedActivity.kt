package com.streann.insidead.demo

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayout
import com.streann.insidead.InsideAdSdk
import com.streann.insidead.InsideAdView
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.models.InsideAd

/**
 * Combined preroll + regular ad activity.
 * Phase 1: Fullscreen preroll ad plays immediately.
 * Phase 2: After preroll completes, shows simulated player with split-screen regular ads on interval.
 */
class CombinedActivity : AppCompatActivity() {

    private val TAG = "CombinedActivity"

    // Preroll views
    private lateinit var prerollAdContainer: FrameLayout
    private lateinit var loadingProgressBar: ProgressBar
    private var dynamicPrerollAdView: InsideAdView? = null
    private var isCleaningUpPrerollAd = false
    private var timeoutHandler: Handler? = null

    // Regular ad views (split-screen)
    private lateinit var playerWrapper: FlexboxLayout
    private lateinit var insideAdContainerRight: FrameLayout
    private lateinit var insideAdView: InsideAdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate - Starting CombinedActivity")
        setContentView(R.layout.activity_combined)

        // Preroll views
        prerollAdContainer = findViewById(R.id.prerollAdContainer)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)

        // Regular ad views
        playerWrapper = findViewById(R.id.player_wrapper)
        insideAdContainerRight = findViewById(R.id.inside_ad_container_right)

        // Create regular InsideAdView programmatically
        insideAdView = InsideAdView(applicationContext)
        insideAdView.setResizeMode(InsideAdView.ResizeMode.FIT)

        // Block back during preroll
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (prerollAdContainer.visibility == View.VISIBLE) {
                    Log.i(TAG, "Back pressed - ignoring during preroll ad")
                } else {
                    finish()
                }
            }
        })

        // Start with preroll
        showPrerollAd()
    }

    // ========== Phase 1: Preroll ==========

    private fun showPrerollAd() {
        Log.i(TAG, "Phase 1: Showing preroll ad")
        loadingProgressBar.visibility = View.VISIBLE

        dynamicPrerollAdView = InsideAdView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setResizeMode(InsideAdView.ResizeMode.FILL)
        }

        prerollAdContainer.addView(dynamicPrerollAdView, 0)
        setupPrerollCallback()
        startTimeout(10000)

        dynamicPrerollAdView?.let { adView ->
            InsideAdSdk.requestPrerollAd(
                context = this,
                adContainer = adView,
                screen = "Video Player",
                isAdMuted = true,
                targetingFilters = null
            )
        }
    }

    private fun setupPrerollCallback() {
        InsideAdSdk.setPrerollAdCallback(object : InsideAdCallback {
            override fun insideAdReceived(insideAd: InsideAd) {
                Log.i(TAG, "PREROLL - ad received: ${insideAd.name}")
                cancelTimeout()
                startTimeout(30000)
            }

            override fun insideAdLoaded() {
                Log.i(TAG, "PREROLL - ad loaded, playing")
                cancelTimeout()
                loadingProgressBar.visibility = View.GONE
                dynamicPrerollAdView?.let {
                    it.visibility = View.VISIBLE
                    it.playAd()
                }
            }

            override fun insideAdPlay() {
                Log.i(TAG, "PREROLL - playing")
            }

            override fun insideAdStop() {
                if (isCleaningUpPrerollAd) return
                Log.i(TAG, "PREROLL - completed")
                finishPrerollAndStartContent(shouldStopAd = false)
            }

            override fun insideAdSkipped() {
                if (isCleaningUpPrerollAd) return
                Log.i(TAG, "PREROLL - skipped")
                finishPrerollAndStartContent(shouldStopAd = false)
            }

            override fun insideAdClicked() {
                Log.i(TAG, "PREROLL - clicked")
            }

            override fun insideAdError(error: String) {
                if (isCleaningUpPrerollAd) return
                Log.i(TAG, "PREROLL - error: $error")
                finishPrerollAndStartContent(shouldStopAd = false)
            }

            override fun insideAdVolumeChanged(level: Int) {
                Log.i(TAG, "PREROLL - volume changed: $level")
            }
        })
    }

    private fun finishPrerollAndStartContent(shouldStopAd: Boolean) {
        Log.i(TAG, "Finishing preroll, transitioning to content")

        if (isCleaningUpPrerollAd) return
        isCleaningUpPrerollAd = true

        cancelTimeout()

        dynamicPrerollAdView?.let { adView ->
            if (shouldStopAd) adView.stopAd()
            prerollAdContainer.removeView(adView)
        }
        dynamicPrerollAdView = null
        loadingProgressBar.visibility = View.GONE
        prerollAdContainer.visibility = View.GONE

        InsideAdSdk.cancelPrerollAdRequest()
        isCleaningUpPrerollAd = false

        // Transition to Phase 2
        startContentWithRegularAds()
    }

    private fun cancelTimeout() {
        timeoutHandler?.removeCallbacksAndMessages(null)
        timeoutHandler = null
    }

    private fun startTimeout(millis: Long) {
        cancelTimeout()
        timeoutHandler = Handler(Looper.getMainLooper())
        timeoutHandler?.postDelayed({
            Log.i(TAG, "PREROLL - timeout after ${millis}ms")
            finishPrerollAndStartContent(shouldStopAd = true)
        }, millis)
    }

    // ========== Phase 2: Content + Regular Ads ==========

    private fun startContentWithRegularAds() {
        Log.i(TAG, "Phase 2: Starting content with regular ads")
        playerWrapper.visibility = View.VISIBLE

        setupRegularAdCallback()

        // Add the ad view to the container if not already added
        if (insideAdView.parent == null) {
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            insideAdContainerRight.addView(insideAdView, params)
        }

        insideAdView.requestAd(
            screen = "Video Player",
            isAdMuted = true,
            targetingFilters = null
        )
    }

    private fun setupRegularAdCallback() {
        InsideAdSdk.setInsideAdCallback(object : InsideAdCallback {
            override fun insideAdReceived(insideAd: InsideAd) {
                Log.i(TAG, "REGULAR - ad received: ${insideAd.name}")
            }

            override fun insideAdLoaded() {
                Log.i(TAG, "REGULAR - ad loaded, playing")
                Handler(Looper.getMainLooper()).post {
                    showInsideAdView()
                    insideAdView.playAd()
                }
            }

            override fun insideAdPlay() {
                Log.i(TAG, "REGULAR - playing")
            }

            override fun insideAdStop() {
                Log.i(TAG, "REGULAR - stopped (next ad will play after interval)")
                Handler(Looper.getMainLooper()).post {
                    hideInsideAdView()
                }
            }

            override fun insideAdSkipped() {
                Log.i(TAG, "REGULAR - skipped")
                Handler(Looper.getMainLooper()).post {
                    insideAdView.stopAd()
                }
            }

            override fun insideAdClicked() {
                Log.i(TAG, "REGULAR - clicked")
            }

            override fun insideAdError(error: String) {
                Log.i(TAG, "REGULAR - error: $error")
                Handler(Looper.getMainLooper()).post {
                    hideInsideAdView()
                }
            }

            override fun insideAdVolumeChanged(level: Int) {
                Log.i(TAG, "REGULAR - volume changed: $level")
            }
        })
    }

    private fun showInsideAdView() {
        if (playerWrapper.flexDirection == FlexDirection.ROW) {
            playerWrapper.flexDirection = FlexDirection.ROW_REVERSE
        }
        insideAdContainerRight.visibility = View.VISIBLE
    }

    private fun hideInsideAdView() {
        if (insideAdContainerRight.visibility == View.VISIBLE) {
            insideAdContainerRight.visibility = View.GONE

            if (playerWrapper.flexDirection == FlexDirection.ROW_REVERSE) {
                playerWrapper.flexDirection = FlexDirection.ROW
            }
        }
    }

    // ========== Lifecycle ==========

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.i(TAG, "onConfigurationChanged - Orientation: ${newConfig.orientation}")

        // Only handle split-screen orientation if we're in Phase 2
        if (playerWrapper.visibility != View.VISIBLE) return

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            playerWrapper.flexDirection = FlexDirection.COLUMN
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (insideAdContainerRight.visibility == View.VISIBLE) {
                playerWrapper.flexDirection = FlexDirection.ROW_REVERSE
            } else {
                playerWrapper.flexDirection = FlexDirection.ROW
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy - Cleaning up")

        // Clean up preroll
        isCleaningUpPrerollAd = false
        cancelTimeout()
        dynamicPrerollAdView?.let { adView ->
            adView.stopAd()
            adView.cancelAdRequest()
            prerollAdContainer.removeView(adView)
        }
        dynamicPrerollAdView = null
        InsideAdSdk.cancelPrerollAdRequest()

        // Clean up regular ad
        insideAdView.stopAd()
        insideAdView.cancelAdRequest()
        InsideAdSdk.removeInsideAdCallback()
    }
}
