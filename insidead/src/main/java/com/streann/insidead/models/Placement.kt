package com.streann.insidead.models

import org.json.JSONObject

data class Placement(
    var id: String? = null,
    var name: String? = null,
    var viewType: String? = null,
    var screens: ArrayList<String>? = null,
    var startAfterSeconds: Int? = null,
    var showCloseButtonAfterSeconds: Int? = null,
    var properties: JSONObject? = null,
    var ads: ArrayList<InsideAd>? = null
)
