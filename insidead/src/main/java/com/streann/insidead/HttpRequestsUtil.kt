package com.streann.insidead

import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object HttpRequestsUtil {

    fun getGeoIp(): JSONObject? {
        var jsonObject: JSONObject? = null

        try {
            val url = URL("https://geoip.streann.com/")
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
                    return jsonObject
                } finally {
                    urlConnection.disconnect()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return jsonObject
    }

}
