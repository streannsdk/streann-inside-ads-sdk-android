package com.streann.insidead.utils

import android.util.Log
import com.streann.insidead.InsideAdSdk
import com.streann.insidead.callbacks.CampaignCallback
import com.streann.insidead.models.Campaign
import com.streann.insidead.models.GeoIp
import com.streann.insidead.models.InsideAd
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.ProtocolException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object HttpRequestsUtil {
    private val TAG = "InsideAdSdk"

    fun getGeoIpUrl(): String? {
        var jsonObject: JSONObject? = null

        try {
            val url = URL(
                InsideAdSdk.baseUrl + "v1/geo-ip-config"
            )

            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"
            urlConnection.instanceFollowRedirects = true

            val responseCode: Int = urlConnection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try {
                    val inputStream =
                        BufferedReader(InputStreamReader(urlConnection.inputStream))
                    var inputLine: String?
                    val response = StringBuffer()

                    while (inputStream.readLine().also { inputLine = it } != null) {
                        response.append(inputLine)
                    }
                    inputStream.close()

                    jsonObject = JSONObject(response.toString())
                } finally {
                    urlConnection.disconnect()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        var geoIpUrl: String? = null
        try {
            geoIpUrl = jsonObject?.let { jsonObject.getString("geoIpUrl") }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return geoIpUrl
    }

    fun getGeoIp(geoIpUrl: String): GeoIp? {
        var jsonObject: JSONObject? = null

        try {
            val url = URL(geoIpUrl)
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"
            urlConnection.instanceFollowRedirects = true
            val responseCode: Int = urlConnection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try {
                    val inputStream =
                        BufferedReader(InputStreamReader(urlConnection.inputStream))
                    var inputLine: String?
                    val response = StringBuffer()

                    while (inputStream.readLine().also { inputLine = it } != null) {
                        response.append(inputLine)
                    }
                    inputStream.close()

                    jsonObject = JSONObject(response.toString())
                } finally {
                    urlConnection.disconnect()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        var geoIp: GeoIp? = null
        try {
            geoIp = jsonObject?.let { parseGeoIpJSONResponse(it) }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return geoIp
    }

    private fun parseGeoIpJSONResponse(jsonObject: JSONObject): GeoIp? {
        val geoIp = GeoIp()

        if (jsonObject.has("AsName")) {
            try {
                geoIp.asName = jsonObject.getString("AsName")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } else {
            geoIp.asName = ""
        }

        if (jsonObject.has("ConnType")) {
            try {
                geoIp.connType = jsonObject.getString("ConnType")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } else {
            geoIp.connType = ""
        }

        if (jsonObject.has("countryCode")) {
            try {
                geoIp.countryCode = jsonObject.getString("countryCode")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } else {
            geoIp.countryCode = ""
        }

        if (jsonObject.has("latitude")) {
            try {
                geoIp.latitude = jsonObject.getString("latitude")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } else {
            geoIp.latitude = ""
        }

        if (jsonObject.has("longitude")) {
            try {
                geoIp.longitude = jsonObject.getString("longitude")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } else {
            geoIp.longitude = ""
        }

        if (jsonObject.has("ip")) {
            try {
                geoIp.ip = jsonObject.getString("ip")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } else {
            geoIp.ip = ""
        }

        return geoIp
    }

    fun getCampaign(
        apiKey: String,
        countryCode: String,
        screen: String,
        campaignCallback: CampaignCallback
    ) {
        val url: URL
        var jsonObject: JSONObject? = null
        try {
            val urlParameters = "platform=ANDROID&country=" + countryCode +
                    "&r=" + apiKey + "&screen=" + screen

            url = URL(
                InsideAdSdk.baseUrl + "v1/campaigns/app"
                        + "?" + urlParameters
            )

            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            connection.useCaches = false
            connection.connect()

            if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
                try {
                    var line: String?
                    val br = BufferedReader(
                        InputStreamReader(connection.inputStream)
                    )
                    val response = StringBuilder()

                    while (br.readLine().also { line = it } != null) {
                        response.append(line)
                    }

                    jsonObject = JSONObject(response.toString())
                } finally {
                    connection.disconnect()
                }
            }
        } catch (e: ProtocolException) {
            Log.e(TAG, "ProtocolException: ", e)
        } catch (e: MalformedURLException) {
            Log.e(TAG, "MalformedURLException: ", e)
        } catch (e: IOException) {
            Log.e(TAG, "IOException: ", e)
        } catch (e: JSONException) {
            Log.e(TAG, "JSONException: ", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ", e)
        }

        if (jsonObject == null) {
            if (campaignCallback != null) campaignCallback.onError("Error while getting AD.")
            return
        }

        var insideAd: InsideAd? = null
        try {
            insideAd = parseInsideAdJSONResponse(jsonObject)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        if (insideAd == null) {
            if (campaignCallback != null) campaignCallback.onError("No AD at the moment.")
            return
        }

        if (campaignCallback != null) {
            val campaign = Campaign(insideAd)
            campaignCallback.onSuccess(campaign);
        }
    }

    private fun parseInsideAdJSONResponse(jsonObject: JSONObject): InsideAd? {
        val insideAd = InsideAd()

        if (jsonObject.has("adId")) {
            try {
                insideAd.adId = jsonObject.getString("adId")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } else {
            insideAd.adId = ""
        }

        if (jsonObject.has("campaignId")) {
            try {
                insideAd.campaignId = jsonObject.getString("campaignId")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } else {
            insideAd.campaignId = ""
        }

        if (jsonObject.has("placementId")) {
            try {
                insideAd.placementId = jsonObject.getString("placementId")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } else {
            insideAd.placementId = ""
        }

        if (jsonObject.has("url")) {
            try {
                insideAd.url = jsonObject.getString("url")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } else {
            insideAd.url = ""
        }

        if (jsonObject.has("adType")) {
            try {
                insideAd.adType = jsonObject.getString("adType")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } else {
            insideAd.adType = ""
        }

        if (jsonObject.has("properties")) {
            try {
                insideAd.properties = jsonObject.getJSONObject("properties")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } else {
            insideAd.properties = JSONObject()
        }

        return insideAd
    }

}