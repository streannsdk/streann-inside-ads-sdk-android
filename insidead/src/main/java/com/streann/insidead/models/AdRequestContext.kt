package com.streann.insidead.models

import com.streann.insidead.InsideAdSdk
import com.streann.insidead.InsideAdView
import com.streann.insidead.utils.enums.ViewType

/**
 * State belonging to a single ad request, owned by the [InsideAdView] that made it.
 *
 * Historically all of this lived as mutable fields on the [InsideAdSdk] singleton, which meant
 * only one ad could be in flight at a time: preroll worked around it by saving and restoring the
 * globals. That does not generalise, and the multiview slots (canvas and right bar) must be on
 * screen simultaneously, so each request carries its own copy instead.
 *
 * The context outlives a single impression - it is reused across interval repeats and is cleared
 * only when the request is cancelled.
 */
internal class AdRequestContext(
    /** The dedicated slot being filled, or null for a regular ad request. */
    val viewType: ViewType?,
    var screen: String = "",
    var isAdMuted: Boolean? = true,
    var targetingFilters: TargetingFilters? = null
) {

    // Timings resolved from the selected ad's placement and campaign
    var startAfterSecondsMillis: Long? = null
    var showCloseButtonAfterSecondsMillis: Long? = null
    var intervalInMinutesMillis: Long? = null
    var intervalForReels: Int? = null

    /**
     * Duration of the ad currently being shown. Set from the selected ad at show time - the
     * global equivalent is written during JSON parsing, so it holds whichever ad happened to be
     * parsed last rather than the one on screen.
     */
    var durationInSecondsMillis: Long? = null

    // Geometry and presentation, which are per-view rather than per-app
    var playerWidth: Int = 0
    var playerHeight: Int = 0
    var resizeMode: InsideAdView.ResizeMode? = null
    var showAdForReels: Boolean = false

    /** True for the multiview canvas and right-bar slots. */
    val isMultiview: Boolean
        get() = viewType == ViewType.MULTIVIEW_CANVAS || viewType == ViewType.MULTIVIEW_RIGHT_BAR

    /**
     * Mirrors this request's values onto the deprecated [InsideAdSdk] globals.
     *
     * Kept so that any code path not yet reading from a context - including integrator code that
     * reads public fields such as `intervalForReels` - continues to observe the most recent
     * request exactly as it did before.
     */
    fun publishToGlobals() {
        InsideAdSdk.isAdMuted = isAdMuted
        InsideAdSdk.targetingFilters = targetingFilters
        InsideAdSdk.startAfterSeconds = startAfterSecondsMillis
        InsideAdSdk.showCloseButtonAfterSeconds = showCloseButtonAfterSecondsMillis
        InsideAdSdk.intervalInMinutes = intervalInMinutesMillis
        InsideAdSdk.intervalForReels = intervalForReels
        InsideAdSdk.showAdForReels = showAdForReels
        durationInSecondsMillis?.let { InsideAdSdk.durationInSeconds = it }
        if (playerWidth > 0) InsideAdSdk.playerWidth = playerWidth
        if (playerHeight > 0) InsideAdSdk.playerHeight = playerHeight
        resizeMode?.let { InsideAdSdk.resizeMode = it }
    }

    /** Copies the timings from a selection into this context. */
    fun applySelection(selection: com.streann.insidead.utils.CampaignsFilterUtil.AdSelection) {
        startAfterSecondsMillis = selection.startAfterSecondsMillis
        showCloseButtonAfterSecondsMillis = selection.showCloseButtonAfterSecondsMillis
        intervalInMinutesMillis = selection.intervalInMinutesMillis
        intervalForReels = selection.intervalForReels
        durationInSecondsMillis = selection.insideAd.properties?.durationInSeconds
            ?.toLong()?.let { com.streann.insidead.utils.Helper.getMillisFromSeconds(it) }
    }
}
