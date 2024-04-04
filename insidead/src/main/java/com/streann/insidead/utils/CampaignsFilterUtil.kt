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

    // method to return an ad from the campaigns list
    fun getInsideAd(campaigns: ArrayList<Campaign>?, screen: String): InsideAd? {
        val insideAd: InsideAd?

        val activeCampaign = getActiveCampaign(campaigns, screen)

        val intervalInMinutes =
            activeCampaign?.properties?.get("intervalInMinutes")
        val intervalInMillis = intervalInMinutes?.toFloat()?.let {
            Helper.getMillisFromMinutes(it)
        }
        InsideAdSdk.intervalInMinutes = intervalInMillis ?: 0

        insideAd = getInsideAdByPlacement(
            activeCampaign?.placements
        )
        Log.i(InsideAdSdk.LOG_TAG, "insideAd $insideAd")

        setCurrentPlacement(insideAd, activeCampaign?.placements)

        return insideAd
    }

    // method to get the active campaigns from the campaigns list
    private fun getActiveCampaign(
        campaigns: ArrayList<Campaign>?,
        screen: String
    ): Campaign? {
        var activeCampaigns: ArrayList<Campaign>?
        var activeCampaign: Campaign? = null

        activeCampaigns = filterCampaignsByTimePeriod(campaigns)

        if (activeCampaigns.isNotEmpty())
            activeCampaigns = getActiveCampaignByPlacements(activeCampaigns, screen)

        if (activeCampaigns.isNotEmpty()) {
            // if you have ad targeting filters object filter them by content targeting
            // if we have multiple active campaigns return only one by weight, if not return the 1
            // if we don't have content targeting just apply this weight filtering logic if we have multiple campaigns if not return the 1
            activeCampaign = if (activeCampaigns.size > 1) {
                filterItemsByWeight(activeCampaigns) { it.weight!! }
            } else activeCampaigns[0]
        }

        return activeCampaign
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

        Log.i(InsideAdSdk.LOG_TAG, "filteredCampaigns $filteredCampaigns")
        return filteredCampaigns
    }

    // method to get an active campaign according to the filtered/active placements in the list of active campaigns
    // iterate through campaigns and create a list of placements for each campaign and if it's not null or empty that means that campaign is active
    // because its placements contain the screen that the user sent
    private fun getActiveCampaignByPlacements(
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

        Log.i(InsideAdSdk.LOG_TAG, "activeCampaignsByPlacement $activeCampaigns")
        return activeCampaigns
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

        Log.d(InsideAdSdk.LOG_TAG, "filteredPlacements $filteredPlacements")
        return filteredPlacements
    }

    // method to get an inside ad of the list of filtered placements
    private fun getInsideAdByPlacement(
        placements: List<Placement>?,
    ): InsideAd? {
        Log.i(InsideAdSdk.LOG_TAG, "getInsideAdByPlacement")
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
        Log.i(InsideAdSdk.LOG_TAG, "getInsideAdByPlacement")
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
        Log.i(InsideAdSdk.LOG_TAG, "getInsideAdFilteredByWeight")
        var activeInsideAd: InsideAd? = null

        if (ads?.isNotEmpty() == true) {
            activeInsideAd = if (ads.size > 1) {
                filterItemsByWeight(ads) { it.weight!! }
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

        Log.i(InsideAdSdk.LOG_TAG, "activePlacement: $placement")

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

    // method to filter campaigns by content targeting
    private fun filterCampaignByAdTargetFilters(campaign: Campaign) {
        val adTargetFilters = InsideAdSdk.adTargetFilters ?: return
        val vodId = adTargetFilters.vodId
        val channelId = adTargetFilters.channelId
        val radioId = adTargetFilters.radioId
        val seriesId = adTargetFilters.seriesId
        val categoryId = adTargetFilters.categoryId
        val contentProviderId = adTargetFilters.contentProviderId

        val contentTargeting = campaign.targeting ?: return
        for (contentTarget in contentTargeting) {
            val targetsList = contentTarget.targets ?: continue

            if (vodId.isNullOrEmpty()) {
                targetsList.forEach { target ->
                    if (target.type == TargetType.VOD.value &&
                        target.ids?.contains(vodId) == true
                    ) {
                        Log.d("mano", "show this campaign/ad for the current ${target.type}")
                        return
                    }
                }
            }

            if (channelId.isNullOrEmpty()) {
                targetsList.forEach { target ->
                    if (target.type == TargetType.CHANNEL.value &&
                        target.ids?.contains(channelId) == true
                    ) {
                        Log.d("mano", "show this campaign/ad for the current ${target.type}")
                        return
                    }
                }
            }

            if (radioId.isNullOrEmpty()) {
                targetsList.forEach { target ->
                    if (target.type == TargetType.RADIO.value &&
                        target.ids?.contains(radioId) == true
                    ) {
                        Log.d("mano", "show this campaign/ad for the current ${target.type}")
                        return
                    }
                }
            }

            if (seriesId.isNullOrEmpty()) {
                targetsList.forEach { target ->
                    if (target.type == TargetType.SERIES.value &&
                        target.ids?.contains(seriesId) == true
                    ) {
                        Log.d("mano", "show this campaign/ad for the current ${target.type}")
                        return
                    }
                }
            }

            if (categoryId.isNullOrEmpty()) {
                targetsList.forEach { target ->
                    if (target.type == TargetType.CATEGORY.value &&
                        target.ids?.contains(categoryId) == true
                    ) {
                        Log.d("mano", "show this campaign/ad for the current ${target.type}")
                        return
                    }
                }
            }

            if (contentProviderId.isNullOrEmpty()) {
                targetsList.forEach { target ->
                    if (target.type == TargetType.CONTENT_PROVIDER.value &&
                        target.ids?.contains(contentProviderId) == true
                    ) {
                        Log.d("mano", "show this campaign/ad for the current ${target.type}")
                        return
                    }
                }
            }
        }
    }

    // Define a generic function to select an object by it's weight randomly
    private fun <T> filterItemsByWeight(objects: List<T>, getWeight: (T) -> Int): T? {
        Log.i(InsideAdSdk.LOG_TAG, "filterItemsByWeight")
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