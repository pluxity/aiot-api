package com.pluxity.aiot.data.enum

import java.time.temporal.ChronoUnit

enum class DataInterval(
    val unit: ChronoUnit,
    val format: String,
    val description: String,
) {
    HOUR(ChronoUnit.HOURS, "HH:mm", "시간별"),
    DAY(ChronoUnit.DAYS, "yyyy-MM-dd", "일별"),
    WEEK(ChronoUnit.WEEKS, "yyyy-ww", "주별"),
    MONTH(ChronoUnit.MONTHS, "yyyy-MM", "월별"),
    YEAR(ChronoUnit.YEARS, "yyyy", "년별"),
}
