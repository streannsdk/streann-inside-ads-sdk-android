package com.streann.insidead.demo

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayout
import com.streann.insidead.InsideAdSdk
import com.streann.insidead.InsideAdView
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.models.InsideAd
import com.streann.insidead.models.TargetingFilters

/**
 * Player with regular ads scenario.
 * This demonstrates the exact flow used in production for mid-roll/regular ads:
 * 1. Create InsideAdView programmatically with ResizeMode.FIT
 * 2. Request regular ad (not preroll)
 * 3. Add view to container when ad is received
 * 4. Show ad in split-screen using FlexboxLayout (ROW_REVERSE)
 * 5. Play ad and handle all states
 * 6. Remove view when ad stops or errors
 * 7. Clean up in onStop
 */
class PlayerActivity : AppCompatActivity() {

    private val TAG = "PlayerActivity"
    private lateinit var playerWrapper: FlexboxLayout
    private lateinit var insideAdContainerRight: FrameLayout
    private lateinit var insideAdView: InsideAdView

    // Counts how many times an ad error has occurred. Used to make the fallback
    // loop observable: without the one-shot fallback fix this climbs forever
    // (~every 5s); with the fix it stops at 2 (primary + single fallback).
    private var errorIterationCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate - Starting PlayerActivity")
        setContentView(R.layout.activity_player)

        playerWrapper = findViewById(R.id.player_wrapper)
        insideAdContainerRight = findViewById(R.id.inside_ad_container_right)

        // Create InsideAdView programmatically
        insideAdView = InsideAdView(applicationContext)
        // Set resize mode to FIT for split-screen ads (not fullscreen like preroll)
        insideAdView.setResizeMode(InsideAdView.ResizeMode.FIT)

        setupInsideAd()
    }

    private fun setupInsideAd() {
        Log.i(TAG, "setupInsideAd - Requesting regular ad")
        requestAd()
    }

    private fun requestAd() {
        InsideAdSdk.setInsideAdCallback(object : InsideAdCallback {
            var insideAdReceived: InsideAd? = null

            override fun insideAdReceived(insideAd: InsideAd) {
                Log.i(TAG, "insideAdReceived: ${insideAd.name}")
                insideAdReceived = insideAd
                handleInsideAdReceived(insideAd)
            }

            override fun insideAdSkipped() {
                Log.i(TAG, "insideAdSkipped")
                handleInsideAdSkipped()
            }

            override fun insideAdLoaded() {
                Log.i(TAG, "insideAdLoaded")
                handleInsideAdLoaded(insideAdReceived?.name, insideAdReceived?.adType)
            }

            override fun insideAdPlay() {
                Log.i(TAG, "insideAdPlay")
            }

            override fun insideAdStop() {
                Log.i(TAG, "insideAdStop")
                handleInsideAdStop()
            }

            override fun insideAdVolumeChanged(level: Int) {
                Log.i(TAG, "insideAdVolumeChanged: $level")
                // In production, this would adjust the player volume
            }

            override fun insideAdClicked() {
                Log.i(TAG, "insideAdClicked")
            }

            override fun insideAdError(error: String) {
                Log.i(TAG, "insideAdError: $error")
                handleInsideAdError()
            }
        })

        Log.i(TAG, "Requesting ad")
        insideAdView.requestAd(
            screen = "Video Player",
            isAdMuted = true,
            targetingFilters = null
        )
    }

    private fun handleInsideAdReceived(insideAd: InsideAd) {
        Handler(Looper.getMainLooper()).post {
            Log.i(TAG, "handleInsideAdReceived: Adding InsideAdView to container")
            if (insideAdView.parent == null) {
                addInsideAdView()
            }
        }
    }

    private fun handleInsideAdSkipped() {
        Handler(Looper.getMainLooper()).post {
            Log.i(TAG, "handleInsideAdSkipped: Stopping ad")
            insideAdView.stopAd()
        }
    }

    private fun handleInsideAdLoaded(name: String?, adType: String?) {
        Handler(Looper.getMainLooper()).post {
            Log.i(TAG, "handleInsideAdLoaded: Showing ad and playing")
            showInsideAdView(name, adType)
            insideAdView.playAd()
        }
    }

    private fun handleInsideAdStop() {
        Handler(Looper.getMainLooper()).post {
            Log.i(TAG, "handleInsideAdStop: Hiding ad view")
            hideInsideAdView()
        }
    }

    private fun handleInsideAdError() {
        errorIterationCount++
        Handler(Looper.getMainLooper()).post {
            Log.i(TAG, "handleInsideAdError: iteration #$errorIterationCount - Hiding ad view")
            hideInsideAdView()
            // NOTE: Intentionally NOT calling removeInsideAdView() here.
            // Removing the view detaches it from the window, which triggers
            // InsideAdView.onDetachedFromWindow() -> cancelAdRequest(), masking
            // the fallback loop. Keeping the view attached (as real integrations
            // like flex-flix do) lets the loop run so it can be reproduced.
        }
    }

    private fun addInsideAdView() {
        Handler(Looper.getMainLooper()).postDelayed({
            val params = getInsideAdViewParams()
            insideAdContainerRight.addView(insideAdView, params)
            Log.i(TAG, "InsideAdView added to container")
        }, 0)
    }

    private fun removeInsideAdView() {
        insideAdContainerRight.removeView(insideAdView)
        Log.i(TAG, "InsideAdView removed from container")
    }

    private fun getInsideAdViewParams(): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
    }

    private fun showInsideAdView(name: String?, adType: String?) {
        // Switch to ROW_REVERSE to show ad on right side in landscape
        if (playerWrapper.flexDirection == FlexDirection.ROW) {
            playerWrapper.flexDirection = FlexDirection.ROW_REVERSE
        }
        insideAdContainerRight.visibility = View.VISIBLE
        Log.i(TAG, "InsideAdView visible - Ad: $name, Type: $adType, FlexDirection: ${playerWrapper.flexDirection}")
    }

    private fun hideInsideAdView() {
        if (insideAdContainerRight.visibility == View.VISIBLE) {
            insideAdContainerRight.visibility = View.GONE

            // Restore original flex direction
            if (playerWrapper.flexDirection == FlexDirection.ROW_REVERSE) {
                playerWrapper.flexDirection = FlexDirection.ROW
            }
            Log.i(TAG, "InsideAdView hidden, FlexDirection restored")
        }
    }

    private fun isInsideAdVisible(): Boolean {
        return insideAdContainerRight.visibility == View.VISIBLE
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.i(TAG, "onConfigurationChanged - Orientation: ${newConfig.orientation}")

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Portrait: vertical stacking (COLUMN)
            playerWrapper.flexDirection = FlexDirection.COLUMN
            Log.i(TAG, "Portrait mode - FlexDirection: COLUMN")
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Landscape: horizontal layout
            if (insideAdContainerRight.visibility == View.VISIBLE) {
                // Ad visible: show on right side
                playerWrapper.flexDirection = FlexDirection.ROW_REVERSE
                Log.i(TAG, "Landscape mode with ad - FlexDirection: ROW_REVERSE")
            } else {
                // No ad: normal row
                playerWrapper.flexDirection = FlexDirection.ROW
                Log.i(TAG, "Landscape mode without ad - FlexDirection: ROW")
            }
        }
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "onStop - Removing ad view")
        removeInsideAdView()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy - Cleaning up")

        // Stop ad playback
        insideAdView.stopAd()

        // Cancel any pending requests
        insideAdView.cancelAdRequest()
    }
}
