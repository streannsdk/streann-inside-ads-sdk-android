package com.streann.insidead.demo

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.streann.insidead.InsideAdView
import com.streann.insidead.callbacks.InsideAdCallback

class MainActivity : AppCompatActivity() {
    private val TAG = "InsideAdStreann"
    private var mInsideAdView: InsideAdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupInsideAdView()
    }

    private fun setupInsideAdView() {
        mInsideAdView = findViewById(R.id.insideAdView)
        mInsideAdView!!.initializeSdk(
            "559ff7ade4b0d0aff40888dd", bundleId = packageName,
            appName = getString(R.string.app_name), appVersion = "1.0", userGender = "Female"
        )

        val showInsideAd = findViewById<TextView>(R.id.showInsideAd)
        showInsideAd.setOnClickListener {
            mInsideAdView?.requestAd("", object : InsideAdCallback {
                override fun insideAdReceived() {
                    Log.i(TAG, "insideAdReceived: ")
                }

                override fun insideAdBuffering() {
                    Log.i(TAG, "insideAdBuffering: ")
                }

                override fun insideAdLoaded() {
                    Log.i(TAG, "insideAdLoaded: ")
                }

                override fun insideAdPlay() {
                    Log.i(TAG, "insideAdPlay: ")
                }

                override fun insideAdResume() {
                    Log.i(TAG, "insideAdResume: ")
                }

                override fun insideAdPause() {
                    Log.i(TAG, "insideAdPause: ")
                }

                override fun insideAdStop() {
                    Log.i(TAG, "insideAdStop: ")
                }

                override fun insideAdError() {
                    Log.i(TAG, "insideAdError: ")
                }

                override fun insideAdError(error: String) {
                    Log.i(TAG, "insideAdError: $error")
                }

                override fun insideAdVolumeChanged(level: Float) {
                    Log.i(TAG, "insideAdVolumeChanged: $level")
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mInsideAdView?.shutdownInsideAdExecutor()
    }
}