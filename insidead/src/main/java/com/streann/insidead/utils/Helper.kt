package com.streann.insidead.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.view.View
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
        callback: (Bitmap?) -> Unit
    ) {
        Thread {
            var bitmap: Bitmap? = null
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()
                val inputStream = connection.inputStream
                bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                connection.disconnect()
                bitmap?.let { bitmap = getResizedBitmap(it, resources) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            callback(bitmap)
        }.start()
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

    fun getMillisFromMinutes(minutes: Float): Long {
        return (minutes * 60000).toLong()
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

    /**
     * Sets the view size based on screen dimensions and resize mode.
     *
     * @param view The view to resize
     * @param resources Resources for accessing display metrics
     * @param resizeMode How to resize the view (null defaults to FIT for backward compatibility)
     */
    fun setViewSize(
        view: View?,
        resources: Resources,
        resizeMode: com.streann.insidead.InsideAdView.ResizeMode? = null
    ) {
        val mode = resizeMode ?: com.streann.insidead.InsideAdView.ResizeMode.FIT
        val isLandscape =
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        // Use orientation-aware aspect ratio: 16:9 for landscape, 9:16 for portrait
        val aspectRatio = if (isLandscape) 9.0 / 16.0 else 9.0 / 16.0

        val calculatedWidth: Int
        val calculatedHeight: Int

        when (mode) {
            com.streann.insidead.InsideAdView.ResizeMode.FIT -> {
                // Original behavior: split-screen in landscape, full width in portrait
                if (isLandscape) {
                    val videoWidth = screenWidth / 2
                    val videoHeight = (videoWidth * aspectRatio).toInt()
                    calculatedWidth = videoWidth
                    calculatedHeight = videoHeight
                } else {
                    val videoHeight = (screenWidth * aspectRatio).toInt()
                    calculatedWidth = screenWidth
                    calculatedHeight = videoHeight
                }
            }

            com.streann.insidead.InsideAdView.ResizeMode.FILL -> {
                // Fill screen while maintaining aspect ratio - choose dimension that fits
                val widthBasedHeight = (screenWidth * aspectRatio).toInt()
                val heightBasedWidth = (screenHeight / aspectRatio).toInt()

                if (widthBasedHeight <= screenHeight) {
                    // Width-based calculation fits - use full width
                    calculatedWidth = screenWidth
                    calculatedHeight = widthBasedHeight
                } else {
                    // Height-based calculation needed - use full height
                    calculatedWidth = heightBasedWidth
                    calculatedHeight = screenHeight
                }
            }

            com.streann.insidead.InsideAdView.ResizeMode.ZOOM -> {
                // Fill entire screen (may crop)
                calculatedWidth = screenWidth
                calculatedHeight = screenHeight
            }

            com.streann.insidead.InsideAdView.ResizeMode.FIXED_WIDTH -> {
                // Use full width, adjust height based on aspect ratio
                calculatedWidth = screenWidth
                calculatedHeight = (screenWidth * aspectRatio).toInt()
            }

            com.streann.insidead.InsideAdView.ResizeMode.FIXED_HEIGHT -> {
                // Use full height, adjust width based on aspect ratio
                calculatedHeight = screenHeight
                calculatedWidth = (screenHeight / aspectRatio).toInt()
            }
        }

        // Create new layout params with calculated dimensions and center gravity
        val parentView = view?.parent
        if (parentView is android.view.ViewGroup) {
            val newParams = when (view.layoutParams) {
                is android.widget.FrameLayout.LayoutParams -> {
                    android.widget.FrameLayout.LayoutParams(calculatedWidth, calculatedHeight).apply {
                        gravity = android.view.Gravity.CENTER
                    }
                }
                else -> {
                    // Fallback: modify existing params
                    view.layoutParams.apply {
                        width = calculatedWidth
                        height = calculatedHeight
                        (this as? android.widget.FrameLayout.LayoutParams)?.gravity = android.view.Gravity.CENTER
                    }
                }
            }
            view.layoutParams = newParams
        } else {
            // No parent, just modify existing params
            view?.layoutParams?.width = calculatedWidth
            view?.layoutParams?.height = calculatedHeight
            (view?.layoutParams as? android.widget.FrameLayout.LayoutParams)?.gravity =
                android.view.Gravity.CENTER
        }
    }

}