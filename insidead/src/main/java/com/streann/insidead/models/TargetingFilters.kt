package com.streann.insidead.models

data class TargetingFilters(
    var vodId: String? = null,
    var channelId: String? = null,
    var radioId: String? = null,
    var seriesId: String? = null,
    var categoryIds: ArrayList<String>? = null,
    var contentProviderId: String? = null,
    val contentTitle: String? = null
)

/**
 * True when no content targeting is set, so every campaign is eligible.
 *
 * Defined on the nullable receiver so callers can pass their own per-request filters rather than
 * reading the shared [com.streann.insidead.InsideAdSdk] singleton.
 */
internal fun TargetingFilters?.isEmpty(): Boolean {
    val filters = this ?: return true
    return filters.vodId.isNullOrEmpty() &&
            filters.channelId.isNullOrEmpty() &&
            filters.radioId.isNullOrEmpty() &&
            filters.seriesId.isNullOrEmpty() &&
            filters.categoryIds.isNullOrEmpty() &&
            filters.contentProviderId.isNullOrEmpty() &&
            filters.contentTitle.isNullOrEmpty()
}
