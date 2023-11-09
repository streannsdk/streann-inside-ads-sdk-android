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
            mInsideAdView?.requestAd(
                screen = "",
                isAdMuted = true,
                insideAdCallback = object : InsideAdCallback {
                    override fun insideAdReceived(insideAd: InsideAd) {
                        Log.i(TAG, "insideAdReceived: $insideAd")
                    }

                    override fun insideAdLoaded() {
                        Log.i(TAG, "insideAdLoaded")
                    }

                    override fun insideAdPlay() {
                        Log.i(TAG, "insideAdPlay")
                        adProgressText.text = ""
                        mInsideAdView?.visibility = View.VISIBLE
                    }

                    override fun insideAdStop() {
                        Log.i(TAG, "insideAdStop")
                        adProgressText.text = "Show Ad"
                        mInsideAdView?.visibility = View.INVISIBLE
                    }

                    override fun insideAdSkipped() {
                        Log.i(TAG, "insideAdSkipped")
                    }

                    override fun insideAdClicked() {
                        Log.i(TAG, "insideAdClicked")
                    }

                    override fun insideAdError(error: String) {
                        Log.i(TAG, "insideAdError: $error")
                        adProgressText.text = "Error: $error \nShow Ad"
                    }

                    override fun insideAdVolumeChanged(level: Int) {
                        Log.i(TAG, "insideAdVolumeChanged: $level")
                    }
                })
        }
    }

}