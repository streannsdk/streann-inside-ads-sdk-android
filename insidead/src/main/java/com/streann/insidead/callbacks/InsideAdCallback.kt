package com.streann.insidead.callbacks

import com.streann.insidead.models.InsideAd

interface InsideAdCallback {

    fun insideAdReceived(insideAd: InsideAd)

    fun insideAdBuffering()

    fun insideAdLoaded()

    fun insideAdPlay()

    fun insideAdResume()

    fun insideAdPause()

    fun insideAdStop()

    fun insideAdError()

    fun insideAdError(error: String)

    fun insideAdVolumeChanged(level: Float)

}