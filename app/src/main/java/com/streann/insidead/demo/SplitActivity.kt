package com.streann.insidead.demo

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.streann.insidead.SplitInsideAdView
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.models.InsideAd

class SplitActivity : AppCompatActivity() {
    private val TAG = this.javaClass.simpleName
    private lateinit var mSplitInsideAdView: SplitInsideAdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_split)

        val splitActivityWrapper = findViewById<View>(R.id.splitActivityWrapper)
        mSplitInsideAdView = SplitInsideAdView(this)
        mSplitInsideAdView.showSplitScreen(
            splitActivityWrapper,
            findViewById(android.R.id.content),
            screen = "Reels",
            isAdMuted = false,
            isInsideAdAbove = false,
            insideAdCallback = object : InsideAdCallback {
                override fun insideAdReceived(insideAd: InsideAd) {
                    Log.i(TAG, "insideAdReceived: $insideAd")
                }

                override fun insideAdLoaded() {
                    Log.i(TAG, "insideAdLoaded")
                }

                override fun insideAdPlay() {
                    Log.i(TAG, "insideAdPlay")
                }

                override fun insideAdStop() {
                    Log.i(TAG, "insideAdStop")
                }

                override fun insideAdSkipped() {
                    Log.i(TAG, "insideAdSkipped")
                }

                override fun insideAdClicked() {
                    Log.i(TAG, "insideAdClicked")
                }

                override fun insideAdError(error: String) {
                    Log.i(TAG, "insideAdError: $error")
                }

                override fun insideAdVolumeChanged(level: Int) {
                    Log.i(TAG, "insideAdVolumeChanged: $level")
                }
            })
    }

}