package com.streann.insidead.models

data class InsideAd(
    var id: String? = null,
    var name: String? = null,
    var weight: Int? = null,
    var adType: String? = null,
    var resellerId: String? = null,
    var fallbackId: String? = null,
    var url: String? = null,
    var durationInSeconds: Int? = null
)