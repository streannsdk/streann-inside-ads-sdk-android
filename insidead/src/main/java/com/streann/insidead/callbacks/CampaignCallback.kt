package com.streann.insidead.callbacks

import com.streann.insidead.models.Campaign

interface CampaignCallback {
    fun onSuccess(campaign: Campaign)

    fun onError(error: String?)
}