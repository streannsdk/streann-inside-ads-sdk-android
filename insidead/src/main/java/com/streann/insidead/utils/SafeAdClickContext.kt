package com.streann.insidead.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.streann.insidead.InsideAdSdk

/**
 * Context handed to the Google IMA ads loader so that an unresolvable ad click cannot kill the
 * host app.
 *
 * IMA handles clickthroughs itself and calls startActivity without catching
 * ActivityNotFoundException. A creative whose clickthrough is an `intent://` deep link - a Play
 * Store link, typically - then crashes the app on any device that cannot resolve it. The SDK's own
 * "Learn More" button has always caught this; IMA's internal path is only reachable through the
 * Context it is given, which is what this wraps.
 *
 * Where the deep link carries a `browser_fallback_url`, that is opened instead, which is what the
 * advertiser intended for exactly this case.
 */
internal class SafeAdClickContext(base: Context) : ContextWrapper(base) {

    private companion object {
        const val TAG = "SafeAdClickContext"

        /** Convention used by `intent://` URIs to name a web URL to use when nothing handles them. */
        const val EXTRA_BROWSER_FALLBACK_URL = "browser_fallback_url"
    }

    override fun startActivity(intent: Intent) {
        startAdClick(intent, null)
    }

    override fun startActivity(intent: Intent, options: Bundle?) {
        startAdClick(intent, options)
    }

    private fun startAdClick(intent: Intent, options: Bundle?) {
        try {
            if (options != null) super.startActivity(intent, options) else super.startActivity(intent)
            return
        } catch (e: ActivityNotFoundException) {
            Log.e(
                InsideAdSdk.LOG_TAG,
                "$TAG: nothing can handle this ad click (${intent.data}) - the creative's " +
                        "clickthrough is probably a deep link to an app that is not installed",
                e
            )
        }

        openBrowserFallback(intent)
    }

    /** Last resort: the web URL the deep link nominates for when it cannot be resolved. */
    private fun openBrowserFallback(intent: Intent) {
        val fallback = intent.getStringExtra(EXTRA_BROWSER_FALLBACK_URL) ?: return

        try {
            super.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(fallback))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            Log.i(InsideAdSdk.LOG_TAG, "$TAG: opened the click's browser fallback instead")
        } catch (e: ActivityNotFoundException) {
            Log.e(InsideAdSdk.LOG_TAG, "$TAG: the browser fallback could not be opened either", e)
        }
    }
}
