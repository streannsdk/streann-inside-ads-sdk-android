package com.streann.insidead.utils

import android.os.Build
import com.streann.insidead.models.MacrosBundle

object MacrosUtil {

    fun createDefaultMacroBuilder(): MacrosBundle.Builder {
        val builder = MacrosBundle.Builder()
        builder
            .appendsDeviceId("")
            .appendsDeviceManufacturer(Build.MANUFACTURER)
            .appendsDeviceModel(Build.MODEL)
            .appendsDeviceOS("Android")
            .appendsDeviceOSVersion(Build.VERSION.RELEASE)
            .appendsDeviceType(Build.TYPE)
            .appendsAdId("")
            .appendCryptoAdId()
            .appendsAdNotTracking(0)
            .appendsGdp(0)
            .appendsGdpConsent("")
            .appendsCachebuster(System.currentTimeMillis().toString())
            .appendsUsPrivacy("1---")

        return builder
    }

}
