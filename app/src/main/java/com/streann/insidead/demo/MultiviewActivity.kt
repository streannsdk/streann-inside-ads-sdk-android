package com.streann.insidead.demo

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.streann.insidead.InsideAdSdk
import com.streann.insidead.InsideAdView
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.models.InsideAd
import com.streann.insidead.utils.enums.ViewType

/**
 * Exercises the two multiview ad slots at the same time.
 *
 * This is the scenario the old single-slot design could not express: the canvas ad and the right
 * bar ad are requested together, and each must keep its own mute state, timings and callbacks.
 * Watch the two status lines - if one slot's events start showing the other's values, the
 * per-request state has regressed.
 */
class MultiviewActivity : AppCompatActivity() {

    private companion object {
        const val TAG = "MultiviewDemo"
        /**
         * Empty on purpose. The multiview placements on this account carry no "tags", and the
         * filter only matches a tagless placement against an empty screen. If traffickers later
         * tag the multiview placements, this must become the matching tag instead.
         */
        const val SCREEN = ""
    }

    private lateinit var canvasAdView: InsideAdView
    private lateinit var rightBarAdView: InsideAdView

    private lateinit var canvasStatus: TextView
    private lateinit var rightBarStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multiview)

        canvasStatus = findViewById(R.id.canvasStatus)
        rightBarStatus = findViewById(R.id.rightBarStatus)

        InsideAdSdk.debugMode = true

        buildMockPlayerGrid()
        requestCanvasAd()
        requestRightBarAd()
    }

    /** Four placeholder tiles standing in for the real multiview players. */
    private fun buildMockPlayerGrid() {
        val grid = findViewById<GridLayout>(R.id.mockPlayerGrid)
        repeat(4) { index ->
            val tile = TextView(this).apply {
                text = "player ${index + 1}"
                gravity = android.view.Gravity.CENTER
                setTextColor(Color.parseColor("#66FFFFFF"))
                setBackgroundColor(Color.parseColor("#22FFFFFF"))
            }
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(index % 2, 1f)
                rowSpec = GridLayout.spec(index / 2, 1f)
                setMargins(4, 4, 4, 4)
            }
            grid.addView(tile, params)
        }
    }

    private fun requestCanvasAd() {
        val container = findViewById<FrameLayout>(R.id.canvasAdContainer)
        canvasAdView = InsideAdView(this).apply {
            setResizeMode(InsideAdView.ResizeMode.MATCH_CONTAINER)
        }
        container.addView(
            canvasAdView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        InsideAdSdk.requestMultiviewCanvasAd(
            adContainer = canvasAdView,
            screen = SCREEN,
            isAdMuted = true,
            callback = slotCallback("canvas", canvasStatus) { container.visibility = it }
        )
    }

    private fun requestRightBarAd() {
        val container = findViewById<FrameLayout>(R.id.rightBarAdContainer)
        rightBarAdView = InsideAdView(this).apply {
            setResizeMode(InsideAdView.ResizeMode.MATCH_CONTAINER)
        }
        container.addView(
            rightBarAdView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // Deliberately unmuted, unlike the canvas slot, so that a leak of one slot's parameters
        // into the other is immediately audible as well as visible in the logs.
        InsideAdSdk.requestMultiviewRightBarAd(
            adContainer = rightBarAdView,
            screen = SCREEN,
            isAdMuted = false,
            callback = slotCallback("rightBar", rightBarStatus) { container.visibility = it }
        )
    }

    private fun slotCallback(
        slot: String,
        status: TextView,
        onVisibility: (Int) -> Unit
    ) = object : InsideAdCallback {

        private fun report(event: String) {
            Log.i(TAG, "[$slot] $event")
            runOnUiThread { status.text = "$slot: $event" }
        }

        override fun insideAdReceived(insideAd: InsideAd) {
            report("received ${insideAd.adType} '${insideAd.name}' - loading")
            // Show the slot as soon as loading starts so the SDK's own progress spinner is
            // visible. Waiting for insideAdLoaded() leaves the slot blank for the whole buffering
            // period, which on a heavy creative looks like nothing is happening.
            runOnUiThread { onVisibility(android.view.View.VISIBLE) }
        }

        override fun insideAdLoaded() {
            report("loaded")
            runOnUiThread {
                onVisibility(android.view.View.VISIBLE)
                if (slot == "canvas") canvasAdView.playAd() else rightBarAdView.playAd()
            }
        }

        override fun insideAdPlay() = report("playing")

        override fun insideAdStop() {
            report("stopped (will repeat on interval)")
            runOnUiThread { onVisibility(android.view.View.GONE) }
        }

        override fun insideAdSkipped() = report("skipped")

        override fun insideAdClicked() = report("clicked")

        override fun insideAdError(error: String) {
            report("error: $error")
            runOnUiThread { onVisibility(android.view.View.GONE) }
        }

        override fun insideAdVolumeChanged(level: Int) = report("volume: $level")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Releases both slots' callbacks; they capture this Activity.
        InsideAdSdk.cancelAdRequest(ViewType.MULTIVIEW_CANVAS)
        InsideAdSdk.cancelAdRequest(ViewType.MULTIVIEW_RIGHT_BAR)
    }
}
