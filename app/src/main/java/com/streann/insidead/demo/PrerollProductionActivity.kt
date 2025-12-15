package com.streann.insidead.demo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.streann.insidead.InsideAdSdk
import com.streann.insidead.InsideAdView
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.demo.R
import com.streann.insidead.models.InsideAd
import com.streann.insidead.models.TargetingFilters

/**
 * Production-like preroll ad scenario that matches the main app implementation.
 * This demonstrates the exact flow used in production:
 * 1. Show loading indicator
 * 2. Request preroll ad with timeout safety
 * 3. Set layout to MATCH_PARENT when ad loads
 * 4. Use FILL resize mode for proper fullscreen display
 * 5. Open next screen (simulated content) when ad completes
 * 6. Handle all error cases and cleanup properly
 */
class PrerollProductionActivity : AppCompatActivity() {

    private val TAG = "PrerollProduction"
    private lateinit var prerollAdView: InsideAdView
    private lateinit var loadingProgressBar: ProgressBar
    private var hasOpenedNextScreen = false
    private var timeoutHandler: Handler? = null
    private var prerollAd: InsideAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate - Starting PrerollProductionActivity")
        setContentView(R.layout.activity_preroll_production)

        prerollAdView = findViewById(R.id.prerollAdView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)

        // Setup back button handler to prevent going back during ad
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.i(TAG, "Back pressed - ignoring during preroll ad")
            }
        })

        setupPrerollAdCallback()
        requestPrerollAd()
    }

    private fun setupPrerollAdCallback() {
        InsideAdSdk.setPrerollAdCallback(object : InsideAdCallback {
            override fun insideAdReceived(insideAd: InsideAd) {
                Log.i(TAG, "Preroll ad received: ${insideAd.name}")
                prerollAd = insideAd
                // Cancel timeout since SDK is responding - it will call other callbacks
                cancelTimeout()
                // Start a new longer timeout in case the ad never loads
                startLongTimeout()
            }

            override fun insideAdLoaded() {
                Log.i(TAG, "Preroll ad loaded - playing")
                cancelTimeout()
                loadingProgressBar.visibility = View.GONE

                // CRITICAL: Set layout params to match_parent for height
                // This ensures the ad fills the screen in landscape
                setPrerollAdViewLayoutParams()

                // Set resize mode to FILL to fill screen properly
                // FILL maintains aspect ratio and centers the content
                prerollAdView.setResizeMode(InsideAdView.ResizeMode.FILL)

                prerollAdView.playAd()
            }

            override fun insideAdPlay() {
                Log.i(TAG, "Preroll ad playing")
            }

            override fun insideAdStop() {
                Log.i(TAG, "Preroll ad completed - opening next screen")
                cancelTimeout()
                openNextScreen()
            }

            override fun insideAdSkipped() {
                Log.i(TAG, "Preroll ad skipped - opening next screen")
                cancelTimeout()
                prerollAdView.stopAd()
                openNextScreen()
            }

            override fun insideAdClicked() {
                Log.i(TAG, "Preroll ad clicked")
            }

            override fun insideAdError(error: String) {
                Log.i(TAG, "Preroll ad error: $error - opening next screen anyway")
                cancelTimeout()
                loadingProgressBar.visibility = View.GONE
                openNextScreen()
            }

            override fun insideAdVolumeChanged(level: Int) {
                Log.i(TAG, "Preroll ad volume changed: $level")
            }
        })
    }

    private fun cancelTimeout() {
        timeoutHandler?.removeCallbacksAndMessages(null)
        timeoutHandler = null
    }

    private fun startTimeout() {
        timeoutHandler = Handler(Looper.getMainLooper())
        timeoutHandler?.postDelayed({
            Log.i(TAG, "Preroll ad timeout (SDK not responding) - opening next screen anyway")
            openNextScreen()
        }, 5000) // 5 second timeout for SDK to respond
    }

    private fun startLongTimeout() {
        timeoutHandler = Handler(Looper.getMainLooper())
        timeoutHandler?.postDelayed({
            Log.i(TAG, "Preroll ad long timeout (ad not loading) - opening next screen anyway")
            openNextScreen()
        }, 30000) // 30 second timeout for ad to load/play
    }

    private fun requestPrerollAd() {
        Log.i(TAG, "Requesting preroll ad")

        // Start timeout safety - if SDK doesn't respond in 5 seconds, open next screen anyway
        startTimeout()


        InsideAdSdk.requestPrerollAd(
            context = this,
            adContainer = prerollAdView,
            screen = "Video Player",
            isAdMuted = true,
            targetingFilters = null
        )
    }

    private fun setPrerollAdViewLayoutParams() {
        // Set layout params to fill screen (critical for landscape fullscreen)
        // Using MATCH_PARENT for both width and height ensures proper fullscreen display
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        prerollAdView.layoutParams = layoutParams
        prerollAdView.visibility = View.VISIBLE

        Log.i(TAG, "Layout params set to MATCH_PARENT for fullscreen display")
    }

    private fun openNextScreen() {
        // Prevent double-opening (multiple callbacks firing)
        if (hasOpenedNextScreen) {
            Log.i(TAG, "Next screen already opened, ignoring duplicate call")
            return
        }

        Log.i(TAG, "Opening next screen (simulated content)")
        hasOpenedNextScreen = true

        cancelTimeout()
        loadingProgressBar.visibility = View.GONE

        // In production this would open PlayerActivity
        // For demo, we'll just go back to MainActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)

        // Finish this activity so user can't go back to ad
        finishAndRemoveTask()
    }

    override fun onStop() {
        super.onStop()
        // If activity is stopping and we haven't opened next screen yet, open it now
        // This handles cases where the activity is being destroyed unexpectedly
        if (!hasOpenedNextScreen) {
            Log.i(TAG, "Activity stopping without opening next screen - opening now")
            openNextScreen()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy - Cleaning up")

        // Cancel timeout
        cancelTimeout()

        // Stop ad playback
        prerollAdView.stopAd()

        // CRITICAL: Clean up callbacks and cancel ongoing requests to prevent
        // callbacks from firing after activity is destroyed
        prerollAdView.cancelAdRequest()

        Log.i(TAG, "Callbacks and requests cleaned up")
    }
}
