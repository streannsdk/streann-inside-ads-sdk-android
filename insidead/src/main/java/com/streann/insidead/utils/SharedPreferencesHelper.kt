package com.streann.insidead.utils

import android.text.TextUtils
import com.streann.insidead.InsideAdSdk
import com.streann.insidead.utils.constants.SharedPrefKeys

object SharedPreferencesHelper {

    private val sharedPreferences = InsideAdSdk.appPreferences

    private fun saveDeviceId(deviceId: String?) {
        sharedPreferences?.edit()?.putString(SharedPrefKeys.PREF_DEVICE_ID, deviceId)
            ?.apply()
    }

    fun getDeviceId(): String? {
        var deviceId: String? =
            sharedPreferences?.getString(SharedPrefKeys.PREF_DEVICE_ID, "")
        if (TextUtils.isEmpty(deviceId)) {
            deviceId = Helper.generateDeviceId()
            saveDeviceId(deviceId)
        }
        return deviceId
    }

    fun putAdId(adId: String?) {
        sharedPreferences?.edit()?.putString(SharedPrefKeys.PREF_AD_ID, adId)
            ?.apply()
    }

    fun getAdId(): String? {
        return sharedPreferences?.getString(SharedPrefKeys.PREF_AD_ID, "")
    }

    fun putAdLimitTracking(limitAdTrackingEnabled: Boolean) {
        var limit = 0
        if (limitAdTrackingEnabled) {
            limit = 1
        }
        sharedPreferences?.edit()
            ?.putInt(SharedPrefKeys.PREF_AD_LIMIT_TRACKING, limit)?.apply()
    }

    fun getAdLimitTracking(): Int {
        return sharedPreferences?.getInt(SharedPrefKeys.PREF_AD_LIMIT_TRACKING, 0) ?: 0
    }

}