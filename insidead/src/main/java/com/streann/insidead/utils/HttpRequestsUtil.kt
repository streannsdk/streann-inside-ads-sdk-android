package com.streann.insidead.utils

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.streann.insidead.InsideAdSdk
import com.streann.insidead.callbacks.CampaignCallback
import com.streann.insidead.models.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.ProtocolException
import java.net.URL
import java.time.DayOfWeek
import java.time.Instant
import javax.net.ssl.HttpsURLConnection

object HttpRequestsUtil {
    private const val TAG = "InsideAdSdk"

    fun getGeoIpUrl(): String? {
        var jsonObject: JSONObject? = null

        try {
            val url = URL(
                InsideAdSdk.baseUrl + "v1/geo-ip-config"
            )

            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"
            urlConnection.instanceFollowRedirects = true
            urlConnection.setRequestProperty("Authorization", "ApiToken ${InsideAdSdk.apiToken}")

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
        countryCode: String,
        campaignCallback: CampaignCallback
    ) {
        val url: URL
        var campaignResponseArray: JSONArray? = null

        try {
            val urlParameters = "country=$countryCode"
            url =
                URL(InsideAdSdk.baseUrl + "v1/r/" + InsideAdSdk.apiKey + "/campaigns/ANDROID?" + urlParameters)

            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            connection.useCaches = false
            connection.setRequestProperty("Authorization", "ApiToken ${InsideAdSdk.apiToken}")
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

                    campaignResponseArray = JSONArray(response.toString())
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

        if (campaignResponseArray == null) {
            if (campaignCallback != null) campaignCallback.onError("No campaigns at the moment.")
            return
        }

        var campaigns: ArrayList<Campaign>? = null
        try {
            campaigns = parseCampaignJSONResponse(campaignResponseArray)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        if (campaigns == null) {
            if (campaignCallback != null) campaignCallback.onError("No campaigns at the moment.")
            return
        }

        if (campaignCallback != null) {
            campaignCallback.onSuccess(campaigns)
        }
    }

    private fun parseCampaignJSONResponse(campaignArray: JSONArray): ArrayList<Campaign> {
        val campaigns = ArrayList<Campaign>()

        for (i in 0 until campaignArray.length()) {
            val campaignObject: JSONObject = campaignArray.getJSONObject(i)
            val campaign = Campaign()

            if (campaignObject.has("id")) {
                try {
                    campaign.id = campaignObject.getString("id")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                campaign.id = ""
            }

            if (campaignObject.has("name")) {
                try {
                    campaign.name = campaignObject.getString("name")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                campaign.name = ""
            }

            if (campaignObject.has("startDate")) {
                try {
                    val campaignStartDate = campaignObject.getString("startDate")
                    campaign.startDate = Instant.parse(campaignStartDate)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                campaign.startDate = null
            }

            if (campaignObject.has("endDate")) {
                try {
                    val campaignEndDate = campaignObject.getString("endDate")
                    campaign.endDate = Instant.parse(campaignEndDate)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                campaign.endDate = null
            }

            if (campaignObject.has("timePeriods") && !campaignObject.isNull("timePeriods")) {
                try {
                    campaign.timePeriods =
                        parseTimePeriodJSONResponse(campaignObject.getJSONArray("timePeriods"))
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                campaign.timePeriods = arrayListOf()
            }

            if (campaignObject.has("weight")) {
                try {
                    campaign.weight = campaignObject.getInt("weight")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                campaign.weight = 0
            }

            if (campaignObject.has("placements") && !campaignObject.isNull("placements")) {
                try {
                    campaign.placements =
                        parsePlacementsJSONResponse(campaignObject.getJSONArray("placements"))
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                campaign.placements = arrayListOf()
            }

            if (campaignObject.has("properties") && !campaignObject.isNull("properties")) {
                try {
                    val properties = campaignObject.getJSONObject("properties").toString()

                    val gson = Gson()
                    val type = object : TypeToken<Map<String, Int>>() {}.type
                    val map: Map<String, Int> = gson.fromJson(properties, type)

                    campaign.properties = map
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                campaign.properties = mapOf()
            }

            campaigns.add(campaign)
        }

        return campaigns
    }

    private fun parseTimePeriodJSONResponse(timePeriodArray: JSONArray): ArrayList<TimePeriod> {
        val timePeriods = ArrayList<TimePeriod>()

        for (i in 0 until timePeriodArray.length()) {
            val timePeriodObject: JSONObject = timePeriodArray.getJSONObject(i)
            val timePeriod = TimePeriod()

            if (timePeriodObject.has("daysOfWeek")) {
                try {
                    val daysOfWeekObject = timePeriodObject.getJSONArray("daysOfWeek").toString()

                    val gson = Gson()
                    val daysOfWeekList: List<DayOfWeek> =
                        gson.fromJson(daysOfWeekObject, Array<String>::class.java)
                            .map { dayString -> DayOfWeek.valueOf(dayString) }

                    timePeriod.daysOfWeek = daysOfWeekList
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                timePeriod.daysOfWeek = arrayListOf()
            }

            if (timePeriodObject.has("startTime")) {
                try {
                    timePeriod.startTime = timePeriodObject.getString("startTime")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                timePeriod.startTime = ""
            }

            if (timePeriodObject.has("endTime")) {
                try {
                    timePeriod.endTime = timePeriodObject.getString("endTime")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                timePeriod.endTime = ""
            }

            timePeriods.add(timePeriod)
        }

        return timePeriods
    }

    private fun parsePlacementsJSONResponse(placementsArray: JSONArray): ArrayList<Placement> {
        val placements = ArrayList<Placement>()

        for (i in 0 until placementsArray.length()) {
            val placementObject: JSONObject = placementsArray.getJSONObject(i)
            val placement = Placement()

            if (placementObject.has("id")) {
                try {
                    placement.id = placementObject.getString("id")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                placement.id = ""
            }

            if (placementObject.has("name")) {
                try {
                    placement.name = placementObject.getString("name")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                placement.name = ""
            }

            if (placementObject.has("viewType")) {
                try {
                    placement.viewType = placementObject.getString("viewType")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                placement.viewType = ""
            }

            if (placementObject.has("tags") && !placementObject.isNull("tags")) {
                try {
                    val tagsObject = placementObject.getJSONArray("tags").toString()

                    val gson = Gson()
                    val tagsList: ArrayList<String> = gson.fromJson(
                        tagsObject,
                        object : TypeToken<ArrayList<String>>() {}.type
                    )

                    placement.tags = tagsList
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                placement.tags = arrayListOf()
            }

            if (placementObject.has("ads") && !placementObject.isNull("ads")) {
                try {
                    placement.ads =
                        parseInsideAdJSONResponse(placementObject.getJSONArray("ads"))
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                placement.ads = arrayListOf()
            }

            if (placementObject.has("properties") && !placementObject.isNull("properties")) {
                try {
                    val properties = placementObject.getJSONObject("properties").toString()

                    val gson = Gson()
                    val type = object : TypeToken<Map<String, Int>>() {}.type
                    val map: Map<String, Int> = gson.fromJson(properties, type)

                    placement.properties = map
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                placement.properties = mapOf()
            }

            placements.add(placement)
        }

        return placements
    }

    private fun parseInsideAdJSONResponse(adsArray: JSONArray): ArrayList<InsideAd> {
        val ads = ArrayList<InsideAd>()

        for (i in 0 until adsArray.length()) {
            val adsObject: JSONObject = adsArray.getJSONObject(i)
            val insideAd = InsideAd()

            if (adsObject.has("id")) {
                try {
                    insideAd.id = adsObject.getString("id")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                insideAd.id = ""
            }

            if (adsObject.has("name")) {
                try {
                    insideAd.name = adsObject.getString("name")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                insideAd.name = ""
            }

            if (adsObject.has("weight") && !adsObject.isNull("weight")) {
                try {
                    insideAd.weight = adsObject.getInt("weight")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                insideAd.weight = 0
            }

            if (adsObject.has("adType")) {
                try {
                    insideAd.adType = adsObject.getString("adType")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                insideAd.adType = ""
            }

            if (adsObject.has("resellerId")) {
                try {
                    insideAd.resellerId = adsObject.getString("resellerId")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                insideAd.resellerId = ""
            }

            if (adsObject.has("fallbackId")) {
                try {
                    insideAd.fallbackId = adsObject.getString("fallbackId")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                insideAd.fallbackId = ""
            }

            if (adsObject.has("url") && !adsObject.isNull("url")) {
                try {
                    insideAd.url = adsObject.getString("url")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                insideAd.url = ""
            }

            if (adsObject.has("properties") && !adsObject.isNull("properties")) {
                try {
                    insideAd.properties = adsObject.getJSONObject("properties")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                insideAd.properties = JSONObject()
            }

            ads.add(insideAd)
        }

        return ads
    }

}
