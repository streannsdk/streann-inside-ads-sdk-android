package com.streann.insidead.utils

import android.os.Build
import android.webkit.WebSettings
import com.streann.insidead.application.AppController
import com.streann.insidead.models.MacrosBundle

object MacrosUtil {

    fun createDefaultMacroBuilder(): MacrosBundle.Builder {
        val builder = MacrosBundle.Builder()
        builder
            .appendsBundleId("")
            .appendsAppName("")
            .appendsAppVersion("")
            .appendsDomain("")
            .appendsPlaystoreUrl("")
            .appendsSiteUrl("")
            .appendsDescriptionUrl("")
            .appendsDeviceId("")
            .appendsDeviceManufacturer(Build.MANUFACTURER)
            .appendsDeviceModel(Build.MODEL)
            .appendsDeviceOS("Android")
            .appendsDeviceOSVersion(Build.VERSION.RELEASE)
            .appendsDeviceType(Build.TYPE)
            .appendsUserAgent(WebSettings.getDefaultUserAgent(AppController.mInstance))
            .appendsAdId("")
            .appendCryptoAdId()
            .appendsAdNotTracking(0)
            .appendsGdp(0)
            .appendsGdpConsent("")
            .appendsBirthYear(0)
            .appendsGender("")
            .appendsCachebuster(System.currentTimeMillis().toString())
            .appendsUsPrivacy("1---")

        return builder
    }

}
