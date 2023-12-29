package com.streann.insidead.models

import org.json.JSONObject

data class InsideAd(
    var id: String? = null,
    var name: String? = null,
    var weight: Int? = null,
    var adType: String? = null,
    var resellerId: String? = null,
    var fallbackId: String? = null,
    var url: String? = null,
    var properties: JSONObject? = null
)