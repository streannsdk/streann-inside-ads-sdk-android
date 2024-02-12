package com.streann.insidead.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.view.WindowInsets
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdSize
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.concurrent.TimeUnit

object Helper {

    private var bannerAdSize: AdSize? = null

    @Suppress("DEPRECATION")
    fun getPackageVersionCode(
        packageManager: PackageManager,
        packageName: String,
        flags: Int = 0
    ): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val pInfo =
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(flags.toLong())
                )
            pInfo.longVersionCode
        } else {
            val pInfo = packageManager.getPackageInfo(packageName, flags)
            pInfo.versionCode.toLong()
        }

    fun generateDeviceId(): String? {
        return UUID.randomUUID().toString()
    }

    fun convertToMd5(s: String): String? {
        try {
            // Create MD5 Hash
            val digest = MessageDigest.getInstance("MD5")
            digest.update(s.toByteArray())
            val messageDigest = digest.digest()

            // Create Hex String
            val hexString = StringBuffer()
            for (i in messageDigest.indices) {
                hexString.append(Integer.toHexString(0xFF and messageDigest[i].toInt()))
            }
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return ""
    }

    fun toHex(input: String): String? {
        return String.format("%040x", BigInteger(1, input.toByteArray()))
    }

    fun getBitmapFromURL(
        imageUrl: String,
        resources: Resources,
    ): Bitmap? {
        var bitmap: Bitmap?
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()
            val inputStream = connection.inputStream
            bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            connection.disconnect()
            bitmap?.let { bitmap = getResizedBitmap(it, resources) }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun getResizedBitmap(
        bitmap: Bitmap,
        resources: Resources
    ): Bitmap? {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        val desiredWidth: Int
        val desiredHeight: Int

        if (bitmapWidth > bitmapHeight) {
            desiredWidth = screenWidth
            desiredHeight =
                (bitmapHeight.toFloat() / bitmapWidth.toFloat() * screenWidth).toInt()
        } else {
            desiredHeight = screenHeight
            desiredWidth =
                (bitmapWidth.toFloat() / bitmapHeight.toFloat() * screenHeight).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, desiredWidth, desiredHeight, true)
    }

    fun getMillisFromSeconds(seconds: Long): Long {
        return TimeUnit.SECONDS.toMillis(seconds)
    }

    fun getMillisFromMinutes(seconds: Long): Long {
        return TimeUnit.MINUTES.toMillis(seconds)
    }

    fun getScreenHeight(activity: Activity, resources: Resources): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = activity.windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsets(WindowInsets.Type.systemBars())
            metrics.bounds.height() - insets.bottom - insets.top
        } else {
            val view = activity.window.decorView
            val insets = WindowInsetsCompat.toWindowInsetsCompat(view.rootWindowInsets, view)
                .getInsets(WindowInsetsCompat.Type.systemBars())
            resources.displayMetrics.heightPixels - insets.bottom - insets.top
        }
    }

    fun setBannerAdHeight(adSize: AdSize?) {
        this.bannerAdSize = adSize
    }

    fun getBannerAdHeight(context: Context): Int? {
        return bannerAdSize?.getHeightInPixels(context)
    }

}