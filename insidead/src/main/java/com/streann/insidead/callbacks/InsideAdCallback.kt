package com.streann.insidead.callbacks

interface InsideAdCallback {

    fun insideAdReceived()

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