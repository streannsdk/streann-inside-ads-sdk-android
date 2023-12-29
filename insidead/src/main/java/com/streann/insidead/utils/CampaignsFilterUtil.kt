package com.streann.insidead.utils

import android.util.Log
import com.streann.insidead.models.Campaign
import com.streann.insidead.models.InsideAd
import com.streann.insidead.models.Placement
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

object CampaignsFilterUtil {

    private val LOGTAG = "InsideAdSdk"

    fun getCampaign(campaigns: ArrayList<Campaign>?): Campaign? {
        var activeCampaign: Campaign? = null
        val filteredCampaigns = ArrayList<Campaign>()

        if (campaigns != null) {
            for (campaign in campaigns) {
                val isActiveCampaign =
                    filterCampaignsByDate(campaign.startDate, campaign.endDate)
                if (isActiveCampaign) filteredCampaigns.add(campaign)
            }
        }

        Log.i(LOGTAG, "filteredCampaignsByDate $filteredCampaigns")

        if (filteredCampaigns.isNotEmpty())
            activeCampaign = filterCampaignsByTimePeriod(filteredCampaigns)

        Log.i(LOGTAG, "activeCampaign $activeCampaign")

        return activeCampaign
    }

    private fun filterCampaignsByDate(startDate: Instant?, endDate: Instant?): Boolean {
        val currentDate = Instant.now()
        return currentDate.isAfter(startDate) && currentDate.isBefore(endDate)
    }

    private fun filterCampaignsByTimePeriod(campaigns: ArrayList<Campaign>): Campaign? {
        var activeCampaign: Campaign? = null
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

        Log.i(LOGTAG, "filteredTimePeriodCampaigns $filteredCampaigns")

        if (filteredCampaigns.isNotEmpty()) {
            activeCampaign = if (filteredCampaigns.size > 1) {
                filterItemsByWeight(filteredCampaigns) { it.weight!! }
            } else filteredCampaigns[0]
        }

        return activeCampaign
    }

    fun getPlacements(
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

        Log.i(LOGTAG, "filteredPlacements $filteredPlacements")
        return filteredPlacements
    }

    fun getInsideAdByPlacement(
        placements: List<Placement>?,
    ): InsideAd? {
        var activeInsideAd: InsideAd? = null

        if (placements?.isNotEmpty() == true) {
            activeInsideAd = if (placements.size > 1) {
                getInsideAdByMultiplePlacements(placements)
            } else getInsideAdFilteredByWeight(placements[0].ads)
        }

        Log.i(LOGTAG, "activeInsideAd $activeInsideAd")
        return activeInsideAd
    }

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

        Log.i(LOGTAG, "adsList $adsList")

        activeInsideAd = getInsideAdFilteredByWeight(adsList)

        return activeInsideAd
    }

    private fun getInsideAdFilteredByWeight(ads: ArrayList<InsideAd>?): InsideAd? {
        var activeInsideAd: InsideAd? = null

        Log.i(LOGTAG, "ads $ads")

        if (ads?.isNotEmpty() == true) {
            activeInsideAd = if (ads.size > 1) {
                filterItemsByWeight(ads) { it.weight!! }
            } else ads[0]
        }

        return activeInsideAd
    }

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