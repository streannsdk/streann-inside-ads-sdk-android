package com.streann.insidead.utils

import android.util.Log
import com.streann.insidead.InsideAdSdk
import com.streann.insidead.models.Campaign
import com.streann.insidead.models.InsideAd
import com.streann.insidead.models.Placement
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

object CampaignsFilterUtil {

    // method to return an ad from the campaigns list
    fun getInsideAd(campaigns: ArrayList<Campaign>?, screen: String): InsideAd? {
        val activeCampaigns = getActiveCampaigns(campaigns)
        Log.d(InsideAdSdk.LOG_TAG, "activeCampaigns $activeCampaigns")

        val placements = getPlacementsByCampaigns(
            activeCampaigns,
            screen
        )

        val insideAd = getInsideAdByPlacement(
            placements
        )
        Log.d(InsideAdSdk.LOG_TAG, "insideAd $insideAd")

        setCurrentPlacementAndCampaign(placements, campaigns, insideAd)

        return insideAd
    }

    // method to get the active campaigns from the campaigns list
    private fun getActiveCampaigns(campaigns: ArrayList<Campaign>?): ArrayList<Campaign>? {
        var activeCampaigns = ArrayList<Campaign>()

        if (campaigns != null) {
            for (campaign in campaigns) {
                val isActiveCampaign =
                    filterCampaignsByDate(campaign.startDate, campaign.endDate)
                if (isActiveCampaign) activeCampaigns.add(campaign)
            }
        }

        if (activeCampaigns.isNotEmpty())
            activeCampaigns = filterCampaignsByTimePeriod(activeCampaigns)

        return activeCampaigns
    }

    // method to filter active campaigns by its start and end date
    private fun filterCampaignsByDate(startDate: Instant?, endDate: Instant?): Boolean {
        val currentDate = Instant.now()
        return currentDate.isAfter(startDate) && currentDate.isBefore(endDate)
    }

    // method to filter active campaigns by comparing the current time and day with the time period set in the campaign
    private fun filterCampaignsByTimePeriod(campaigns: ArrayList<Campaign>): ArrayList<Campaign> {
        val filteredCampaigns = ArrayList<Campaign>()

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

        return filteredCampaigns
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

        return filteredPlacements
    }

    // method to set the active placement and placement's properties according to the returned active ad
    private fun setCurrentPlacementAndCampaign(
        placements: List<Placement>?,
        campaigns: ArrayList<Campaign>?,
        insideAd: InsideAd?
    ) {
        val placement = placements?.find { placement ->
            placement.ads!!.contains(
                insideAd
            )
        }

        Log.i(InsideAdSdk.LOG_TAG, "activePlacement: $placement")

        val startAfterSeconds =
            placement?.properties?.get("startAfterSeconds")
        InsideAdSdk.startAfterSeconds = startAfterSeconds?.toLong()?.let {
            Helper.getMillisFromSeconds(it)
        }

        val showCloseButtonAfterSeconds =
            placement?.properties?.get("showCloseButtonAfterSeconds")
        InsideAdSdk.showCloseButtonAfterSeconds = showCloseButtonAfterSeconds?.toLong()?.let {
            Helper.getMillisFromSeconds(it)
        }

        InsideAdSdk.intervalForReels = placement?.properties?.get("intervalForReels")

        setCurrentCampaign(campaigns, placement)
    }

    // method to set the active campaign and campaign's properties according to the active placement
    private fun setCurrentCampaign(
        campaigns: ArrayList<Campaign>?,
        placement: Placement?
    ) {
        val activeCampaign = campaigns?.find { campaign ->
            campaign.placements!!.contains(
                placement
            )
        }

        Log.i(InsideAdSdk.LOG_TAG, "activeCampaign: $activeCampaign")

        val intervalInMinutes =
            activeCampaign?.properties?.get("intervalInMinutes")
        val intervalInMillis = intervalInMinutes?.toFloat()?.let {
            Helper.getMillisFromMinutes(it)
        }
        InsideAdSdk.intervalInMinutes = intervalInMillis ?: 0
    }

    // method to get an inside ad of the list of filtered placements
    private fun getInsideAdByPlacement(
        placements: List<Placement>?,
    ): InsideAd? {
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
        var activeInsideAd: InsideAd? = null

        if (ads?.isNotEmpty() == true) {
            activeInsideAd = if (ads.size > 1) {
                filterItemsByWeight(ads) { it.weight!! }
            } else ads[0]
        }

        return activeInsideAd
    }

    // method to filter any object by weight
    // create a list of items and return the item with the biggest weight
    // if the list contains only one item then return a random item from the list
    private fun <T : Any> filterItemsByWeight(items: List<T>, getWeight: (T) -> Int): T? {
        if (items.isEmpty()) return null

        val maxItems = mutableListOf<T>()
        maxItems.add(items[0])
        var maxWeight = getWeight(items[0])

        for (item in items) {
            val weight = getWeight(item)
            if (weight > maxWeight) {
                maxItems.clear()
                maxItems.add(item)
                maxWeight = weight
            } else if (weight == maxWeight) {
                maxItems.add(item)
            }
        }

        return maxItems.randomOrNull()
    }

}