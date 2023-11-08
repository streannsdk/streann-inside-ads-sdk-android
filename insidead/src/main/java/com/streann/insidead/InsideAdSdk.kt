package com.streann.insidead

import android.content.SharedPreferences
import com.streann.insidead.models.GeoIp

object InsideAdSdk {

    var apiKey: String = ""
    var baseUrl: String = ""
    var bundleId: String? = ""
    var appName: String? = ""
    var appVersion: String? = ""
    var appDomain: String? = ""
    var siteUrl: String? = ""
    var storeUrl: String? = ""
    var descriptionUrl: String? = ""
    var userBirthYear: Int? = 0
    var userGender: String? = ""
    var adId: String? = ""
    var adLimitTracking: Int? = 0
    var playerWidth: Int = 0
    var playerHeight: Int = 0
    var geoIp: GeoIp? = null
    var isAdMuted: Boolean? = false
    var appPreferences: SharedPreferences? = null

    fun initializeSdk(
        apiKey: String, baseUrl: String, appDomain: String? = "",
        siteUrl: String? = "", storeUrl: String? = "", descriptionUrl: String? = "",
        userBirthYear: Int? = 0, userGender: String? = ""
    ) {
        this.apiKey = apiKey
        this.baseUrl = baseUrl
        this.appDomain = appDomain
        this.siteUrl = siteUrl
        this.storeUrl = storeUrl
        this.descriptionUrl = descriptionUrl
        this.userBirthYear = userBirthYear
        this.userGender = userGender
    }

}