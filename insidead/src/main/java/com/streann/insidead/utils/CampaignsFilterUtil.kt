package com.streann.insidead.utils

import android.util.Log
import com.streann.insidead.InsideAdSdk
import com.streann.insidead.models.Campaign
import com.streann.insidead.models.InsideAd
import com.streann.insidead.models.Placement
import com.streann.insidead.utils.enums.TargetType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random

object CampaignsFilterUtil {
    private const val LOG_TAG = "CampaignsFilterUtil"

    // method to return an ad from the campaigns list
    fun getInsideAd(campaigns: ArrayList<Campaign>?, screen: String): InsideAd? {
        var insideAd: InsideAd? = null

        val activeCampaign = getActiveCampaign(campaigns, screen)
        Log.i(LOG_TAG, "activeCampaign $activeCampaign")

        activeCampaign?.let {
            val intervalInMinutes =
                activeCampaign.properties?.get("intervalInMinutes")
            val intervalInMillis = intervalInMinutes?.toFloat()?.let {
                Helper.getMillisFromMinutes(it)
            }
            InsideAdSdk.intervalInMinutes = intervalInMillis ?: 0

            val campaignPlacements = getPlacementsByCampaign(activeCampaign, screen)

            insideAd = getInsideAdByPlacements(campaignPlacements)
            Log.i(LOG_TAG, "insideAd $insideAd")

            setCurrentPlacement(insideAd, activeCampaign.placements)
        }

        return insideAd
    }

    // method to get the active campaigns from the campaigns list
    private fun getActiveCampaign(campaigns: ArrayList<Campaign>?, screen: String): Campaign? {
        return campaigns?.let { allCampaigns ->
            val activeCampaigns = filterCampaignsByTimePeriod(allCampaigns)
                .takeIf { it.isNotEmpty() }
                ?.let { getActiveCampaignsByPlacements(it, screen) }
                ?.takeIf { it.isNotEmpty() }
                ?.let { getCampaignsByContentTargeting(it) }

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

        Log.i(LOG_TAG, "filteredCampaigns $filteredCampaigns")
        return filteredCampaigns
    }

    // method to get active campaigns according to the filtered/active placements in the list of active campaigns
    // iterate through campaigns and create a list of placements for each campaign and if it's not null or empty
    // that means that campaign is active because its placements contain the screen that the user sent
    private fun getActiveCampaignsByPlacements(
        campaigns: ArrayList<Campaign>,
        screen: String
    ): ArrayList<Campaign> {
        val activeCampaigns = ArrayList<Campaign>()
        var activePlacements: List<Placement>?

        for (campaign in campaigns) {
            activePlacements = getFilteredPlacements(campaign.placements, screen)
            val isActiveCampaign = !activePlacements.isNullOrEmpty()
            if (isActiveCampaign) activeCampaigns.add(campaign)
        }

        Log.i(LOG_TAG, "activeCampaignsByPlacement $activeCampaigns")
        return activeCampaigns
    }

    // method to check if the user has sent targeting filters
    private fun getCampaignsByContentTargeting(campaigns: ArrayList<Campaign>): ArrayList<Campaign> {
        return if (InsideAdSdk.areTargetingFiltersEmpty()) {
            Log.i(LOG_TAG, "no targeting filters, return not modified campaigns")
            campaigns
        } else {
            filterCampaignsByContentTargeting(campaigns)
        }
    }

    // method to filter campaigns by content targeting
    private fun filterCampaignsByContentTargeting(campaigns: ArrayList<Campaign>): ArrayList<Campaign> {
        Log.i(LOG_TAG, "filterCampaignsByContentTargeting")
        val targetingFilters = InsideAdSdk.targetingFilters ?: return arrayListOf()

        val activeCampaigns = mutableListOf<Campaign>()
        val vodId = targetingFilters.vodId
        val channelId = targetingFilters.channelId
        val radioId = targetingFilters.radioId
        val seriesId = targetingFilters.seriesId
        val categoryId = targetingFilters.categoryId
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

                if (!categoryId.isNullOrEmpty() && targetsList.any {
                        it.type == TargetType.CATEGORY.value && it.ids?.contains(
                            categoryId
                        ) == true
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
            Log.i(LOG_TAG, "no matches, find campaigns without targeting")
            activeCampaigns.addAll(campaigns.filter { it.targeting.isNullOrEmpty() })
        }

        return ArrayList(activeCampaigns)
    }

    // method to get a filtered list of placements of the active campaign
    private fun getPlacementsByCampaign(
        activeCampaign: Campaign?,
        screen: String
    ): List<Placement>? {
        Log.i(LOG_TAG, "getPlacementsByCampaign")
        var filteredPlacements: List<Placement>? = null

        activeCampaign?.let { campaign ->
            campaign.placements?.let { placements ->
                if (placements.isNotEmpty()) {
                    filteredPlacements = getFilteredPlacements(placements, screen)
                }
            }
        }

        return filteredPlacements
    }

    // method to get a filtered list of placements of the active campaigns
    private fun getPlacementsByCampaigns(
        campaigns: ArrayList<Campaign>?,
        screen: String
    ): List<Placement>? {
        var placements: List<Placement>? = null

        if (campaigns?.isNotEmpty() == true) {
            placements = if (campaigns.size > 1) {
                getPlacementsByMultipleCampaigns(campaigns, screen)
            } else getFilteredPlacements(campaigns[0].placements, screen)
        }

        return placements
    }

    // if we have multiple campaigns combine a list of placements of all campaigns
    private fun getPlacementsByMultipleCampaigns(
        campaigns: ArrayList<Campaign>,
        screen: String
    ): List<Placement>? {
        val placementsList = ArrayList<Placement>()

        for (campaign in campaigns) {
            if (campaign.placements?.isNotEmpty() == true) {
                val placements = campaign.placements
                if (placements != null) {
                    for (placement in placements) {
                        placementsList.add(placement)
                    }
                }
            }
        }

        return getFilteredPlacements(placementsList, screen)
    }

    // filter the list of placements according to screen
    private fun getFilteredPlacements(
        placements: ArrayList<Placement>?,
        screen: String
    ): List<Placement>? {
        var filteredPlacements: List<Placement>? = null

        if (placements != null) {
            filteredPlacements = placements.filter { placement ->
                if (screen.isEmpty()) {
                    (placement.tags?.isEmpty() == true)
                } else {
                    placement.tags?.any { it == screen } == true
                }
            }
        }

        Log.i(LOG_TAG, "getFilteredPlacements $filteredPlacements")
        return filteredPlacements
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

    // method to set the active placement and placement's properties according to the returned active ad
    private fun setCurrentPlacement(
        insideAd: InsideAd?,
        placements: List<Placement>?
    ) {
        val placement = placements?.find { placement ->
            placement.ads?.contains(
                insideAd
            ) == true
        }

        Log.i(LOG_TAG, "activePlacement: $placement")

        val startAfterSeconds =
            placement?.properties?.get("startAfterSeconds")
        InsideAdSdk.startAfterSeconds = startAfterSeconds?.toLong()?.let {
            Helper.getMillisFromSeconds(it)
        }

        val showCloseButtonAfterSeconds =
            placement?.properties?.get("showCloseButtonAfterSeconds")
        InsideAdSdk.showCloseButtonAfterSeconds =
            showCloseButtonAfterSeconds?.toLong()?.let {
                Helper.getMillisFromSeconds(it)
            }

        InsideAdSdk.intervalForReels = placement?.properties?.get("intervalForReels")
    }

    // Define a generic function to select an object by it's weight randomly
    private fun <T> filterItemsByWeight(objects: ArrayList<T>, getWeight: (T) -> Int): T? {
        Log.i(LOG_TAG, "filterItemsByWeight")
        if (objects.isEmpty()) {
            return null
        }

        // Calculate total weight
        val totalWeight = objects.sumOf { getWeight(it) }

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