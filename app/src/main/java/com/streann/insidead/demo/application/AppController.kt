package com.streann.insidead.demo.application

import android.app.Application
import com.streann.insidead.InsideAdSdk

class AppController : Application() {

    override fun onCreate() {
        super.onCreate()

        InsideAdSdk.initializeSdk(
            "babe0a4fcd3f42c1848bcf932e1e95ca833392dad9e9487ab7fb2af20ddffd81",
            "https://inside-ads.services.c1.streann.com/"
        )
    }

}