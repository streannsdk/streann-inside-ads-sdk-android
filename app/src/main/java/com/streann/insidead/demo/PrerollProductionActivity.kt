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
 * 3. Create dynamic InsideAdView when ad is received
 * 4. Use FILL resize mode for proper fullscreen display
 * 5. Open next screen (simulated content) when ad completes
 * 6. Handle all error cases and cleanup properly with re-entrancy protection
 */
class PrerollProductionActivity : AppCompatActivity() {

    private val TAG = "PrerollProduction"
    private lateinit var prerollAdContainer: FrameLayout
    private lateinit var loadingProgressBar: ProgressBar
    private var hasOpenedNextScreen = false
    private var timeoutHandler: Handler? = null
    private var dynamicPrerollAdView: InsideAdView? = null
    private var isCleaningUpPrerollAd: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate - Starting PrerollProductionActivity")
        setContentView(R.layout.activity_preroll_production)

        prerollAdContainer = findViewById(R.id.prerollAdContainer)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)

        // Setup back button handler to prevent going back during ad
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.i(TAG, "Back pressed - ignoring during preroll ad")
            }
        })

        showPrerollAd()
    }

    private fun showPrerollAd() {
        Log.i(TAG, "showPrerollAd: showing preroll ad overlay")

        // Show loading indicator
        loadingProgressBar.visibility = View.VISIBLE

        // Create preroll ad view dynamically (matches PlayerActivity pattern)
        dynamicPrerollAdView = InsideAdView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            // Set resize mode BEFORE requesting ad
            setResizeMode(InsideAdView.ResizeMode.FILL)
        }

        // Add to container
        prerollAdContainer.addView(dynamicPrerollAdView, 0)
        Log.i(TAG, "Created and added dynamic preroll ad view with FILL mode")

        // Setup callbacks
        setupPrerollAdCallback()

        // Start timeout
        startTimeout()

        // Request ad
        requestPrerollAd()
    }

    private fun setupPrerollAdCallback() {
        Log.i(TAG, "Setting up preroll ad callback")
        InsideAdSdk.setPrerollAdCallback(object : InsideAdCallback {
            override fun insideAdReceived(insideAd: InsideAd) {
                Log.i(TAG, "✓ Preroll ad received: ${insideAd.name}")
                cancelTimeout()
                startLongTimeout()
            }

            override fun insideAdLoaded() {
                Log.i(TAG, "✓ Preroll ad loaded - playing")
                cancelTimeout()
                loadingProgressBar.visibility = View.GONE

                dynamicPrerollAdView?.let { adView ->
                    adView.visibility = View.VISIBLE
                    adView.playAd()
                }
            }

            override fun insideAdPlay() {
                Log.i(TAG, "Preroll ad playing")
            }

            override fun insideAdStop() {
                if (isCleaningUpPrerollAd) {
                    Log.i(TAG, "⚠️ insideAdStop called during cleanup, ignoring to prevent loop")
                    return
                }
                Log.i(TAG, "Preroll ad completed")
                hidePrerollAdAndContinue(shouldStopAd = false)
            }

            override fun insideAdSkipped() {
                if (isCleaningUpPrerollAd) {
                    Log.i(TAG, "⚠️ insideAdSkipped called during cleanup, ignoring to prevent loop")
                    return
                }
                Log.i(TAG, "Preroll ad skipped")
                hidePrerollAdAndContinue(shouldStopAd = false)
            }

            override fun insideAdClicked() {
                Log.i(TAG, "Preroll ad clicked")
            }

            override fun insideAdError(error: String) {
                if (isCleaningUpPrerollAd) {
                    Log.i(TAG, "⚠️ insideAdError called during cleanup, ignoring to prevent loop")
                    return
                }
                Log.i(TAG, "Preroll ad error: $error")
                hidePrerollAdAndContinue(shouldStopAd = false)
            }

            override fun insideAdVolumeChanged(level: Int) {
                Log.i(TAG, "Preroll ad volume changed: $level")
            }
        })
    }

    private fun cancelTimeout() {
        if (timeoutHandler != null) {
            Log.i(TAG, "Canceling preroll timeout")
            timeoutHandler?.removeCallbacksAndMessages(null)
            timeoutHandler = null
        }
    }

    private fun startTimeout() {
        Log.i(TAG, "Starting 10 second timeout for SDK response")
        cancelTimeout() // Cancel any existing timeout first
        timeoutHandler = Handler(Looper.getMainLooper())
        timeoutHandler?.postDelayed({
            Log.i(TAG, "Preroll ad timeout (SDK not responding after 10s)")
            hidePrerollAdAndContinue(shouldStopAd = true)
        }, 10000) // 10 second timeout - increased from 5s to match PlayerActivity
    }

    private fun startLongTimeout() {
        Log.i(TAG, "Starting 30 second timeout for ad loading")
        cancelTimeout() // Cancel previous timeout
        timeoutHandler = Handler(Looper.getMainLooper())
        timeoutHandler?.postDelayed({
            Log.i(TAG, "Preroll ad long timeout (ad not loading after 30s)")
            hidePrerollAdAndContinue(shouldStopAd = true)
        }, 30000) // 30 second timeout
    }

    private fun hidePrerollAdAndContinue(shouldStopAd: Boolean = true) {
        Log.i(TAG, "hidePrerollAdAndContinue: continuing to next screen (shouldStopAd=$shouldStopAd)")

        // Prevent re-entrancy
        if (isCleaningUpPrerollAd) {
            Log.i(TAG, "⚠️ Already cleaning up preroll ad, returning early")
            return
        }
        isCleaningUpPrerollAd = true  // SET FLAG BEFORE SDK CALLS

        cancelTimeout()

        // Stop ad and remove the dynamic view completely
        dynamicPrerollAdView?.let { adView ->
            // Only call stopAd if we're forcibly stopping (timeout), not if SDK already stopped (callbacks)
            if (shouldStopAd) {
                adView.stopAd()
            }

            // Remove from parent view hierarchy
            prerollAdContainer.removeView(adView)
            Log.i(TAG, "Removed dynamic preroll ad view from hierarchy")
        }

        // Nullify the reference for garbage collection
        dynamicPrerollAdView = null

        loadingProgressBar.visibility = View.GONE

        // Clean up callback - this internally calls stopAd() which may trigger callbacks
        InsideAdSdk.cancelPrerollAdRequest()

        // Reset flag after cleanup is complete
        isCleaningUpPrerollAd = false

        // Open next screen
        openNextScreen()
    }

    private fun requestPrerollAd() {
        Log.i(TAG, "Requesting preroll ad")

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

        // Reset flags
        isCleaningUpPrerollAd = false

        // Cancel timeout
        cancelTimeout()

        // Clean up dynamic ad view
        dynamicPrerollAdView?.let { adView ->
            adView.stopAd()
            adView.cancelAdRequest()
            prerollAdContainer.removeView(adView)
        }
        dynamicPrerollAdView = null

        // Clean up preroll callback
        InsideAdSdk.cancelPrerollAdRequest()

        Log.i(TAG, "Callbacks and requests cleaned up")
    }
}
