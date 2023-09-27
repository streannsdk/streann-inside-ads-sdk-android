package com.streann.insidead.callbacks

import com.streann.insidead.models.InsideAd

interface CampaignCallback {
    fun onSuccess(insideAd: InsideAd)

    fun onError(error: String?)
}