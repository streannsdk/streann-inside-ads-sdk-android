package com.streann.insidead.models

import org.json.JSONObject

data class InsideAd(
    var adId: String? = null,
    var campaignId: String? = null,
    var adType: String? = null,
    var url: String? = null,
    var placementId: String? = null,
    var properties: JSONObject? = null
)