package com.streann.insidead.callbacks

import com.streann.insidead.models.Campaign

interface CampaignCallback {
    fun onSuccess(campaigns: ArrayList<Campaign>?)

    fun onError(error: String?)
}