package com.streann.insidead.demo

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
import com.streann.insidead.utils.enums.AdType

class PrerollActivity : AppCompatActivity() {

    private val TAG = this.javaClass.simpleName
    private var insideAd: InsideAd? = null
    private var prerollAdView: InsideAdView? = null
    private var contentView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preroll)
        setupPrerollAd()
    }

    private fun setupPrerollAd() {
        prerollAdView = findViewById(R.id.prerollAdView)
        contentView = findViewById(R.id.contentView)
        val statusText = findViewById<TextView>(R.id.statusText)
        val requestPrerollButton = findViewById<Button>(R.id.requestPrerollButton)

        // Set preroll callback
        InsideAdSdk.setPrerollAdCallback(object : InsideAdCallback {
            override fun insideAdReceived(insideAd: InsideAd) {
                Log.i(TAG, "Preroll insideAdReceived: $insideAd")
                this@PrerollActivity.insideAd = insideAd
                statusText.text = "Preroll ad received: ${insideAd.name}"
            }

            override fun insideAdLoaded() {
                Log.i(TAG, "Preroll insideAdLoaded")
                statusText.text = "Preroll ad loaded - Playing now..."

                // Hide content, show ad
                contentView?.visibility = View.GONE

                setAdViewLayoutParams()
                prerollAdView?.playAd()
            }

            override fun insideAdPlay() {
                Log.i(TAG, "Preroll insideAdPlay")
                statusText.text = "Preroll ad playing..."
            }

            override fun insideAdStop() {
                Log.i(TAG, "Preroll insideAdStop")
                statusText.text = "Preroll completed! Starting main content..."

                // Hide ad, show content
                prerollAdView?.visibility = View.GONE
                contentView?.visibility = View.VISIBLE

                // Simulate starting main content after a delay
                statusText.postDelayed({
                    statusText.text = "Main content playing..."
                }, 500)
            }

            override fun insideAdSkipped() {
                Log.i(TAG, "Preroll insideAdSkipped")
                statusText.text = "Preroll ad skipped"
                prerollAdView?.stopAd()
            }

            override fun insideAdClicked() {
                Log.i(TAG, "Preroll insideAdClicked")
            }

            override fun insideAdError(error: String) {
                Log.e(TAG, "Preroll insideAdError: $error")
                statusText.text = "Preroll error: $error\nStarting main content..."

                // On error, skip to content
                prerollAdView?.visibility = View.GONE
                contentView?.visibility = View.VISIBLE

                statusText.postDelayed({
                    statusText.text = "Main content playing (no preroll ad available)"
                }, 1000)
            }

            override fun insideAdVolumeChanged(level: Int) {
                Log.i(TAG, "Preroll insideAdVolumeChanged: $level")
            }
        })

        // Request preroll ad button
        requestPrerollButton.setOnClickListener {
            statusText.text = "Requesting preroll ad..."
            prerollAdView?.visibility = View.GONE
            contentView?.visibility = View.GONE

            // Request preroll ad for "Video Player" screen
            InsideAdSdk.requestPrerollAd(
                context = this,
                adContainer = prerollAdView!!,
                screen = "Video Player"
            )
        }

        // Auto-request preroll on activity start
        statusText.text = "Loading preroll ad..."
        InsideAdSdk.requestPrerollAd(
            context = this,
            adContainer = prerollAdView!!,
            screen = "Video Player"
        )
    }

    private fun setAdViewLayoutParams() {
        val layoutParams =
            if (insideAd?.adType == AdType.FULLSCREEN_NATIVE.value) {
                ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.MATCH_PARENT
                )
            } else {
                ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
            }

        prerollAdView?.layoutParams = layoutParams
        prerollAdView?.visibility = View.VISIBLE
    }
}
