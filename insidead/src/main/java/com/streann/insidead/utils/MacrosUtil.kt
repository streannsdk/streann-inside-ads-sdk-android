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
            .appendsPlaystoreUrl(AppController.reseller.storeUrl)
            .appendsSiteUrl(AppController.reseller.siteUrl)
            .appendsDescriptionUrl(AppController.reseller.descriptionUrl)
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
            .appendsBirthYear(AppController.user.dateOfBirth)
            .appendsGender(AppController.user.userGender)
            .appendsCachebuster(System.currentTimeMillis().toString())
            .appendsUsPrivacy("1---")

        return builder
    }

}
