package com.streann.insidead.models

data class InsideAd(
    val id: String? = null,
    var name: String? = null,
    private var type: String? = null,
    private var url: String? = null,
    private var isForAll: Boolean? = null,
    private var preloadImage: String? = null,
    private var showInsideAdsInFullScreen: Boolean? = null,
    var startAfterSeconds: Int = 0,
    var showCloseButtonAfterSeconds: Int = 0,
    var durationInSeconds: Int = 0,
    var clickActionURL: String? = null,
    var clientIp: String? = null
)