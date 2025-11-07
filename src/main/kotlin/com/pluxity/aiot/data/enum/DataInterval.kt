package com.pluxity.aiot.data.enum

import java.time.temporal.ChronoUnit

enum class DataInterval(
    val unit: ChronoUnit,
    val pgUnit: String,
    val format: String,
    val description: String,
) {
    HOUR(ChronoUnit.HOURS, "hour", "HH:mm", "시간별"),
    DAY(ChronoUnit.DAYS, "day", "yyyy-MM-dd", "일별"),
    WEEK(ChronoUnit.WEEKS, "week", "yyyy-ww", "주별"),
    MONTH(ChronoUnit.MONTHS, "month", "yyyy-MM", "월별"),
    YEAR(ChronoUnit.YEARS, "year", "yyyy", "년별"),
}
