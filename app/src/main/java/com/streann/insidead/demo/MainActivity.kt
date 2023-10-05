package com.streann.insidead.demo

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.streann.insidead.InsideAdView
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.models.InsideAd

class MainActivity : AppCompatActivity() {

    private val TAG = this.javaClass.simpleName
    private var mInsideAdView: InsideAdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupInsideAdView()
    }

    private fun setupInsideAdView() {
        mInsideAdView = findViewById(R.id.insideAdView)

        val adProgressText = findViewById<TextView>(R.id.adProgressText)
        adProgressText.setOnClickListener {
            mInsideAdView?.requestAd("", object : InsideAdCallback {
                override fun insideAdReceived(insideAd: InsideAd) {
                    Log.i(TAG, "insideAdReceived: $insideAd")
                }

                override fun insideAdBuffering() {
                    Log.i(TAG, "insideAdBuffering")
                }

                override fun insideAdLoaded() {
                    Log.i(TAG, "insideAdLoaded")
                    adProgressText.text = ""
                    mInsideAdView?.visibility = View.VISIBLE
                }

                override fun insideAdPlay() {
                    Log.i(TAG, "insideAdPlay")
                }

                override fun insideAdResume() {
                    Log.i(TAG, "insideAdResume")
                }

                override fun insideAdPause() {
                    Log.i(TAG, "insideAdPause")
                }

                override fun insideAdStop() {
                    Log.i(TAG, "insideAdStop")
                    adProgressText.text = "Show Ad"
                    mInsideAdView?.visibility = View.GONE
                }

                override fun insideAdError() {
                    Log.i(TAG, "insideAdError")
                }

                override fun insideAdError(error: String) {
                    Log.i(TAG, "insideAdError: $error")
                    adProgressText.text = "Error: $error \nShow Ad"
                }

                override fun insideAdVolumeChanged(level: Float) {
                    Log.i(TAG, "insideAdVolumeChanged: $level")
                }
            })
        }
    }

}