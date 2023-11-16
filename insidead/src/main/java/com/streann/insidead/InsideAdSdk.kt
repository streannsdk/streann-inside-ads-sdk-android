package com.streann.insidead

import android.content.SharedPreferences

object InsideAdSdk {

    internal var apiKey: String = ""
    internal var baseUrl: String = ""
    internal var bundleId: String? = ""
    internal var appName: String? = ""
    internal var appVersion: String? = ""
    internal var appDomain: String? = ""
    internal var siteUrl: String? = ""
    internal var storeUrl: String? = ""
    internal var descriptionUrl: String? = ""
    internal var userBirthYear: Int? = 0
    internal var userGender: String? = ""
    internal var adId: String? = ""
    internal var adLimitTracking: Int? = 0
    internal var playerWidth: Int = 0
    internal var playerHeight: Int = 0
    internal var isAdMuted: Boolean? = false
    internal var appPreferences: SharedPreferences? = null

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