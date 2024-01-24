package com.streann.insidead.demo.application

import android.app.Application
import com.streann.insidead.InsideAdSdk

class AppController : Application() {

    override fun onCreate() {
        super.onCreate()

        InsideAdSdk.initializeSdk(
            "559ff7ade4b0d0aff40888dd",
            "d04481f94ba54bba9e455cb7b2e2a69247875eb2cb804fd984c3a56a6f16b2a8",
            "https://inside-ads.services.c1.streann.com/"
        )
    }

}