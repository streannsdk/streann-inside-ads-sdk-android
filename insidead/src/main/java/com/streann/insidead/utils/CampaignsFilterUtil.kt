package com.streann.insidead.utils

import android.util.Log
import com.streann.insidead.InsideAdSdk
import com.streann.insidead.models.Campaign
import com.streann.insidead.models.InsideAd
import com.streann.insidead.models.Placement
import com.streann.insidead.models.TargetingFilters
import com.streann.insidead.models.isEmpty
import com.streann.insidead.utils.enums.TargetType
import com.streann.insidead.utils.enums.ViewType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random

object CampaignsFilterUtil {
    private const val LOG_TAG = "CampaignsFilterUtil"

    /**
     * The outcome of selecting an ad: the ad itself plus the timings that belong to *its*
     * placement and campaign.
     *
     * These used to be written straight into [InsideAdSdk] as a side effect of selection, which
     * meant two concurrent ad requests silently overwrote each other's timings. Returning them
     * keeps selection pure and lets each request own its own values.
     */
    internal data class AdSelection(
        val insideAd: InsideAd,
        val startAfterSecondsMillis: Long?,
        val showCloseButtonAfterSecondsMillis: Long?,
        val intervalInMinutesMillis: Long?,
        val intervalForReels: Int?
    )

    /**
     * Selects an ad without touching any global state.
     *
     * @param viewType the dedicated slot being filled, or null for a regular ad request.
     * @param targetingFilters passed in rather than read from [InsideAdSdk] so that concurrent
     *   requests can target different content.
     */
    internal fun selectAd(
        campaigns: ArrayList<Campaign>?,
        screen: String,
        viewType: ViewType?,
        targetingFilters: TargetingFilters?
    ): AdSelection? {
        val activeCampaign = getActiveCampaign(campaigns, screen, viewType, targetingFilters)
        Log.i(LOG_TAG, "activeCampaign $activeCampaign")
        if (activeCampaign == null) return null

        val campaignPlacements = getPlacementsByCampaign(activeCampaign, screen, viewType)
        val insideAd = getInsideAdByPlacements(campaignPlacements) ?: return null
        Log.i(LOG_TAG, "insideAd $insideAd")

        val intervalInMillis = activeCampaign.properties?.get("intervalInMinutes")
            ?.toFloat()?.let { Helper.getMillisFromMinutes(it) }

        val placement = findPlacementForAd(insideAd, activeCampaign.placements)
        Log.i(LOG_TAG, "activePlacement: $placement")

        return AdSelection(
            insideAd = insideAd,
            startAfterSecondsMillis = placement?.properties?.get("startAfterSeconds")
                ?.toLong()?.let { Helper.getMillisFromSeconds(it) },
            showCloseButtonAfterSecondsMillis = placement?.properties?.get("showCloseButtonAfterSeconds")
                ?.toLong()?.let { Helper.getMillisFromSeconds(it) },
            intervalInMinutesMillis = intervalInMillis ?: 0,
            intervalForReels = placement?.properties?.get("intervalForReels")
        )
    }

    /**
     * Legacy entry point: selects an ad and publishes its timings into the [InsideAdSdk]
     * singleton, exactly as before.
     *
     * Retained because it is public API on a public object and integrators may call it. New code
     * should use [selectAd], which does not mutate shared state.
     */
    @Deprecated(
        "Mutates global SDK state, so it cannot support concurrent ad requests. Prefer requesting " +
                "ads through InsideAdView, which keeps per-request state."
    )
    fun getInsideAd(campaigns: ArrayList<Campaign>?, screen: String, viewType: String? = null): InsideAd? {
        val selection = selectAd(
            campaigns,
            screen,
            ViewType.fromRaw(viewType),
            InsideAdSdk.targetingFilters
        )

        // Preserve the historical side effects for callers that still read these globals.
        InsideAdSdk.intervalInMinutes = selection?.intervalInMinutesMillis ?: 0
        InsideAdSdk.startAfterSeconds = selection?.startAfterSecondsMillis
        InsideAdSdk.showCloseButtonAfterSeconds = selection?.showCloseButtonAfterSecondsMillis
        InsideAdSdk.intervalForReels = selection?.intervalForReels

        return selection?.insideAd
    }

    // method to get the active campaigns from the campaigns list
    private fun getActiveCampaign(
        campaigns: ArrayList<Campaign>?,
        screen: String,
        viewType: ViewType? = null,
        targetingFilters: TargetingFilters? = null
    ): Campaign? {
        Log.i(LOG_TAG, "getActiveCampaign")

        return campaigns?.let { allCampaigns ->
            val activeCampaigns = filterCampaignsByTimePeriod(allCampaigns)
                .takeIf { it.isNotEmpty() }
                ?.let { getActiveCampaignsByPlacements(it, screen, viewType) }
                ?.takeIf { it.isNotEmpty() }
                ?.let { getCampaignsByContentTargeting(it, targetingFilters) }

            activeCampaigns?.let { filteredCampaigns ->
                if (filteredCampaigns.isNotEmpty()) {
                    Log.i(LOG_TAG, "filteredCampaigns $filteredCampaigns")
                    if (filteredCampaigns.size > 1) {
                        return filterItemsByWeight(filteredCampaigns) { it.weight ?: 0 }
                    } else {
                        return filteredCampaigns.first()
                    }
                } else {
                    return null
                }
            }

            return null
        }
    }

    // method to filter active campaigns by its start and end date
    private fun filterCampaignsByDate(startDate: Instant?, endDate: Instant?): Boolean {
        val currentDate = Instant.now()
        return currentDate.isAfter(startDate) && currentDate.isBefore(endDate)
    }

    // method to filter active campaigns by comparing the current time and day with the time period set in the campaign
    private fun filterCampaignsByTimePeriod(campaigns: ArrayList<Campaign>?): ArrayList<Campaign> {
        val filteredCampaigns = ArrayList<Campaign>()

        campaigns?.let {
            for (campaign in campaigns) {
                if (campaign.timePeriods != null) {
                    if (campaign.timePeriods!!.isNotEmpty()) {
                        for (timePeriod in campaign.timePeriods!!) {
                            val startTime = LocalTime.parse(timePeriod.startTime)
                            val endTime = LocalTime.parse(timePeriod.endTime)
                            val daysOfWeek = timePeriod.daysOfWeek

                            val currentTime = LocalTime.now()
                            val currentDate = LocalDate.now()
                            val currentDay: DayOfWeek = currentDate.dayOfWeek

                            if (currentTime.isAfter(startTime) && currentTime.isBefore(endTime)
                                && daysOfWeek?.contains(currentDay) == true
                            ) {
                                filteredCampaigns.add(campaign)
                            }
                        }
                    } else {
                        filteredCampaigns.add(campaign)
                    }
                }
            }
        }

        return filteredCampaigns
    }

    // method to get active campaigns according to the filtered/active placements in the list of active campaigns
    // iterate through campaigns and create a list of placements for each campaign and if it's not null or empty
    // that means that campaign is active because its placements contain the screen that the user sent
    private fun getActiveCampaignsByPlacements(
        campaigns: ArrayList<Campaign>,
        screen: String,
        viewType: ViewType? = null
    ): ArrayList<Campaign> {
        val activeCampaigns = ArrayList<Campaign>()
        var activePlacements: List<Placement>?

        for (campaign in campaigns) {
            activePlacements = getFilteredPlacements(campaign.placements, screen, viewType)
            val isActiveCampaign = !activePlacements.isNullOrEmpty()
            if (isActiveCampaign) activeCampaigns.add(campaign)
        }

        return activeCampaigns
    }

    // method to check if the user has sent targeting filters
    private fun getCampaignsByContentTargeting(
        campaigns: ArrayList<Campaign>,
        targetingFilters: TargetingFilters?
    ): ArrayList<Campaign> {
        return if (targetingFilters.isEmpty()) {
            campaigns
        } else {
            filterCampaignsByContentTargeting(campaigns, targetingFilters)
        }
    }

    // method to filter campaigns by content targeting
    private fun filterCampaignsByContentTargeting(
        campaigns: ArrayList<Campaign>,
        filters: TargetingFilters?
    ): ArrayList<Campaign> {
        Log.i(LOG_TAG, "filterCampaignsByContentTargeting")
        val targetingFilters = filters ?: return arrayListOf()

        val activeCampaigns = mutableListOf<Campaign>()
        val vodId = targetingFilters.vodId
        val channelId = targetingFilters.channelId
        val radioId = targetingFilters.radioId
        val seriesId = targetingFilters.seriesId
        val categoryIds = targetingFilters.categoryIds
        val contentProviderId = targetingFilters.contentProviderId

        for (campaign in campaigns) {
            campaign.targeting?.forEach { contentTarget ->
                val targetsList = contentTarget.targets ?: return arrayListOf()

                if (!vodId.isNullOrEmpty() && targetsList.any {
                        it.type == TargetType.VOD.value && it.ids?.contains(
                            vodId
                        ) == true
                    }) {
                    activeCampaigns.add(campaign)
                    return@forEach
                }

                if (!channelId.isNullOrEmpty() && targetsList.any {
                        it.type == TargetType.CHANNEL.value && it.ids?.contains(
                            channelId
                        ) == true
                    }) {
                    activeCampaigns.add(campaign)
                    return@forEach
                }

                if (!radioId.isNullOrEmpty() && targetsList.any {
                        it.type == TargetType.RADIO.value && it.ids?.contains(
                            radioId
                        ) == true
                    }) {
                    activeCampaigns.add(campaign)
                    return@forEach
                }

                if (!seriesId.isNullOrEmpty() && targetsList.any {
                        it.type == TargetType.SERIES.value && it.ids?.contains(
                            seriesId
                        ) == true
                    }) {
                    activeCampaigns.add(campaign)
                    return@forEach
                }

                if (!categoryIds.isNullOrEmpty() && targetsList.any { target ->
                        target.type == TargetType.CATEGORY.value &&
                                isCategoryIdContained(target.ids ?: arrayListOf(), categoryIds)
                    }) {
                    activeCampaigns.add(campaign)
                    return@forEach
                }

                if (!contentProviderId.isNullOrEmpty() && targetsList.any {
                        it.type == TargetType.CONTENT_PROVIDER.value && it.ids?.contains(
                            contentProviderId
                        ) == true
                    }) {
                    activeCampaigns.add(campaign)
                    return@forEach
                }
            }
        }

        // Filter campaigns without targeting if the content id is not contained in the campaigns targets
        if (activeCampaigns.isEmpty()) {
            activeCampaigns.addAll(campaigns.filter { it.targeting.isNullOrEmpty() })
        }

        return ArrayList(activeCampaigns)
    }

    private fun isCategoryIdContained(
        targetIds: ArrayList<String>,
        categoryIds: ArrayList<String>
    ): Boolean {
        for (categoryId in categoryIds) {
            if (categoryId in targetIds) {
                return true
            }
        }
        return false
    }

    // method to get a filtered list of placements of the active campaign
    private fun getPlacementsByCampaign(
        activeCampaign: Campaign?,
        screen: String,
        viewType: ViewType? = null
    ): List<Placement>? {
        Log.i(LOG_TAG, "getPlacementsByCampaign")
        var filteredPlacements: List<Placement>? = null

        activeCampaign?.let { campaign ->
            campaign.placements?.let { placements ->
                if (placements.isNotEmpty()) {
                    filteredPlacements = getFilteredPlacements(placements, screen, viewType)
                }
            }
        }

        return filteredPlacements
    }

    // filter the list of placements according to screen tags AND viewType
    internal fun getFilteredPlacements(
        placements: List<Placement>?,
        screen: String,
        viewType: ViewType? = null
    ): List<Placement>? {
        if (placements == null) return null

        return placements.filter { placement ->
            // Filter by screen/tags
            val screenMatch = if (screen.isEmpty()) {
                (placement.tags?.isEmpty() == true)
            } else {
                placement.tags?.any { it == screen } == true
            }

            // Filter by viewType. Comparison runs on the normalised enum rather than the raw
            // string, so backend casing ("multiview_canvas" vs "MULTIVIEW_CANVAS") cannot leak
            // dedicated inventory into regular traffic.
            val placementViewType = ViewType.fromRaw(placement.viewType)
            val viewTypeMatch = if (viewType == null) {
                // Regular ad request: serve only placements that are not reserved for a slot.
                // An absent, empty or unrecognised viewType is not a dedicated slot, so those
                // placements stay eligible here.
                placementViewType !in ViewType.DEDICATED_SLOTS
            } else {
                // Dedicated slot request: only that exact slot.
                placementViewType == viewType
            }

            // Both must match
            screenMatch && viewTypeMatch
        }
    }

    // method to get an inside ad of the list of filtered placements
    private fun getInsideAdByPlacements(
        placements: List<Placement>?,
    ): InsideAd? {
        Log.i(LOG_TAG, "getInsideAdByPlacement")
        var activeInsideAd: InsideAd? = null

        if (placements?.isNotEmpty() == true) {
            activeInsideAd = if (placements.size > 1) {
                getInsideAdByMultiplePlacements(placements)
            } else getInsideAdFilteredByWeight(placements[0].ads)
        }

        return activeInsideAd
    }

    // if we have multiple placements combine a list of ads of all placements
    private fun getInsideAdByMultiplePlacements(placements: List<Placement>): InsideAd? {
        val activeInsideAd: InsideAd?
        val adsList = ArrayList<InsideAd>()

        for (placement in placements) {
            if (placement.ads?.isNotEmpty() == true) {
                val ads = placement.ads
                if (ads != null) {
                    for (ad in ads) {
                        adsList.add(ad)
                    }
                }
            }
        }

        activeInsideAd = getInsideAdFilteredByWeight(adsList)
        return activeInsideAd
    }

    // method to get an inside ad
    // if we have multiple ads then filter them and return an ad by its weight
    // if we have only one ad just return it
    private fun getInsideAdFilteredByWeight(ads: ArrayList<InsideAd>?): InsideAd? {
        Log.i(LOG_TAG, "getInsideAdFilteredByWeight")
        var activeInsideAd: InsideAd? = null

        if (ads?.isNotEmpty() == true) {
            activeInsideAd = if (ads.size > 1) {
                filterItemsByWeight(ads) { it.weight ?: 0 }
            } else ads[0]
        }

        return activeInsideAd
    }

    // finds the placement that owns the selected ad, so its properties can be read
    private fun findPlacementForAd(
        insideAd: InsideAd?,
        placements: List<Placement>?
    ): Placement? = placements?.find { placement ->
        placement.ads?.any { it === insideAd } == true
    }

    // Define a generic function to select an object by it's weight randomly
    private fun <T> filterItemsByWeight(objects: ArrayList<T>, getWeight: (T) -> Int): T? {
        Log.i(LOG_TAG, "filterItemsByWeight")
        if (objects.isEmpty()) {
            return null
        }

        // Calculate total weight
        val totalWeight = objects.sumOf { getWeight(it) }

        // Random.nextInt throws IllegalArgumentException on a non-positive bound, which happens
        // whenever every candidate has weight 0 (or the weights are missing). Fall back to an
        // even pick rather than crashing the ad request.
        if (totalWeight <= 0) {
            return objects[Random.nextInt(objects.size)]
        }

        // Generate a random value between 0 and totalWeight
        val randomNumber = Random.nextInt(totalWeight)

        // Iterate over the objects and find the selected one
        var sum = 0
        for (obj in objects) {
            sum += getWeight(obj)
            if (sum > randomNumber) {
                return obj
            }
        }

        // This should never be reached, but if it does, return the last object
        return objects.last()
    }

}