package com.streann.insidead.utils

import android.os.Build
import com.streann.insidead.InsideAdSdk
import com.streann.insidead.models.MacrosBundle

object MacrosUtil {

    var bundleId: String? = InsideAdSdk.bundleId
    var appName: String? = InsideAdSdk.appName
    var appVersion: String? = InsideAdSdk.appVersion

    fun createDefaultMacroBuilder(): MacrosBundle.Builder {
        val builder = MacrosBundle.Builder()
        builder
            .appendsBundleId(bundleId)
            .appendsAppName(appName)
            .appendsAppVersion(appVersion)
            .appendsDeviceId(SharedPreferencesHelper.getDeviceId())
            .appendsDeviceManufacturer(Build.MANUFACTURER)
            .appendsDeviceModel(Build.MODEL)
            .appendsDeviceOS("Android")
            .appendsDeviceOSVersion(Build.VERSION.RELEASE)
            .appendsDeviceType(Build.TYPE)
            .appendsAdId(SharedPreferencesHelper.getAdId())
            .appendsCryptoAdId()
            .appendsAdNotTracking(SharedPreferencesHelper.getAdLimitTracking())
            .appendsGdp(0)
            .appendsGdpConsent("")
            .appendsCachebuster(System.currentTimeMillis().toString())
            .appendsUsPrivacy("1---")

        return builder
    }

}
