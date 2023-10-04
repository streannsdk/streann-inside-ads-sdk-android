package com.streann.insidead.application

import android.app.Application
import com.streann.insidead.models.ResellerInfo
import com.streann.insidead.models.UserInfo

class AppController : Application() {

    companion object {
        lateinit var mInstance: AppController
//        lateinit var reseller: ResellerInfo
//        lateinit var user: UserInfo

        lateinit var macrosHashMap: HashMap<String, Any>
    }

    override fun onCreate() {
        super.onCreate()
        mInstance = this

        macrosHashMap = HashMap()
        populateMacrosHashMap(macrosHashMap)
    }

    private fun populateMacrosHashMap(macrosHashMap: HashMap<String, Any>) {
        macrosHashMap["PLAYER_WIDTH"] = "[STREANN-PLAYER-WIDTH]"
        macrosHashMap["PLAYER_HEIGHT"] = "[STREANN-PLAYER-HEIGHT]"
        macrosHashMap["STORE_ID"] = "[STREANN-STORE-BUNDLE-ID]"
        macrosHashMap["PACKAGE"] = "[STREANN-APP-BUNDLE-ID]"
        macrosHashMap["APP_NAME"] = "[STREANN-APP-NAME]"
        macrosHashMap["APP_VERSION"] = "[STREANN-APP-VERSION]"
        macrosHashMap["DOMAIN"] = "[STREANN-APP-DOMAIN]"
        macrosHashMap["PLAYSTORE_URL"] = "[STREANN-APP-STORE-URL]"
        macrosHashMap["SITE_URL"] = "[STREANN-SITE_URL]"
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
        macrosHashMap["LOCATION_LAT"] = "[STREANN-LOCATION-LAT]"
        macrosHashMap["LOCATION_LONG"] = "[STREANN-LOCATION-LONG]"
        macrosHashMap["COUNTRY"] = "[STREANN-COUNTRY-ID]"
        macrosHashMap["AD_NOT_TRACKING"] = "[STREANN-DO-NOT-TRACK]"
        macrosHashMap["GDPR"] = "[STREANN-GDPR]"
        macrosHashMap["GDPR_CONSENT"] = "[STREANN-GDPR-CONSENT]"
        macrosHashMap["BIRTH_YEAR"] = "[STREANN-USER-BIRTHYEAR]"
        macrosHashMap["GENDER"] = "[STREANN-USER-GENDER]"
        macrosHashMap["CACHEBUSTER"] = "[STREANN-CACHEBUSTER]"
        macrosHashMap["US_PRIVACY"] = "[STREANN-DESCRIPTION-URL]"
    }

}