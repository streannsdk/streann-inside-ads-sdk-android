package com.streann.insidead.demo.application

import android.app.Application
import com.streann.insidead.InsideAdSdk

class AppController : Application() {

    override fun onCreate() {
        super.onCreate()

        InsideAdSdk.initializeSdk(
            "61290efae4b0304f3eb75567",
            "https://inside-ads.services.c1.streann.com/"
        )
    }

}