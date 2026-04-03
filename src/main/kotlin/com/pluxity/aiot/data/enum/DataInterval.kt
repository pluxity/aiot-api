package com.pluxity.aiot.data.enum

import java.time.temporal.ChronoUnit

enum class DataInterval(
    val unit: ChronoUnit,
    val pgUnit: String,
    val fluxUnit: String,
    val format: String,
    val description: String,
) {
    HOUR(ChronoUnit.HOURS, "hour", "h", "HH:mm", "시간별"),
    DAY(ChronoUnit.DAYS, "day", "d", "yyyy-MM-dd", "일별"),
    WEEK(ChronoUnit.WEEKS, "week", "w", "yyyy-ww", "주별"),
    MONTH(ChronoUnit.MONTHS, "month", "mo", "yyyy-MM", "월별"),
    YEAR(ChronoUnit.YEARS, "year", "y", "yyyy", "년별"),
}
