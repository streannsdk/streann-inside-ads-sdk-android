package com.streann.insidead.demo.application

import android.app.Application
import com.streann.insidead.InsideAdSdk

class AppController : Application() {

    override fun onCreate() {
        super.onCreate()

        InsideAdSdk.initializeSdk(
            "559ff7ade4b0d0aff40888dd",
            "https://inside-ads.services.c1.streann.com/"
        )
    }

}