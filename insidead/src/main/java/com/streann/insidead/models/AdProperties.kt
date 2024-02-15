package com.streann.insidead.models

data class AdProperties(
    var durationInSeconds: Int? = null,
    val sizes: List<SdkAdSize>? = null
)

data class SdkAdSize(
    val width: Int? = null,
    val height: Int? = null
)