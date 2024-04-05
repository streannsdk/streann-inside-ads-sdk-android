package com.streann.insidead.demo

import android.content.Intent
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
import com.streann.insidead.models.TargetingFilters
import com.streann.insidead.models.InsideAd
import com.streann.insidead.utils.enums.AdType

class MainActivity : AppCompatActivity() {

    private val TAG = this.javaClass.simpleName
    private var insideAd: InsideAd? = null
    private var mInsideAdView: InsideAdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupInsideAdView()
    }

    private fun setupInsideAdView() {
        mInsideAdView = findViewById(R.id.insideAdView)

        val adProgressText = findViewById<TextView>(R.id.adProgressText)
        val adStopText = findViewById<TextView>(R.id.adStopText)
        val splitActivityButton = findViewById<Button>(R.id.splitActivityButton)

        InsideAdSdk.setInsideAdCallback(object : InsideAdCallback {
            override fun insideAdReceived(insideAd: InsideAd) {
                Log.i(TAG, "insideAdReceived: $insideAd")
                this@MainActivity.insideAd = insideAd
            }

            override fun insideAdLoaded() {
                Log.i(TAG, "insideAdLoaded")
                adProgressText.text = ""
                adStopText.visibility = View.VISIBLE
                splitActivityButton.visibility = View.GONE

                setAdViewLayoutParams()
                mInsideAdView?.playAd()
            }

            override fun insideAdPlay() {
                Log.i(TAG, "insideAdPlay")
            }

            override fun insideAdStop() {
                Log.i(TAG, "insideAdStop")
                adStopText.visibility = View.GONE
                mInsideAdView?.visibility = View.GONE
                splitActivityButton.visibility = View.VISIBLE
            }

            override fun insideAdSkipped() {
                Log.i(TAG, "insideAdSkipped")
                mInsideAdView?.stopAd()
            }

            override fun insideAdClicked() {
                Log.i(TAG, "insideAdClicked")
            }

            override fun insideAdError(error: String) {
                Log.i(TAG, "insideAdError: $error")
                adProgressText.text = "Error: $error"
            }

            override fun insideAdVolumeChanged(level: Int) {
                Log.i(TAG, "insideAdVolumeChanged: $level")
            }
        })

        mInsideAdView?.requestAd(
            screen = "Splash",
            isAdMuted = false,
            targetingFilters = TargetingFilters(
                vodId = "659d4a92e4b04b818b1257db",
                seriesId = "659ef73de4b065a6e1319f88"
            )
        )

        adStopText.setOnClickListener {
            mInsideAdView?.stopAd()
        }

        splitActivityButton?.setOnClickListener {
            val intent = Intent(this, SplitActivity::class.java)
            this.startActivity(intent)
        }
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
                ).apply {
                    topToBottom = R.id.adStopText
                    topMargin = 100
                }
            }

        mInsideAdView?.layoutParams = layoutParams
        mInsideAdView?.visibility = View.VISIBLE
    }

}