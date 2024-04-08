package com.streann.insidead.models

data class TargetingFilters(
    var vodId: String? = null,
    var channelId: String? = null,
    var radioId: String? = null,
    var seriesId: String? = null,
    var categoryIds: ArrayList<String>? = null,
    var contentProviderId: String? = null
)