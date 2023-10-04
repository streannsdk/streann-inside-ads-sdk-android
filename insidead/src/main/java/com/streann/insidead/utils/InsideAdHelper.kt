package com.streann.insidead.utils

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.streann.insidead.application.AppController
import com.streann.insidead.models.GeoIp
import com.streann.insidead.models.InsideAd
import com.streann.insidead.models.Macros
import com.streann.insidead.models.MacrosBundle

object InsideAdHelper {
    private val LOGTAG = "InsideAdStreann"

    private fun replaceMacros(url: String, keyword: String, replacement: Double?): String {
        Log.d(LOGTAG, "replaceMacros $keyword $replacement")
        var url: String = url
        url =
            if (replacement != 0.0) url.replace(
                keyword,
                replacement.toString()
            ) else url.replace(keyword, "")
        return url
    }

    private fun replaceMacros(url: String, keyword: String, replacement: Int): String {
        Log.d(LOGTAG, "replaceMacros $keyword $replacement")
        var url: String = url
        url =
            if (replacement != 0) url.replace(
                keyword,
                replacement.toString()
            ) else url.replace(keyword, "")
        return url
    }

    private fun replaceMacros(url: String, keyword: String, replacement: String?): String {
        Log.d(LOGTAG, "replaceMacros $keyword $replacement")
        var url: String = url
        url = if (!TextUtils.isEmpty(replacement)) replacement?.let {
            url.replace(
                keyword,
                it
            )
        }.toString() else url.replace(keyword, "")
        return url
    }

    private fun populateMacros(url: String, macrosBundle: MacrosBundle): String {
        var url = url
        Log.e(LOGTAG, "populateMacros - pre macros url: $url")
        val macrosHashMap = AppController.macrosHashMap

        for (macro in macrosHashMap.keys) {
            var value = macrosHashMap[macro] as String
            when (value) {
                Macros.PLAYER_WIDTH -> {
                    val playerWidth: Int = macrosBundle.playerWidth
                    url = replaceMacros(url, value, playerWidth)
                }
                Macros.PLAYER_HEIGHT -> {
                    val playerHeight: Int = macrosBundle.playerHeight
                    url = replaceMacros(url, value, playerHeight)
                }
                Macros.GENDER -> {
                    val gender: String? = macrosBundle.gender
                    url = replaceMacros(url, value, gender)
                }
                Macros.BIRTH_YEAR -> {
                    val year: Int = macrosBundle.birthYear
                    url = replaceMacros(url, value, year)
                }
                Macros.PACKAGE -> {
                    val packageName: String? = macrosBundle.packageName
                    url = replaceMacros(url, value, packageName)
                }
                Macros.STORE_ID -> {
                    val storeId: String? = macrosBundle.storeId
                    url = replaceMacros(url, value, storeId)
                }
                Macros.DEVICE_ID -> {
                    val deviceId: String? = macrosBundle.deviceId
                    url = replaceMacros(url, value, deviceId)
                }
                Macros.APP_NAME -> {
                    url = replaceMacros(url, value, macrosBundle.appName)
                }
                Macros.APP_VERSION -> {
                    url = replaceMacros(url, value, macrosBundle.appVersion)
                }
                Macros.USER_AGENT -> {
                    url = replaceMacros(url, value, macrosBundle.userAgent)
                }
                Macros.IP -> {
                    url = replaceMacros(url, value, macrosBundle.ipAddress)
                }
                Macros.PLAYSTORE_URL -> {
                    url = replaceMacros(url, value, macrosBundle.playstoreUrl)
                }
                Macros.NETWORK -> {
                    url = replaceMacros(url, value, macrosBundle.network)
                }
                Macros.AD_ID -> {
                    url = replaceMacros(url, value, macrosBundle.adId)
                }
                Macros.AD_ID_MD5 -> {
                    url = replaceMacros(url, value, macrosBundle.adIdMd5)
                }
                Macros.IFA_TYPE -> {
                    url = replaceMacros(url, value, "")
                }
                Macros.AD_NOT_TRACKING -> {
                    url = replaceMacros(
                        url,
                        value,
                        macrosBundle.adNotTracking.toString()
                    )
                }
                Macros.DOMAIN -> {
                    url = replaceMacros(url, value, macrosBundle.domain)
                }
                Macros.CONTENT_ID -> {
                    url = replaceMacros(url, value, macrosBundle.contentId)
                }
                Macros.CONTENT_TITLE -> {
                    url = replaceMacros(url, value, macrosBundle.contentTitle)
                }
                Macros.CONTENT_LENGTH -> {
                    url = replaceMacros(url, value, macrosBundle.contentLength)
                }
                Macros.CONTENT_URL -> {
                    url = replaceMacros(url, value, macrosBundle.contentUrl)
                }
                Macros.CONTENT_ENCODED -> {
                    url = replaceMacros(url, value, macrosBundle.contentEncodedUrl)
                }
                Macros.DESCRIPTION_URL_VAR -> {
                    url = replaceMacros(url, value, macrosBundle.descriptionUrl)
                }
                Macros.DEVICE_OS -> {
                    url = replaceMacros(url, value, macrosBundle.deviceOS)
                }
                Macros.DEVICE_OS_VERSION -> {
                    url = replaceMacros(url, value, macrosBundle.deviceOSVersion)
                }
                Macros.DEVICE_MODEL -> {
                    url = replaceMacros(url, value, macrosBundle.deviceModel)
                }
                Macros.DEVICE_MANUFACTURER -> {
                    url = replaceMacros(url, value, macrosBundle.deviceManufacturer)
                }
                Macros.DEVICE_TYPE -> {
                    url = replaceMacros(url, value, macrosBundle.deviceType)
                }
                Macros.CARRIER -> {
                    url = replaceMacros(url, value, macrosBundle.carrier)
                }
                Macros.LOCATION_LAT -> {
                    url = replaceMacros(url, value, macrosBundle.latitude)
                }
                Macros.LOCATION_LONG -> {
                    url = replaceMacros(url, value, macrosBundle.longitude)
                }
                Macros.CACHEBUSTER -> {
                    url = replaceMacros(url, value, macrosBundle.cachebuster)
                }
                Macros.COUNTRY -> {
                    url = replaceMacros(url, value, macrosBundle.country)
                }
                Macros.GDPR -> {
                    url = replaceMacros(url, value, macrosBundle.gdp)
                }
                Macros.GDPR_CONSENT -> {
                    url = replaceMacros(url, value, macrosBundle.gdpConsent)
                }
                Macros.US_PRIVACY -> {
                    url = replaceMacros(url, value, macrosBundle.usPrivacy)
                }
            }
        }

        return url
    }

    fun populateVASTURL(
        insideAd: InsideAd,
        geoIp: GeoIp,
        context: Context?
    ): String? {
        var url: String? = insideAd.url

        val macros: MacrosBundle = MacrosUtil.createDefaultMacroBuilder()
            .appendsLatitude(geoIp.latitude!!.toDouble())
            .appendsLongitude(geoIp.longitude!!.toDouble())
            .appendsNetwork(geoIp.connType)
            .appendsCarrier(geoIp.asName)
            .appendsIpAddress(geoIp.ip)
            .appendsCountry(geoIp.countryCode)
            .build()

        url = url?.let { populateMacros(it, macros) }

        return url
    }

}