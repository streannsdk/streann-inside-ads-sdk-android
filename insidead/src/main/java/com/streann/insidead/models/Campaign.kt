package com.streann.insidead.models

import java.time.Instant

data class Campaign(
    var id: String? = null,
    var name: String? = null,
    var startDate: Instant? = null,
    var endDate: Instant? = null,
    var timePeriods: ArrayList<TimePeriod>? = null,
    var weight: Int? = null,
    var placements: ArrayList<Placement>? = null,
    var properties: Map<String, Number>? = null
)