package com.streann.insidead.demo.application

import android.app.Application
import com.streann.insidead.InsideAdSdk

class AppController : Application() {

    override fun onCreate() {
        super.onCreate()

        InsideAdSdk.initializeSdk(
            "61290efae4b0304f3eb75567",
            "ced862e686294f3097b8737c3af35e8565d45e2f8bfa4b8ba93a935435d58c7f",
            "https://inside-ads.services.c1.streann.com/"
        )
    }

}