package com.streann.insidead.utils

import android.content.pm.PackageManager
import android.os.Build
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

object Helper {

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

}