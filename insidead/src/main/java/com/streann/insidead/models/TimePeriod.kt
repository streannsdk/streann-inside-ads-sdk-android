package com.streann.insidead.models

import java.time.DayOfWeek

data class TimePeriod(
    var startTime: String? = null,
    var endTime: String? = null,
    var daysOfWeek: List<DayOfWeek>? = null
)