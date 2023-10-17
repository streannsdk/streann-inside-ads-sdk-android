package com.streann.insidead.utils

import android.content.Context
import android.text.TextUtils
import android.webkit.WebSettings
import com.streann.insidead.InsideAdSdk
import com.streann.insidead.models.GeoIp
import com.streann.insidead.models.InsideAd
import com.streann.insidead.models.Macros
import com.streann.insidead.models.MacrosBundle

object InsideAdHelper {

    private fun replaceMacros(url: String, keyword: String, replacement: Double?): String {
        var url: String = url
        url =
            if (replacement != 0.0) url.replace(
                keyword,
                replacement.toString()
            ) else url.replace(keyword, "")
        return url
    }

    private fun replaceMacros(url: String, keyword: String, replacement: Int): String {
        var url: String = url
        url =
            if (replacement != 0) url.replace(
                keyword,
                replacement.toString()
            ) else url.replace(keyword, "")
        return url
    }

    private fun replaceMacros(url: String, keyword: String, replacement: String?): String {
        var url: String = url
        url = if (!TextUtils.isEmpty(replacement)) replacement?.let {
            url.replace(
                keyword,
                it
            )
        }.toString() else url.replace(keyword, "")
        return url
    }

    private fun populateMacros(adUrl: String, macrosBundle: MacrosBundle): String {
        var url = adUrl

        val macrosHashMap = getMacrosHashMap()
        for (macro in macrosHashMap.keys) {
            when (val value = macrosHashMap[macro] as String) {
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
                Macros.BUNDLE_ID -> {
                    val appBundleId: String? = macrosBundle.appBundleId
                    url = replaceMacros(url, value, appBundleId)
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
                Macros.SITE_URL -> {
                    url = replaceMacros(url, value, macrosBundle.siteUrl)
                }
                Macros.STORE_URL -> {
                    url = replaceMacros(url, value, macrosBundle.storeUrl)
                }
                Macros.NETWORK -> {
                    url = replaceMacros(url, value, macrosBundle.network)
                }
                Macros.AD_ID, Macros.AD_IDFA -> {
                    url = replaceMacros(url, value, macrosBundle.adId)
                }
                Macros.AD_ID_MD5, Macros.AD_IDFA_MD5 -> {
                    url = replaceMacros(url, value, macrosBundle.adIdMd5)
                }
                Macros.AD_ID_HEX, Macros.AD_IDFA_HEX -> {
                    url = replaceMacros(url, value, macrosBundle.adIdHex)
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

    fun populateVASTURL(context: Context?, insideAd: InsideAd, geoIp: GeoIp): String? {
        val appDomain: String? = InsideAdSdk.appDomain
        val siteUrl: String? = InsideAdSdk.siteUrl
        val storeUrl: String? = InsideAdSdk.storeUrl
        val descriptionUrl: String? = InsideAdSdk.descriptionUrl
        val userBirthYear: Int = InsideAdSdk.userBirthYear ?: 0
        val userGender: String? = InsideAdSdk.userGender

        val macros: MacrosBundle = MacrosUtil.createDefaultMacroBuilder()
            .appendsDomain(appDomain)
            .appendsPlayerWidth(InsideAdSdk.playerWidth)
            .appendsPlayerHeight(InsideAdSdk.playerHeight)
            .appendsLatitude(geoIp.latitude!!.toDouble())
            .appendsLongitude(geoIp.longitude!!.toDouble())
            .appendsNetwork(geoIp.connType)
            .appendsCarrier(geoIp.asName)
            .appendsIpAddress(geoIp.ip)
            .appendsCountry(geoIp.countryCode)
            .appendsStoreUrl(storeUrl)
            .appendsSiteUrl(siteUrl)
            .appendsDescriptionUrl(descriptionUrl)
            .appendsBirthYear(userBirthYear)
            .appendsGender(userGender)
            .appendsUserAgent(WebSettings.getDefaultUserAgent(context))
            .build()

        var url: String? = insideAd.url
        url = url?.let { populateMacros(it, macros) }
        return url
    }

    private fun getMacrosHashMap(): HashMap<String, Any> {
        val macrosHashMap: HashMap<String, Any> = HashMap()

        macrosHashMap["PLAYER_WIDTH"] = "[STREANN-PLAYER-WIDTH]"
        macrosHashMap["PLAYER_HEIGHT"] = "[STREANN-PLAYER-HEIGHT]"
        macrosHashMap["BUNDLE_ID"] = "[STREANN-APP-BUNDLE-ID]"
        macrosHashMap["APP_NAME"] = "[STREANN-APP-NAME]"
        macrosHashMap["APP_VERSION"] = "[STREANN-APP-VERSION]"
        macrosHashMap["DOMAIN"] = "[STREANN-APP-DOMAIN]"
        macrosHashMap["STORE_URL"] = "[STREANN-APP-STORE-URL]"
        macrosHashMap["SITE_URL"] = "[STREANN-SITE-URL]"
        macrosHashMap["CONTENT_ID"] = "[STREANN-CONTENT-ID]"
        macrosHashMap["CONTENT_TITLE"] = "[STREANN-CONTENT-TITLE]"
        macrosHashMap["CONTENT_LENGTH"] = "[STREANN-CONTENT-LENGTH]"
        macrosHashMap["CONTENT_URL"] = "[STREANN-CONTENT-URL]"
        macrosHashMap["CONTENT_ENCODED"] = "[STREANN-CONTENT-ENCODED-URL]"
        macrosHashMap["DESCRIPTION_URL_VAR"] = "[STREANN-DESCRIPTION-URL]"
        macrosHashMap["DEVICE_ID"] = "[STREANN-DEVICE-ID]"
        macrosHashMap["NETWORK"] = "[STREANN-NETWORK]"
        macrosHashMap["CARRIER"] = "[STREANN-CARRIER]"
        macrosHashMap["DEVICE_MANUFACTURER"] = "[STREANN-DEVICE-MANUFACTURER]"
        macrosHashMap["DEVICE_MODEL"] = "[STREANN-DEVICE-MODEL]"
        macrosHashMap["DEVICE_OS"] = "[STREANN-DEVICE-OS]"
        macrosHashMap["DEVICE_OS_VERSION"] = "[STREANN-DEVICE-OS-VERSION]"
        macrosHashMap["DEVICE_TYPE"] = "[STREANN-DEVICE-TYPE]"
        macrosHashMap["IP"] = "[STREANN-IP]"
        macrosHashMap["USER_AGENT"] = "[STREANN-UA]"
        macrosHashMap["IFA_TYPE"] = "[STREANN-IFA-TYPE]"
        macrosHashMap["AD_ID"] = "[STREANN-ADVERTISING-ID]"
        macrosHashMap["AD_ID_HEX"] = "[STREANN-ADVERTISING-ID-HEX]"
        macrosHashMap["AD_ID_MD5"] = "[STREANN-ADVERTISING-ID-MD5]"
        macrosHashMap["AD_IDFA"] = "[STREANN-IDFA]"
        macrosHashMap["AD_IDFA_MD5"] = "[STREANN-IDFA-MD5]"
        macrosHashMap["AD_IDFA_HEX"] = "[STREANN-IDFA-HEX]"
        macrosHashMap["LOCATION_LAT"] = "[STREANN-LOCATION-LAT]"
        macrosHashMap["LOCATION_LONG"] = "[STREANN-LOCATION-LONG]"
        macrosHashMap["COUNTRY"] = "[STREANN-COUNTRY-ID]"
        macrosHashMap["AD_NOT_TRACKING"] = "[STREANN-DO-NOT-TRACK]"
        macrosHashMap["GDPR"] = "[STREANN-GDPR]"
        macrosHashMap["GDPR_CONSENT"] = "[STREANN-GDPR-CONSENT]"
        macrosHashMap["BIRTH_YEAR"] = "[STREANN-USER-BIRTHYEAR]"
        macrosHashMap["GENDER"] = "[STREANN-USER-GENDER]"
        macrosHashMap["CACHEBUSTER"] = "[STREANN-CACHEBUSTER]"
        macrosHashMap["US_PRIVACY"] = "[STREANN-US-PRIVACY]"

        return macrosHashMap
    }

}