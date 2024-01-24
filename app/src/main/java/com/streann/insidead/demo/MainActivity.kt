package com.streann.insidead.demo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
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
        val adStopText = findViewById<TextView>(R.id.adStopText)
        val splitActivityButton = findViewById<Button>(R.id.splitActivityButton)

        adProgressText.setOnClickListener {
            mInsideAdView?.requestAd(
                screen = "Main",
                insideAdCallback = object : InsideAdCallback {
                    override fun insideAdReceived(insideAd: InsideAd) {
                        Log.i(TAG, "insideAdReceived: $insideAd")
                    }

                    override fun insideAdLoaded() {
                        Log.i(TAG, "insideAdLoaded")
                        adProgressText.text = ""
                        adStopText?.visibility = View.VISIBLE
                        mInsideAdView?.visibility = View.VISIBLE
                        mInsideAdView?.playAd()
                    }

                    override fun insideAdPlay() {
                        Log.i(TAG, "insideAdPlay")
                    }

                    override fun insideAdStop() {
                        Log.i(TAG, "insideAdStop")
                        adProgressText.text = "Show Ad"
                        adStopText?.visibility = View.GONE
                        mInsideAdView?.visibility = View.GONE
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
                        adProgressText.text = "Error: $error \nShow Ad"
                    }

                    override fun insideAdVolumeChanged(level: Int) {
                        Log.i(TAG, "insideAdVolumeChanged: $level")
                    }
                })
        }

        adStopText?.setOnClickListener {
            mInsideAdView?.stopAd()
        }

        splitActivityButton?.setOnClickListener {
            val intent = Intent(this, SplitActivity::class.java)
            this.startActivity(intent)
        }
    }

}