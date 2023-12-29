package com.streann.insidead.models

data class Placement(
    var id: String? = null,
    var name: String? = null,
    var viewType: String? = null,
    var tags: ArrayList<String>? = null,
    var ads: ArrayList<InsideAd>? = null,
    var properties: Map<String, Int>? = null
)