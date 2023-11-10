package com.streann.insidead.callbacks

import com.streann.insidead.models.InsideAd

interface InsideAdCallback {

    fun insideAdReceived(insideAd: InsideAd)

    fun insideAdLoaded()

    fun insideAdPlay()

    fun insideAdStop()

    fun insideAdSkipped()

    fun insideAdClicked()

    fun insideAdError(error: String)

    fun insideAdVolumeChanged(level: Int)

}