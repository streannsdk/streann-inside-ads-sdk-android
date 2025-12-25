package com.streann.insidead.demo

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.streann.insidead.InsideAdSdk
import com.streann.insidead.InsideAdView
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.models.InsideAd
import com.streann.insidead.models.TargetingFilters

class PrerollActivity : AppCompatActivity() {

    private val TAG = this.javaClass.simpleName
    private var prerollAd: InsideAd? = null
    private var regularAd: InsideAd? = null
    private var prerollAdView: InsideAdView? = null
    private var regularAdView: InsideAdView? = null
    private var contentView: View? = null
    private var statusText: TextView? = null
    private var requestPrerollButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preroll)
        setupViews()
        setupPrerollAd()
        setupRegularAd()

        // Auto-start preroll ad
        requestPreroll()
    }

    private fun setupViews() {
        prerollAdView = findViewById(R.id.prerollAdView)
        regularAdView = findViewById(R.id.regularAdView)
        contentView = findViewById(R.id.contentView)
        statusText = findViewById(R.id.statusText)
        requestPrerollButton = findViewById(R.id.requestPrerollButton)

        requestPrerollButton?.setOnClickListener {
            requestPreroll()
        }
    }

    private fun setupPrerollAd() {
        // Set preroll callback
        InsideAdSdk.setPrerollAdCallback(object : InsideAdCallback {
            override fun insideAdReceived(insideAd: InsideAd) {
                Log.i(TAG, "PREROLL - insideAdReceived: ${insideAd.name}")
                prerollAd = insideAd
                statusText?.text = "Preroll ad received: ${insideAd.name}"
            }

            override fun insideAdLoaded() {
                Log.i(TAG, "PREROLL - insideAdLoaded")
                statusText?.text = "Preroll ad loaded - Playing now..."

                // Hide everything except preroll ad
                contentView?.visibility = View.GONE
                regularAdView?.visibility = View.GONE

                setPrerollAdViewLayoutParams()
                prerollAdView?.playAd()
            }

            override fun insideAdPlay() {
                Log.i(TAG, "PREROLL - insideAdPlay")
                statusText?.text = "Preroll ad playing..."
            }

            override fun insideAdStop() {
                Log.i(TAG, "PREROLL - insideAdStop")
                statusText?.text = "Preroll completed! Starting main content..."

                // Hide preroll ad
                prerollAdView?.visibility = View.GONE

                // Show content
                contentView?.visibility = View.VISIBLE

                // Simulate content starting, then request regular ad after delay
                statusText?.postDelayed({
                    statusText?.text = "Main content playing... (requesting regular ad)"
                    requestRegularAd()
                }, 1000)
            }

            override fun insideAdSkipped() {
                Log.i(TAG, "PREROLL - insideAdSkipped")
                statusText?.text = "Preroll ad skipped"
                prerollAdView?.stopAd()
            }

            override fun insideAdClicked() {
                Log.i(TAG, "PREROLL - insideAdClicked")
            }

            override fun insideAdError(error: String) {
                Log.e(TAG, "PREROLL - insideAdError: $error")
                statusText?.text = "Preroll error: $error\nStarting main content..."

                // On error, skip to content and request regular ad
                prerollAdView?.visibility = View.GONE
                contentView?.visibility = View.VISIBLE

                statusText?.postDelayed({
                    statusText?.text = "Main content playing (no preroll available)"
                    requestRegularAd()
                }, 1000)
            }

            override fun insideAdVolumeChanged(level: Int) {
                Log.i(TAG, "PREROLL - insideAdVolumeChanged: $level")
            }
        })
    }

    private fun setupRegularAd() {
        // Set regular ad callback (same callback that was set before preroll implementation)
        InsideAdSdk.setInsideAdCallback(object : InsideAdCallback {
            override fun insideAdReceived(insideAd: InsideAd) {
                Log.i(TAG, "REGULAR - insideAdReceived: ${insideAd.name}")
                regularAd = insideAd
                statusText?.text = "Regular ad received: ${insideAd.name}"
            }

            override fun insideAdLoaded() {
                Log.i(TAG, "REGULAR - insideAdLoaded")
                statusText?.text = "Regular ad loaded - Playing now..."

                // Keep content visible, show ad on top
                setRegularAdViewLayoutParams()
                regularAdView?.playAd()
            }

            override fun insideAdPlay() {
                Log.i(TAG, "REGULAR - insideAdPlay")
                statusText?.text = "Regular ad playing..."
            }

            override fun insideAdStop() {
                Log.i(TAG, "REGULAR - insideAdStop")
                statusText?.text = "Regular ad stopped. Content continues... (will show another ad after interval)"

                // Hide regular ad, keep content visible
                regularAdView?.visibility = View.GONE

                // The SDK should automatically request another ad after the interval
            }

            override fun insideAdSkipped() {
                Log.i(TAG, "REGULAR - insideAdSkipped")
                statusText?.text = "Regular ad skipped"
                regularAdView?.stopAd()
            }

            override fun insideAdClicked() {
                Log.i(TAG, "REGULAR - insideAdClicked")
            }

            override fun insideAdError(error: String) {
                Log.e(TAG, "REGULAR - insideAdError: $error")
                statusText?.text = "Regular ad error: $error"
                regularAdView?.visibility = View.GONE
            }

            override fun insideAdVolumeChanged(level: Int) {
                Log.i(TAG, "REGULAR - insideAdVolumeChanged: $level")
            }
        })
    }

    private fun requestPreroll() {
        statusText?.text = "Requesting preroll ad..."
        prerollAdView?.visibility = View.GONE
        regularAdView?.visibility = View.GONE
        contentView?.visibility = View.GONE

        // IMPORTANT: Set resize mode BEFORE requesting ad for best results
        // ResizeMode.FILL fills screen width (recommended for fullscreen landscape ads)
        // Other options: FIT (default split-screen), ZOOM (crop to fill), FIXED_WIDTH, FIXED_HEIGHT
        prerollAdView?.setResizeMode(InsideAdView.ResizeMode.FILL)

        // Request preroll ad for "Video Player" screen
        InsideAdSdk.requestPrerollAd(
            context = this,
            adContainer = prerollAdView!!,
            screen = "Video Player",
            isAdMuted = true
        )
    }

    private fun requestRegularAd() {
        Log.i(TAG, "Requesting regular ad (should work with intervals as before)")

        // Create targeting filters (optional)
        val filters = TargetingFilters(
            vodId = null,
            channelId = null,
            radioId = null,
            seriesId = null,
            categoryIds = null,
            contentProviderId = null,
            contentTitle = null
        )

        // Request regular ad using the original requestAd flow
        // This should:
        // 1. Start after X seconds (startAfterSeconds)
        // 2. Repeat after interval (intervalInMinutes)
        // 3. Work exactly as it did before preroll implementation
        regularAdView?.requestAd(
            screen = "Video Player",
            isAdMuted = true,
            targetingFilters = filters
        )
    }

    private fun setPrerollAdViewLayoutParams() {
        // Always use MATCH_PARENT for both width and height to ensure proper centering
        // The resize mode (FILL/FIT) will handle aspect ratio and sizing internally
        val layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        )

        prerollAdView?.layoutParams = layoutParams
        prerollAdView?.visibility = View.VISIBLE
    }

    private fun setRegularAdViewLayoutParams() {
        // Always use MATCH_PARENT for both width and height to ensure proper centering
        // The resize mode (FILL/FIT) will handle aspect ratio and sizing internally
        val layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        )

        regularAdView?.layoutParams = layoutParams
        regularAdView?.visibility = View.VISIBLE
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        Log.d(TAG, "onConfigurationChanged - Orientation: ${newConfig.orientation}")

        // When orientation changes, re-apply layout params for visible ad views
        // This ensures the ConstraintLayout.LayoutParams are updated for the new orientation
        if (prerollAdView?.visibility == View.VISIBLE) {
            Log.d(TAG, "Updating preroll ad view layout params for new orientation")
            setPrerollAdViewLayoutParams()
        }

        if (regularAdView?.visibility == View.VISIBLE) {
            Log.d(TAG, "Updating regular ad view layout params for new orientation")
            setRegularAdViewLayoutParams()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy - Cleaning up callbacks and canceling ad requests")

        // IMPORTANT: Cancel preroll ad request and remove callback
        // This prevents callbacks from firing after the activity is destroyed
        InsideAdSdk.cancelPrerollAdRequest()

        // Also remove regular ad callback
        InsideAdSdk.removeInsideAdCallback()

        // Cancel any ongoing ad requests in the ad views
        prerollAdView?.cancelAdRequest()
        regularAdView?.cancelAdRequest()
    }
}
