package com.streann.insidead.models

import android.text.TextUtils
import com.streann.insidead.utils.Helper
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

class MacrosBundle {
    var playerWidth = 0
    var playerHeight = 0
    var gender: String? = null
    var birthYear = 0
    var gdp = 0
    var gdpConsent: String? = null
    var deviceId: String? = null
    var appName: String? = null
    var appBundleId: String? = null
    var appVersion: String? = null
    var userAgent: String? = null
    var ipAddress: String? = null
    var usPrivacy: String? = null
    var siteUrl: String? = null
    var storeUrl: String? = null
    var domain: String? = null
    var contentId: String? = null
    var contentTitle: String? = null
    var contentLength: String? = null
    var contentUrl: String? = null
    var contentEncodedUrl: String? = null
    var descriptionUrl: String? = null
    var network: String? = null
    var deviceModel: String? = null
    var deviceManufacturer: String? = null
    var deviceOS: String? = null
    var deviceOSVersion: String? = null
    var deviceType: String? = null
    var adId: String? = null
    var adIdMd5: String? = null
    var adIdHex: String? = null
    var adNotTracking = 0
    var latitude: Double? = null
    var longitude: Double? = null
    var carrier: String? = null
    var country: String? = null
    var cachebuster: String? = null

    class Builder {
        private var playerWidth = 0
        private var playerHeight = 0
        private var gender: String? = null
        private var birthYear = 0
        private var gdp = 0
        private var gdpConsent: String? = null
        private var deviceId: String? = null
        private var appName: String? = null
        private var userAgent: String? = null
        private var ipAddress: String? = null
        private var usPrivacy: String? = null
        private var storeUrl: String? = null
        private var siteUrl: String? = null
        private var appBundleId: String? = null
        private var domain: String? = null
        private var contentId = ""
        private var contentTitle = ""
        private var contentLength = ""
        private var contentUrl = ""
        private var contentEncodedUrl = ""
        private var descriptionUrl: String? = null
        private var network: String? = null
        private var deviceOS: String? = null
        private var deviceOSVersion: String? = null
        private var deviceType: String? = null
        private var adId: String? = null
        private var adIdHex: String? = null
        private var adIdMD5: String? = null
        private var latitude: Double? = null
        private var longitude: Double? = null
        private var appVersion: String? = null
        private var adNotTracking = 0
        private var deviceModel: String? = null
        private var deviceManufacturer: String? = null
        private var country: String? = null
        private var carrier: String? = null
        private var cachebuster: String? = null

        fun appendsCarrier(carrier: String?): Builder {
            this.carrier = carrier
            return this
        }

        fun appendsDeviceModel(model: String?): Builder {
            deviceModel = model
            return this
        }

        fun appendsDeviceManufacturer(deviceManufacturer: String?): Builder {
            this.deviceManufacturer = deviceManufacturer
            return this
        }

        fun appendsDomain(domain: String?): Builder {
            this.domain = domain
            return this
        }

        fun appendsDescriptionUrl(descriptionUrl: String?): Builder {
            this.descriptionUrl = descriptionUrl
            return this
        }

        fun appendsNetwork(network: String?): Builder {
            this.network = network
            return this
        }

        fun appendsDeviceOS(deviceOS: String?): Builder {
            this.deviceOS = deviceOS
            return this
        }

        fun appendsDeviceOSVersion(deviceOSVersion: String?): Builder {
            this.deviceOSVersion = deviceOSVersion
            return this
        }

        fun appendsDeviceType(deviceType: String?): Builder {
            this.deviceType = deviceType
            return this
        }

        fun appendsAdId(adId: String?): Builder {
            this.adId = adId
            return this
        }

        fun appendsCryptoAdId(): Builder {
            adId?.let {
                if (!TextUtils.isEmpty(it)) {
                    adIdMD5 = Helper.convertToMd5(it)
                    adIdHex = Helper.toHex(it)
                }
            }
            return this
        }

        fun appendsLatitude(latitude: Double): Builder {
            this.latitude = latitude
            return this
        }

        fun appendsLongitude(longitude: Double): Builder {
            this.longitude = longitude
            return this
        }

        fun appendsPlayerWidth(playerWidth: Int): Builder {
            this.playerWidth = playerWidth
            return this
        }

        fun appendsPlayerHeight(playerHeight: Int): Builder {
            this.playerHeight = playerHeight
            return this
        }

        fun appendsGender(gender: String?): Builder {
            this.gender = gender
            return this
        }

        fun appendsBirthYear(birthYear: Int): Builder {
            this.birthYear = birthYear
            return this
        }

        fun appendsGdp(gdp: Int): Builder {
            this.gdp = gdp
            return this
        }

        fun appendsGdpConsent(gdpConsent: String?): Builder {
            this.gdpConsent = gdpConsent
            return this
        }

        fun appendsDeviceId(deviceId: String?): Builder {
            this.deviceId = deviceId
            return this
        }

        fun appendsAppName(app: String?): Builder {
            appName = app
            return this
        }

        fun appendsUserAgent(userAgent: String?): Builder {
            this.userAgent = userAgent
            return this
        }

        fun appendsUsPrivacy(usPrivacy: String?): Builder {
            this.usPrivacy = usPrivacy
            return this
        }

        fun appendsIpAddress(ipAddress: String?): Builder {
            this.ipAddress = ipAddress
            return this
        }

        fun appendsStoreUrl(storeUrl: String?): Builder {
            this.storeUrl = storeUrl
            return this
        }

        fun appendsSiteUrl(siteUrl: String?): Builder {
            this.siteUrl = siteUrl
            return this
        }

        fun appendsCountry(country: String?): Builder {
            this.country = country
            return this
        }

        fun appendsBundleId(appBundleId: String?): Builder {
            this.appBundleId = appBundleId
            return this
        }

        fun appendsAppVersion(appVersion: String?): Builder {
            this.appVersion = appVersion
            return this
        }

        fun appendsCachebuster(cachebuster: String?): Builder {
            this.cachebuster = cachebuster
            return this
        }

        fun appendsAdNotTracking(adNotTracking: Int): Builder {
            this.adNotTracking = adNotTracking
            return this
        }

        fun build(): MacrosBundle {
            val macrosBundle = MacrosBundle()
            macrosBundle.playerHeight = playerHeight
            macrosBundle.gdp = gdp
            macrosBundle.playerWidth = playerWidth
            macrosBundle.gdpConsent = gdpConsent
            macrosBundle.birthYear = birthYear
            macrosBundle.gender = gender
            macrosBundle.deviceId = encode(deviceId)
            macrosBundle.appName = encode(appName)
            macrosBundle.userAgent = encode(userAgent)
            macrosBundle.usPrivacy = usPrivacy
            macrosBundle.ipAddress = encode(ipAddress)
            macrosBundle.storeUrl = encode(storeUrl)
            macrosBundle.appBundleId = encode(appBundleId)
            macrosBundle.domain = encode(domain)
            macrosBundle.contentId = encode(contentId)
            macrosBundle.contentTitle = encode(contentTitle)
            macrosBundle.contentLength = encode(contentLength)
            macrosBundle.contentUrl = encode(contentUrl)
            macrosBundle.contentEncodedUrl = encode(contentEncodedUrl)
            macrosBundle.descriptionUrl = encode(descriptionUrl)
            macrosBundle.network = encode(network)
            macrosBundle.deviceOS = encode(deviceOS)
            macrosBundle.deviceModel = encode(deviceModel)
            macrosBundle.deviceManufacturer = encode(deviceManufacturer)
            macrosBundle.deviceOSVersion = encode(deviceOSVersion)
            macrosBundle.deviceType = encode(deviceType)
            macrosBundle.adId = encode(adId)
            macrosBundle.adIdMd5 = encode(adIdMD5)
            macrosBundle.latitude = latitude
            macrosBundle.longitude = longitude
            macrosBundle.appVersion = encode(appVersion)
            macrosBundle.adNotTracking = adNotTracking
            macrosBundle.country = encode(country)
            macrosBundle.carrier = encode(carrier)
            macrosBundle.cachebuster = encode(cachebuster)
            return macrosBundle
        }

        private fun encode(s: String?): String? {
            var s = s
            return if (!TextUtils.isEmpty(s)) {
                try {
                    s = URLEncoder.encode(s, "UTF-8")
                    s
                } catch (e: UnsupportedEncodingException) {
                    ""
                }
            } else ""
        }
    }
}