package com.streann.insidead.models

data class AdProperties(
    var durationInSeconds: Int? = null,
    var clickThroughUrl: String? = null,
    val sizes: List<SdkAdSize>? = null
)

data class SdkAdSize(
    val width: Int? = null,
    val height: Int? = null
)