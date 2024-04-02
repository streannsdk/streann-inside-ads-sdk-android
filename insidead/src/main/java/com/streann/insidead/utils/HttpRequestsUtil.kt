package com.streann.insidead.utils

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.streann.insidead.InsideAdSdk
import com.streann.insidead.callbacks.CampaignCallback
import com.streann.insidead.models.AdProperties
import com.streann.insidead.models.Campaign
import com.streann.insidead.models.GeoIp
import com.streann.insidead.models.InsideAd
import com.streann.insidead.models.Placement
import com.streann.insidead.models.Targeting
import com.streann.insidead.models.Targets
import com.streann.insidead.models.TimePeriod
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
            Log.e(InsideAdSdk.LOG_TAG, "ProtocolException: ", e)
        } catch (e: MalformedURLException) {
            Log.e(InsideAdSdk.LOG_TAG, "MalformedURLException: ", e)
        } catch (e: IOException) {
            Log.e(InsideAdSdk.LOG_TAG, "IOException: ", e)
        } catch (e: JSONException) {
            Log.e(InsideAdSdk.LOG_TAG, "JSONException: ", e)
        } catch (e: Exception) {
            Log.e(InsideAdSdk.LOG_TAG, "Exception: ", e)
        }

        if (campaignResponseArray == null) {
            campaignCallback.onError("No campaigns at the moment.")
            return
        }

        var campaigns: ArrayList<Campaign>? = null
        try {
            campaigns = parseCampaignJSONResponse(campaignResponseArray)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        if (campaigns.isNullOrEmpty()) {
            campaignCallback.onError("No campaigns at the moment.")
            return
        }

        campaignCallback.onSuccess(campaigns)
    }

    private fun parseCampaignJSONResponse(campaignArray: JSONArray): ArrayList<Campaign> {
        val campaigns: ArrayList<Campaign> = arrayListOf()

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
                    val properties = campaignObject.getJSONObject("properties")
                    val map = HashMap<String, Number>()

                    properties.keys().forEach { key ->
                        val value = properties.optString(key)

                        val numberValue = when {
                            value.isNullOrBlank() -> 0.0
                            else -> {
                                val intValue = value.toIntOrNull()
                                intValue ?: try {
                                    value.toDouble()
                                } catch (e: NumberFormatException) {
                                    0.0
                                }
                            }
                        }

                        map[key] = numberValue
                    }

                    campaign.properties = map
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                campaign.properties = emptyMap()
            }

            if (campaignObject.has("targeting") && !campaignObject.isNull("targeting")) {
                Log.d("mano", "has targeting")
                try {
                    campaign.targeting =
                        parseContentTargetingJSONResponse(campaignObject.getJSONArray("targeting"))
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                Log.d("mano", "doesn't have targeting")
                campaign.targeting = arrayListOf()
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
                    val daysOfWeekArray = timePeriodObject.getJSONArray("daysOfWeek")

                    val daysOfWeekList = mutableListOf<DayOfWeek>()
                    for (i in 0 until daysOfWeekArray.length()) {
                        val dayString = daysOfWeekArray.getString(i)
                        val dayOfWeek = DayOfWeek.valueOf(dayString)
                        daysOfWeekList.add(dayOfWeek)
                    }

                    timePeriod.daysOfWeek = daysOfWeekList
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                timePeriod.daysOfWeek = emptyList()
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
                    val tagsArray = placementObject.getJSONArray("tags")

                    val tagsList = ArrayList<String>()
                    for (i in 0 until tagsArray.length()) {
                        tagsList.add(tagsArray.getString(i))
                    }

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
                    val properties = placementObject.getJSONObject("properties")
                    val map = HashMap<String, Int>()
                    properties.keys().forEach { key ->
                        val value = properties.optString(key)
                        val intValue = if (value.isNullOrBlank()) 0 else try {
                            value.toInt()
                        } catch (e: NumberFormatException) {
                            0
                        }
                        map[key] = intValue
                    }

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
                    val propertiesJson = adsObject.getJSONObject("properties").toString()
                    insideAd.properties = Gson().fromJson(propertiesJson, AdProperties::class.java)

                    insideAd.properties?.durationInSeconds?.let {
                        InsideAdSdk.durationInSeconds = Helper.getMillisFromSeconds(it.toLong())
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                insideAd.properties = AdProperties()
            }

            if (adsObject.has("fallback") && !adsObject.isNull("fallback")) {
                try {
                    val fallbackJsonObject = adsObject.getJSONObject("fallback").toString()
                    insideAd.fallback = try {
                        Gson().fromJson(fallbackJsonObject, InsideAd::class.java)
                    } catch (e: JsonSyntaxException) {
                        null
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                insideAd.fallback = null
            }

            ads.add(insideAd)
        }

        return ads
    }

    private fun parseContentTargetingJSONResponse(targetingArray: JSONArray): ArrayList<Targeting> {
        Log.d("mano", "parseContentTargetingJSONResponse")
        val targetingList = ArrayList<Targeting>()

        for (i in 0 until targetingArray.length()) {
            val targetingObject: JSONObject = targetingArray.getJSONObject(i)
            val targeting = Targeting()

            if (targetingObject.has("id")) {
                try {
                    targeting.id = targetingObject.getString("id")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                targeting.id = ""
            }

            if (targetingObject.has("version")) {
                try {
                    targeting.version = targetingObject.getInt("version")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                targeting.version = null
            }

            if (targetingObject.has("createdOn")) {
                try {
                    targeting.createdOn = targetingObject.getString("createdOn")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                targeting.createdOn = ""
            }

            if (targetingObject.has("modifiedOn")) {
                try {
                    targeting.modifiedOn = targetingObject.getString("modifiedOn")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                targeting.modifiedOn = ""
            }

            if (targetingObject.has("name")) {
                try {
                    targeting.name = targetingObject.getString("name")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                targeting.name = ""
            }

            if (targetingObject.has("resellerId")) {
                try {
                    targeting.resellerId = targetingObject.getString("resellerId")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                targeting.resellerId = ""
            }

            if (targetingObject.has("targets") && !targetingObject.isNull("targets")) {
                try {
                    val targetsJson = targetingObject.getJSONObject("targets").toString()
                    targeting.targets =
                        arrayListOf(Gson().fromJson(targetsJson, Targets::class.java))
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                targeting.targets = arrayListOf()
            }

            Log.d("mano", "targeting $targeting")
            targetingList.add(targeting)
        }

        Log.d("mano", "targetingList $targetingList")
        return targetingList
    }

}
