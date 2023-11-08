package com.streann.insidead

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.streann.insidead.callbacks.InsideAdCallback
import com.streann.insidead.models.InsideAd

class InsideAdPlayer constructor(context: Context) :
    FrameLayout(context) {

    init {
        init()
    }

    private fun init() {
        LayoutInflater.from(context).inflate(R.layout.inside_ad_player, this)
    }

    fun playAd(insideAd: InsideAd, insideAdCallback: InsideAdCallback) {}

}